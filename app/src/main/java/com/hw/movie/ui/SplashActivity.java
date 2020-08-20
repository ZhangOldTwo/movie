package com.hw.movie.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.hw.movie.R;
import com.hw.movie.common.Constants;
import com.hw.movie.base.BaseActivity;
import com.hw.movie.tools.StatusBarHelper;
import com.tencent.mmkv.MMKV;

/**
 * Created by Administrator on 2018/2/27.
 */

public class SplashActivity extends BaseActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_splash);
        initViews();
        initListener();
        initData();

    }

    public void initViews() {
        StatusBarHelper.translucentStatusBar(this, true);
        findViewById(R.id.rl).postDelayed(new Runnable() {
            @Override
            public void run() {
                MMKV kv = MMKV.defaultMMKV();
                if (kv.decodeBool(Constants.IS_LOCK, false)) {
                    startActivity(new Intent(SplashActivity.this, PatternUnlockActivity.class));
                    finish();
                } else if (kv.decodeBool(Constants.IS_FACE, false)) {
                    startActivity(new Intent(SplashActivity.this, FaceLoginActivity.class));
                    finish();
                } else {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                }
            }
        }, 1500);


    }

    public void initListener() {

    }

    public void initData() {

    }

    @Override
    public void afterRequestPermission(int requestCode, boolean isAllGranted) {

    }
}
