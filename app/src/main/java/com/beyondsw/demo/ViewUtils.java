package com.beyondsw.demo;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by wensefu on 16-7-18.
 */
public class ViewUtils {

    @SuppressWarnings("unchecked")
    public static <T> T findView(View parent, int id) {
        return (T) parent.findViewById(id);
    }

    @SuppressWarnings("unchecked")
    public static <T> T findView(Activity act, int id) {
        return (T) act.findViewById(id);
    }

    public static void showIme(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
        }
    }

    public static void hideIme(Context context, IBinder token) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(token, 0); //强制隐藏键盘
        }
    }

    /**
     * 增加view的可点击区域大小,四边增加相同大小(像素)
     *
     * @param view
     * @param px
     */
    public static void expandClickArea(View view, int px) {
        expandClickArea(view, px, px, px, px);
    }

    /**
     * 增加view的可点击区域大小,水平和垂直方向分别增加相同大小(像素)
     *
     * @param view
     * @param vpx  垂直方向扩展大小
     * @param hpx  水平方向扩展大小
     */
    public static void expandClickArea(View view, int vpx, int hpx) {
        expandClickArea(view, hpx, vpx, hpx, vpx);
    }


    /**
     * 增加view的可点击区域大小(像素)
     *
     * @param view
     * @param top
     * @param bottom
     * @param left
     * @param right
     */
    public static void expandClickArea(final View view, final int left, final int top, final int right, final int bottom) {
        ViewParent parent = view.getParent();
        if (parent instanceof View) {
            final View parentView = (View) parent;
            parentView.post(new Runnable() {
                @Override
                public void run() {
                    Rect bounds = new Rect();
                    view.setEnabled(true);
                    view.getHitRect(bounds);
                    bounds.top -= top;
                    bounds.bottom += bottom;
                    bounds.left -= left;
                    bounds.right += right;

                    TouchDelegate touchDelegate = new TouchDelegate(bounds, view);
                    parentView.setTouchDelegate(touchDelegate);
                }
            });
        }
    }

    public static void setActivityFullScreen(Activity act, boolean fullscreen) {
        ActionBar actionBar = act.getActionBar();
        if (actionBar != null) {
            if (fullscreen) {
                actionBar.hide();
            } else {
                actionBar.show();
            }
        }
        Window window = act.getWindow();
        if (fullscreen) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            window.setAttributes(params);
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        } else {
            WindowManager.LayoutParams params = window.getAttributes();
            params.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.setAttributes(params);
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }

    /**
     * 获取屏幕大小（不含statusBar和navigationBar）
     *
     * @param context
     * @return
     */
    public static int[] getScreenSize(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int[] result = new int[2];
        result[0] = dm.widthPixels;
        result[1] = dm.heightPixels;
        return result;
    }

    /**
     * 获取物理分辩率
     *
     * @param context
     * @return
     */
    @TargetApi(17)
    public static Point getPhysicalScreenSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        final Display display = wm.getDefaultDisplay();
        Point point = new Point();
        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealSize(point);
        } else {
            try {
                point.x = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
                point.y = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        return point;
    }
}
