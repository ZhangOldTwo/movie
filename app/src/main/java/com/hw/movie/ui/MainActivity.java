package com.hw.movie.ui;

import android.Manifest;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.hw.movie.R;
import com.hw.movie.adapter.MyFragmentPagerAdapter;
import com.hw.movie.base.BaseActivity;
import com.hw.movie.tools.StatusBarHelper;
import com.hw.movie.ui.fragment.HomeFragment;
import com.hw.movie.ui.fragment.MyFragment;

import java.util.ArrayList;
import java.util.List;

import static com.hw.movie.common.Constants.APP_ID;
import static com.hw.movie.common.Constants.SDK_KEY;

public class MainActivity extends BaseActivity {
    private ViewPager viewPager;
    private RadioGroup radioGroup;
    private RadioButton rbHome, rbMy;
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_PHONE_STATE
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, 666);
        }else {
            int code = FaceEngine.activeOnline(this, APP_ID, SDK_KEY);
            if(code == ErrorInfo.MOK){
                Log.i(getClass().getSimpleName(), "activeOnline success");
            }else if(code == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED){
                Log.i(getClass().getSimpleName(), "already activated");
            }else{
                Log.i(getClass().getSimpleName(), "activeOnline failed, code is : " + code);
            }
        }



    }

    private void initView() {
        /**
         * RadioGroup部分
         */
        radioGroup = findViewById(R.id.radioGroup);
        rbHome = findViewById(R.id.rb_home);
        rbMy = findViewById(R.id.rb_my);
        //RadioGroup选中状态改变监听
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_home:
                        /**
                         * setCurrentItem第二个参数控制页面切换动画
                         * true:打开/false:关闭
                         */
                        viewPager.setCurrentItem(0, false);
                        StatusBarHelper.setStatusBarColor(MainActivity.this,getResources().getColor(R.color.colorPrimary));
                        break;
                    case R.id.rb_my:
                        viewPager.setCurrentItem(1, false);
                        StatusBarHelper.translucentStatusBar(MainActivity.this, true);
                        break;
                    default:
                        break;
                }
            }
        });

        /**
         * ViewPager部分
         */
        viewPager = findViewById(R.id.viewPager);
        HomeFragment homeFragment = new HomeFragment();
        MyFragment myFragment = new MyFragment();

        List<Fragment> alFragment = new ArrayList<Fragment>();
        alFragment.add(homeFragment);
        alFragment.add(myFragment);
        //ViewPager设置适配器
        viewPager.setAdapter(new MyFragmentPagerAdapter(getSupportFragmentManager(), alFragment));
        //ViewPager显示第一个Fragment
        viewPager.setCurrentItem(0);
        //ViewPager页面切换监听
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        radioGroup.check(R.id.rb_home);
                        StatusBarHelper.setStatusBarColor(MainActivity.this,getResources().getColor(R.color.colorPrimary));
                        break;
                    case 1:
                        radioGroup.check(R.id.rb_my);
                        StatusBarHelper.translucentStatusBar(MainActivity.this, true);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    public void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (requestCode == 666) {
            if (isAllGranted) {
                int code = FaceEngine.activeOnline(this, APP_ID, SDK_KEY);
                if(code == ErrorInfo.MOK){
                    Log.i(getClass().getSimpleName(), "activeOnline success");
                }else if(code == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED){
                    Log.i(getClass().getSimpleName(), "already activated");
                }else{
                    Log.i(getClass().getSimpleName(), "activeOnline failed, code is : " + code);
                }
            } else {
                showToast(getString(R.string.permission_denied));
            }
        }
    }
}
