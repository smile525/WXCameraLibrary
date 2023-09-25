package com.smile525.albumcamerarecorder.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.smile525.albumcamerarecorder.R;
import com.smile525.albumcamerarecorder.camera.listener.ClickOrLongListener;
import com.smile525.albumcamerarecorder.camera.util.LogUtil;
import com.smile525.albumcamerarecorder.widget.clickorlongbutton.ClickOrLongButton;
import com.smile525.common.listener.OnMoreClickListener;
import com.zhongjh.circularprogressview.CircularProgress;
import com.zhongjh.circularprogressview.CircularProgressListener;


import java.util.ArrayList;

/**
 * 集成开始功能按钮、确认按钮、取消按钮的布局
 * {@link ClickOrLongButton 点击或者长按的按钮 }
 * {@link com.zhongjh.circularprogressview.CircularProgress 操作按钮(取消和确认) }
 *
 * @author zhongjh
 * @date 2018/8/7
 */
public abstract class BaseOperationLayout extends FrameLayout {

    // region 回调事件监听

    /**
     * 点击或长按监听
     */
    private ClickOrLongListener mClickOrLongListener;
    /**
     * 点击或长按监听结束后的 确认取消事件监控
     */
    private OperateListener mOperateListener;

    /**
     * 操作按钮的Listener
     */
    public interface OperateListener {

        /**
         * 取消
         */
        void cancel();

        /**
         * 确认
         */
        void confirm();

        /**
         * 开始进度操作，目前只用于分段录制
         */
        void startProgress();

        /**
         * 取消进度操作，目前只用于分段录制
         */
        void stopProgress();

        /**
         * 进度完成
         */
        void doneProgress();

    }

    public void setPhotoVideoListener(ClickOrLongListener clickOrLongListener) {
        this.mClickOrLongListener = clickOrLongListener;
    }

    public void setOperateListener(OperateListener mOperateListener) {
        this.mOperateListener = mOperateListener;
    }

    // endregion

    /**
     * 控件集合
     */
    protected ViewHolder viewHolder;

    /**
     * 是否第一次
     */
    private boolean mIsFirst = true;

    /**
     * 按钮左右分开移动动画
     */
    ObjectAnimator mAnimatorConfirm;
    ObjectAnimator mAnimatorCancel;

    /**
     * 创建
     *
     * @return ViewHolder
     */
    protected abstract ViewHolder newViewHolder();

    public BaseOperationLayout(@NonNull Context context) {
        this(context, null);
    }

    public BaseOperationLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseOperationLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 获取宽的模式
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        // 获取高的模式
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        // 获取宽的尺寸
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        // 获取高的尺寸
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        heightSize = heightSize / 3;
        mAnimatorConfirm = ObjectAnimator.ofFloat(viewHolder.btnConfirm, "translationX", -widthSize / 5F, 0);
        mAnimatorCancel = ObjectAnimator.ofFloat(viewHolder.btnCancel, "translationX", widthSize / 5F, 0);

