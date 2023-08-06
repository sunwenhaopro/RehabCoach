package com.example.myapplication;

import mmdeploy.*;



public class Utils {


    public static Mat cvMatToMat(org.opencv.core.Mat cvMat)
    {
        byte[] dataPointer = new byte[cvMat.rows() * cvMat.cols() * cvMat.channels() * (int)cvMat.elemSize()];
        cvMat.get(0, 0, dataPointer);
        return new Mat(cvMat.rows(), cvMat.cols(), cvMat.channels(),
                PixelFormat.BGR, DataType.INT8, dataPointer);
    }




}