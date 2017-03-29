package com.beyondsw.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.beyondsw.widget.gesture.GestureHelper;

/**
 * Created by wensefu on 17-3-28.
 */
public class BeyondImageView extends ImageView{
    
    private static final String TAG = "BeyondImageView";
    private static final boolean LOG_ENABLE = true;
    private GestureHelper mGestureHelper;
    private Matrix mMatrix;

    public BeyondImageView(Context context) {
        this(context, null);
    }

    public BeyondImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){
        mMatrix = new Matrix();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mGestureHelper == null) {
                mGestureHelper = new GestureHelper(getContext(),getGestureListener());
            }
        }
        return mGestureHelper.onTouchEvent(event);
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
    
    private static void log(String TAG,String msg){
        if(LOG_ENABLE){
            Log.d(TAG, msg);
        }
    }
}
