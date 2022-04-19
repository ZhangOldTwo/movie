package com.hw.movie.ui.fragment;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.hw.movie.R;
import com.hw.movie.common.Constants;
import com.hw.movie.event.MessageEvent;
import com.hw.movie.ui.SetPatternLockerActivity;
import com.suke.widget.SwitchButton;
import com.tencent.mmkv.MMKV;

import org.greenrobot.eventbus.EventBus;

public class MyFragment extends Fragment {

    // 记录N次时间戳的数组
    long[] mHits = null;
    private SwitchButton sbGgesture,  sbHappy;
    private ImageView mineIcon;
    private LinearLayout llHappy;
    private boolean isLock;
    private boolean isHappy;
    private MMKV mmkv;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_my, container, false);
        sbGgesture = rootView.findViewById(R.id.switch_gesture);
        sbHappy = rootView.findViewById(R.id.switch_happy);
        llHappy = rootView.findViewById(R.id.ll_happy);
        mineIcon = rootView.findViewById(R.id.mine_icon);
        initView();
        initListener();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mmkv = MMKV.defaultMMKV();
        isLock = mmkv.decodeBool(Constants.IS_LOCK);
        isHappy = mmkv.decodeBool(Constants.IS_HAPPY, false);
        sbGgesture.setChecked(isLock);
        if (isHappy) {
            llHappy.setVisibility(View.VISIBLE);
            sbHappy.setChecked(isHappy);
        }

    }

    private void initView() {

    }

    private void initListener() {
        mineIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDisplayHappyButton();
            }
        });
        sbGgesture.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                if (isChecked && !isLock) {
                    showOpenLockerDialog();
                } else if (!isChecked && isLock) {
                    showCloseLockerDialog();
                }

            }
        });

        sbHappy.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                if (isChecked && !isHappy) {
                    showOpenHappyModelDialog();
                } else if (!isChecked && isHappy) {
                    showCloseHappyDialog();
                }
            }
        });
    }

    public void onDisplayHappyButton() {
        if (mHits == null) {
            mHits = new long[6];
        }
        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);//把从第二位至最后一位之间的数字复制到第一位至倒数第一位
        mHits[mHits.length - 1] = System.currentTimeMillis();//记录一个时间放到最后一位
        if (mHits[mHits.length - 1] - mHits[0] <= 1500) {//一秒内连续点击。
            mHits = null;    //这里说明一下，我们在进来以后需要还原状态，否则如果点击过快，第六次，第七次 都会不断进来触发该效果。重新开始计数即可
            showView(llHappy);
        }
    }

    private void showOpenLockerDialog() {
        final AlertDialog alertDialog2 = new AlertDialog.Builder(getActivity())
                .setTitle("提示")
                .setMessage("当前无手势动作，是否先去设置？")
                .setIcon(R.mipmap.icon)
                .setPositiveButton("是", new DialogInterface.OnClickListener() {//添加"Yes"按钮
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(new Intent(getActivity(), SetPatternLockerActivity.class));
                    }
                })

                .setNegativeButton("取消", new DialogInterface.OnClickListener() {//添加取消
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        sbGgesture.setChecked(false);
                    }
                })
                .create();
        alertDialog2.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    alertDialog2.dismiss();
                    sbGgesture.setChecked(false);
                }

                return true;
            }
        });
        alertDialog2.show();

    }

    private void showCloseLockerDialog() {
        final AlertDialog alertDialog2 = new AlertDialog.Builder(getActivity())
                .setTitle("提示")
                .setMessage("关闭后下次需重新设置手势动作，是否确认关闭手势操作？")
                .setIcon(R.mipmap.icon)
                .setPositiveButton("是", new DialogInterface.OnClickListener() {//添加"Yes"按钮
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mmkv.encode(Constants.IS_LOCK, false);
                        mmkv.encode(Constants.LOCK_PASSWORD, "");
                        isLock = false;
                    }
                })

                .setNegativeButton("取消", new DialogInterface.OnClickListener() {//添加取消
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        sbGgesture.setChecked(true);
                    }
                })
                .create();
        alertDialog2.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    alertDialog2.dismiss();
                    sbGgesture.setChecked(true);
                }
                return true;
            }
        });
        alertDialog2.show();

    }


    private void showOpenHappyModelDialog() {
        final AlertDialog alertDialog2 = new AlertDialog.Builder(getActivity())
                .setTitle("提示")
                .setMessage("是否开启快乐模式？")
                .setIcon(R.mipmap.icon)
                .setPositiveButton("是", new DialogInterface.OnClickListener() {//添加"Yes"按钮
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mmkv.encode(Constants.IS_HAPPY,true);
                        isHappy =true;
                        EventBus.getDefault().postSticky(new MessageEvent("开启快乐模式"));
                    }
                })

                .setNegativeButton("取消", new DialogInterface.OnClickListener() {//添加取消
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        sbHappy.setChecked(false);
                        hideView(llHappy);
                    }
                })
                .create();
        alertDialog2.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    alertDialog2.dismiss();
                    sbHappy.setChecked(false);
                    hideView(llHappy);
                }

                return true;
            }
        });
        alertDialog2.show();

    }

    private void showCloseHappyDialog() {
        final AlertDialog alertDialog2 = new AlertDialog.Builder(getActivity())
                .setTitle("提示")
                .setMessage("是否关闭快乐模式？")
                .setIcon(R.mipmap.icon)
                .setPositiveButton("是", new DialogInterface.OnClickListener() {//添加"Yes"按钮
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        sbHappy.setChecked(false);
                        mmkv.encode(Constants.IS_HAPPY, false);
                        isHappy = false;
                        EventBus.getDefault().postSticky(new MessageEvent("关闭快乐模式"));
                        hideView(llHappy);
                    }
                })

                .setNegativeButton("取消", new DialogInterface.OnClickListener() {//添加取消
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        sbHappy.setChecked(true);
                    }
                })
                .create();
        alertDialog2.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    alertDialog2.dismiss();
                    sbHappy.setChecked(true);
                }
                return true;
            }
        });
        alertDialog2.show();
    }

    private void showView(View view) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        objectAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                llHappy.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        objectAnimator.setDuration(1500);
        objectAnimator.start();
    }

    private void hideView(View view) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        objectAnimator.setStartDelay(500);
        objectAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                llHappy.setVisibility(View.GONE);


            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        objectAnimator.setDuration(1500);
        objectAnimator.start();
    }
}

