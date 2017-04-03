package com.beyondsw.demo;

import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.beyondsw.widget.BeyondImageView;

public class MainActivity extends AppCompatActivity{

    private static final String TAG = "BeyondImageView";
    private float[] mValues = new float[9];

    private BeyondImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = (BeyondImageView)findViewById(R.id.image);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.matrix:
                mImageView.setScaleType(ImageView.ScaleType.MATRIX);
                break;
            case R.id.fit_xy:
                mImageView.setScaleType(ImageView.ScaleType.FIT_XY);
                break;
            case R.id.fit_start:
                mImageView.setScaleType(ImageView.ScaleType.FIT_START);
                break;
            case R.id.fit_center:
                mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                break;
            case R.id.fit_end:
                mImageView.setScaleType(ImageView.ScaleType.FIT_END);
                break;
            case R.id.center:
                mImageView.setScaleType(ImageView.ScaleType.CENTER);
                break;
            case R.id.center_crop:
                mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                break;
            case R.id.center_inside:
                mImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                break;
        }
        return true;
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
