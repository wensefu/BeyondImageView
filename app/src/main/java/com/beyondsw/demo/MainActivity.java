package com.beyondsw.demo;

import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity{

    private ViewPager mImagePager;
    private LayoutInflater mInflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mInflater = LayoutInflater.from(this);
        mImagePager = ViewUtils.findView(this, R.id.image_pager);
        mImagePager.setPageMargin(DimenUtils.dp2pxInt(6));
        mImagePager.setAdapter(new ImageAdapter());
    }

    private class ImageAdapter extends PagerAdapter{

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View layout = mInflater.inflate(R.layout.item_image,container,false);
            ImageView imageView = ViewUtils.findView(layout,R.id.image);
            Glide.with(MainActivity.this).load(PhotoLoadTest.rc_images[position][1]).into(imageView);
            container.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View)object);
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
