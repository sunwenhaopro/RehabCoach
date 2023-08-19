package com.example.myapplication;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import com.blankj.utilcode.util.PathUtils;
import com.blankj.utilcode.util.ResourceUtils;

import mmdeploy.Context;
import mmdeploy.Device;
import mmdeploy.Model;
import mmdeploy.PoseTracker;

import java.io.File;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private ImageView videoFrameView;
    // 姿势跟踪器及其相关变量
    private PoseTracker poseTracker;
    private long stateHandle;
    private int frameID;
    private VideoCapture videoCapture;

    // 加载OpenCV本地库
    static {
        System.loadLibrary("opencv_java4");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // 初始化模型部署
            stateHandle = initMMDeploy();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        setContentView(R.layout.activity_main);
        initializeViews(); // 初始化UI元素
    }

    // 初始化UI元素
    private void initializeViews() {
        // 声明UI元素
        Button addVideoButton = findViewById(R.id.addVideoButton);
        videoFrameView = findViewById(R.id.videoFrameView);

        // 为添加视频按钮添加点击监听器
        addVideoButton.setOnClickListener(v -> checkAndOpenAlbum());

        Button addCameraButton = findViewById(R.id.addCameraButton);
        // 为添加相机按钮添加点击监听器
        addCameraButton.setOnClickListener(v -> startCamActivity());
    }

    // 检查权限并打开相册
    private void checkAndOpenAlbum() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            openAlbum();
        }
    }

    // 打开相册
    private void openAlbum() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        startActivityForResult(intent, 2);
    }

    // 启动相机活动
    private void startCamActivity() {
        Intent intent = new Intent(this, CamActivity.class);
        startActivity(intent);
    }

    // 初始化姿势跟踪器
    private long initPoseTracker(String workDir) throws Exception {
        String detModelPath = workDir + "/rtmdet-nano-ncnn-fp16";
        String poseModelPath = workDir + "/rtmpose-tiny-ncnn-fp16";
        String deviceName = "cpu";
        int deviceID = 0;

        Model detModel = new Model(detModelPath);
        Model poseModel = new Model(poseModelPath);
        Device device = new Device(deviceName, deviceID);
        Context context = new Context();
        context.add(device);

        poseTracker = new PoseTracker(detModel, poseModel, context);

        PoseTracker.Params params = poseTracker.initParams();
        params.detInterval = 5;
        params.poseMaxNumBboxes = 6;

        return poseTracker.createState(params);
    }

    // 初始化模型部署
    private long initMMDeploy() throws Exception {
        String workDir = PathUtils.getExternalAppFilesPath() + File.separator + "file";

        if (ResourceUtils.copyFileFromAssets("models", workDir)) {
            return initPoseTracker(workDir);
        }
        return -1;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openAlbum();
            } else {
                Toast.makeText(MainActivity.this, "拒绝访问存储权限。", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2) {
            Uri uri = Objects.requireNonNull(data).getData();
            String path = getPathFromUri(uri);

            if (path != null) {
                // 打开选定视频
                videoCapture = new VideoCapture(path, Videoio.CAP_ANDROID);
                if (!videoCapture.isOpened()) {
                    System.out.printf("无法打开视频：%s", path);
                }
            }

            double fps = videoCapture.get(Videoio.CAP_PROP_FPS);
            frameID = 0;
            Handler handler = new Handler();
            Runnable drawThread = new Runnable() {
                @Override
                public void run() {
                    Mat frame = new Mat();
                    videoCapture.read(frame);
//                    System.out.printf("处理帧 %d\n", frameID);
                    if (frame.empty()) {
                        return;
                    }
                    Imgproc.resize(frame, frame, new org.opencv.core.Size(600, 800));  // 调整帧大小
                    Mat cvMat = new Mat();
                    Imgproc.cvtColor(frame, cvMat, Imgproc.COLOR_RGB2BGR);
                    mmdeploy.Mat mat = Utils.cvMatToMat(cvMat);
                    if (stateHandle == -1) {
//                        System.out.println("状态创建失败！");
                        return;
                    }
                    PoseTracker.Result[] results = new PoseTracker.Result[0];
                    try {
                        results = poseTracker.apply(stateHandle, mat, -1);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    // TODO: fix bug
                    Mat clonedFrame = Draw.drawPoseTrackerResult(frame, results, 600);
                    Bitmap bitmap = Bitmap.createBitmap(clonedFrame.width(), clonedFrame.height(), Bitmap.Config.ARGB_8888);
                    org.opencv.android.Utils.matToBitmap(clonedFrame, bitmap);
                    videoFrameView.setImageBitmap(bitmap);
                    videoFrameView.invalidate();
                    handler.postDelayed(this, (long) (1000 / fps));
                    frameID++;
                }
            };
            handler.post(drawThread);
        }
    }

    // 从Uri获取文件路径
    private String getPathFromUri(Uri uri) {
        String path = null;
        String[] projection = {MediaStore.Images.Media.DATA};

        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                path = cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return path;
    }
}

