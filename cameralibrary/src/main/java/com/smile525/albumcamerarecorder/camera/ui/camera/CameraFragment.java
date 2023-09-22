package com.smile525.albumcamerarecorder.camera.ui.camera;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.otaliastudios.cameraview.CameraView;
import com.smile525.albumcamerarecorder.camera.ui.camera.presenter.BaseCameraPicturePresenter;
import com.smile525.albumcamerarecorder.camera.ui.camera.presenter.BaseCameraVideoPresenter;
import com.smile525.albumcamerarecorder.camera.ui.camera.state.CameraStateManagement;
import com.smile525.albumcamerarecorder.camera.widget.PhotoVideoLayout;
import com.smile525.albumcamerarecorder.widget.ImageViewTouch;
import com.smile525.albumcamerarecorder.widget.childclickable.ChildClickableRelativeLayout;
import com.smile525.albumcamerarecorder.widget.childclickable.IChildClickableLayout;
import com.smile525.albumcamerarecorder.R;
/**
 * 继承于BaseCameraFragment
 *
 * @author zhongjh
 * @date 2022/8/12
 */
public class CameraFragment extends BaseCameraFragment<CameraStateManagement, BaseCameraPicturePresenter, BaseCameraVideoPresenter> {

    ViewHolder mViewHolder;
    BaseCameraPicturePresenter cameraPicturePresenter = new BaseCameraPicturePresenter(this);
    BaseCameraVideoPresenter cameraVideoPresenter = new BaseCameraVideoPresenter(this);
    CameraStateManagement cameraStateManagement = new CameraStateManagement(this);

    public static CameraFragment newInstance() {
        return new CameraFragment();
    }

    @Override
    public View setContentView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.fragment_camera_zjh, container, false);
    }

    @Override
    public void initView(View view, Bundle savedInstanceState) {
        mViewHolder = new ViewHolder(view);
    }

    @NonNull
    @Override
    public IChildClickableLayout getChildClickableLayout() {
        return mViewHolder.rlMain;
    }

    @Nullable
    @Override
    public View getTopView() {
        return mViewHolder.clMenu;
    }

    @NonNull
    @Override
    public CameraView getCameraView() {
        return mViewHolder.cameraView;
    }

    @Override
    public RecyclerView getRecyclerViewPhoto() {
        return mViewHolder.rlPhoto;
    }

    @Nullable
    @Override
    public View[] getMultiplePhotoView() {
        return new View[]{mViewHolder.vLine1, mViewHolder.vLine2};
    }

    @NonNull
    @Override
    public PhotoVideoLayout getPhotoVideoLayout() {
        return mViewHolder.pvLayout;
    }

    @NonNull
    @Override
    public ImageViewTouch getSinglePhotoView() {
        return mViewHolder.imgPhoto;
    }

    @Nullable
    @Override
    public View getCloseView() {
        return mViewHolder.ctvClose;
    }

    @Nullable
    @Override
    public ImageView getFlashView() {
        return mViewHolder.imgFlash;
    }

    @Nullable
    @Override
    public ImageView getSwitchView() {
        return mViewHolder.imgSwitch;
    }

    @NonNull
    @Override
    public CameraStateManagement getCameraStateManagement() {
        return cameraStateManagement;
    }

    @NonNull
    @Override
    public BaseCameraPicturePresenter getCameraPicturePresenter() {
        return cameraPicturePresenter;
    }

    @NonNull
    @Override
    public BaseCameraVideoPresenter getCameraVideoPresenter() {
        return cameraVideoPresenter;
    }

    @Override
    public void showBottomMenu() {

    }

    public static class ViewHolder {

        View rootView;
        ChildClickableRelativeLayout rlMain;
        ImageViewTouch imgPhoto;
        ImageView imgFlash;
        ImageView imgSwitch;
        PhotoVideoLayout pvLayout;
        RecyclerView rlPhoto;
        View vLine1;
        View vLine2;
        TextView ctvClose;
        CameraView cameraView;
        ConstraintLayout clMenu;

        ViewHolder(View rootView) {
            this.rootView = rootView;
            this.rlMain = rootView.findViewById(R.id.rlMain);
            this.imgPhoto = rootView.findViewById(R.id.imgPhoto);
            this.imgFlash = rootView.findViewById(R.id.imgFlash);
            this.imgSwitch = rootView.findViewById(R.id.imgSwitch);
            this.pvLayout = rootView.findViewById(R.id.pvLayout);
            this.rlPhoto = rootView.findViewById(R.id.rlPhoto);
            this.vLine1 = rootView.findViewById(R.id.vLine1);
            this.vLine2 = rootView.findViewById(R.id.vLine2);
            this.ctvClose = rootView.findViewById(R.id.ctvClose);
            this.cameraView = rootView.findViewById(R.id.cameraView);
            this.clMenu = rootView.findViewById(R.id.clMenu);
        }
    }

}
