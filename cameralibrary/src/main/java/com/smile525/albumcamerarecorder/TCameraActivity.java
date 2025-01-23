package com.smile525.albumcamerarecorder;

import static androidx.core.content.PermissionChecker.PERMISSION_DENIED;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.otaliastudios.cameraview.controls.Audio;
import com.smile525.albumcamerarecorder.settings.GlobalSpec;
import com.smile525.albumcamerarecorder.utils.HandleBackUtil;
import com.smile525.albumcamerarecorder.utils.HandleOnKeyUtil;
import com.smile525.common.utils.AppUtils;
import com.smile525.common.utils.StatusBarUtils;
import com.smile525.albumcamerarecorder.camera.ui.camera.CameraFragment;

import java.util.ArrayList;

/**
 * 拍照页面
 *
 * @author zhongjh
 * @date 2018/8/22
 */
public class TCameraActivity extends AppCompatActivity {

    /**
     * 权限申请自定义码
     */
    protected static final int GET_PERMISSION_REQUEST = 100;
    /**
     * 跳转到设置界面
     */
    private static final int REQUEST_CODE_SETTING = 101;
    /**
     * 界面屏幕方向切换\切换别的界面时 会触发onSaveInstanceState
     * 会存储该key的值设置为true
     * 然后在恢复界面时根据该值进行相应处理
     */
    private static final String IS_SAVE_INSTANCE_STATE = "IS_SAVE_INSTANCE_STATE";

    /**
     * 默认索引
     */
    private int mDefaultPosition;

    GlobalSpec mSpec = GlobalSpec.INSTANCE;
    /**
     * 是否初始化完毕
     */
    boolean mIsInit;
    /**
     * 是否弹出提示多次拒绝权限的dialog
     */
    private boolean mIsShowDialog;


