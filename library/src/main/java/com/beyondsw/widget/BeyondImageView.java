package com.beyondsw.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.OverScroller;

import com.beyondsw.fastimages.R;
import com.beyondsw.widget.gesture.BeyondGestureDetector;

import java.lang.reflect.Field;

/**
 * Created by wensefu on 17-3-28.
 */
public class BeyondImageView extends ImageView {

    private static final String TAG = "BeyondImageView";
    private static final boolean LOG_ENABLE = true;
    private static final float MAX_SCALE = 3f;
    private static final float DOUBLE_TAB_SCALE = 2.5f;
    private float mMaxScale;
    private float mUserMaxScale;
    private float mDoubleTabScale;
    private float mUserDoubleTabScale;
    private boolean mDoubleTabAdjustBounds;
    private Matrix mMatrix;
    private Matrix mTempMatrix;
    private ValueAnimator mScaleAnimator;
    private ValueAnimator mFixTranslationAnimator;
    private float[] mValues;
    private float mScale = 1f;
    private float mScaleBeginPx;
    private float mScaleBeginPy;
    private RectF mInitRect = new RectF();
    private RectF mTempRect = new RectF();
    private boolean mCropToPadding;
    private OverScroller mScroller;
    private BeyondGestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private static final float FLING_OVER_SCROLL_FACTOR = .125f;
    private static final float OVER_SCROLL_FACTOR = .25f;
    private int mFlingOverScrollX;
    private int mFlingOverScrollY;
    private int mOverScrollX;
    private int mOverScrollY;
    private boolean mFlingOverScrollEnabled;
    private boolean mOverScrollEnabled;
    private Rect mViewRect;


    public BeyondImageView(Context context) {
        this(context, null);
    }

