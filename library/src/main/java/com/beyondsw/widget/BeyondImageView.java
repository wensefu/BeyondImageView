package com.beyondsw.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.OverScroller;

import com.beyondsw.widget.gesture.GestureHelper;

import java.lang.reflect.Field;

/**
 * Created by wensefu on 17-3-28.
 */
public class BeyondImageView extends ImageView {

    private static final String TAG = "BeyondImageView";
    private static final boolean LOG_ENABLE = true;
    private static final float MAX_SCALE = 2.5f;
    private GestureHelper mGestureHelper;
    private Matrix mMatrix;
    private boolean mScaling;
    private ValueAnimator mDoubleTabScaleAnimator;
    private float[] mValues;
    private float mScale = 1f;
    private float mScaleBeginPx;
    private float mScaleBeginPy;
    private RectF mInitRect = new RectF();
    private RectF mTempRect = new RectF();
    private boolean mCropToPadding;
    private OverScroller mScroller;

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
        mValues = new float[9];
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

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        Drawable drawable = getDrawable();
        if (drawable != null) {
            mInitRect.set(drawable.getBounds());
            log(TAG, "onLayout,drawable.getBounds=" + drawable.getBounds());
            log(TAG, "onLayout,before map mInitRect=" + mInitRect);
            Matrix matrix = getImageMatrix();
            if (matrix != null) {
                matrix.mapRect(mInitRect);
                log(TAG, "onLayout,after map mInitRect=" + mInitRect);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mGestureHelper == null) {
                mGestureHelper = new GestureHelper(getContext(), getGestureListener());
            }
        }
        return mGestureHelper.onTouchEvent(event);
    }

    private void onGestureScaleBegin(float px, float py) {
        mScaleBeginPx = px;
        mScaleBeginPy = py;
        if (mDoubleTabScaleAnimator != null && mDoubleTabScaleAnimator.isRunning()) {
            mDoubleTabScaleAnimator.cancel();
        }
    }

    private void onGestureScale(float px, float py, float factor) {
        mMatrix.postScale(factor, factor, px, py);
        invalidate();
        mScale *= factor;
    }

    private void onGestureScaleEnd(float px, float py) {
        if (mScale > MAX_SCALE) {
            doScaleAnim(mScaleBeginPx, mScaleBeginPy, MAX_SCALE);
        } else if (mScale < 1) {
            animToInitPosition();
        }
    }

    @TargetApi(16)
    private class FlingRunnable implements Runnable {

        int oldX;
        int oldY;

        FlingRunnable(int startX, int startY, int vx, int vy, int minX, int maxX, int minY, int maxY) {
            oldX = startX;
            oldY = startY;
            mScroller.fling(startX, startY, vx, vy, minX, maxX, minY, maxY);
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
                log(TAG, "oldx=" + oldX + ",newX=" + newX + ",oldy=" + oldY + ",newY=" + newY + ",diffX=" + diffX + ",diffY=" + diffY);
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
            }
        }
    }

    @TargetApi(16)
    private void doFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (mScale == 1) {
            return;
        }
        if (mScroller == null) {
            mScroller = new OverScroller(getContext());
        }
        int startX = 0;
        int startY = 0;
        int minX;
        int minY;
        int maxX;
        int maxY;
        if (velocityX > 0) {
            minX = 0;
            maxX = -Math.round(mTempRect.left);
        } else {
            minX = Math.round(getWidth() - getPaddingLeft() - getPaddingRight() - mTempRect.right);
            maxX = 0;
        }
        if (velocityY > 0) {
            minY = 0;
            maxY = -Math.round(mTempRect.top);
        } else {
            minY = Math.round(getHeight() - getPaddingTop() - getPaddingBottom() - mTempRect.bottom);
            maxY = 0;
        }
        log(TAG, "doFling,velocityX=" + velocityX + ",velocityY=" + velocityY + ",minx=" + minX + ",maxX=" + maxX + ",miny=" + minY + ",maxy=" + maxY);
        final Runnable flingRunnable = new FlingRunnable(startX, startY, (int) velocityX, (int) velocityY, minX, maxX, minY, maxY);
        if (Build.VERSION.SDK_INT >= 16) {
            postOnAnimation(flingRunnable);
        } else {
            post(flingRunnable);
        }
    }

    private void doScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (mScale == 1) {
            return;
        }
        mMatrix.postTranslate(-distanceX, -distanceY);
        mMatrix.mapRect(mTempRect, mInitRect);
        log(TAG, "mTempRect=" + mTempRect + ",mInitRect=" + mInitRect);
        if (mTempRect.left > 0) {
            mMatrix.postTranslate(-mTempRect.left, 0);
        }
        if (mTempRect.top > 0) {
            mMatrix.postTranslate(0, -mTempRect.top);
        }
        if (mTempRect.right < getWidth() - getPaddingLeft() - getPaddingRight()) {
            mMatrix.postTranslate(getWidth() - getPaddingLeft() - getPaddingRight() - mTempRect.right, 0);
        }
        if (mTempRect.bottom < getHeight() - getPaddingTop() - getPaddingBottom()) {
            mMatrix.postTranslate(0, getHeight() - getPaddingTop() - getPaddingBottom() - mTempRect.bottom);
        }
        invalidate();
    }

    private void printMatrix() {
        mMatrix.getValues(mValues);
        log(TAG, "----------------------------");
        log(TAG, "mMatrix.scaleX=" + mValues[Matrix.MSCALE_X]);
        log(TAG, "mMatrix.scaleY=" + mValues[Matrix.MSCALE_Y]);
        log(TAG, "mMatrix.tx=" + mValues[Matrix.MTRANS_X]);
        log(TAG, "mMatrix.ty=" + mValues[Matrix.MTRANS_Y]);
        log(TAG, "mMatrix.sx=" + mValues[Matrix.MSKEW_X]);
        log(TAG, "mMatrix.sy=" + mValues[Matrix.MSKEW_Y]);
        log(TAG, "#############################");
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

    private void doDoubleTabScale(float x, final float y) {
        if (mScaling) {
            return;
        }
        mScaling = true;
        if (mScale == 1) {
            doScaleAnim(x, y, MAX_SCALE);
        } else {
            animToInitPosition();
        }
    }

    private void onGestureDown(MotionEvent e) {
        if (mScroller != null && !mScroller.isFinished()) {
            mScroller.forceFinished(true);
        }
    }

    private GestureHelper.Listener getGestureListener() {
        return new GestureHelper.Listener() {

            @Override
            public void onDown(MotionEvent e) {
                onGestureDown(e);
            }

            @Override
            public void onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                doScroll(e1, e2, distanceX, distanceY);
            }

            @Override
            public void onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                doFling(e1, e2, velocityX, velocityY);
            }

            @Override
            public void onDoubleTap(MotionEvent e) {
                log(TAG, "onDoubleTap");
                doDoubleTabScale(e.getX(), e.getY());
            }

            @Override
            public void onScaleBegin(float px, float py) {
                log(TAG, "onScaleBegin");
                onGestureScaleBegin(px, py);
            }

            @Override
            public void onScale(float px, float py, float factor) {
                log(TAG, "onScale,px=" + px + ",py=" + py + ",factor=" + factor);
                onGestureScale(px, py, factor);
            }

            @Override
            public void onScaleEnd(float px, float py) {
                log(TAG, "onScaleEnd");
                onGestureScaleEnd(px, py);
            }
        };
    }

    private static void log(String TAG, String msg) {
        if (LOG_ENABLE) {
            Log.d(TAG, msg);
        }
    }
}
