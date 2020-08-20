/*
 * Tencent is pleased to support the open source community by making QMUI_Android available.
 *
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the MIT License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hw.movie.tools;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.RequiresApi;
import android.support.v4.view.ViewCompat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.hw.movie.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 透明状态栏不好用，暂时没用，用到了设置状态栏字体颜色
 *
 * @date 2016-03-27
 */
public class StatusBarHelper {

    private static final String TAG_FAKE_STATUS_BAR_VIEW = "statusBarView";
    private static final String TAG_MARGIN_ADDED = "marginAdded";
    private final static int STATUSBAR_TYPE_DEFAULT = 0;
    private final static int STATUSBAR_TYPE_MIUI = 1;
    private final static int STATUSBAR_TYPE_FLYME = 2;
    private final static int STATUSBAR_TYPE_ANDROID6 = 3; // Android 6.0
    private final static int STATUS_BAR_DEFAULT_HEIGHT_DP = 25; // 大部分状态栏都是25dp
    // 在某些机子上存在不同的density值，所以增加两个虚拟值
    public static float sVirtualDensity = -1;
    public static float sVirtualDensityDpi = -1;
    private static int sStatusbarHeight = -1;
    private static @StatusBarType
    int mStatuBarType = STATUSBAR_TYPE_DEFAULT;
    private static Integer sTransparentValue;

    /**
     * 设置透明状态栏
     */
    public static void translucentStatusBar(Activity activity) {
        translucentStatusBar(activity, false);
    }

    /**
     * 设置透明状态栏
     *
     * @param hideStatusBarBackground 是否隐藏状态栏背景色，隐藏为全透明
     */
    public static void translucentStatusBar(Activity activity, boolean hideStatusBarBackground) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            translucentStatusBar_21(activity, hideStatusBarBackground);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            translucentStatusBar_19(activity);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void translucentStatusBar_21(Activity activity, boolean hideStatusBarBackground) {
        Window window = activity.getWindow();
        //添加Flag把状态栏设为可绘制模式
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (hideStatusBarBackground) {
            //如果为全透明模式，取消设置Window半透明的Flag
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            //设置状态栏为透明
            window.setStatusBarColor(Color.TRANSPARENT);
            //设置window的状态栏不可见
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        } else {
            //如果为半透明模式，添加设置Window半透明的Flag
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            //设置系统状态栏处于可见状态
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
        //view不根据系统窗口来调整自己的布局
        ViewGroup mContentView = (ViewGroup) window.findViewById(Window.ID_ANDROID_CONTENT);
        View mChildView = mContentView.getChildAt(0);
        if (mChildView != null) {
            ViewCompat.setFitsSystemWindows(mChildView, false);
            ViewCompat.requestApplyInsets(mChildView);
        }
    }


    private static void translucentStatusBar_19(Activity activity) {
        Window window = activity.getWindow();
        //设置Window为透明
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        ViewGroup mContentView = (ViewGroup) activity.findViewById(Window.ID_ANDROID_CONTENT);
        View mContentChild = mContentView.getChildAt(0);

        //移除已经存在假状态栏则,并且取消它的Margin间距
        removeFakeStatusBarViewIfExist(activity);
        removeMarginTopOfContentChild(mContentChild, getStatusbarHeight(activity));
        if (mContentChild != null) {
            //fitsSystemWindow 为 false, 不预留系统栏位置.
            ViewCompat.setFitsSystemWindows(mContentChild, false);
        }
    }

    private static void removeFakeStatusBarViewIfExist(Activity activity) {
        Window window = activity.getWindow();
        ViewGroup mDecorView = (ViewGroup) window.getDecorView();

        View fakeView = mDecorView.findViewWithTag(TAG_FAKE_STATUS_BAR_VIEW);
        if (fakeView != null) {
            mDecorView.removeView(fakeView);
        }
    }

    private static void removeMarginTopOfContentChild(View mContentChild, int statusBarHeight) {
        if (mContentChild == null) {
            return;
        }
        if (TAG_MARGIN_ADDED.equals(mContentChild.getTag())) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mContentChild.getLayoutParams();
            lp.topMargin -= statusBarHeight;
            mContentChild.setLayoutParams(lp);
            mContentChild.setTag(null);
        }
    }

    /**
     * 获取状态栏的高度。
     */
    public static int getStatusbarHeight(Context context) {
        if (sStatusbarHeight == -1) {
            initStatusBarHeight(context);
        }
        return sStatusbarHeight;
    }