    public BeyondImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BeyondImageView);
        mDoubleTabScale = mUserDoubleTabScale = a.getFloat(R.styleable.BeyondImageView_doubleTabScale, DOUBLE_TAB_SCALE);
        mDoubleTabAdjustBounds = a.getBoolean(R.styleable.BeyondImageView_doubleTabAdjustBounds, false);
        mUserMaxScale = mMaxScale = a.getFloat(R.styleable.BeyondImageView_maxScale, MAX_SCALE);
        if (mMaxScale < mDoubleTabScale) {
            throw new IllegalArgumentException("maxScale should not be smaller than doubleTabScale");
        }
        mFlingOverScrollEnabled = a.getBoolean(R.styleable.BeyondImageView_flingOverScrollEnabled, true);
        mOverScrollEnabled = a.getBoolean(R.styleable.BeyondImageView_scrollOverScrollEnabled, true);
        a.recycle();

        init();
    }

    private static class ViscousFluidInterpolator implements Interpolator {

        /**
         * Controls the viscous fluid effect (how much of it).
         */
        private static final float VISCOUS_FLUID_SCALE = 3f;

        private static final float VISCOUS_FLUID_NORMALIZE;
        private static final float VISCOUS_FLUID_OFFSET;

        static {
            // must be set to 1.0 (used in viscousFluid())
            VISCOUS_FLUID_NORMALIZE = 1.0f / viscousFluid(1.0f);
            // account for very small floating-point error
            VISCOUS_FLUID_OFFSET = 1.0f - VISCOUS_FLUID_NORMALIZE * viscousFluid(1.0f);
        }

        private static float viscousFluid(float x) {
            x *= VISCOUS_FLUID_SCALE;
            if (x < 1.0f) {
                x -= (1.0f - (float) Math.exp(-x));
            } else {
                float start = 0.36787944117f;   // 1/e == exp(-1)
                x = 1.0f - (float) Math.exp(1.0f - x);
                x = start + x * (1.0f - start);
            }
            return x;
        }

        @Override
        public float getInterpolation(float input) {
            final float interpolated = VISCOUS_FLUID_NORMALIZE * viscousFluid(input);
            if (interpolated > 0) {
                return interpolated + VISCOUS_FLUID_OFFSET;
            }
            return interpolated;
        }
    }

    private void initCropToPadding() {
        try {
            Field field = ImageView.class.getDeclaredField("mCropToPadding");
            field.setAccessible(true);
            mCropToPadding = field.getBoolean(this);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        if (Build.VERSION.SDK_INT < 16) {
            initCropToPadding();
        }
        mMatrix = new Matrix();
        mTempMatrix = new Matrix();
        mValues = new float[9];
        mScroller = new OverScroller(getContext());
    }

    @TargetApi(16)
    private boolean getCropToPaddingCompat() {
        return Build.VERSION.SDK_INT < 16 ? mCropToPadding : getCropToPadding();
    }

    @TargetApi(16)
    @Override
    protected void onDraw(Canvas canvas) {

        Drawable drawable = getDrawable();
        if (drawable == null) {
            return; // couldn't resolve the URI
        }

        if (drawable.getIntrinsicWidth() == 0 || drawable.getIntrinsicHeight() == 0) {
            return;     // nothing to draw (empty bounds)
        }
        Matrix matrix = getImageMatrix();
        if (matrix.isIdentity()) {
            matrix = null;
        }
        if (matrix == null && getPaddingTop() == 0 && getPaddingLeft() == 0) {
            canvas.concat(mMatrix);
            drawable.draw(canvas);
        } else {
            int saveCount = canvas.getSaveCount();
            canvas.save();

            if (getCropToPaddingCompat()) {
                final int scrollX = getScrollX();
                final int scrollY = getScrollY();
                canvas.clipRect(scrollX + getPaddingLeft(), scrollY + getPaddingTop(),
                        scrollX + getRight() - getLeft() - getPaddingRight(),
                        scrollY + getBottom() - getTop() - getPaddingBottom());
            }

            canvas.translate(getPaddingLeft(), getPaddingTop());
            canvas.concat(mMatrix);
            if (matrix != null) {
                canvas.concat(matrix);
            }
            drawable.draw(canvas);
            canvas.restoreToCount(saveCount);
        }
    }

    private void updateViewRect() {
        int left;
        int top;
        int right;
        int bottom;
        if (getCropToPaddingCompat()) {
            left = getPaddingLeft();
            top = getPaddingTop();
            right = getWidth() - left - getPaddingRight();
            bottom = getHeight() - top - getPaddingBottom();
        } else {
            left = top = 0;
            right = getWidth();
            bottom = getHeight();
        }
        if (mViewRect == null) {
            mViewRect = new Rect();
        }
        mViewRect.set(left, top, right, bottom);
    }

    private void initMatrix() {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            mInitRect.set(drawable.getBounds());
            Matrix matrix = getImageMatrix();
            if (matrix != null) {
                matrix.mapRect(mInitRect);
            }
            mMatrix.reset();
            mScale = 1;
        }
    }

    private void updateOverScroll() {
        mFlingOverScrollX = Math.round(mViewRect.width() * FLING_OVER_SCROLL_FACTOR);
        mFlingOverScrollY = Math.round(mViewRect.height() * FLING_OVER_SCROLL_FACTOR);
        mOverScrollX = Math.round(mViewRect.width() * OVER_SCROLL_FACTOR);
        mOverScrollY = Math.round(mViewRect.height() * OVER_SCROLL_FACTOR);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateViewRect();
        initMatrix();
        updateOverScroll();
        updateDoubleTabScale();
    }

    private void updateDoubleTabScale() {
        if (mDoubleTabAdjustBounds) {
            float scaleX = getWidth() / mInitRect.width();
            float scaleY = getHeight() / mInitRect.height();
            mDoubleTabScale = Math.max(scaleX, scaleY);
            if (mMaxScale < mDoubleTabScale) {
                mMaxScale = mDoubleTabScale;
            }
        }
    }

    private void initGestureDetector() {
        GestureCallback callback = new GestureCallback();
        mGestureDetector = new BeyondGestureDetector(getContext(), callback);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        int touchSlop = configuration.getScaledPagingTouchSlop();
        mGestureDetector.setScrollSlop(touchSlop / 2);
        mScaleGestureDetector = new ScaleGestureDetector(getContext(), callback);
    }

    /**
     * 设置双击时放大的倍数（相对初始大小）
     *
     * @param scale
     */
    public void setDoubleTabScale(float scale) {
        mDoubleTabScale = scale;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction() & MotionEvent.ACTION_MASK;
        if (action == MotionEvent.ACTION_DOWN) {
            if (mGestureDetector == null) {
                initGestureDetector();
            }
        }
        mGestureDetector.onTouchEvent(event);
        mScaleGestureDetector.onTouchEvent(event);
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            animToEdgeIfNeeded();
        }
        return true;
    }

    private void animToEdgeIfNeeded() {
        if ((mScaleAnimator != null && mScaleAnimator.isRunning())
                || (mFixTranslationAnimator != null && mFixTranslationAnimator.isRunning())) {
            return;
        }
        if (!mScroller.isFinished()) {
            return;
        }
        mMatrix.mapRect(mTempRect, mInitRect);

        float dx = 0;
        float dy = 0;
        final Rect vRect = mViewRect;
        if (mTempRect.left > vRect.left && (mTempRect.right - vRect.right >= mTempRect.left - vRect.left)) {
            dx = vRect.left - mTempRect.left;
        }
        if (mTempRect.right < vRect.right && (vRect.left - mTempRect.left >= vRect.right - mTempRect.right)) {
            dx = vRect.right - mTempRect.right;
        }
        if (mTempRect.top > vRect.top && (mTempRect.bottom - vRect.bottom >= mTempRect.top - vRect.top)) {
            dy = vRect.top - mTempRect.top;
        }

        if (mTempRect.bottom < vRect.bottom && (vRect.top - mTempRect.top >= vRect.bottom - mTempRect.bottom)) {
            dy = vRect.bottom - mTempRect.bottom;
        }

        if (dx != 0 || dy != 0) {
            final PropertyValuesHolder xph = PropertyValuesHolder.ofFloat("x", 0, dx);
            final PropertyValuesHolder yph = PropertyValuesHolder.ofFloat("y", 0, dy);
            final ValueAnimator animator = ValueAnimator.ofPropertyValuesHolder(xph, yph).setDuration(300);
            animator.setInterpolator(new ViscousFluidInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                float prevdx = 0;
                float prevdy = 0;

                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float dx = (float) animation.getAnimatedValue("x");
                    float dy = (float) animation.getAnimatedValue("y");
                    mMatrix.postTranslate(dx - prevdx, dy - prevdy);
                    invalidate();
                    prevdx = dx;
                    prevdy = dy;
                }
            });
            animator.start();
        }
    }

    private void animTranslationToInit() {
        mMatrix.mapRect(mTempRect, mInitRect);
        final ScaleType scaleType = getScaleType();
        final Rect viewRect = mViewRect;
        float dx = 0;
        float dy;
        if (scaleType == ScaleType.FIT_START) {
            dy = viewRect.top - mTempRect.top;
            Log.d(TAG, "animTranslationToInit: 1");
        } else if (scaleType == ScaleType.FIT_END) {
            dy = viewRect.bottom - mTempRect.bottom;
            Log.d(TAG, "animTranslationToInit: 2");
        } else {
            dy = viewRect.centerY() - mTempRect.centerY();
            Log.d(TAG, "animTranslationToInit: 3");
        }

        if (mTempRect.left > viewRect.left && (mTempRect.right - viewRect.right >= mTempRect.left - viewRect.left)) {
            dx = viewRect.left - mTempRect.left;
        }
        if (mTempRect.right < viewRect.right && (viewRect.left - mTempRect.left >= viewRect.right - mTempRect.right)) {
            dx = viewRect.right - mTempRect.right;
        }

        if (dy != 0) {
            PropertyValuesHolder vx = PropertyValuesHolder.ofFloat("tx", 0, dx);
            PropertyValuesHolder vy = PropertyValuesHolder.ofFloat("ty", 0, dy);
            mFixTranslationAnimator = ValueAnimator.ofPropertyValuesHolder(vx, vy).setDuration(220);
            mFixTranslationAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                float ty = 0;
                float tx = 0;

                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float valueY = (float) animation.getAnimatedValue("ty");
                    float valueX = (float) animation.getAnimatedValue("tx");
                    mMatrix.postTranslate(valueX - tx, valueY - ty);
                    invalidate();
                    ty = valueY;
                    tx = valueX;
                }
            });
            mFixTranslationAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mMatrix.mapRect(mTempRect, mInitRect);
                    Log.d(TAG, "onAnimationEnd: mTempRect.cx=" + mTempRect.centerX() + ",vrect.cx=" + mViewRect.centerX());
                    Log.d(TAG, "onAnimationEnd: mTempRect.cy=" + mTempRect.centerY() + ",vrect.cy=" + mViewRect.centerY());
                }
            });
            mFixTranslationAnimator.start();
        }
    }

    private boolean isFillWithImage(RectF rect) {
        final Rect viewRect = mViewRect;
        return rect.contains(viewRect.left, viewRect.top, viewRect.right, viewRect.bottom);
    }

    private static float getPivot(float scale, float startT, float endT) {
        return (endT - scale * startT) / (1 - scale);
    }

    @TargetApi(16)
    private class FlingRunnable implements Runnable {

        int oldX;
        int oldY;
        int startX;
        int startY;
        int vx;
        int vy;
        int minX;
        int maxX;
        int minY;
        int maxY;
        int overX;
        int overY;

        void start() {
            mScroller.fling(startX, startY, vx, vy, minX, maxX, minY, maxY, overX, overY);
            if (Build.VERSION.SDK_INT >= 16) {
                postOnAnimation(this);
            } else {
                post(this);
            }
        }

        FlingRunnable startX(int startX) {
            this.startX = startX;
            return this;
        }

        FlingRunnable startY(int startY) {
            this.startY = startY;
            return this;
        }

        FlingRunnable vx(int vx) {
            this.vx = vx;
            return this;
        }

        FlingRunnable vy(int vy) {
            this.vy = vy;
            return this;
        }

        FlingRunnable minX(int minX) {
            this.minX = minX;
            return this;
        }

        FlingRunnable maxX(int maxX) {
            this.maxX = maxX;
            return this;
        }

        FlingRunnable minY(int minY) {
            this.minY = minY;
            return this;
        }

        FlingRunnable maxY(int maxY) {
            this.maxY = maxY;
            return this;
        }

        FlingRunnable overX(int overX) {
            this.overX = overX;
            return this;
        }

        FlingRunnable overY(int overY) {
            this.overY = overY;
            return this;
        }

        @Override
        public void run() {
            if (mScroller.isFinished()) {
                return;
            }
            if (mScroller.computeScrollOffset()) {
                int newX = mScroller.getCurrX();
                int newY = mScroller.getCurrY();
                int diffX = newX - oldX;
                int diffY = newY - oldY;
                if (diffX != 0 || diffY != 0) {
                    log(TAG, "FlingRunnable,diffX=" + diffX + ",diffY=" + diffY);
                    mMatrix.postTranslate(diffX, diffY);
                    invalidate();
                    oldX = newX;
                    oldY = newY;
                }
                if (Build.VERSION.SDK_INT >= 16) {
                    postOnAnimation(this);
                } else {
                    post(this);
                }
            }
        }
    }

    private boolean isScaling() {
        return mScaleAnimator != null && mScaleAnimator.isRunning();
    }

    private boolean isTransing() {
        return mFixTranslationAnimator != null && mFixTranslationAnimator.isRunning();
    }

    private void doScaleAnim(final float px, final float py, float toScale) {
        mScaleAnimator = ValueAnimator.ofFloat(mScale, toScale).setDuration(400);
        mScaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                float radioScale = value / mScale;
                mMatrix.postScale(radioScale, radioScale, px, py);
                invalidate();
                mScale = value;
            }
        });
        mScaleAnimator.setInterpolator(new ViscousFluidInterpolator());
        mScaleAnimator.start();
    }

    private void animToInitPosition() {
        mMatrix.getValues(mValues);
        float tx = mValues[Matrix.MTRANS_X];
        float ty = mValues[Matrix.MTRANS_Y];
        float px = tx / (1 - mScale);
        float py = ty / (1 - mScale);
        doScaleAnim(px, py, 1);
    }

    private float[] getZoomOutPivot(float x, float y) {
        mTempMatrix.set(mMatrix);
        mTempMatrix.postScale(mDoubleTabScale, mDoubleTabScale, x, y);
        mTempMatrix.mapRect(mTempRect, mInitRect);
        final Rect vRect = mViewRect;
        if (mTempRect.left >= vRect.left && mTempRect.right <= vRect.right) {
            mTempMatrix.postTranslate(vRect.centerX() - mTempRect.centerX(), 0);
        } else if (mTempRect.left > vRect.left) {
            if (mTempRect.right - vRect.right >= mTempRect.left - vRect.left) {
                mTempMatrix.postTranslate(vRect.left - mTempRect.left, 0);
            } else {
                mTempMatrix.postTranslate(vRect.centerX() - mTempRect.centerX(), 0);
            }
        } else if (mTempRect.right < vRect.right) {
            if (vRect.left - mTempRect.left >= vRect.right - mTempRect.right) {
                mTempMatrix.postTranslate(vRect.right - mTempRect.right, 0);
            } else {
                mTempMatrix.postTranslate(vRect.centerX() - mTempRect.centerX(), 0);
            }
        }

        final ScaleType scaleType = getScaleType();
        float fixedInitDy;
        if (scaleType == ScaleType.FIT_START) {
            fixedInitDy = vRect.top - mTempRect.top;
        } else if (scaleType == ScaleType.FIT_END) {
            fixedInitDy = vRect.bottom - mTempRect.bottom;
        } else {
            fixedInitDy = vRect.centerY() - mTempRect.centerY();
        }
        if (mTempRect.top >= vRect.top && mTempRect.bottom <= vRect.bottom) {
            mTempMatrix.postTranslate(0, fixedInitDy);
        } else if (mTempRect.top > vRect.top) {
            if (mTempRect.bottom - vRect.bottom >= mTempRect.top - vRect.top) {
                mTempMatrix.postTranslate(0, vRect.top - mTempRect.top);
            } else {
                mTempMatrix.postTranslate(0, fixedInitDy);
            }
        } else if (mTempRect.bottom < vRect.bottom) {
            if (vRect.top - mTempRect.top >= vRect.bottom - mTempRect.bottom) {
                mTempMatrix.postTranslate(0, vRect.bottom - mTempRect.bottom);
            } else {
                mTempMatrix.postTranslate(0, fixedInitDy);
            }
        }

        mTempMatrix.getValues(mValues);
        float endTx = mValues[Matrix.MTRANS_X];
        float endTy = mValues[Matrix.MTRANS_Y];
        mMatrix.getValues(mValues);
        float startTx = mValues[Matrix.MTRANS_X];
        float startTy = mValues[Matrix.MTRANS_Y];
        float px = getPivot(mDoubleTabScale, startTx, endTx);
        float py = getPivot(mDoubleTabScale, startTy, endTy);
        return new float[]{px, py};
    }

    private class GestureCallback extends BeyondGestureDetector.SimpleOnGestureListener implements ScaleGestureDetector.OnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float factor = detector.getScaleFactor();
            mMatrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
            invalidate();
            mScale *= factor;
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mScaleBeginPx = detector.getFocusX();
            mScaleBeginPy = detector.getFocusY();
            if (mScaleAnimator != null && mScaleAnimator.isRunning()) {
                mScaleAnimator.cancel();
            }
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (mScale < 1) {
                animToInitPosition();
                Log.d(TAG, "onScaleEnd: 1");
            } else if (mScale > mMaxScale) {
                float px, py;
                mTempMatrix.set(mMatrix);
                final float targetScale = mMaxScale / mScale;
                mTempMatrix.postScale(targetScale, targetScale, mScaleBeginPx, mScaleBeginPy);
                mTempMatrix.mapRect(mTempRect, mInitRect);
                final Rect viewRect = mViewRect;
                final boolean fillHorizontal = mTempRect.left <= viewRect.left && mTempRect.right >= viewRect.right;
                final boolean fillVertical = mTempRect.top <= viewRect.top && mTempRect.bottom >= viewRect.bottom;
                if (fillHorizontal && fillVertical) {
                    doScaleAnim(mScaleBeginPx, mScaleBeginPy, mMaxScale);
                    Log.d(TAG, "onScaleEnd: 21");
                } else {
                    Log.d(TAG, "onScaleEnd: 22");
                    float dx = 0;
                    float dy = 0;
                    if (!fillHorizontal) {
                        dx = mInitRect.centerX() - mTempRect.centerX();
                    }
                    if (!fillVertical) {
                        ScaleType type = getScaleType();
                        if (type == ScaleType.FIT_START) {
                            dy = mInitRect.top - mTempRect.top;
                        } else if (type == ScaleType.FIT_END) {
                            dy = mInitRect.bottom - mTempRect.bottom;
                        } else {
                            dy = mInitRect.centerY() - mTempRect.centerY();
                        }
                    }
                    px = mScaleBeginPx;
                    mTempMatrix.getValues(mValues);
                    float endTx = mValues[Matrix.MTRANS_X] + dx;
                    float endTy = mValues[Matrix.MTRANS_Y] + dy;
                    mMatrix.getValues(mValues);
                    float startTx = mValues[Matrix.MTRANS_X];
                    float startTy = mValues[Matrix.MTRANS_Y];
                    if (dx != 0) {
                        px = getPivot(targetScale, startTx, endTx);
                    }
                    py = getPivot(targetScale, startTy, endTy);
                    doScaleAnim(px, py, mMaxScale);
                }
            } else {
                Log.d(TAG, "onScaleEnd: 3");
                mMatrix.mapRect(mTempRect, mInitRect);
                if (!isFillWithImage(mTempRect)) {
                    Log.d(TAG, "onScaleEnd: 31");
                    animTranslationToInit();
                }
            }
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (isScaling()) {
                return true;
            }
            if (mScale == 1) {
                float[] pivot = getZoomOutPivot(e.getX(), e.getY());
                doScaleAnim(pivot[0], pivot[1], mDoubleTabScale);
            } else {
                animToInitPosition();
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            if (mScroller != null && !mScroller.isFinished()) {
                mScroller.forceFinished(true);
            }
            return true;
        }

        @TargetApi(16)
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (isScaling() || isTransing()) {
                Log.d(TAG, "onFling: return on scaling | transing");
                return true;
            }
            mMatrix.mapRect(mTempRect, mInitRect);
            final Rect vRect = mViewRect;
            if (vRect.left <= mTempRect.left && vRect.right >= mTempRect.right) {
                velocityX = 0;
            }
            if (vRect.top <= mTempRect.top && vRect.bottom >= mTempRect.bottom) {
                velocityY = 0;
            }

            int startX = 0;
            int startY = 0;
            int minX = 0;
            int minY = 0;
            int maxX = 0;
            int maxY = 0;
            if (velocityX > 0) {
                minX = 0;
                maxX = -Math.round(mTempRect.left);
            } else if (velocityX < 0) {
                minX = Math.round(getWidth() - getPaddingLeft() - getPaddingRight() - mTempRect.right);
                maxX = 0;
            }
            if (velocityY > 0) {
                minY = 0;
                maxY = -Math.round(mTempRect.top);
            } else if (velocityY < 0) {
                minY = Math.round(getHeight() - getPaddingTop() - getPaddingBottom() - mTempRect.bottom);
                maxY = 0;
            }
            if (velocityX != 0 || velocityY != 0) {
                final int overX;
                final int overY;
                if (mFlingOverScrollEnabled) {
                    overX = mFlingOverScrollX;
                    overY = mFlingOverScrollY;
                } else {
                    overX = 0;
                    overY = 0;
                }
                new FlingRunnable()
                        .startX(startX)
                        .startY(startY)
                        .vx((int) velocityX)
                        .vy((int) velocityY)
                        .minX(minX)
                        .maxX(maxX)
                        .minY(minY)
                        .maxY(maxY)
                        .overX(overX)
                        .overY(overY)
                        .start();
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {

            mMatrix.mapRect(mTempRect, mInitRect);
            final Rect vRect = mViewRect;
            if (vRect.left <= mTempRect.left && vRect.right >= mTempRect.right) {
                dx = 0;
            }
            if (vRect.top <= mTempRect.top && vRect.bottom >= mTempRect.bottom) {
                dy = 0;
            }
            dx = -dx;
            dy = -dy;

            //Log.d(TAG, "onScroll: dx=" + dx + ",dy=" + dy + ",tempRect=" + mTempRect + ",vrect=" + mViewRect);
            if (dx < 0) {
                getParent().requestDisallowInterceptTouchEvent(mTempRect.right > vRect.right);
            } else {
                getParent().requestDisallowInterceptTouchEvent(mTempRect.left < vRect.left);
            }
            mTempRect.offset(dx, dy);

            if (dx != 0) {
                int maxLeft;
                int minRight;
                if (mOverScrollEnabled) {
                    maxLeft = vRect.left + mOverScrollX;
                    minRight = vRect.right - mOverScrollX;
                } else {
                    maxLeft = vRect.left;
                    minRight = vRect.right;
                }
                if (mTempRect.left > maxLeft) {
                    dx += (maxLeft - mTempRect.left);
                }
                if (mTempRect.right < minRight) {
                    dx += (minRight - mTempRect.right);
                }
            }
            if (dy != 0) {
                int maxTop;
                int minBottom;
                if (mOverScrollEnabled) {
                    maxTop = vRect.top + mOverScrollY;
                    minBottom = vRect.bottom - mOverScrollY;
                } else {
                    maxTop = vRect.top;
                    minBottom = vRect.bottom;
                }
                if (mTempRect.top > maxTop) {
                    dy += (maxTop - mTempRect.top);
                }
                if (mTempRect.bottom < minBottom) {
                    dy += (minBottom - mTempRect.bottom);
                }
            }
            if (dx != 0 || dy != 0) {
                mMatrix.postTranslate(dx, dy);
                invalidate();
            }
            return true;
        }
    }

    private static void log(String TAG, String msg) {
        if (LOG_ENABLE) {
            Log.d(TAG, msg);
        }
    }
}
