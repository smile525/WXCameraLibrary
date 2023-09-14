package com.smile525.albumcamerarecorder.album

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.smile525.albumcamerarecorder.BaseFragment
import com.smile525.albumcamerarecorder.R
import com.smile525.albumcamerarecorder.album.ui.MatissFragment

/**
 * 一个容器，容纳CameraFragment和别的Fragment,一切都是为了过渡动画
 * @author zhongjh
 * @date 2022/7/26
 */
class MainFragment : com.smile525.albumcamerarecorder.BaseFragment() {

    companion object {
        /**
         * @param marginBottom 底部间距
         */
        fun newInstance(marginBottom: Int): com.smile525.albumcamerarecorder.album.MainFragment {
            val mainFragment = com.smile525.albumcamerarecorder.album.MainFragment()
            val args = Bundle()
            mainFragment.arguments = args
            args.putInt(com.smile525.albumcamerarecorder.album.ui.MatissFragment.ARGUMENTS_MARGIN_BOTTOM, marginBottom)
            return mainFragment
        }

        const val MATISS_FRAGMENT_TAG = "matissFragment"
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        Log.d("MainFragment", "onCreateView")
        return inflater.inflate(R.layout.fragment_containerview_zjh, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("MainFragment", "onViewCreated")
        // 先通过标签形式查找
        val matissFragment = childFragmentManager.findFragmentByTag(com.smile525.albumcamerarecorder.album.MainFragment.Companion.MATISS_FRAGMENT_TAG)
        // 如果不存在，则重新创建并添加，如果已经存在就不用处理了，因为FragmentStateAdapter已经帮我们处理了
        matissFragment ?: let {
            val newMatissFragment = com.smile525.albumcamerarecorder.album.ui.MatissFragment.newInstance(
                    arguments?.getInt(com.smile525.albumcamerarecorder.album.ui.MatissFragment.ARGUMENTS_MARGIN_BOTTOM)
                            ?: 0
            )
            childFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainerView, newMatissFragment,
                        com.smile525.albumcamerarecorder.album.MainFragment.Companion.MATISS_FRAGMENT_TAG
                    )
                    .commitAllowingStateLoss()
        }
    }

}