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
    private static final float SCALE_SPEED = 1.02f;
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
    private float mMaxVelocity;
    private float mMinVelocity;
    private boolean mFlingX;
    private boolean mFlingY;
    private Animator mFixEdgeAnimator;
    private boolean mMulTouchScaling;


    public BeyondImageView(Context context) {
        this(context, null);
    }

    public BeyondImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BeyondImageView);
        mDoubleTabScale = mUserDoubleTabScale = a.getFloat(R.styleable.BeyondImageView_doubleTabScale, DOUBLE_TAB_SCALE);
        mDoubleTabAdjustBounds = a.getBoolean(R.styleable.BeyondImageView_doubleTabAdjustBounds, false);
        mUserMaxScale = mMaxScale = a.getFloat(R.styleable.BeyondImageView_maxScale, MAX_SCALE);
        Log.d(TAG, "BeyondImageView: mMaxScale=" + mMaxScale);
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
        final ViewConfiguration vc = ViewConfiguration.get(getContext());

        mMaxVelocity = vc.getScaledMaximumFlingVelocity();
        mMinVelocity = vc.getScaledMinimumFlingVelocity();

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

    private void mapRectInt() {
        mMatrix.mapRect(mTempRect, mInitRect);
        mTempRect.left = Math.round(mTempRect.left);
        mTempRect.top = Math.round(mTempRect.top);
        mTempRect.right = Math.round(mTempRect.right);
        mTempRect.bottom = Math.round(mTempRect.bottom);
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
        if (mInitRect.width() == 0) {
            return;
        }
        if (mDoubleTabAdjustBounds) {
            float scaleX = getWidth() / mInitRect.width();
            float scaleY = getHeight() / mInitRect.height();
            mDoubleTabScale = Math.max(scaleX, scaleY);
            if (mMaxScale < mDoubleTabScale) {
                mMaxScale = mDoubleTabScale;
                Log.d(TAG, "updateDoubleTabScale: mMaxScale=" + mMaxScale);
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

    public void setDoubleTabAdjustBounds(boolean adjustBounds) {
        if (adjustBounds != mDoubleTabAdjustBounds) {
            mDoubleTabAdjustBounds = adjustBounds;
            if (!adjustBounds) {
                mDoubleTabScale = mUserDoubleTabScale;
                mMaxScale = mUserMaxScale;
                Log.d(TAG, "setDoubleTabAdjustBounds: mMaxScale=" + mMaxScale);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction() & MotionEvent.ACTION_MASK;
        if (action == MotionEvent.ACTION_DOWN) {
            if (mGestureDetector == null) {
                initGestureDetector();
            }
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            animToEdgeIfNeeded();
        }
        mGestureDetector.onTouchEvent(event);
        mScaleGestureDetector.onTouchEvent(event);
        return true;
    }

    private void animToEdgeIfNeeded() {
        if ((mScaleAnimator != null && mScaleAnimator.isRunning())
                || (mFixTranslationAnimator != null && mFixTranslationAnimator.isRunning())) {
            Log.d(TAG, "animToEdgeIfNeeded: isScaling=" + isScaling() + ",isTransing=" + isTransing());
            return;
        }
        final boolean flingX = mFlingX;
        final boolean flingY = mFlingY;
        Log.d(TAG, "animToEdgeIfNeeded: flingx=" + mFlingX + ",flingy=" + mFlingY);
        if (mFlingX && mFlingY) {
            return;
        }
        mapRectInt();

        final Rect vRect = mViewRect;
        float dx = 0;
        if (!flingX) {
            if (mTempRect.left > vRect.left && ((mTempRect.right - vRect.right) >= (mTempRect.left - vRect.left))) {
                dx = vRect.left - mTempRect.left;
            }
            if (mTempRect.right < vRect.right && ((vRect.left - mTempRect.left) >= (vRect.right - mTempRect.right))) {
                dx = vRect.right - mTempRect.right;
            }
        }

        float dy = 0;
        if (!flingY) {
            if (mTempRect.top > vRect.top && ((mTempRect.bottom - vRect.bottom) >= (mTempRect.top - vRect.top))) {
                dy = vRect.top - mTempRect.top;
            }

            if (mTempRect.bottom < vRect.bottom && ((vRect.top - mTempRect.top) >= (vRect.bottom - mTempRect.bottom))) {
                dy = vRect.bottom - mTempRect.bottom;
            }
        }

        Log.d(TAG, "animToEdgeIfNeeded:dx=" + dx + ",dy=" + dy + ",vrect=" + vRect + ",tempRect=" + mTempRect);

        if (dx != 0 || dy != 0) {
            final PropertyValuesHolder xph = PropertyValuesHolder.ofFloat("x", 0, dx);
            final PropertyValuesHolder yph = PropertyValuesHolder.ofFloat("y", 0, dy);
            final ValueAnimator animator;
            if (!flingX && flingY) {
                animator = ValueAnimator.ofPropertyValuesHolder(xph);
            } else if (flingX) {
                animator = ValueAnimator.ofPropertyValuesHolder(yph);
            } else {
                animator = ValueAnimator.ofPropertyValuesHolder(xph, yph);
            }
            animator.setDuration(300);
            animator.setInterpolator(new ViscousFluidInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                float prevdx = 0;
                float prevdy = 0;

                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (!flingX) {
                        float dx = (float) animation.getAnimatedValue("x");
                        mMatrix.postTranslate(dx - prevdx, 0);
                        prevdx = dx;
                    }
                    if (!flingY) {
                        float dy = (float) animation.getAnimatedValue("y");
                        mMatrix.postTranslate(0, dy - prevdy);
                        prevdy = dy;
                    }
                    invalidate();
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mMatrix.mapRect(mTempRect, mInitRect);
                    Log.d(TAG, "animToEdgeIfNeeded onAnimationEnd,mTempRect=" + mTempRect);
                }
            });
            mFixEdgeAnimator = animator;
            animator.start();
        }
    }

    private void animTranslationToInit() {
        mapRectInt();
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

        if (mTempRect.left > viewRect.left && ((mTempRect.right - viewRect.right) >= (mTempRect.left - viewRect.left))) {
            dx = viewRect.left - mTempRect.left;
        }
        if (mTempRect.right < viewRect.right && ((viewRect.left - mTempRect.left) >= (viewRect.right - mTempRect.right))) {
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
                    Log.d(TAG, "animTranslationToInit onAnimationEnd");
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

    /**
     * Clamp the magnitude of value for absMin and absMax.
     * If the value is below the minimum, it will be clamped to zero.
     * If the value is above the maximum, it will be clamped to the maximum.
     *
     * @param value  Value to clamp
     * @param absMin Absolute value of the minimum significant value to return
     * @param absMax Absolute value of the maximum value to return
     * @return The clamped value with the same sign as <code>value</code>
     */
    private static float clampMag(float value, float absMin, float absMax) {
        final float absValue = Math.abs(value);
        if (absValue < absMin) return 0;
        if (absValue > absMax) return value > 0 ? absMax : -absMax;
        return value;
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
            Log.d(TAG, "onFling: minY=" + minY + ",maxY=" + maxY + ",minX=" + minX + ",maxX=" + maxX + ",vx=" + vx + ",vy=" + vy);
            Log.d(TAG, "onFling,endX=" + mScroller.getFinalX() + ",endY=" + mScroller.getFinalY());
            if (Build.VERSION.SDK_INT >= 16) {
                postOnAnimation(this);
            } else {
                post(this);
            }
        }

        FlingRunnable startX(int startX) {
            this.oldX = this.startX = startX;
            return this;
        }

        FlingRunnable startY(int startY) {
            this.oldY = this.startY = startY;
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
                mMatrix.mapRect(mTempRect, mInitRect);
                Log.d(TAG, "run: mScroller.isFinished(),mTempRect=" + mTempRect + ",vRect=" + mViewRect);
                mFlingY = false;
                mFlingX = false;
                return;
            }
            if (mScroller.computeScrollOffset()) {
                int newX = mScroller.getCurrX();
                int newY = mScroller.getCurrY();
                int diffX = newX - oldX;
                int diffY = newY - oldY;
                if (diffX != 0 || diffY != 0) {
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
            } else {
                Log.d(TAG, "run: fling done");
                mFlingY = false;
                mFlingX = false;
            }
        }
    }

    private boolean isScaling() {
        return mMulTouchScaling || (mScaleAnimator != null && mScaleAnimator.isRunning());
    }

    private boolean isTransing() {
        return mFixTranslationAnimator != null && mFixTranslationAnimator.isRunning();
    }

    private boolean isFixingEdge() {
        return mFixEdgeAnimator != null && mFixEdgeAnimator.isRunning();
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
        mScaleAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationCancel(animation);
                Log.d(TAG, "doScaleAnim onAnimationEnd");
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
            if (Math.round(mTempRect.right - vRect.right) >= Math.round(mTempRect.left - vRect.left)) {
                mTempMatrix.postTranslate(vRect.left - mTempRect.left, 0);
            } else {
                mTempMatrix.postTranslate(vRect.centerX() - mTempRect.centerX(), 0);
            }
        } else if (mTempRect.right < vRect.right) {
            if (Math.round(vRect.left - mTempRect.left) >= Math.round(vRect.right - mTempRect.right)) {
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
            if (Math.round(mTempRect.bottom - vRect.bottom) >= Math.round(mTempRect.top - vRect.top)) {
                mTempMatrix.postTranslate(0, vRect.top - mTempRect.top);
            } else {
                mTempMatrix.postTranslate(0, fixedInitDy);
            }
        } else if (mTempRect.bottom < vRect.bottom) {
            if (Math.round(vRect.top - mTempRect.top) >= Math.round(vRect.bottom - mTempRect.bottom)) {
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
            if (factor > 1) {
                factor *= SCALE_SPEED;
            } else if (factor < 1) {
                factor /= SCALE_SPEED;
            }
            mMatrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
            invalidate();
            mScale *= factor;
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mMulTouchScaling = true;
            mScaleBeginPx = detector.getFocusX();
            mScaleBeginPy = detector.getFocusY();
            if (mScaleAnimator != null && mScaleAnimator.isRunning()) {
                mScaleAnimator.cancel();
            }
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            Log.d(TAG, "onScaleEnd: ");
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
                mapRectInt();
                if (!isFillWithImage(mTempRect)) {
                    Log.d(TAG, "onScaleEnd: 31");
                    animTranslationToInit();
                }
            }
            mMulTouchScaling = false;
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
                Log.d(TAG, "onDown: forceFinished");
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
            mapRectInt();
            final Rect vRect = mViewRect;
            if (vRect.left <= mTempRect.left && vRect.right >= mTempRect.right) {
                if (!mFlingOverScrollEnabled) {
                    velocityX = 0;
                }
            }
            if (vRect.top <= mTempRect.top && vRect.bottom >= mTempRect.bottom) {
                if (!mFlingOverScrollEnabled) {
                    velocityY = 0;
                }
            }

            velocityX = clampMag(velocityX, mMinVelocity, mMaxVelocity);
            velocityY = clampMag(velocityY, mMinVelocity, mMaxVelocity);

            Log.d(TAG, "onFling:vx=" + velocityX + ",vy=" + velocityY + ",mTempRect=" + mTempRect + ",vrect=" + mViewRect);


            int startX = 0;
            int startY = 0;
            int minX = 0;
            int minY = 0;
            int maxX = 0;
            int maxY = 0;
            if (velocityX > 0) {
                if (mTempRect.left < vRect.left && mTempRect.right > vRect.right) {
                    maxX = vRect.left - Math.round(mTempRect.left);
                } else {
                    velocityX = 0;
                }
            } else if (velocityX < 0) {
                if (mTempRect.left < vRect.left && mTempRect.right > vRect.right) {
                    minX = vRect.right - Math.round(mTempRect.right);
                } else {
                    velocityX = 0;
                }
            }
            if (velocityY > 0) {
                if (mTempRect.bottom > vRect.bottom && mTempRect.top < vRect.top) {
                    maxY = vRect.top - Math.round(mTempRect.top);
                } else {
                    velocityY = 0;
                }
            } else if (velocityY < 0) {
                if (mTempRect.bottom > vRect.bottom && mTempRect.top < vRect.top) {
                    minY = vRect.bottom - Math.round(mTempRect.bottom);
                } else {
                    velocityY = 0;
                }
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
                mFlingX = velocityX != 0;
                mFlingY = velocityY != 0;
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
            if (isScaling()) {
                Log.d(TAG, "onScroll: scaling,return");
                return true;
            }
            if (isTransing()) {
                Log.d(TAG, "onScroll: transing,return");
                return true;
            }
            mapRectInt();
            final Rect vRect = mViewRect;
            if (vRect.left <= mTempRect.left && vRect.right >= mTempRect.right) {
                if (!mOverScrollEnabled) {
                    dx = 0;
                }
            }
            if (vRect.top <= mTempRect.top && vRect.bottom >= mTempRect.bottom) {
                if (!mOverScrollEnabled) {
                    dy = 0;
                }
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
            if (dx != 0 || dy != 0) {
                Log.d(TAG, "onScroll: dx=" + dx + ",dy=" + dy);
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
