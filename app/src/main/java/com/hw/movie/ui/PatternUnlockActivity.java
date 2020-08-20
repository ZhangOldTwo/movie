package com.hw.movie.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.github.ihsg.patternlocker.OnPatternChangeListener;
import com.github.ihsg.patternlocker.PatternIndicatorView;
import com.github.ihsg.patternlocker.PatternLockerView;
import com.hw.movie.R;
import com.hw.movie.common.Constants;
import com.hw.movie.base.BaseActivity;
import com.tencent.mmkv.MMKV;

import java.util.List;

public class PatternUnlockActivity extends BaseActivity {
    private String TAG = getClass().getSimpleName();
    private PatternIndicatorView unlockPatternIndicatorView;
    private PatternLockerView unlockPatternLockView;
    private MMKV mmkv;
    private String password;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patternunlock);
        initViews();
        initListener();
        initData();
    }

    public void initViews() {
        unlockPatternIndicatorView = findViewById(R.id.unlock_pattern_indicator_view);
        unlockPatternLockView = findViewById(R.id.unlock_pattern_lock_view);
        mmkv = MMKV.defaultMMKV();
        password = mmkv.decodeString(Constants.LOCK_PASSWORD, "");
    }

    public void initListener() {
    }


    public void initData() {
        unlockPatternLockView.setOnPatternChangedListener(new OnPatternChangeListener() {
            @Override
            public void onChange(PatternLockerView patternLockerView, List<Integer> list) {
                Log.d(TAG, "onChange");
            }

            @Override
            public void onClear(PatternLockerView patternLockerView) {
                Log.d(TAG, "onClear");
            }

            @Override
            public void onComplete(PatternLockerView patternLockerView, List<Integer> list) {
                if (list.size() < 4) {
                    unlockPatternLockView.updateStatus(true);
//                    unlockPatternIndicatorView.updateState(list, true);
                    Toast.makeText(PatternUnlockActivity.this, "请至少连接4个点", Toast.LENGTH_SHORT).show();
                    return;
                }

                String string = "";
                for (int i = 0; i < list.size(); i++) {
                    string += list.get(i);
                }

                if (string.equals(password)) {
                    if (mmkv.decodeBool(Constants.IS_FACE, false)) {
                        startActivity(new Intent(PatternUnlockActivity.this, FaceLoginActivity.class));
                        finish();
                    }else {
                        startActivity(new Intent(PatternUnlockActivity.this, MainActivity.class));
                        PatternUnlockActivity.this.finish();
                    }
                } else {
//                        oldList.clear();
                    unlockPatternLockView.updateStatus(true);
//                    unlockPatternIndicatorView.updateState(list, true);
                }
            }


            @Override
            public void onStart(PatternLockerView patternLockerView) {
                Log.d(TAG, "onStart");
            }
        });
    }


    @Override
    public void afterRequestPermission(int requestCode, boolean isAllGranted) {

    }
}
