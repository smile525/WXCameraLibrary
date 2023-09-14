package com.smile525.albumcamerarecorder.camera.ui.camera.impl;

import android.graphics.Bitmap;

import com.smile525.albumcamerarecorder.camera.entity.BitmapData;
import com.smile525.common.entity.LocalFile;
import com.smile525.common.utils.ThreadUtils;
import com.smile525.albumcamerarecorder.camera.entity.BitmapData;
import com.smile525.common.entity.LocalFile;
import com.smile525.common.utils.ThreadUtils;

import java.util.ArrayList;

/**
 * 拍摄界面的有关图片View的接口
 * 控制多图Adapter也是在这里实现
 *
 * @author zhongjh
 * @date 2022/8/23
 */
public interface ICameraPicture {

    /**
     * 初始化有关图片的配置数据
     */
    void initData();

    /**
     * 初始化多图适配器
     */
    void initMultiplePhotoAdapter();

    /**
     * 生命周期onDestroy
     *
     * @param isCommit 是否提交了数据,如果不是提交则要删除冗余文件
     */
    void onDestroy(boolean isCommit);

    /**
     * 拍照
     */
    void takePhoto();

    /**
     * 添加入数据源
     *
     * @param bitmap bitmap
     */
    void addCaptureData(Bitmap bitmap);

    /**
     * 刷新多个图片
     *
     * @param bitmapDatas 最新的多图数据源
     */
    void refreshMultiPhoto(ArrayList<BitmapData> bitmapDatas);

    /**
     * 返回迁移图片的线程
     * @return 迁移图片的线程
     */
    ThreadUtils.SimpleTask<ArrayList<LocalFile>> getMovePictureFileTask();

    /**
     * 删除临时图片
     */
    void deletePhotoFile();

    /**
     * 清除数据源
     */
    void clearBitmapDatas();

    /**
     * 停止迁移图片的线程运行
     */
    void cancelMovePictureFileTask();
}