    private static void initStatusBarHeight(Context context) {
        Class<?> clazz;
        Object obj = null;
        Field field = null;
        try {
            clazz = Class.forName("com.android.internal.R$dimen");
            obj = clazz.newInstance();
            if (DeviceHelper.isMeizu()) {
                try {
                    field = clazz.getField("status_bar_height_large");
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            if (field == null) {
                field = clazz.getField("status_bar_height");
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        if (field != null && obj != null) {
            try {
                int id = Integer.parseInt(field.get(obj).toString());
                sStatusbarHeight = context.getResources().getDimensionPixelSize(id);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        if (DeviceHelper.isTablet(context)
                && sStatusbarHeight > SystemUtils.dp2px(STATUS_BAR_DEFAULT_HEIGHT_DP)) {
            //状态栏高度大于25dp的平板，状态栏通常在下方
            sStatusbarHeight = 0;
        } else {
            if (sStatusbarHeight <= 0) {
                if (sVirtualDensity == -1) {
                    sStatusbarHeight = SystemUtils.dp2px(STATUS_BAR_DEFAULT_HEIGHT_DP);
                } else {
                    sStatusbarHeight = (int) (STATUS_BAR_DEFAULT_HEIGHT_DP * sVirtualDensity + 0.5f);
                }
            }
        }
    }

    /**
     * 设置状态栏颜色
     *
     * @param activity
     * @param statusColor
     */
    public static void setStatusBarColor(Activity activity, int statusColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setStatusBarColor_21(activity, statusColor);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setStatusBarColor_19(activity, statusColor);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void setStatusBarColor_21(Activity activity, int statusColor) {
        Window window = activity.getWindow();
        //取消状态栏透明
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //添加Flag把状态栏设为可绘制模式
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        //设置状态栏颜色
        window.setStatusBarColor(statusColor);
        //设置系统状态栏处于可见状态
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        //让view不根据系统窗口来调整自己的布局
        ViewGroup mContentView = (ViewGroup) window.findViewById(Window.ID_ANDROID_CONTENT);
        View mChildView = mContentView.getChildAt(0);
        if (mChildView != null) {
            ViewCompat.setFitsSystemWindows(mChildView, false);
            ViewCompat.requestApplyInsets(mChildView);
        }
    }

    private static void setStatusBarColor_19(Activity activity, int statusColor) {
        Window window = activity.getWindow();
        //设置Window为全透明
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        ViewGroup mContentView = (ViewGroup) window.findViewById(Window.ID_ANDROID_CONTENT);
        //获取父布局
        View mContentChild = mContentView.getChildAt(0);
        //获取状态栏高度
        int statusBarHeight = getStatusbarHeight(activity);

        //如果已经存在假状态栏则移除，防止重复添加
        removeFakeStatusBarViewIfExist(activity);
        //添加一个View来作为状态栏的填充
        addFakeStatusBarView(activity, statusColor, statusBarHeight);
        //设置子控件到状态栏的间距
        addMarginTopToContentChild(mContentChild, statusBarHeight);
        //不预留系统栏位置
        if (mContentChild != null) {
            ViewCompat.setFitsSystemWindows(mContentChild, false);
        }
        //如果在Activity中使用了ActionBar则需要再将布局与状态栏的高度跳高一个ActionBar的高度，否则内容会被ActionBar遮挡
        int action_bar_id = activity.getResources().getIdentifier("action_bar", "id", activity.getPackageName());
        View view = activity.findViewById(action_bar_id);
        if (view != null) {
            TypedValue typedValue = new TypedValue();
            if (activity.getTheme().resolveAttribute(R.attr.actionBarSize, typedValue, true)) {
                int actionBarHeight = TypedValue.complexToDimensionPixelSize(typedValue.data, activity.getResources().getDisplayMetrics());
                setContentTopPadding(activity, actionBarHeight);
            }
        }
    }

    private static View addFakeStatusBarView(Activity activity, int statusBarColor, int statusBarHeight) {
        Window window = activity.getWindow();
        ViewGroup mDecorView = (ViewGroup) window.getDecorView();

        View mStatusBarView = new View(activity);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, statusBarHeight);
        layoutParams.gravity = Gravity.TOP;
        mStatusBarView.setLayoutParams(layoutParams);
        mStatusBarView.setBackgroundColor(statusBarColor);
        mStatusBarView.setTag(TAG_FAKE_STATUS_BAR_VIEW);

        mDecorView.addView(mStatusBarView);
        return mStatusBarView;
    }

    private static void addMarginTopToContentChild(View mContentChild, int statusBarHeight) {
        if (mContentChild == null) {
            return;
        }
        if (!TAG_MARGIN_ADDED.equals(mContentChild.getTag())) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mContentChild.getLayoutParams();
            lp.topMargin += statusBarHeight;
            mContentChild.setLayoutParams(lp);
            mContentChild.setTag(TAG_MARGIN_ADDED);
        }
    }

    public static void setContentTopPadding(Activity activity, int padding) {
        ViewGroup mContentView = (ViewGroup) activity.getWindow().findViewById(Window.ID_ANDROID_CONTENT);
        mContentView.setPadding(0, padding, 0, 0);
    }

    /**
     * 设置状态栏黑色字体图标，
     * 支持 4.4 以上版本 MIUI 和 Flyme，以及 6.0 以上版本的其他 Android
     *
     * @param activity 需要被处理的 Activity
     */
    public static boolean setStatusBarLightMode(Activity activity) {
        if (activity == null) {
            return false;
        }
        // 无语系列：ZTK C2016只能时间和电池图标变色。。。。
        if (DeviceHelper.isZTKC2016()) {
            return false;
        }

        if (mStatuBarType != STATUSBAR_TYPE_DEFAULT) {
            return setStatusBarLightMode(activity, mStatuBarType);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (isMIUICustomStatusBarLightModeImpl() && MIUISetStatusBarLightMode(activity.getWindow(), true)) {
                mStatuBarType = STATUSBAR_TYPE_MIUI;
                return true;
            } else if (FlymeSetStatusBarLightMode(activity.getWindow(), true)) {
                mStatuBarType = STATUSBAR_TYPE_FLYME;
                return true;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Android6SetStatusBarLightMode(activity.getWindow(), true);
                mStatuBarType = STATUSBAR_TYPE_ANDROID6;
                return true;
            }
        }
        return false;
    }

    /**
     * 已知系统类型时，设置状态栏黑色字体图标。
     * 支持 4.4 以上版本 MIUI 和 Flyme，以及 6.0 以上版本的其他 Android
     *
     * @param activity 需要被处理的 Activity
     * @param type     StatusBar 类型，对应不同的系统
     */
    private static boolean setStatusBarLightMode(Activity activity, @StatusBarType int type) {
        if (type == STATUSBAR_TYPE_MIUI) {
            return MIUISetStatusBarLightMode(activity.getWindow(), true);
        } else if (type == STATUSBAR_TYPE_FLYME) {
            return FlymeSetStatusBarLightMode(activity.getWindow(), true);
        } else if (type == STATUSBAR_TYPE_ANDROID6) {
            return Android6SetStatusBarLightMode(activity.getWindow(), true);
        }
        return false;
    }

    /**
     * 设置状态栏白色字体图标
     * 支持 4.4 以上版本 MIUI 和 Flyme，以及 6.0 以上版本的其他 Android
     */
    public static boolean setStatusBarDarkMode(Activity activity) {
        if (activity == null) {
            return false;
        }
        if (mStatuBarType == STATUSBAR_TYPE_DEFAULT) {
            // 默认状态，不需要处理
            return true;
        }

        if (mStatuBarType == STATUSBAR_TYPE_MIUI) {
            return MIUISetStatusBarLightMode(activity.getWindow(), false);
        } else if (mStatuBarType == STATUSBAR_TYPE_FLYME) {
            return FlymeSetStatusBarLightMode(activity.getWindow(), false);
        } else if (mStatuBarType == STATUSBAR_TYPE_ANDROID6) {
            return Android6SetStatusBarLightMode(activity.getWindow(), false);
        }
        return true;
    }

    @TargetApi(23)
    private static int changeStatusBarModeRetainFlag(Window window, int out) {
        out = retainSystemUiFlag(window, out, View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        out = retainSystemUiFlag(window, out, View.SYSTEM_UI_FLAG_FULLSCREEN);
        out = retainSystemUiFlag(window, out, View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        out = retainSystemUiFlag(window, out, View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        out = retainSystemUiFlag(window, out, View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        out = retainSystemUiFlag(window, out, View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        return out;
    }

    public static int retainSystemUiFlag(Window window, int out, int type) {
        int now = window.getDecorView().getSystemUiVisibility();
        if ((now & type) == type) {
            out |= type;
        }
        return out;
    }

    /**
     * 设置状态栏字体图标为深色，Android 6
     *
     * @param window 需要设置的窗口
     * @param light  是否把状态栏字体及图标颜色设置为深色
     * @return boolean 成功执行返回true
     */
    @TargetApi(23)
    private static boolean Android6SetStatusBarLightMode(Window window, boolean light) {
        View decorView = window.getDecorView();
        int systemUi = light ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        systemUi = changeStatusBarModeRetainFlag(window, systemUi);
        decorView.setSystemUiVisibility(systemUi);
        if (DeviceHelper.isMIUIV9()) {
            // MIUI 9 低于 6.0 版本依旧只能回退到以前的方案
            // https://github.com/Tencent/QMUI_Android/issues/160
            MIUISetStatusBarLightMode(window, light);
        }
        return true;
    }

    /**
     * 设置状态栏字体图标为深色，需要 MIUIV6 以上
     *
     * @param window 需要设置的窗口
     * @param light  是否把状态栏字体及图标颜色设置为深色
     * @return boolean 成功执行返回 true
     */
    @SuppressWarnings("unchecked")
    public static boolean MIUISetStatusBarLightMode(Window window, boolean light) {
        boolean result = false;
        if (window != null) {
            Class clazz = window.getClass();
            try {
                int darkModeFlag;
                Class layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
                Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
                darkModeFlag = field.getInt(layoutParams);
                Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
                if (light) {
                    extraFlagField.invoke(window, darkModeFlag, darkModeFlag);//状态栏透明且黑色字体
                } else {
                    extraFlagField.invoke(window, 0, darkModeFlag);//清除黑色字体
                }
                result = true;
            } catch (Exception ignored) {

            }
        }
        return result;
    }

    /**
     * 更改状态栏图标、文字颜色的方案是否是MIUI自家的， MIUI9 && Android 6 之后用回Android原生实现
     * 见小米开发文档说明：https://dev.mi.com/console/doc/detail?pId=1159
     */
    private static boolean isMIUICustomStatusBarLightModeImpl() {
        if (DeviceHelper.isMIUIV9() && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return DeviceHelper.isMIUIV5() || DeviceHelper.isMIUIV6() ||
                DeviceHelper.isMIUIV7() || DeviceHelper.isMIUIV8();
    }

    /**
     * 设置状态栏图标为深色和魅族特定的文字风格
     * 可以用来判断是否为 Flyme 用户
     *
     * @param window 需要设置的窗口
     * @param light  是否把状态栏字体及图标颜色设置为深色
     * @return boolean 成功执行返回true
     */
    public static boolean FlymeSetStatusBarLightMode(Window window, boolean light) {
        boolean result = false;
        if (window != null) {
            // flyme 在 6.2.0.0A 支持了 Android 官方的实现方案，旧的方案失效
            Android6SetStatusBarLightMode(window, light);

            try {
                WindowManager.LayoutParams lp = window.getAttributes();
                Field darkFlag = WindowManager.LayoutParams.class
                        .getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON");
                Field meizuFlags = WindowManager.LayoutParams.class
                        .getDeclaredField("meizuFlags");
                darkFlag.setAccessible(true);
                meizuFlags.setAccessible(true);
                int bit = darkFlag.getInt(null);
                int value = meizuFlags.getInt(lp);
                if (light) {
                    value |= bit;
                } else {
                    value &= ~bit;
                }
                meizuFlags.setInt(lp, value);
                window.setAttributes(lp);
                result = true;
            } catch (Exception ignored) {

            }
        }
        return result;
    }

    /**
     * 获取是否全屏
     *
     * @return 是否全屏
     */
    public static boolean isFullScreen(Activity activity) {
        boolean ret = false;
        try {
            WindowManager.LayoutParams attrs = activity.getWindow().getAttributes();
            ret = (attrs.flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * API19之前透明状态栏：获取设置透明状态栏的system ui visibility的值，这是部分有提供接口的rom使用的
     * http://stackoverflow.com/questions/21865621/transparent-status-bar-before-4-4-kitkat
     */
    public static Integer getStatusBarAPITransparentValue(Context context) {
        if (sTransparentValue != null) {
            return sTransparentValue;
        }
        String[] systemSharedLibraryNames = context.getPackageManager()
                .getSystemSharedLibraryNames();
        String fieldName = null;
        for (String lib : systemSharedLibraryNames) {
            if ("touchwiz".equals(lib)) {
                fieldName = "SYSTEM_UI_FLAG_TRANSPARENT_BACKGROUND";
            } else if (lib.startsWith("com.sonyericsson.navigationbar")) {
                fieldName = "SYSTEM_UI_FLAG_TRANSPARENT";
            }
        }

        if (fieldName != null) {
            try {
                Field field = View.class.getField(fieldName);
                if (field != null) {
                    Class<?> type = field.getType();
                    if (type == int.class) {
                        sTransparentValue = field.getInt(null);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return sTransparentValue;
    }

    public static void setVirtualDensity(float density) {
        sVirtualDensity = density;
    }

    public static void setVirtualDensityDpi(float densityDpi) {
        sVirtualDensityDpi = densityDpi;
    }

    @IntDef({STATUSBAR_TYPE_DEFAULT, STATUSBAR_TYPE_MIUI, STATUSBAR_TYPE_FLYME, STATUSBAR_TYPE_ANDROID6})
    @Retention(RetentionPolicy.SOURCE)
    private @interface StatusBarType {
    }

}
