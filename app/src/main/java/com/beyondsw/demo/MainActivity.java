package com.beyondsw.demo;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BeyondImageView";
    private float[] mValues = new float[9];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RectF rect = new RectF(0,0,100,100);
        RectF rect2 = new RectF();
        Matrix matrix = new Matrix();
        matrix.postScale(2,2);
        matrix.postTranslate(20,20);
        matrix.mapRect(rect2,rect);
        Log.d(TAG, "onCreate: rect=" + rect2);
    }

    private void printMatrix(String tag,Matrix matrix){
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
