package com.smile525.albumcamerarecorder.camera.ui.camera.presenter;

import static android.app.Activity.RESULT_OK;
import static com.smile525.albumcamerarecorder.camera.constants.FlashModels.TYPE_FLASH_AUTO;
import static com.smile525.albumcamerarecorder.utils.MediaStoreUtils.MediaTypes.TYPE_PICTURE;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.otaliastudios.cameraview.controls.Flash;
import com.smile525.albumcamerarecorder.camera.constants.FlashModels;
import com.smile525.albumcamerarecorder.camera.entity.BitmapData;
import com.smile525.albumcamerarecorder.camera.ui.camera.impl.ICameraPicture;
import com.smile525.albumcamerarecorder.camera.ui.camera.state.CameraStateManagement;
import com.smile525.albumcamerarecorder.utils.MediaStoreUtils;
import com.smile525.albumcamerarecorder.utils.SelectableUtils;
import com.smile525.common.entity.LocalFile;
import com.smile525.common.enums.MimeType;
import com.smile525.common.utils.MediaStoreCompat;
import com.smile525.common.utils.ThreadUtils;
import com.smile525.albumcamerarecorder.R;
import com.smile525.albumcamerarecorder.camera.ui.camera.adapter.PhotoAdapter;
import com.smile525.albumcamerarecorder.camera.ui.camera.adapter.PhotoAdapterListener;
import com.smile525.albumcamerarecorder.camera.entity.BitmapData;
import com.smile525.albumcamerarecorder.camera.ui.camera.BaseCameraFragment;
import com.smile525.albumcamerarecorder.camera.ui.camera.impl.ICameraPicture;
import com.smile525.albumcamerarecorder.camera.ui.camera.state.CameraStateManagement;
import com.smile525.albumcamerarecorder.camera.util.FileUtil;
import com.smile525.albumcamerarecorder.camera.util.LogUtil;
import com.smile525.albumcamerarecorder.utils.MediaStoreUtils;
import com.smile525.albumcamerarecorder.utils.SelectableUtils;
import com.smile525.common.entity.LocalFile;
import com.smile525.common.enums.MimeType;
import com.smile525.common.utils.MediaStoreCompat;
import com.smile525.common.utils.ThreadUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 这是专门负责图片的有关逻辑
 * 涉及到图片的拍照、编辑图片、提交图片、多图系列等等都是该类负责
 *
 * @author zhongjh
 * @date 2022/8/22
 */
