package com.beyondsw.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import android.widget.OverScroller;

import java.lang.reflect.Field;

/**
 * Created by wensefu on 17-3-28.
 */
public class BeyondImageView extends ImageView {

    private static final String TAG = "BeyondImageView";
    private static final boolean LOG_ENABLE = true;
    private static final float MAX_SCALE = 3f;
    private static final float DOUBLE_TAB_SCALE = 2.5f;
    private float mMaxScale = MAX_SCALE;
    private float mDoubleTabScale = DOUBLE_TAB_SCALE;
    private Matrix mMatrix;
    private Matrix mTempMatrix;
    private boolean mScaling;
    private ValueAnimator mDoubleTabScaleAnimator;
    private ValueAnimator mFixTranslationAnimator;
    private float[] mValues;
    private float mScale = 1f;
    private float mScaleBeginPx;
    private float mScaleBeginPy;
    private RectF mInitRect = new RectF();
    private RectF mTempRect = new RectF();
    private boolean mCropToPadding;
    private OverScroller mScroller;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private static final float FLING_OVER_SCROLL_FACTOR = .125f;
    private static final float OVER_SCROLL_FACTOR = .25f;
    private int mFlingOverScrollX;
    private int mFlingOverScrollY;
    private int mOverScrollX;
    private int mOverScrollY;
    private boolean mFlingOverScrollEnabled = true;
    private boolean mOverScrollEnabled = true;
    private Rect mViewRect;

    public BeyondImageView(Context context) {
        this(context, null);
    }

    public BeyondImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
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

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.YELLOW);
        paint.setStrokeWidth(10);
    }

    @TargetApi(16)
    private boolean getCropToPaddingCompat() {
        return Build.VERSION.SDK_INT < 16 ? mCropToPadding : getCropToPadding();
    }

    Paint paint = new Paint();

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
        if (mInitRect.width() > 0) {
            canvas.drawRect(mInitRect, paint);
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

    private void initMatrix(){
        Drawable drawable = getDrawable();
        if (drawable != null) {
            mInitRect.set(drawable.getBounds());
            Matrix matrix = getImageMatrix();
            if (matrix != null) {
                matrix.mapRect(mInitRect);
            }
            mMatrix.reset();
            mScale = 1;
            mScaling = false;
        }
    }

    private void updateOverScroll(){
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
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mGestureDetector == null) {
                GestureCallback callback = new GestureCallback();
                mGestureDetector = new GestureDetector(getContext(), callback);
                mScaleGestureDetector = new ScaleGestureDetector(getContext(), callback);
            }
        }
        mGestureDetector.onTouchEvent(event);
        mScaleGestureDetector.onTouchEvent(event);
        return true;
    }

    private void animTranslationToInit() {
        mMatrix.mapRect(mTempRect, mInitRect);
        final ScaleType scaleType = getScaleType();
        final Rect viewRect = mViewRect;
        float dy;
        if (scaleType == ScaleType.FIT_START) {
            dy = viewRect.top - mTempRect.top;
        } else if (scaleType == ScaleType.FIT_END) {
            dy = viewRect.bottom - mTempRect.bottom;
        } else {
            dy = viewRect.centerY() - mTempRect.centerY();
        }
        if (dy != 0) {
            mFixTranslationAnimator = ValueAnimator.ofFloat(0, dy).setDuration(220);
            mFixTranslationAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                float t = 0;
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float value = (float) animation.getAnimatedValue();
                    mMatrix.postTranslate(0, value - t);
                    invalidate();
                    t = value;
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

    private void doScaleAnim(final float px, final float py, float toScale) {
        mDoubleTabScaleAnimator = ValueAnimator.ofFloat(mScale, toScale).setDuration(200);
        mDoubleTabScaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                float radioScale = value / mScale;
                mMatrix.postScale(radioScale, radioScale, px, py);
                invalidate();
                mScale = value;
            }
        });
        mDoubleTabScaleAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mScaling = false;
            }
        });
        mDoubleTabScaleAnimator.start();
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

    private class GestureCallback extends GestureDetector.SimpleOnGestureListener implements ScaleGestureDetector.OnScaleGestureListener {

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
            mScaling = true;
            mScaleBeginPx = detector.getFocusX();
            mScaleBeginPy = detector.getFocusY();
            if (mDoubleTabScaleAnimator != null && mDoubleTabScaleAnimator.isRunning()) {
                mDoubleTabScaleAnimator.cancel();
            }
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (mScale < 1) {
                animToInitPosition();
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
                } else {
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
                mMatrix.mapRect(mTempRect, mInitRect);
                if (!isFillWithImage(mTempRect)) {
                    animTranslationToInit();
                }
            }
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (mScaling) {
                return true;
            }
            mScaling = true;
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
                new FlingRunnable()
                        .startX(startX)
                        .startY(startY)
                        .vx((int) velocityX)
                        .vy((int) velocityY)
                        .minX(minX)
                        .maxX(maxX)
                        .minY(minY)
                        .maxY(maxY)
                        .overX(mFlingOverScrollX)
                        .overY(mFlingOverScrollY)
                        .start();
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            mMatrix.postTranslate(-distanceX, -distanceY);

            invalidate();
            return true;
        }
    }

    private static void log(String TAG, String msg) {
        if (LOG_ENABLE) {
            Log.d(TAG, msg);
        }
    }
}
