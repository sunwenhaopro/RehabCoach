// TODO: ChatGPT improved
package com.example.myapplication;

import mmdeploy.PointF;
import org.opencv.core.*;
import org.opencv.imgproc.*;

/**
 * 绘制骨架和关键点的工具类
 */
public class Draw {

    // 骨架连接关系
    private static final int[][] SKELETON = {
            {15, 13}, {13, 11}, {16, 14}, {14, 12}, {11, 12}, {5, 11}, {6, 12},
            {5, 6}, {5, 7}, {6, 8}, {7, 9}, {8, 10}, {1, 2}, {0, 1},
            {0, 2}, {1, 3}, {2, 4}, {3, 5}, {4, 6}
    };

    // 调色板，用于绘制骨架和关键点
    private static final Scalar[] PALETTE = {
            new Scalar(255, 128, 0), new Scalar(255, 153, 51), new Scalar(255, 178, 102),
            new Scalar(230, 230, 0), new Scalar(255, 153, 255), new Scalar(153, 204, 255),
            new Scalar(255, 102, 255), new Scalar(255, 51, 255), new Scalar(102, 178, 255),
            new Scalar(51, 153, 255), new Scalar(255, 153, 153), new Scalar(255, 102, 102),
            new Scalar(255, 51, 51), new Scalar(153, 255, 153), new Scalar(102, 255, 102),
            new Scalar(51, 255, 51), new Scalar(0, 255, 0), new Scalar(0, 0, 255),
            new Scalar(255, 0, 0), new Scalar(255, 255, 255)
    };

    // 骨架连接线颜色
    private static final int[] LINK_COLOR = {
            0, 0, 0, 0, 7, 7, 7, 9, 9, 9, 9, 9, 16, 16, 16, 16, 16, 16, 16
    };

    // 关键点颜色
    private static final int[] POINT_COLOR = {
            16, 16, 16, 16, 16, 9, 9, 9, 9, 9, 9, 0, 0, 0, 0, 0, 0
    };


    /**
     * 绘制PoseTracker检测结果，包括骨架、关键点和包围框
     *
     * @param frame     原始图像
     * @param results   PoseTracker检测结果数组
     * @param newWidth  缩放后的宽度
     * @return 绘制了骨架、关键点和包围框的图像
     */
    public static Mat drawPoseTrackerResult(org.opencv.core.Mat frame, mmdeploy.PoseTracker.Result[] results, int newWidth) {
        // 计算缩放比例
        float scale = newWidth / (float) Math.max(frame.cols(), frame.rows());

        // 克隆原始图像，以免修改原始图像 -> 针对VIDEO功能
        Mat clonedFrame = frame.clone();

        // 遍历检测结果
        for (mmdeploy.PoseTracker.Result pt : results) {
            PointF[] keypoints = pt.keypoints;
            float scoreThr = 0.3f;
            int[] used = new int[keypoints.length * 2];

            // 绘制骨架连接线
            for (int j = 0; j < SKELETON.length; j++) {
                int u = SKELETON[j][0];
                int v = SKELETON[j][1];
                if (pt.scores[u] > scoreThr && pt.scores[v] > scoreThr) {
                    used[u] = used[v] = 1;
                    Point pointU = new Point(keypoints[u].x * scale, keypoints[u].y * scale);
                    Point pointV = new Point(keypoints[v].x * scale, keypoints[v].y * scale);
                    Imgproc.line(clonedFrame, pointU, pointV, PALETTE[LINK_COLOR[j]], 3);
                }
            }

            // 绘制关键点
            for (int j = 0; j < keypoints.length; j++) {
                if (used[j] == 1) {
                    Point p = new Point(keypoints[j].x * scale, keypoints[j].y * scale);
                    Imgproc.circle(clonedFrame, p, 1, PALETTE[POINT_COLOR[j]], 8);
                }
            }

            // 绘制包围框
            float bbox[] = {pt.bbox.left, pt.bbox.top, pt.bbox.right, pt.bbox.bottom};
            for (int j = 0; j < 4; j++) {
                bbox[j] *= scale;
            }
            Imgproc.rectangle(clonedFrame, new Point(bbox[0], bbox[1]),
                    new Point(bbox[2], bbox[3]), new Scalar(0, 255, 0), 2);
        }

        return clonedFrame;
    }

}