    private boolean mAlreadyShowAudioPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (mSpec.needOrientationRestriction()) {
            setRequestedOrientation(mSpec.getOrientation());
        }
        setTheme(mSpec.getThemeId());
        StatusBarUtils.initStatusBar(TCameraActivity.this);
        super.onCreate(savedInstanceState);
        // 确认是否进行了配置
        if (!mSpec.getHasInited()) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        setContentView(R.layout.activity_main_zjh);
        requestPermissions(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_SAVE_INSTANCE_STATE, true);
    }

    @Override
    public void onBackPressed() {
        if (!HandleBackUtil.handleBackPress(this)) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (HandleOnKeyUtil.handleOnKey(this, keyCode, event)) {
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void finish() {
        super.finish();
//        if (mSpec.getCutscenesEnabled()) {
//            //关闭窗体动画显示
//            this.overridePendingTransition(0, R.anim.activity_close_zjh);
//        }
    }

    @Override
    protected void onDestroy() {
        if (mSpec.getCameraSetting() != null) {
            mSpec.getCameraSetting().clearCameraFragment();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SETTING) {
            // 因为权限一直拒绝后，只能跑到系统设置界面调整，这个是系统设置界面返回后的回调，重新验证权限
            requestPermissions(null);
        }
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!mIsShowDialog) {
            // 全部拒绝后就提示去到应用设置里面修改配置
            int permissionsLength = 0;
            for (int i = 0; i < grantResults.length; i++) {
                // 只有当用户同时点选了拒绝开启权限和不再提醒后才会true
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])) {
                    if (grantResults[i] == PERMISSION_DENIED) {
                        permissionsLength++;
                    }
                }
            }
            // 至少一个不再提醒
            if (permissionsLength > 0) {

                // 获取 LayoutInflater 来加载自定义布局
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View customView = inflater.inflate(R.layout.ti_camera_custom_dialog_layout, null);

                // 获取自定义布局中的控件
                TextView dialogTitle = customView.findViewById(R.id.dialog_title);
                TextView dialogContent = customView.findViewById(R.id.dialog_message);
                TextView positiveButton = customView.findViewById(R.id.dialog_positive_button);
                TextView negativeButton = customView.findViewById(R.id.dialog_negative_button);

                AlertDialog.Builder builder = new AlertDialog.Builder(TCameraActivity.this, R.style.MyAlertDialogStyle);
                builder.setView(customView);

                // 获取app名称
                String appName = AppUtils.getAppName(getApplicationContext());
                if (TextUtils.isEmpty(appName)) {
                    dialogContent.setText(getString(R.string.permission_has_been_set_and_will_no_longer_be_asked));
                } else {
                    String toSettingTipStr = getString(R.string.z_multi_library_in_settings_apply) +
                            appName +
                            getString(R.string.z_multi_library_enable_storage_and_camera_permissions_for_normal_use_of_related_functions);
                    dialogContent.setText(toSettingTipStr);
                }

                dialogTitle.setText(getString(R.string.z_multi_library_hint));
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        mIsShowDialog = false;
                    }
                });
                Dialog dialog = builder.create();
                dialog.setCanceledOnTouchOutside(false);
                positiveButton.setText(getString(R.string.z_multi_library_setting));
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.fromParts("package", getPackageName(), null));
                        TCameraActivity.this.startActivityForResult(intent, REQUEST_CODE_SETTING);
                        mIsShowDialog = false;
                    }
                });

                negativeButton.setText(getString(R.string.z_multi_library_cancel));
                negativeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                        TCameraActivity.this.finish();
                    }
                });
                dialog.setOnKeyListener((dialog1, keyCode, event) -> {
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                        finish();
                    }
                    return false;
                });
                dialog.show();
                mIsShowDialog = true;
            }
        }
        if (!mIsShowDialog) {
            if (requestCode == GET_PERMISSION_REQUEST) {
                int permissionsLength = 0;
                for (int grantResult : grantResults) {
                    if (grantResult == PERMISSION_DENIED) {
                        // 如果拒绝后
                        permissionsLength++;
                    }
                }
                if (permissionsLength > 0) {
                    requestPermissionsDialog();
                } else {
                    requestPermissions(null);
                }
            }
        }
    }

    private void showRecordAudioHintDialog() {

        // 获取 LayoutInflater 来加载自定义布局
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.ti_camera_custom_dialog_layout, null);

        // 获取自定义布局中的控件
        TextView dialogTitle = customView.findViewById(R.id.dialog_title);
        TextView tvContent = customView.findViewById(R.id.dialog_message);
        TextView positiveButton = customView.findViewById(R.id.dialog_positive_button);
        TextView negativeButton = customView.findViewById(R.id.dialog_negative_button);

        // 设置自定义文本内容
        dialogTitle.setText("麦克风权限未开启");
        positiveButton.setText("去设置");
        negativeButton.setText("继续拍摄");

        // 获取app名称
        String appName = AppUtils.getAppName(getApplicationContext());
        if (TextUtils.isEmpty(appName)) {
            // 设置自定义文本内容
            tvContent.setText(getString(R.string.permission_has_been_set_and_will_no_longer_be_asked));
        } else {
            String toSettingTipStr = "无法录制声音，前往“设置>应用>" + appName + ">权限\"中打开麦克风权限。";
            // 设置自定义文本内容
            tvContent.setText(toSettingTipStr);
        }

        // 创建 AlertDialog.Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(customView);  // 设置自定义布局
        builder.setCancelable(true);   // 点击外部区域是否可以取消

        // 创建并显示 AlertDialog
        AlertDialog dialog = builder.create();
        // 设置按钮的点击事件
        positiveButton.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            TCameraActivity.this.startActivityForResult(intent, REQUEST_CODE_SETTING);
            mIsShowDialog = false;
        });
        negativeButton.setOnClickListener(v -> {
            dialog.dismiss();
            // 没有所需要请求的权限，就进行初始化
            mAlreadyShowAudioPermission = true;
            init(null);
        });
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener((dialog1, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                finish();
            }
            return false;
        });
        dialog.show();
        mIsShowDialog = true;
    }

    /**
     * 初始化，在权限全部通过后才进行该初始化
     *
     * @param savedInstanceState 恢复的数值
     */
    private void init(Bundle savedInstanceState) {
        if (!mIsInit) {
            // : 2025/1/8 判断是否只有录音权限未授权
            if (!isMicrophonePermissionGranted()) {
                if (!mAlreadyShowAudioPermission) {
                    showRecordAudioHintDialog();
                    return;
                }
            }
            // : 2023/8/30 加载相机页面
            //获取管理者
            FragmentManager supportFragmentManager = getSupportFragmentManager();
            //开启事务
            FragmentTransaction fragmentTransaction = supportFragmentManager.beginTransaction();
            //碎片
            CameraFragment cameraFragment = CameraFragment.newInstance();

            //提交事务
            fragmentTransaction.add(R.id.fl_camera_view, cameraFragment).commit();

            if (!isMicrophonePermissionGranted()) {
                //关闭录制声音
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        cameraFragment.getCameraView().setAudio(Audio.OFF);
                    }
                }, 50);
            }

            mIsInit = true;
        }
    }

    /**
     * 请求权限
     *
     * @param savedInstanceState 恢复的数值
     */
    private void requestPermissions(Bundle savedInstanceState) {
        // 判断权限，权限通过才可以初始化相关
        ArrayList<String> needPermissions = getNeedPermissions();
        if (needPermissions.size() > 0) {
            // 请求权限
            requestPermissions2(needPermissions);
        } else {
            // 没有所需要请求的权限，就进行初始化
            init(savedInstanceState);
        }
    }

    /**
     * 请求权限 - 如果曾经拒绝过，则弹出dialog
     */
    private void requestPermissionsDialog() {
        // 判断权限，权限通过才可以初始化相关
        ArrayList<String> needPermissions = getNeedPermissions();
        if (needPermissions.size() > 0) {
            // 动态消息
            StringBuilder message = new StringBuilder();
            message.append(getString(R.string.z_multi_library_to_use_this_feature));
            int permissionsLength = 0;
            for (String item : needPermissions) {
                switch (item) {
                    case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                    case Manifest.permission.READ_MEDIA_IMAGES:
                        message.append(getString(R.string.z_multi_library_file_read_and_write_permission_to_read_and_store_related_files));
                        permissionsLength++;
                        break;
                    case Manifest.permission.RECORD_AUDIO:
                        // 弹窗提示为什么要请求这个权限
                        message.append(getString(R.string.z_multi_library_record_permission_to_record_sound));
                        permissionsLength++;
                        break;
                    case Manifest.permission.CAMERA:
                        // 弹窗提示为什么要请求这个权限
                        message.append(getString(R.string.z_multi_library_record_permission_to_shoot));
                        permissionsLength++;
                        break;
                    default:
                        break;
                }
            }

            // 获取 LayoutInflater 来加载自定义布局
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View customView = inflater.inflate(R.layout.ti_camera_custom_dialog_layout, null);

            // 获取自定义布局中的控件
            TextView dialogTitle = customView.findViewById(R.id.dialog_title);
            TextView dialogContent = customView.findViewById(R.id.dialog_message);
            TextView positiveButton = customView.findViewById(R.id.dialog_positive_button);
            TextView negativeButton = customView.findViewById(R.id.dialog_negative_button);

            AlertDialog.Builder builder = new AlertDialog.Builder(TCameraActivity.this, R.style.MyAlertDialogStyle);
            // 弹窗提示为什么要请求这个权限
            builder.setView(customView);
            dialogTitle.setText(getString(R.string.z_multi_library_hint));
            message.append(getString(R.string.z_multi_library_Otherwise_it_cannot_run_normally_and_will_apply_for_relevant_permissions_from_you));
            dialogContent.setText(message.toString());

            Dialog dialog = builder.create();

            positiveButton.setText(getString(R.string.z_multi_library_ok));
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                    // 请求权限
                    requestPermissions2(needPermissions);
                }
            });

            negativeButton.setText(getString(R.string.z_multi_library_cancel));
            negativeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                    TCameraActivity.this.finish();
                }
            });
            dialog.setCanceledOnTouchOutside(false);
            dialog.setOnKeyListener((dialog1, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                    finish();
                }
                return false;
            });
            dialog.show();
        } else {
            // 没有所需要请求的权限，就进行初始化
            Log.e("TAG","requestPermissions else init");
            init(null);
        }
    }

    /**
     * 获取目前需要请求的权限
     */
    protected ArrayList<String> getNeedPermissions() {
        // 需要请求的权限列表
        ArrayList<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 存储功能必须验证,兼容Android SDK 33
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager
                    .PERMISSION_GRANTED) {
                if (!permissions.contains(Manifest.permission.CAMERA)) {
                    permissions.add(Manifest.permission.CAMERA);
                }
            }
        }
        return permissions;
    }

    /**
     * 请求权限
     *
     * @param permissions 权限
     */
    private void requestPermissions2(ArrayList<String> permissions) {
        ActivityCompat.requestPermissions(TCameraActivity.this, permissions.toArray(new String[0]), GET_PERMISSION_REQUEST);
    }

    /**
     * 检查麦克风权限是否已经授权
     * @return
     */
    private boolean isMicrophonePermissionGranted() {
        return ContextCompat.checkSelfPermission(TCameraActivity.this, android.Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }
}
