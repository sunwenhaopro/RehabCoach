package com.example.myapplication;


import android.os.Bundle;

import android.util.Log;
import android.widget.Button;

import com.blankj.utilcode.util.PathUtils;
import com.blankj.utilcode.util.ResourceUtils;
import mmdeploy.Context;
import mmdeploy.Device;
import mmdeploy.Model;
import mmdeploy.PoseTracker;
import org.opencv.android.*;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CamActivity extends CameraActivity {
    static {
        System.loadLibrary("opencv_java4");
    }

    private static final String TAG = "OpencvCam";
    public VideoCapture videoCapture;
    private PoseTracker poseTracker;
    private JavaCamera2View javaCameraView;
    private Button switchCameraBtn;
    public long stateHandle;
    private int cameraId = JavaCamera2View.CAMERA_ID_ANY;

    private CameraBridgeViewBase.CvCameraViewListener2 cvCameraViewListener2 = new CameraBridgeViewBase.CvCameraViewListener2() {
        @Override
        public void onCameraViewStarted(int width, int height) {
            try {
                stateHandle = initMMDeploy();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Log.i(TAG, "onCameraViewStarted width=" + width + ", height=" + height);
        }

        @Override
        public void onCameraViewStopped() {
            Log.i(TAG, "onCameraViewStopped");
        }

        @Override
        public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                org.opencv.core.Mat frame =inputFrame.rgba();
                org.opencv.core.Mat cvMat = new org.opencv.core.Mat();
                Imgproc.cvtColor(frame, cvMat, Imgproc.COLOR_RGB2BGR);
                mmdeploy.Mat mat = Utils.cvMatToMat(cvMat);
                PoseTracker.Result[] results = new PoseTracker.Result[0];
                try {
                    results = poseTracker.apply(stateHandle, mat, -1);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            return Draw.drawPoseTrackerResult(frame, results);
        }
    };

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            Log.i(TAG, "onManagerConnected status=" + status + ", javaCameraView=" + javaCameraView);
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    if (javaCameraView != null) {
                        javaCameraView.setCvCameraViewListener(cvCameraViewListener2);
                        // 禁用帧率显示
                        javaCameraView.enableFpsMeter();
                        javaCameraView.enableView();
                    }
                }
                break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        Log.i(TAG, "getCameraViewList");
        List<CameraBridgeViewBase> list = new ArrayList<>();
        list.add(javaCameraView);
        return list;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        findView();
        setListener();
    }

    public long initPoseTracker(String workDir) throws Exception {
        String detModelPath = workDir + "/rtmdet-nano-ncnn-fp16";
        String poseModelPath = workDir + "/rtmpose-tiny-ncnn-fp16";
        String deviceName = "cpu";
        int deviceID = 0;
        Model detModel = new Model(detModelPath);
        Model poseModel = new Model(poseModelPath);
        Device device = new Device(deviceName, deviceID);
        Context context = new Context();
        context.add(device);
        poseTracker = new mmdeploy.PoseTracker(detModel, poseModel, context);
        mmdeploy.PoseTracker.Params params = poseTracker.initParams();
        params.detInterval = 5;
        params.poseMaxNumBboxes = 6;
        long stateHandle = poseTracker.createState(params);
        return stateHandle;
    }

    public long initMMDeploy() throws Exception {
        String workDir = PathUtils.getExternalAppFilesPath() + File.separator
                + "file";
        if (ResourceUtils.copyFileFromAssets("models", workDir)) {
            return initPoseTracker(workDir);
        }
        return -1;
    }

    private void findView() {
        javaCameraView = findViewById(R.id.javaCameraView);
        switchCameraBtn = findViewById(R.id.switchCameraBtn);
    }

    private void setListener() {
        switchCameraBtn.setOnClickListener(view -> {
            switch (cameraId) {
                case JavaCamera2View.CAMERA_ID_ANY:
                case JavaCamera2View.CAMERA_ID_BACK:
                    cameraId = JavaCamera2View.CAMERA_ID_FRONT;
                    break;
                case JavaCamera2View.CAMERA_ID_FRONT:
                    cameraId = JavaCamera2View.CAMERA_ID_BACK;
                    break;
            }
            Log.i(TAG, "cameraId : " + cameraId);
            javaCameraView.disableView();
            javaCameraView.setCameraIndex(cameraId);
            javaCameraView.enableView();
        });
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        if (javaCameraView != null) {
            javaCameraView.disableView();
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