public class BaseCameraPicturePresenter
        implements PhotoAdapterListener, ICameraPicture {

    public BaseCameraPicturePresenter(
            BaseCameraFragment<? extends CameraStateManagement,
                    ? extends BaseCameraPicturePresenter,
                    ? extends BaseCameraVideoPresenter> baseCameraFragment) {
        this.baseCameraFragment = baseCameraFragment;
    }

    /**
     * cameraFragment
     */
    protected BaseCameraFragment<? extends CameraStateManagement,
            ? extends BaseCameraPicturePresenter,
            ? extends BaseCameraVideoPresenter> baseCameraFragment;
    /**
     * 从编辑图片界面回来
     */
    private ActivityResultLauncher<Intent> imageEditActivityResult;
    /**
     * 拍照的多图片集合适配器
     */
    private PhotoAdapter photoAdapter;
    /**
     * 图片,单图或者多图都会加入该列表
     */
    List<BitmapData> bitmapData = new ArrayList<>();
    /**
     * 照片File,用于后面能随时删除
     */
    private File photoFile;
    /**
     * 编辑后的照片
     */
    private File photoEditFile;
    /**
     * 照片Uri,作用于单图
     */
    private Uri singlePhotoUri;
    /**
     * 图片的文件操作
     */
    private MediaStoreCompat pictureMediaStoreCompat;
    /**
     * 延迟拍摄，用于打开闪光灯再拍摄
     */
    private final Handler cameraTakePictureHandler = new Handler(Looper.getMainLooper());
    private final Runnable cameraTakePictureRunnable = new Runnable() {
        @Override
        public void run() {
            if (baseCameraFragment.getCameraSpec().getEnableImageHighDefinition()) {
                baseCameraFragment.getCameraView().takePicture();
            } else {
                baseCameraFragment.getCameraView().takePictureSnapshot();
            }
        }
    };
    /**
     * 一个迁移图片的异步线程
     */
    public ThreadUtils.SimpleTask<ArrayList<LocalFile>> movePictureFileTask;

    /**
     * 初始化有关图片的配置数据
     */
    @Override
    public void initData() {
        // 设置图片路径
        if (baseCameraFragment.getGlobalSpec().getPictureStrategy() != null) {
            // 如果设置了视频的文件夹路径，就使用它的
            pictureMediaStoreCompat = new MediaStoreCompat(baseCameraFragment.getMyContext(), baseCameraFragment.getGlobalSpec().getPictureStrategy());
        } else {
            // 否则使用全局的
            if (baseCameraFragment.getGlobalSpec().getSaveStrategy() == null) {
                throw new RuntimeException("Don't forget to set SaveStrategy.");
            } else {
                pictureMediaStoreCompat = new MediaStoreCompat(baseCameraFragment.getMyContext(), baseCameraFragment.getGlobalSpec().getSaveStrategy());
            }
        }
    }

    /**
     * 初始化多图适配器
     */
    @Override
    public void initMultiplePhotoAdapter() {
        // 初始化多图适配器，先判断是不是多图配置
        photoAdapter = new PhotoAdapter(baseCameraFragment.getMainActivity(), baseCameraFragment.getGlobalSpec(), bitmapData, this);
        if (baseCameraFragment.getRecyclerViewPhoto() != null) {
            if (SelectableUtils.getImageMaxCount() > 1) {
                baseCameraFragment.getRecyclerViewPhoto().setLayoutManager(new LinearLayoutManager(baseCameraFragment.getMyContext(), RecyclerView.HORIZONTAL, false));
                baseCameraFragment.getRecyclerViewPhoto().setAdapter(photoAdapter);
                baseCameraFragment.getRecyclerViewPhoto().setVisibility(View.VISIBLE);
            } else {
                baseCameraFragment.getRecyclerViewPhoto().setVisibility(View.GONE);
            }
        }
    }

    /**
     * 生命周期onDestroy
     *
     * @param isCommit 是否提交了数据,如果不是提交则要删除冗余文件
     */
    @Override
    public void onDestroy(boolean isCommit) {
        LogUtil.i("BaseCameraPicturePresenter destroy");
        if (!isCommit) {
            if (photoFile != null) {
                // 删除图片
                FileUtil.deleteFile(photoFile);
            }
            // 删除多个图片
            if (photoAdapter.getListData() != null) {
                for (BitmapData bitmapData : photoAdapter.getListData()) {
                    FileUtil.deleteFile(bitmapData.getPath());
                }
            }
        }
        cameraTakePictureHandler.removeCallbacks(cameraTakePictureRunnable);
        if (movePictureFileTask != null) {
            movePictureFileTask.cancel();
        }
    }

    /**
     * 拍照
     */
    @Override
    public void takePhoto() {
        // 开启才能执行别的事件, 如果已经有分段视频，则不允许拍照了
        if (baseCameraFragment.getCameraView().isOpened() && baseCameraFragment.getCameraVideoPresenter().getVideoTimes().size() <= 0) {
            // 判断数量
            if (photoAdapter.getItemCount() < SelectableUtils.getImageMaxCount()) {
                // 设置不能点击，防止多次点击报错
                baseCameraFragment.getChildClickableLayout().setChildClickable(false);
                // 判断如果是自动闪光灯模式便开启闪光灯
                if (baseCameraFragment.getFlashModel() == FlashModels.TYPE_FLASH_AUTO) {
                    baseCameraFragment.getCameraView().setFlash(Flash.TORCH);
                    // 延迟1秒拍照
                    cameraTakePictureHandler.postDelayed(cameraTakePictureRunnable, 1000);
                } else {
                    cameraTakePictureRunnable.run();
                }
            } else {
                baseCameraFragment.getPhotoVideoLayout().setTipAlphaAnimation(baseCameraFragment.getResources().getString(R.string.z_multi_library_the_camera_limit_has_been_reached));
            }
        }
    }

    /**
     * 添加入数据源
     *
     * @param bitmap bitmap
     */
    @Override
    public void addCaptureData(Bitmap bitmap) {
        // 初始化数据并且存储进file
        File file = pictureMediaStoreCompat.saveFileByBitmap(bitmap, true);
        Uri uri = pictureMediaStoreCompat.getUri(file.getPath());
        BitmapData bitmapData = new BitmapData(file.getPath(), uri, bitmap.getWidth(), bitmap.getHeight());
        // 回收bitmap
        if (bitmap.isRecycled()) {
            // 回收并且置为null
            bitmap.recycle();
        }
        // 加速回收机制
        System.gc();
        // 判断是否多个图片
        if (SelectableUtils.getImageMaxCount() > 1) {
            // 添加入数据源
            this.bitmapData.add(bitmapData);
            // 更新最后一个添加
            photoAdapter.notifyItemInserted(photoAdapter.getItemCount() - 1);
            photoAdapter.notifyItemRangeChanged(photoAdapter.getItemCount() - 1, photoAdapter.getItemCount());
            baseCameraFragment.showMultiplePicture();
        } else {
            this.bitmapData.add(bitmapData);
            photoFile = file;
            baseCameraFragment.showSinglePicture(bitmap, file, uri);
        }

        // 回调接口：添加图片后剩下的相关数据
        if (baseCameraFragment.getCameraSpec().getOnCaptureListener() != null) {
            baseCameraFragment.getCameraSpec().getOnCaptureListener().add(this.bitmapData, this.bitmapData.size() - 1);
        }
    }

    /**
     * 刷新多个图片
     */
    @Override
    public void refreshMultiPhoto(ArrayList<BitmapData> bitmapData) {
        this.bitmapData = bitmapData;
        photoAdapter.setListData(this.bitmapData);
    }

    /**
     * 返回迁移图片的线程
     *
     * @return 迁移图片的线程
     */
    @Override
    public ThreadUtils.SimpleTask<ArrayList<LocalFile>> getMovePictureFileTask() {
        movePictureFileTask = new ThreadUtils.SimpleTask<ArrayList<LocalFile>>() {
            @Override
            public ArrayList<LocalFile> doInBackground() throws IOException {
                // 每次拷贝文件后记录，最后用于全部添加到相册，回调等操作
                ArrayList<LocalFile> newFiles = new ArrayList<>();
                // 将 缓存文件 拷贝到 配置目录
                for (BitmapData item : bitmapData) {
                    File oldFile = new File(item.getPath());
                    // 压缩图片
                    File compressionFile;
                    if (baseCameraFragment.getGlobalSpec().getImageCompressionInterface() != null) {
                        compressionFile = baseCameraFragment.getGlobalSpec().getImageCompressionInterface().compressionFile(baseCameraFragment.getMyContext(), oldFile);
                    } else {
                        compressionFile = oldFile;
                    }
                    // 移动文件,获取文件名称
                    String newFileName = item.getPath().substring(item.getPath().lastIndexOf(File.separator));
                    File newFile = pictureMediaStoreCompat.createFile(newFileName, 0, true);
                    // new localFile
                    LocalFile localFile = new LocalFile();
                    localFile.setPath(newFile.getAbsolutePath());
                    localFile.setWidth(item.getWidth());
                    localFile.setHeight(item.getHeight());
                    localFile.setSize(compressionFile.length());
                    newFiles.add(localFile);
                    FileUtil.copy(compressionFile, newFile, null, (ioProgress, file) -> {
                        if (ioProgress >= 1) {
                            // 每次迁移完一个文件的进度
                            int progress = 100 / bitmapData.size();
                            ThreadUtils.runOnUiThread(() -> baseCameraFragment.setProgress(progress));
                        }
                    });
                }
                for (LocalFile item : newFiles) {
                    if (item.getPath() != null) {
                        // 加入图片到android系统库里面
                        Uri uri = MediaStoreUtils.displayToGallery(baseCameraFragment.getMyContext(), new File(item.getPath()), MediaStoreUtils.MediaTypes.TYPE_PICTURE, -1, item.getWidth(), item.getHeight(),
                                pictureMediaStoreCompat.getSaveStrategy().getDirectory(), pictureMediaStoreCompat);
                        // 加入相册后的最后是id，直接使用该id
                        item.setId(MediaStoreUtils.getId(uri));
                        item.setMimeType(MimeType.JPEG.getMimeTypeName());
                        item.setUri(pictureMediaStoreCompat.getUri(item.getPath()));
                    }
                }
                // 执行完成
                return newFiles;
            }

            @Override
            public void onSuccess(ArrayList<LocalFile> newFiles) {
                baseCameraFragment.commitPictureSuccess(newFiles);
            }

            @Override
            public void onFail(Throwable t) {
                super.onFail(t);
                baseCameraFragment.commitFail(t);
            }
        };
        return movePictureFileTask;
    }

    /**
     * 删除临时图片
     */
    @Override
    public void deletePhotoFile() {
        if (photoFile != null) {
            FileUtil.deleteFile(photoFile);
        }
    }

    /**
     * 清除数据源
     */
    @Override
    public void clearBitmapDatas() {
        bitmapData.clear();
    }

    /**
     * 停止迁移图片的线程运行
     */
    @Override
    public void cancelMovePictureFileTask() {
        if (movePictureFileTask != null) {
            movePictureFileTask.cancel();
        }
    }

    /**
     * 点击图片事件
     *
     * @param intent 点击后，封装相关数据进入该intent
     */
    @Override
    public void onPhotoAdapterClick(Intent intent) {
        baseCameraFragment.openAlbumPreviewActivity(intent);
    }

    /**
     * 多图进行删除的时候
     *
     * @param bitmapData 数据
     * @param position   删除的索引
     */
    @Override
    public void onPhotoAdapterDelete(BitmapData bitmapData, int position) {
        // 删除文件
        FileUtil.deleteFile(bitmapData.getPath());

        if (baseCameraFragment.getCameraSpec().getOnCaptureListener() != null) {
            baseCameraFragment.getCameraSpec().getOnCaptureListener().remove(this.bitmapData, position);
        }

        // 当列表全部删掉隐藏列表框的UI
        if (this.bitmapData.size() <= 0) {
            baseCameraFragment.hideViewByMultipleZero();
        }
    }


}
