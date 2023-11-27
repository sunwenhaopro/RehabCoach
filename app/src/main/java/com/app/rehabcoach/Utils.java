package com.app.rehabcoach;

import mmdeploy.*;

public class Utils {
    /**
     * 将OpenCV的Mat对象转换为Mat对象
     *
     * @param cvMat OpenCV的Mat对象
     * @return 转换后的Mat对象
     */
    public static Mat cvMatToMat(org.opencv.core.Mat cvMat) {
        // 获取Mat的行数、列数和通道数
        int rows = cvMat.rows();
        int cols = cvMat.cols();
        int channels = cvMat.channels();

        // 计算数据大小（字节数）
        int dataSize = rows * cols * channels * (int) cvMat.elemSize();

        // 创建一个字节数组，用于保存Mat数据
        byte[] dataPointer = new byte[dataSize];

        // 从cvMat中复制数据到dataPointer数组中
        cvMat.get(0, 0, dataPointer);

        // 创建一个新的Mat对象，并使用转换后的数据初始化它
        Mat mat = new Mat(rows, cols, channels, PixelFormat.BGR, DataType.INT8, dataPointer);

        return mat;
    }
}
