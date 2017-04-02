package com.beyondsw.widget.gesture;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * Created by wensefu on 17-3-28.
 */
public class GestureHelper {

    private GestureDetector mGesture;
    private ScaleGestureDetector mScaleGesture;
    private Listener mListener;

    public interface Listener {
        void onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY);

        void onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY);

        void onDoubleTap(MotionEvent e);

        void onScaleBegin(float px, float py);

        void onScale(float px, float py, float factor);

        void onScaleEnd(float px, float py);
    }

    public GestureHelper(Context context, Listener listener) {
        mListener = listener;
        mGesture = new GestureDetector(context, mGestureListener);
        mGesture.setOnDoubleTapListener(mDoubleTabListener);
        mScaleGesture = new ScaleGestureDetector(context, mScaleListener);
    }

    public boolean onTouchEvent(MotionEvent event) {
        mGesture.onTouchEvent(event);
        mScaleGesture.onTouchEvent(event);
//        final int action = event.getAction()&MotionEvent.ACTION_MASK;
//        if(action==MotionEvent.ACTION_POINTER_UP)
        return true;
    }

    private GestureDetector.OnGestureListener mGestureListener = new GestureDetector.OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            mListener.onScroll(e1, e2, distanceX, distanceY);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            mListener.onFling(e1, e2, velocityX, velocityY);
            return true;
        }
    };

    private GestureDetector.OnDoubleTapListener mDoubleTabListener = new GestureDetector.OnDoubleTapListener() {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            mListener.onDoubleTap(e);
            return true;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return false;
        }
    };

    private ScaleGestureDetector.OnScaleGestureListener mScaleListener = new ScaleGestureDetector.OnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mListener.onScale(detector.getFocusX(), detector.getFocusY(), detector.getScaleFactor());
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mListener.onScaleBegin(detector.getFocusX(), detector.getFocusY());
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mListener.onScaleEnd(detector.getFocusX(), detector.getFocusY());
        }
    };
}
