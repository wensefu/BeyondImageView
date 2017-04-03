package com.beyondsw.demo;

import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.OverScroller;

public class MainActivity extends AppCompatActivity implements Runnable{

    private static final String TAG = "BeyondImageView";
    private float[] mValues = new float[9];

    OverScroller scroller;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler();
        scroller = new OverScroller(this);
        scroller.fling(0, 0, -300, 3000, 0, -200, 0, 220);
        handler.post(this);
    }

    @Override
    public void run() {
        if(scroller.computeScrollOffset()){
            Log.d(TAG, "run: x=" + scroller.getCurrX() + ",y=" + scroller.getCurrY());
            handler.post(this);
        }
    }

    private void printMatrix(String tag, Matrix matrix){
        matrix.getValues(mValues);
        Log.d(TAG,"----------------------------");
        Log.d(TAG, tag + ",mMatrix.scaleX=" + mValues[Matrix.MSCALE_X]);
        Log.d(TAG, tag + ",mMatrix.scaleY=" + mValues[Matrix.MSCALE_Y]);
        Log.d(TAG, tag + ",mMatrix.tx=" + mValues[Matrix.MTRANS_X]);
        Log.d(TAG, tag + ",mMatrix.ty=" + mValues[Matrix.MTRANS_Y]);
        Log.d(TAG, tag + ",mMatrix.sx=" + mValues[Matrix.MSKEW_X]);
        Log.d(TAG, tag + ",mMatrix.sy=" + mValues[Matrix.MSKEW_Y]);
        Log.d(TAG,"#############################");
    }
}