        setMeasuredDimension(widthSize, heightSize);
        // 传递新创建的宽高给子控件
        super.onMeasure(MeasureSpec.makeMeasureSpec(widthSize, widthMode), MeasureSpec.makeMeasureSpec(heightSize, heightMode));
    }

    /**
     * 初始化view
     */
    private void initView() {
        // 自定义View中如果重写了onDraw()即自定义了绘制，那么就应该在构造函数中调用view的setWillNotDraw(false).
        setWillNotDraw(false);

        viewHolder = newViewHolder();

        // 默认隐藏
        viewHolder.btnCancel.setVisibility(GONE);
        viewHolder.btnConfirm.setVisibility(GONE);

        // 定制样式 .确认按钮,修改主色调
//        viewHolder.btnConfirm.setPrimaryColor(R.color.operation_background);
        // 修改成铺满样式
//        viewHolder.btnConfirm.setFullStyle(true);
        // 修改图片
//        viewHolder.btnConfirm.setFunctionImage(R.drawable.ic_baseline_done,
//                R.drawable.avd_done_to_stop, R.drawable.avd_stop_to_done);
        // 修改进度颜色
//        viewHolder.btnConfirm.setFullProgressColor(R.color.click_button_inner_circle_no_operation_interval);

        // 定制样式 .取消按钮 修改主色调
//        viewHolder.btnCancel.setPrimaryColor(R.color.operation_background);
        // 修改成铺满样式
//        viewHolder.btnCancel.setFullStyle(true);
        // 修改图片
//        viewHolder.btnCancel.setFunctionImage(R.drawable.ic_baseline_keyboard_arrow_left_24,
//                R.drawable.avd_done_to_stop, R.drawable.avd_stop_to_done);
        // 取消进度模式
//        viewHolder.btnCancel.setProgressMode(false);

        initListener();
    }

    /**
     * 初始化事件
     */
    protected void initListener() {
        btnClickOrLongListener();
        btnCancelListener();
        btnConfirmListener();
    }

    /**
     * btnClickOrLong事件
     */
    private void btnClickOrLongListener() {
        viewHolder.btnClickOrLong.setRecordingListener(new ClickOrLongListener() {
            @Override
            public void actionDown() {
                if (mClickOrLongListener != null) {
                    mClickOrLongListener.actionDown();
                }
            }

            @Override
            public void onClick() {
                if (mClickOrLongListener != null) {
                    mClickOrLongListener.onClick();
                }
            }

            @Override
            public void onLongClickShort(long time) {
                if (mClickOrLongListener != null) {
                    mClickOrLongListener.onLongClickShort(time);
                }
                endTipAlphaAnimation();
            }

            @Override
            public void onLongClick() {
                if (mClickOrLongListener != null) {
                    mClickOrLongListener.onLongClick();
                }
                startTipAlphaAnimation();
            }

            @Override
            public void onLongClickEnd(long time) {
                if (mClickOrLongListener != null) {
                    mClickOrLongListener.onLongClickEnd(time);
                }
                endTipAlphaAnimation();
            }

            @Override
            public void onLongClickError() {
                if (mClickOrLongListener != null) {
                    mClickOrLongListener.onLongClickError();
                }
            }

            @Override
            public void onBanClickTips() {
                if (mClickOrLongListener != null) {
                    mClickOrLongListener.onBanClickTips();
                }
            }

            @Override
            public void onClickStopTips() {
                if (mClickOrLongListener != null) {
                    mClickOrLongListener.onClickStopTips();
                }
            }
        });
    }

    /**
     * 返回事件
     */
    private void btnCancelListener() {
        viewHolder.btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOperateListener != null) {
                    mOperateListener.cancel();
                }
                endTipAlphaAnimation();
            }
        });
