package com.hw.movie.ui;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;

import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.VersionInfo;
import com.arcsoft.face.enums.DetectFaceOrientPriority;
import com.arcsoft.face.enums.DetectMode;
import com.hw.movie.R;
import com.hw.movie.base.BaseActivity;
import com.hw.movie.common.Constants;
import com.hw.movie.faceserver.FaceServer;
import com.hw.movie.model.DrawInfo;
import com.hw.movie.model.FacePreviewInfo;
import com.hw.movie.util.ConfigUtil;
import com.hw.movie.util.DrawHelper;
import com.hw.movie.util.camera.CameraHelper;
import com.hw.movie.util.camera.CameraListener;
import com.hw.movie.util.face.FaceHelper;
import com.hw.movie.util.face.FaceListener;
import com.hw.movie.util.face.RecognizeColor;
import com.hw.movie.widget.FaceRectView;
import com.tencent.mmkv.MMKV;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.arcsoft.face.enums.DetectFaceOrientPriority.ASF_OP_270_ONLY;
import static com.arcsoft.face.enums.DetectFaceOrientPriority.ASF_OP_90_ONLY;
import static com.arcsoft.face.enums.DetectFaceOrientPriority.ASF_OP_ALL_OUT;

public class FaceRegisterActivity extends BaseActivity implements ViewTreeObserver.OnGlobalLayoutListener {
    private static final String TAG = "RegisterAndRecognize";
    private static final int MAX_DETECT_NUM = 10;
    /**
     * 注册人脸状态码，准备注册
     */
    private static final int REGISTER_STATUS_READY = 0;
    /**
     * 注册人脸状态码，注册中
     */
    private static final int REGISTER_STATUS_PROCESSING = 1;
    /**
     * 注册人脸状态码，注册结束（无论成功失败）
     */
    private static final int REGISTER_STATUS_DONE = 2;
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    /**
     * 所需的所有权限信息
     */
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE

    };
    private CameraHelper cameraHelper;
    private DrawHelper drawHelper;
    private Camera.Size previewSize;
    /**
     * 优先打开的摄像头，本界面主要用于单目RGB摄像头设备，因此默认打开前置
     */
    private Integer rgbCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
    /**
     * VIDEO模式人脸检测引擎，用于预览帧人脸追踪
     */
    private FaceEngine ftEngine;
    /**
     * 用于特征提取的引擎
     */
    private FaceEngine frEngine;
    /**
     * IMAGE模式活体检测引擎，用于预览帧人脸活体检测
     */
    private FaceEngine flEngine;
    private int ftInitCode = -1;
    private int frInitCode = -1;
    private int flInitCode = -1;
    private FaceHelper faceHelper;
    private ImageView ivBack;
    private int registerStatus = REGISTER_STATUS_DONE;
    /**
     * 相机预览显示的控件，可为SurfaceView或TextureView
     */
    private View previewView;
    /**
     * 绘制人脸框的控件
     */
    private FaceRectView faceRectView;
    private Integer liveness;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_face);
        //保持亮屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //全屏沉浸式状态栏和隐藏标题栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            getWindow().setAttributes(attributes);
        }

        // Activity启动后就锁定为启动时的方向
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        //本地人脸库初始化
        FaceServer.getInstance().init(this);
        ConfigUtil.setFtOrient(this, ASF_OP_270_ONLY);
        initView();
        initListener();
    }

    private void initView() {
        previewView = findViewById(R.id.single_camera_texture_preview);
        //在布局结束后才做初始化操作  布局绘制结束后会回调监听方法
        previewView.getViewTreeObserver().addOnGlobalLayoutListener(this);

        faceRectView = findViewById(R.id.single_camera_face_rect_view);
        ivBack = findViewById(R.id.iv_back);
    }

    private void initListener() {
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {

        if (cameraHelper != null) {
            cameraHelper.release();
            cameraHelper = null;
        }

        unInitEngine();
        if (faceHelper != null) {
            ConfigUtil.setTrackedFaceCount(this, faceHelper.getTrackedFaceCount());
            faceHelper.release();
            faceHelper = null;
        }

        FaceServer.getInstance().unInit();
        super.onDestroy();
    }

    /**
     * 销毁引擎，faceHelper中可能会有特征提取耗时操作仍在执行，加锁防止crash
     */
    private void unInitEngine() {
        if (ftInitCode == ErrorInfo.MOK && ftEngine != null) {
            synchronized (ftEngine) {
                int ftUnInitCode = ftEngine.unInit();
                Log.i(TAG, "unInitEngine: " + ftUnInitCode);
            }
        }
        if (frInitCode == ErrorInfo.MOK && frEngine != null) {
            synchronized (frEngine) {
                int frUnInitCode = frEngine.unInit();
                Log.i(TAG, "unInitEngine: " + frUnInitCode);
            }
        }
        if (flInitCode == ErrorInfo.MOK && flEngine != null) {
            synchronized (flEngine) {
                int flUnInitCode = flEngine.unInit();
                Log.i(TAG, "unInitEngine: " + flUnInitCode);
            }
        }
    }

    @Override
    public void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            if (isAllGranted) {
                initEngine();
                initCamera();
            } else {
                showToast(getString(R.string.permission_denied));
            }
        }
    }

    /**
     * 初始化引擎
     */
    private void initEngine() {
        ftEngine = new FaceEngine();
        ftInitCode = ftEngine.init(this, DetectMode.ASF_DETECT_MODE_VIDEO, ConfigUtil.getFtOrient(this),
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_DETECT);

        frEngine = new FaceEngine();
        frInitCode = frEngine.init(this, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_RECOGNITION);

        flEngine = new FaceEngine();
        flInitCode = flEngine.init(this, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                16, MAX_DETECT_NUM, FaceEngine.ASF_LIVENESS);


        VersionInfo versionInfo = new VersionInfo();
        ftEngine.getVersion(versionInfo);
        Log.i(TAG, "initEngine:  init: " + ftInitCode + "  version:" + versionInfo);

        if (ftInitCode != ErrorInfo.MOK) {
            String error = getString(R.string.specific_engine_init_failed, "ftEngine", ftInitCode);
            Log.i(TAG, "initEngine: " + error);
            showToast(error);
        }
        if (frInitCode != ErrorInfo.MOK) {
            String error = getString(R.string.specific_engine_init_failed, "frEngine", frInitCode);
            Log.i(TAG, "initEngine: " + error);
            showToast(error);
        }
        if (flInitCode != ErrorInfo.MOK) {
            String error = getString(R.string.specific_engine_init_failed, "flEngine", flInitCode);
            Log.i(TAG, "initEngine: " + error);
            showToast(error);
        }
    }

    private void initCamera() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        final FaceListener faceListener = new FaceListener() {
            @Override
            public void onFail(Exception e) {
                Log.e(TAG, "onFail: " + e.getMessage());
            }

            //请求FR的回调
            @Override
            public void onFaceFeatureInfoGet(@Nullable final FaceFeature faceFeature, final Integer requestId, final Integer errorCode) {
            }

            @Override
            public void onFaceLivenessInfoGet(@Nullable LivenessInfo livenessInfo, final Integer requestId, Integer errorCode) {
                if (livenessInfo != null) {
                    liveness = livenessInfo.getLiveness();
                }
//
            }


        };


        CameraListener cameraListener = new CameraListener() {
            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                Camera.Size lastPreviewSize = previewSize;
                previewSize = camera.getParameters().getPreviewSize();
                drawHelper = new DrawHelper(previewSize.width, previewSize.height, previewView.getWidth(), previewView.getHeight(), displayOrientation
                        , cameraId, isMirror, false, false);
                Log.i(TAG, "onCameraOpened: " + drawHelper.toString());
                // 切换相机的时候可能会导致预览尺寸发生变化
                if (faceHelper == null ||
                        lastPreviewSize == null ||
                        lastPreviewSize.width != previewSize.width || lastPreviewSize.height != previewSize.height) {
                    Integer trackedFaceCount = null;
                    // 记录切换时的人脸序号
                    if (faceHelper != null) {
                        trackedFaceCount = faceHelper.getTrackedFaceCount();
                        faceHelper.release();
                    }
                    faceHelper = new FaceHelper.Builder()
                            .ftEngine(ftEngine)
                            .frEngine(frEngine)
                            .flEngine(flEngine)
                            .frQueueSize(MAX_DETECT_NUM)
                            .flQueueSize(MAX_DETECT_NUM)
                            .previewSize(previewSize)
                            .faceListener(faceListener)
                            .trackedFaceCount(trackedFaceCount == null ? ConfigUtil.getTrackedFaceCount(FaceRegisterActivity.this.getApplicationContext()) : trackedFaceCount)
                            .build();
                }
            }


            @Override
            public void onPreview(final byte[] nv21, Camera camera) {
                if (faceRectView != null) {
                    faceRectView.clearFaceInfo();
                }
                List<FacePreviewInfo> facePreviewInfoList = faceHelper.onPreviewFrame(nv21);
                if (facePreviewInfoList != null && faceRectView != null && drawHelper != null) {
                    drawPreviewInfo(facePreviewInfoList);
                }
                registerFace(nv21, facePreviewInfoList);

            }

            @Override
            public void onCameraClosed() {
                Log.i(TAG, "onCameraClosed: ");
            }

            @Override
            public void onCameraError(Exception e) {
                Log.i(TAG, "onCameraError: " + e.getMessage());
            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
                if (drawHelper != null) {
                    drawHelper.setCameraDisplayOrientation(displayOrientation);
                }
                Log.i(TAG, "onCameraConfigurationChanged: " + cameraID + "  " + displayOrientation);
            }
        };

        cameraHelper = new CameraHelper.Builder()
                .previewViewSize(new Point(previewView.getMeasuredWidth(), previewView.getMeasuredHeight()))
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .specificCameraId(rgbCameraID != null ? rgbCameraID : Camera.CameraInfo.CAMERA_FACING_FRONT)
                .isMirror(false)
                .previewOn(previewView)
                .cameraListener(cameraListener)
                .build();
        cameraHelper.init();
        cameraHelper.start();
    }

    private void drawPreviewInfo(List<FacePreviewInfo> facePreviewInfoList) {
        List<DrawInfo> drawInfoList = new ArrayList<>();
        for (int i = 0; i < facePreviewInfoList.size(); i++) {
            String name = faceHelper.getName(facePreviewInfoList.get(i).getTrackId());
            int color = RecognizeColor.COLOR_SUCCESS;
            drawInfoList.add(new DrawInfo(drawHelper.adjustRect(facePreviewInfoList.get(i).getFaceInfo().getRect()),
                    GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE, liveness == null ? LivenessInfo.UNKNOWN : liveness, color,
                    name == null ? String.valueOf(facePreviewInfoList.get(i).getTrackId()) : name));
        }
        drawHelper.draw(faceRectView, drawInfoList);
    }

    private void registerFace(final byte[] nv21, final List<FacePreviewInfo> facePreviewInfoList) {
        if (registerStatus == REGISTER_STATUS_READY && facePreviewInfoList != null && facePreviewInfoList.size() > 0) {
            registerStatus = REGISTER_STATUS_PROCESSING;
            Observable.create(new ObservableOnSubscribe<Boolean>() {
                @Override
                public void subscribe(ObservableEmitter<Boolean> emitter) {

                    boolean success = FaceServer.getInstance().registerNv21(FaceRegisterActivity.this, nv21.clone(), previewSize.width, previewSize.height,
                            facePreviewInfoList.get(0).getFaceInfo(), "registered " + faceHelper.getTrackedFaceCount());
                    emitter.onNext(success);
                }
            })
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Boolean>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(Boolean success) {
                            String result = success ? "人脸注册成功" : "人脸注册失败";
                            showToast(result);
                            if (success) {
                                MMKV.defaultMMKV().encode(Constants.IS_FACE, true);
                                finish();
                            }


                            registerStatus = REGISTER_STATUS_DONE;
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            showToast("register failed!");
                            registerStatus = REGISTER_STATUS_DONE;
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }

    /**
     * 将准备注册的状态置为{@link #REGISTER_STATUS_READY}
     *
     * @param view 注册按钮
     */
    public void register(View view) {
        if (registerStatus == REGISTER_STATUS_DONE) {
            registerStatus = REGISTER_STATUS_READY;
        }
    }

    /**
     * 切换相机。注意：若切换相机发现检测不到人脸，则及有可能是检测角度导致的，需要销毁引擎重新创建或者在设置界面修改配置的检测角度
     *
     * @param view
     */
    public void switchCamera(View view) {
        if (cameraHelper != null) {
            boolean success = cameraHelper.switchCamera();
            if (!success) {
                showToast(getString(R.string.switch_camera_failed));
            } else {
                showLongToast(getString(R.string.notice_change_detect_degree));
            }
        }
    }

    /**
     * 在{@link #previewView}第一次布局完成后，去除该监听，并且进行引擎和相机的初始化
     */
    @Override
    public void onGlobalLayout() {
        previewView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
        } else {
            initEngine();
            initCamera();
        }
    }
}
