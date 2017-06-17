package com.beyondsw.demo;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.OverScroller;

import com.beyondsw.demo.viewpager.PagerAdapter;
import com.beyondsw.demo.viewpager.ViewPager;
import com.beyondsw.widget.BeyondImageView;
import com.bumptech.glide.Glide;

import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity {

    private ViewPager mImagePager;
    private LayoutInflater mInflater;

    private OverScroller mScroller;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Field f = ViewPager.class.getDeclaredField("DEBUG");
            f.setAccessible(true);
            f.set(null, true);
            Log.d(TAG, "onCreate: debug=" + f.get(null));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        setContentView(R.layout.activity_main);
        mInflater = LayoutInflater.from(this);
        mImagePager = ViewUtils.findView(this, R.id.image_pager);
        mImagePager.setPageMargin(DimenUtils.dp2pxInt(12));
        mImagePager.setAdapter(new ImageAdapter());

        mScroller = new OverScroller(this);


//        new FlingRunnable()
//                .startX(0)
//                .startY(2881)
//                .vx(0)
//                .vy(-770)
//                .minX(0)
//                .maxX(0)
//                .minY(1704)
//                .maxY(1704)
//                .overX(0)
//                .overY(0)
//                .start();
    }

    private static final String TAG = "BeyondImageView-demo";

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
            run();
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
                Log.d(TAG, "run: finished");
                return;
            }
            if (mScroller.computeScrollOffset()) {
                int newX = mScroller.getCurrX();
                int newY = mScroller.getCurrY();
                int diffX = newX - oldX;
                int diffY = newY - oldY;
                if (diffX != 0 || diffY != 0) {
                    oldX = newX;
                    oldY = newY;
                }
                Log.d(TAG, "run: x=" + newX + ",y=" + newY);
                mImagePager.post(this);
            }
        }
    }

    private class ImageAdapter extends PagerAdapter implements BeyondImageView.Listener {

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View layout = mInflater.inflate(R.layout.item_image, container, false);
            BeyondImageView imageView = ViewUtils.findView(layout, R.id.image);
            Glide.with(MainActivity.this).load(PhotoLoadTest.rc_images[position][1]).into(imageView);
            layout.setTag(imageView);
            imageView.addListener(this);
            container.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            ((BeyondImageView) view.getTag()).removeListener(this);
            container.removeView(view);
        }

        boolean mStolen;

        @Override
        public void onStolenTouch(BeyondImageView imageView, boolean stolen) {
            if (mStolen == stolen) {
                return;
            }
            mStolen = stolen;
            if (!stolen) {
                if (mField == null) {
                    try {
                        mField = mImagePager.getClass().getDeclaredField("mLastMotionX");
                        mField2 = mImagePager.getClass().getDeclaredField("mInitialMotionX");
                        mField.setAccessible(true);
                        mField2.setAccessible(true);
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    mField.set(mImagePager, imageView.getTouchX());
                    mField2.set(mImagePager, imageView.getTouchX());
                    Log.d(TAG, "onStolenTouch: set x=" + imageView.getTouchX());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onScroll(BeyondImageView imageView, float dx, float dy) {
            Log.d(TAG, "onScroll: dx=" + dx + ",dy=" + dy);
            if (!mImagePager.isFakeDragging()) {
                mImagePager.beginFakeDrag();
            }
            mImagePager.fakeDragBy(-dx);
        }

        @Override
        public void onDown() {
            Log.d(TAG, "onDown: ");
        }

        @Override
        public void onUp() {
            Log.d(TAG, "onUp: ");
            if (mImagePager.isFakeDragging()) {
                mImagePager.endFakeDrag();
            }
        }

        Field mField;
        Field mField2;

        @Override
        public void onMove(float x) {

        }

        @Override
        public int getCount() {
            return PhotoLoadTest.rc_images.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }
}
