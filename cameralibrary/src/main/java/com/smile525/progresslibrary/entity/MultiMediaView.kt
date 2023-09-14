package com.smile525.progresslibrary.entity

import android.os.Parcel
import android.view.View
import com.smile525.common.entity.LocalFile
import com.smile525.common.entity.MultiMedia
import com.smile525.progresslibrary.widget.MaskProgressView

/**
 * 多媒体实体类,包含着view
 *
 * @author zhongjh
 * @date 2021/12/13
 */
class MultiMediaView : MultiMedia {

    companion object {

        private const val FULL_PERCENT = 100

    }

    /**
     * 绑定子view,包含其他所有控件（显示view,删除view）
     */
    lateinit var itemView: View

    /**
     * 绑定子view: 用于显示图片、视频的view
     */
    lateinit var maskProgressView: MaskProgressView

    /**
     * 是否进行上传动作
     */
    var isUploading = false

    constructor() : super()

    constructor(input: Parcel) : super(input)

    constructor(mimeType: String) {
        this.mimeType = mimeType
    }

    constructor(localFile: LocalFile) : super(localFile)

}