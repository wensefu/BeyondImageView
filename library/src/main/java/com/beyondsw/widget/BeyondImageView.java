package com.beyondsw.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.beyondsw.widget.gesture.GestureHelper;

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


    public BeyondImageView(Context context) {
        this(context, null);
    }

    public BeyondImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mMatrix = new Matrix();
        mValues = new float[9];
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.concat(mMatrix);
        super.onDraw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        log(TAG, "onLayout");
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

    private void doScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (mScale == 1) {
            return;
        }
        mMatrix.postTranslate(distanceX, distanceY);
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

    private GestureHelper.Listener getGestureListener() {
        return new GestureHelper.Listener() {

            @Override
            public void onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                log(TAG, "onScroll: distanceX=" + distanceX + ",distanceY=" + distanceY);
                doScroll(e1, e2, distanceX, distanceY);
            }

            @Override
            public void onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                log(TAG, "onFling: distanceX=" + velocityX + ",velocityY=" + velocityY);
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
