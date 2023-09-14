package com.example.myapplication2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.myapplication2.configuration.Glide4Engine;
import com.smile525.albumcamerarecorder.preview.BasePreviewActivity;
import com.smile525.albumcamerarecorder.settings.CameraSetting;
import com.smile525.albumcamerarecorder.settings.GlobalSetting;
import com.smile525.albumcamerarecorder.settings.MultiMediaSetting;
import com.smile525.common.entity.LocalFile;
import com.smile525.common.entity.MediaExtraInfo;
import com.smile525.common.entity.MultiMedia;
import com.smile525.common.entity.SaveStrategy;
import com.smile525.common.enums.MimeType;
import com.smile525.common.utils.MediaUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "TAKE_PHOTO";

    GlobalSetting mGlobalSetting;
    protected static final int REQUEST_CODE_CHOOSE = 236;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initCamera();
            }

        });
    }

    private void initCamera() {
// 拍摄有关设置
        CameraSetting cameraSetting = new CameraSetting();
        // 支持的类型：图片，视频
        cameraSetting.mimeTypeSet(MimeType.ofAll());
        // 设置最长录制时间1分钟
        cameraSetting.duration(60);

        // 全局
        mGlobalSetting = MultiMediaSetting.from(MainActivity.this).choose(MimeType.ofAll());
        // 开启拍摄功能
        mGlobalSetting.cameraSetting(cameraSetting);

        Log.e("getPackageName():","getPackageName:::"+getPackageName());

        mGlobalSetting
                // 设置路径和7.0保护路径等等
                .allStrategy(new SaveStrategy(false, getPackageName()+".fileprovider", "aabb"))
                // for glide-V4
                .imageEngine(new Glide4Engine())
                .isImageEdit(false)
                // 最大5张图片、最大3个视频、最大1个音频
                .maxSelectablePerMediaType(null,
                        1,
                        1,
                        1,
                        0,
                        0,
                        0)
                .forResult(REQUEST_CODE_CHOOSE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CODE_CHOOSE) {
            // 如果是在预览界面点击了确定
            List<LocalFile> result = MultiMediaSetting.obtainLocalFileResult(data);
            for (LocalFile localFile : result) {
                // 绝对路径,AndroidQ如果存在不属于自己App下面的文件夹则无效
                Log.i(TAG, "onResult id:" + localFile.getId());
                Log.i(TAG, "onResult 绝对路径:" + localFile.getPath());
                Log.d(TAG, "onResult 旧图路径:" + localFile.getOldPath());
                Log.d(TAG, "onResult 原图路径:" + localFile.getOriginalPath());
                Log.i(TAG, "onResult Uri:" + localFile.getUri());
                Log.d(TAG, "onResult 旧图Uri:" + localFile.getOldUri());
                Log.d(TAG, "onResult 原图Uri:" + localFile.getOriginalUri());
                Log.i(TAG, "onResult 文件大小: " + localFile.getSize());
                Log.i(TAG, "onResult 视频音频长度: " + localFile.getDuration());
                Log.i(TAG, "onResult 是否选择了原图: " + localFile.isOriginal());
                if (localFile.isImageOrGif()) {
                    if (localFile.isImage()) {
                        Log.d(TAG, "onResult 图片类型");
                    } else if (localFile.isImage()) {
                        Log.d(TAG, "onResult 图片类型");
                    }
                } else if (localFile.isVideo()) {
                    Log.d(TAG, "onResult 视频类型");
                } else if (localFile.isAudio()) {
                    Log.d(TAG, "onResult 音频类型");
                }
                Log.i(TAG, "onResult 具体类型:" + localFile.getMimeType());
                // 某些手机拍摄没有自带宽高，那么我们可以自己获取
                if (localFile.getWidth() == 0 && localFile.isVideo()) {
                    MediaExtraInfo mediaExtraInfo = MediaUtils.getVideoSize(getApplication(), localFile.getPath());
                    localFile.setWidth(mediaExtraInfo.getWidth());
                    localFile.setHeight(mediaExtraInfo.getHeight());
                    localFile.setDuration(mediaExtraInfo.getDuration());
                }
                Log.i(TAG, "onResult 宽高: " + localFile.getWidth() + "x" + localFile.getHeight());
            }
        }
    }
}