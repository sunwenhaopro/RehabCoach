package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.media.MediaPlayer;
import android.net.Uri;
import android.widget.MediaController;
import android.widget.VideoView;

import com.blankj.utilcode.util.PathUtils;
import com.blankj.utilcode.util.ResourceUtils;
import mmdeploy.*;

import org.opencv.android.*;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CamActivity extends CameraActivity {

    private static final String TAG = "OpencvCam";

    static {
        System.loadLibrary("opencv_java4");
    }

    private PoseTracker poseTracker;    // 姿势追踪器对象
    private JavaCamera2View javaCameraView;  // 相机视图对象
    private Button switchCameraBtn;     // 切换相机按钮
    private Button backBtn;             // 返回按钮
    public long stateHandle;            // 状态句柄
    private int cameraId = JavaCamera2View.CAMERA_ID_ANY;  // 相机ID
    private VideoView videoView;        // 视频视图
    private MediaController mediaController;  // 媒体控制器
    public static int newWidth = 500;          // 新的宽度 ----> 4:3
    public static int newHeight = 375;         // 新的高度
    private int frameSkipCounter = 0;
    private final int FRAME_SKIP_INTERVAL = 2; // 隔n帧处理一次
    private ArrayList<Mat> drawFrame = new ArrayList<>();  // 存储处理后的帧
    private int counter = -1;

    private CameraBridgeViewBase.CvCameraViewListener2 cvCameraViewListener2 = new CameraBridgeViewBase.CvCameraViewListener2() {
        @Override
        public void onCameraViewStarted(int width, int height) {
            try {
                stateHandle = initMMDeploy();  // 初始化MMDeploy
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Log.i(TAG, "onCameraViewStarted width=" + width + ", height=" + height);
        }

        @Override
        public void onCameraViewStopped() {
            Log.i(TAG, "onCameraViewStopped");
        }

        public Mat prepareFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
            Mat frame = inputFrame.rgba();
            Imgproc.resize(frame, frame, new org.opencv.core.Size(newWidth, newHeight));  // 调整帧大小
            Core.rotate(frame, frame, Core.ROTATE_90_CLOCKWISE); // 顺时针旋转90度
            if (cameraId == JavaCamera2View.CAMERA_ID_FRONT) {
                Core.flip(frame, frame, 0);  // 翻转帧，前置摄像头镜像显示
            }
            return frame;
        }

        // TODO: 可能还可以提升效果
        @Override
        public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
            if(counter >= 100000){
                drawFrame.clear();
                counter = -1;
            }
            if (frameSkipCounter < FRAME_SKIP_INTERVAL && counter != -1) {
                frameSkipCounter++;
            } else {
                frameSkipCounter = 0; // 重置计数器
                Mat frame = prepareFrame(inputFrame); // 返回已处理的帧

                Mat cvMat = new Mat();  // 使用 OpenCV 的 Mat，不需要完整的包名
                Imgproc.cvtColor(frame, cvMat, Imgproc.COLOR_RGB2BGR);  // 转换颜色空间
                mmdeploy.Mat mat = Utils.cvMatToMat(cvMat);  // 将 OpenCV 的 Mat 转换为 MMDeploy 的 Mat
                PoseTracker.Result[] results;

                try {
                    results = poseTracker.apply(stateHandle, mat, -1);  // 应用姿势追踪器
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                counter += 1;
                drawFrame.add(Draw.drawPoseTrackerResult(frame, results, newWidth));  // 在帧上绘制姿势追踪结果

                return drawFrame.get(counter);
            }
            // 如果跳过帧，直接返回上一帧的绘制结果
            return drawFrame.get(counter);
        }
    };

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            Log.i(TAG, "onManagerConnected status=" + status + ", javaCameraView=" + javaCameraView);
            if (status == LoaderCallbackInterface.SUCCESS && javaCameraView != null) {
                javaCameraView.setCvCameraViewListener(cvCameraViewListener2);
                javaCameraView.enableFpsMeter();
                javaCameraView.enableView();  // 启用相机视图
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        List<CameraBridgeViewBase> list = new ArrayList<>();
        list.add(javaCameraView);
        return list;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);  // 设置布局
        findView();  // 查找视图
        videoSet();  // 设置视频
        setListener();  // 设置监听器
    }

    public void videoSet() {
        String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.test;
        videoView.setVideoURI(Uri.parse(videoPath));  // 设置视频URI
        videoView.setOnPreparedListener(mediaPlayer -> videoView.start());  // 准备好后开始播放视频
    }

    public long initPoseTracker(String workDir) throws Exception {
        String detModelPath = workDir + "/rtmdet-nano-ncnn-fp16";
        String poseModelPath = workDir + "/rtmpose-tiny-ncnn-fp16";
        String deviceName = "cpu";
        int deviceID = 0;
        Model detModel = new Model(detModelPath);  // 创建检测模型
        Model poseModel = new Model(poseModelPath);  // 创建姿势模型
        Device device = new Device(deviceName, deviceID);  // 创建设备
        Context context = new Context();  // 创建上下文
        context.add(device);
        poseTracker = new mmdeploy.PoseTracker(detModel, poseModel, context);  // 创建姿势追踪器
        mmdeploy.PoseTracker.Params params = poseTracker.initParams();  // 初始化姿势追踪器参数
        params.detInterval = 5;
        params.poseMaxNumBboxes = 6;
        long stateHandle = poseTracker.createState(params);  // 创建姿势追踪状态
        return stateHandle;
    }

    public long initMMDeploy() throws Exception {
        String workDir = PathUtils.getExternalAppFilesPath() + File.separator + "file";
        if (ResourceUtils.copyFileFromAssets("models", workDir)) {  // 从资产复制文件
            return initPoseTracker(workDir);  // 初始化姿势追踪器
        }
        return -1;
    }

    private void findView() {
        javaCameraView = findViewById(R.id.javaCameraView);  // 查找相机视图
        switchCameraBtn = findViewById(R.id.switchCameraBtn);  // 查找切换相机按钮
        backBtn = findViewById(R.id.backBtn);  // 查找返回按钮
        videoView = findViewById(R.id.videoView);  // 查找视频视图
        mediaController = new MediaController(this);  // 创建媒体控制器
        videoView.setMediaController(mediaController);  // 设置媒体控制器
    }

    private void setListener() {
        switchCameraBtn.setOnClickListener(view -> {
            cameraId = (cameraId == JavaCamera2View.CAMERA_ID_FRONT) ?
                    JavaCamera2View.CAMERA_ID_BACK : JavaCamera2View.CAMERA_ID_FRONT;  // 切换相机
            Log.i(TAG, "cameraId : " + cameraId);
            javaCameraView.disableView();
            javaCameraView.setCameraIndex(cameraId);
            javaCameraView.enableView();  // 启用相机视图
        });

        backBtn.setOnClickListener(view -> {
            onPause();
            onBackPressed();  // 暂停并返回
        });
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        if (javaCameraView != null) {
            javaCameraView.disableView();  // 暂停相机视图
        }
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "initDebug true");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.i(TAG, "initDebug false");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        }
    }
}