//        viewHolder.btnCancel.setCircularProgressListener(new CircularProgressListener() {
//
//            @Override
//            public void onStart() {
//
//            }
//
//            @Override
//            public void onDone() {
//
//            }
//
//            @Override
//            public void onStop() {
//
//            }
//
//            @Override
//            public void onClickByGeneralMode() {
//                if (mOperateListener != null) {
//                    mOperateListener.cancel();
//                }
//                startTipAlphaAnimation();
//            }
//
//            @Override
//            public void onClickByProgressMode() {
//            }
//        });
    }

    /**
     * 提交事件
     */
    private void btnConfirmListener() {
        viewHolder.btnConfirm.setOnClickListener(new OnMoreClickListener() {
            @Override
            public void onListener(@NonNull View v) {
                if (mOperateListener != null) {
                    mOperateListener.confirm();
                }
            }
        });
//        viewHolder.btnConfirm.setCircularProgressListener(new CircularProgressListener() {
//            @Override
//            public void onStart() {
//                if (mOperateListener != null) {
//                    mOperateListener.startProgress();
//                }
//            }
//
//            @Override
//            public void onDone() {
//                if (mOperateListener != null) {
//                    mOperateListener.doneProgress();
//                }
//            }
//
//            @Override
//            public void onStop() {
//                if (mOperateListener != null) {
//                    mOperateListener.stopProgress();
//                }
//            }
//
//            @Override
//            public void onClickByGeneralMode() {
//
//            }
//
//            @Override
//            public void onClickByProgressMode() {
//                if (mOperateListener != null) {
//                    mOperateListener.confirm();
//                }
//                startTipAlphaAnimation();
//            }
//        });
    }

    /**
     * 隐藏中间的核心操作按钮
     */
    public void hideBtnClickOrLong() {
        viewHolder.btnClickOrLong.setVisibility(INVISIBLE);
    }

    /**
     * 点击长按结果后的动画
     * 显示左右两边的按钮
     */
    public void startShowLeftRightButtonsAnimator() {
        // 显示提交和取消按钮
        viewHolder.btnConfirm.setVisibility(VISIBLE);
        viewHolder.btnCancel.setVisibility(VISIBLE);
        // 动画未结束前不能让它们点击
        viewHolder.btnConfirm.setClickable(false);
        viewHolder.btnCancel.setClickable(false);

        // 显示动画
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(mAnimatorCancel, mAnimatorConfirm);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                // 动画结束使得按钮可点击
                viewHolder.btnConfirm.setClickable(true);
                viewHolder.btnCancel.setClickable(true);
            }
        });
        animatorSet.setDuration(300);
        animatorSet.start();
    }

    /**
     * 多图片拍照后显示的右侧按钮
     */
    public void startOperationBtnAnimatorMulti() {
        // 如果本身隐藏的，就显示出来
        if (viewHolder.btnConfirm.getVisibility() == View.GONE) {
            // 显示提交按钮
            viewHolder.btnConfirm.setVisibility(VISIBLE);
            // 动画未结束前不能让它们点击
            viewHolder.btnConfirm.setClickable(false);

            // 显示动画
            AnimatorSet set = new AnimatorSet();
            set.playTogether(mAnimatorConfirm);
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    // 动画结束使得按钮可点击
                    viewHolder.btnConfirm.setClickable(true);
                }
            });
            set.setDuration(200);
            set.start();
        }
    }

    // region 对外提供的api

    /**
     * 设置提示文本
     *
     * @param tip 提示文本
     */
    public void setTip(String tip) {
        viewHolder.tvTip.setText(tip);
    }

    /**
     * 提示文本框 - 浮现渐现动画（暂时不用）
     */
    public void startTipAlphaAnimation() {
        LogUtil.e("隐藏提示");
//        if (mIsFirst) {
            ObjectAnimator animatorTxtTip = ObjectAnimator.ofFloat(viewHolder.tvTip, "alpha", 1f, 0f);
            animatorTxtTip.setDuration(500);
            animatorTxtTip.start();
//            mIsFirst = false;
//        }
    }

    /**
     * 提示文本框 - 浮现渐现动画（暂时不用）
     */
    public void endTipAlphaAnimation() {
        LogUtil.e("显示提示");
//        if (mIsFirst) {
        ObjectAnimator animatorTxtTip = ObjectAnimator.ofFloat(viewHolder.tvTip, "alpha", 0f, 1f);
        animatorTxtTip.setDuration(500);
        animatorTxtTip.start();
//            mIsFirst = false;
//        }
    }

    /**
     * 提示文本框 - 浮现渐现动画，显示新的文字
     *
     * @param tip 提示文字
     */
    public void setTipAlphaAnimation(String tip) {
        viewHolder.tvTip.setText(tip);
        ObjectAnimator animatorTxtTip = ObjectAnimator.ofFloat(viewHolder.tvTip, "alpha", 0f, 1f, 1f, 0f);
        animatorTxtTip.setDuration(2500);
        animatorTxtTip.start();
    }

    /**
     * 设置按钮 最长长按时间
     *
     * @param duration 时间秒
     */
    public void setDuration(int duration) {
        viewHolder.btnClickOrLong.setDuration(duration);
    }

    /**
     * 最短录制时间
     *
     * @param duration 时间
     */
    public void setMinDuration(int duration) {
        viewHolder.btnClickOrLong.setMinDuration(duration);
    }

    /**
     * 重置本身全部
     */
    public void reset() {
        viewHolder.btnClickOrLong.reset();
        viewHolder.btnClickOrLong.resetState();
        // 隐藏第二层的view
        viewHolder.btnCancel.setVisibility(View.GONE);
        viewHolder.btnConfirm.setVisibility(View.GONE);
        // 显示第一层的view
        viewHolder.btnClickOrLong.setVisibility(View.VISIBLE);
        //
        endTipAlphaAnimation();
    }

    /**
     * 设置按钮支持的功能：
     *
     * @param buttonStateBoth {@link ClickOrLongButton#BUTTON_STATE_ONLY_CLICK 只能点击
     * @link ClickOrLongButton#BUTTON_STATE_ONLY_LONG_CLICK 只能长按
     * @link ClickOrLongButton#BUTTON_STATE_BOTH 两者皆可
     * }
     */
    public void setButtonFeatures(int buttonStateBoth) {
        viewHolder.btnClickOrLong.setButtonFeatures(buttonStateBoth);
    }

    /**
     * 设置是否可点击
     */
    @Override
    public void setEnabled(boolean enabled) {
        viewHolder.btnClickOrLong.setTouchable(enabled);
        viewHolder.btnConfirm.setEnabled(enabled);
        viewHolder.btnCancel.setEnabled(enabled);
        // 录音控件是没拥有该控件的
        if (viewHolder.tvSectionRecord != null) {
            viewHolder.tvSectionRecord.setEnabled(enabled);
        }
    }

    /**
     * 设置提交按钮是否可点击
     *
     * @param enabled 是否可点击
     */
    public void setConfirmEnable(boolean enabled) {
        viewHolder.btnConfirm.setEnabled(enabled);
    }

    /**
     * 设置中间按钮是否可点击
     *
     * @param enabled 是否可点击
     */
    public void setClickOrLongEnable(boolean enabled) {
        viewHolder.btnClickOrLong.setTouchable(enabled);
    }

    /**
     * 赋值时间长度，目前用于分段录制
     */
    public void setData(ArrayList<Long> videoTimes) {
        viewHolder.btnClickOrLong.setCurrentTime(videoTimes);
    }

    /**
     * 刷新点击长按按钮
     */
    public void invalidateClickOrLongButton() {
        viewHolder.btnClickOrLong.invalidate();
    }
//
//    /**
//     * 是否启用进度模式
//     */
//    public void setProgressMode(boolean isProgress) {
//        viewHolder.btnConfirm.setProgressMode(isProgress);
//    }
//
//    /**
//     * @return 获取当前是否进度模式
//     */
//    public boolean getProgressMode() {
//        return viewHolder.btnConfirm.mIsProgress;
//    }
//
//    /**
//     * 重置btnConfirm
//     */
//    public void resetConfirm() {
//        viewHolder.btnConfirm.reset();
//    }

    public static class ViewHolder {
        View rootView;
        public ImageView btnCancel;
        public ImageView btnConfirm;
        public ClickOrLongButton btnClickOrLong;
        public TextView tvTip;
        public TextView tvSectionRecord;

        public ViewHolder(View rootView) {
            this.rootView = rootView;
            this.btnCancel = rootView.findViewById(R.id.btnCancel);
            this.btnConfirm = rootView.findViewById(R.id.btnConfirm);
            this.btnClickOrLong = rootView.findViewById(R.id.btnClickOrLong);
            this.tvTip = rootView.findViewById(R.id.tvTip);
            this.tvSectionRecord = rootView.findViewById(R.id.tvSectionRecord);
        }

    }

    // endregion


}
