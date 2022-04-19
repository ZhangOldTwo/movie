package com.hw.movie.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ihsg.patternlocker.OnPatternChangeListener;
import com.github.ihsg.patternlocker.PatternIndicatorView;
import com.github.ihsg.patternlocker.PatternLockerView;
import com.hw.movie.R;
import com.hw.movie.base.BaseActivity;
import com.hw.movie.common.Constants;
import com.tencent.mmkv.MMKV;

import java.util.ArrayList;
import java.util.List;

public class SetPatternLockerActivity extends BaseActivity implements View.OnClickListener {
    private String TAG = getClass().getSimpleName();
    private PatternIndicatorView patternIndicatorView;
    private PatternLockerView patternLockView;
    private TextView tvHint, tvConfirm, tvRestart;
    private LinearLayout llConfirm;
    private List<Integer> mList = new ArrayList<>();
    private boolean isFirstDraw = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setpatternlock);
        initViews();
        initListener();
        initData();
    }


    public void initViews() {
        patternIndicatorView = findViewById(R.id.pattern_indicator_view);
        patternLockView = findViewById(R.id.pattern_lock_view);
        tvHint = findViewById(R.id.tv_hint);
        tvConfirm = findViewById(R.id.tv_confirm);
        tvRestart = findViewById(R.id.tv_restart);
        llConfirm = findViewById(R.id.ll_confirm);
    }

    public void initListener() {
        tvConfirm.setOnClickListener(this);
        tvRestart.setOnClickListener(this);
    }

    public void initData() {
        patternLockView.setOnPatternChangedListener(new OnPatternChangeListener() {
            @Override
            public void onChange(PatternLockerView patternLockerView, List<Integer> list) {
                Log.d(TAG, "onChange");
            }

            @Override
            public void onClear(PatternLockerView patternLockerView) {
                Log.d(TAG, "onClear");
                patternIndicatorView.updateState(new ArrayList<Integer>(), false);
            }

            @Override
            public void onComplete(PatternLockerView patternLockerView, List<Integer> list) {
                if (list.size() < 4) {
                    patternLockView.updateStatus(true);
                    patternIndicatorView.updateState(list, true);
                    patternLockerView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            patternLockView.clearHitState();
                        }
                    }, 1000);

                    Toast.makeText(SetPatternLockerActivity.this, "请至少连接4个点", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (isFirstDraw) {
                    mList.clear();
                    mList.addAll(list);
                    llConfirm.setVisibility(View.VISIBLE);
                    tvConfirm.setEnabled(false);
                    isFirstDraw = false;
                    patternLockerView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            patternLockView.clearHitState();
                        }
                    }, 1000);

                    tvHint.setText("再次绘制图案");
                } else {
                    if (mList.equals(list)) {
                        tvHint.setText("确认使用此图案解锁？");
                        tvConfirm.setEnabled(true);

                    } else {
//                        oldList.clear();
                        patternLockView.updateStatus(true);
                        patternIndicatorView.updateState(list, true);
                        patternLockerView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                patternLockView.clearHitState();
                            }
                        }, 1000);

                        tvHint.setText("再次绘制图案");
                    }
                }
                Log.d(TAG, "onComplete");

            }

            @Override
            public void onStart(PatternLockerView patternLockerView) {
                Log.d(TAG, "onStart");
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_confirm:
                MMKV kv = MMKV.defaultMMKV();
                kv.encode(Constants.IS_LOCK, true);
                String string = "";
                for (int i = 0; i < mList.size(); i++) {
                    string += mList.get(i);
                }
                kv.encode(Constants.LOCK_PASSWORD, string);
                finish();
                break;
            case R.id.tv_restart:
                mList.clear();
                isFirstDraw = true;
                patternLockView.clearHitState();
                tvHint.setText("绘制解锁图案，请至少连接4个点");
                break;
            default:
                break;
        }

    }

    @Override
    public void afterRequestPermission(int requestCode, boolean isAllGranted) {

    }
}
