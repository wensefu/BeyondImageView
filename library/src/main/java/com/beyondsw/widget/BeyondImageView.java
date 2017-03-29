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
    private static final float SCALE_FACTOR = 2.5f;
    private float mScaleFactor = 1f;
    private GestureHelper mGestureHelper;
    private Matrix mMatrix;
    private boolean mScaling;
    private float mPx;
    private float mPy;

    public BeyondImageView(Context context) {
        this(context, null);
    }

    public BeyondImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mMatrix = new Matrix();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.concat(mMatrix);
        super.onDraw(canvas);
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

    private void doDoubleTabScale(float x,final float y) {
        if (mScaling) {
            return;
        }
        mScaling = true;
        final boolean scaleBigger = Float.compare(mScaleFactor, 1) == 0;
        final float targetScale = scaleBigger ? SCALE_FACTOR : 1f;
        final float px;
        final float py;
        if (scaleBigger) {
            mPx = px = x;
            mPy = py = y;
        } else {
            px = mPx;
            py = mPy;
        }
        final ValueAnimator animator = ValueAnimator.ofFloat(mScaleFactor, targetScale).setDuration(200);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float)animation.getAnimatedValue();
                mMatrix.setScale(value, value, px, py);
                invalidate();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mScaleFactor = targetScale;
                mScaling = false;
            }
        });
        animator.start();
    }

    private GestureHelper.Listener getGestureListener() {
        return new GestureHelper.Listener() {

            @Override
            public void onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                log(TAG, "onScroll: distanceX=" + distanceX + ",distanceY=" + distanceY);
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
            public void onScaleBegin() {
                log(TAG, "onScaleBegin");
            }

            @Override
            public void onScale(float px, float py, float factor) {
                log(TAG, "onScaleBegin");
            }

            @Override
            public void onScaleEnd() {
                log(TAG, "onScaleEnd");
            }
        };
    }

    private static void log(String TAG, String msg) {
        if (LOG_ENABLE) {
            Log.d(TAG, msg);
        }
    }
}
