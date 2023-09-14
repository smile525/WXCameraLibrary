package com.smile525.progresslibrary.widget

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smile525.albumcamerarecorder.R
import com.smile525.common.entity.LocalFile
import com.smile525.common.entity.RecordingItem
import com.smile525.common.entity.SaveStrategy
import com.smile525.common.enums.MimeType
import com.smile525.common.utils.MediaStoreCompat
import com.smile525.common.utils.ThreadUtils
import com.smile525.common.utils.ThreadUtils.SimpleTask
import com.smile525.progresslibrary.apapter.PhotoAdapter
import com.smile525.progresslibrary.api.MaskProgressApi
import com.smile525.progresslibrary.engine.ImageEngine
import com.smile525.progresslibrary.entity.MultiMediaView
import com.smile525.progresslibrary.listener.MaskProgressLayoutListener
import java.util.*

/**
 * 这是返回（图片、视频、录音）等文件后，显示的Layout
 *
 * @author zhongjh
 * @date 2018/10/17
 * https://www.jianshu.com/p/191c41f63dc7
 */
class MaskProgressLayout : FrameLayout, MaskProgressApi {

    private lateinit var mPhotoAdapter: PhotoAdapter

    /**
     * 控件集合
     */
    private lateinit var mViewHolder: ViewHolder

    /**
     * 图片加载方式
     */
    private var mImageEngine: ImageEngine? = null

    /**
     * 文件配置路径
     */
    private lateinit var mMediaStoreCompat: MediaStoreCompat

    /**
     * 是否允许操作
     */
    private var isOperation = true

    /**
     * 音频数据
     */
    val audioList = ArrayList<MultiMediaView>()

    /**
     * 音频 文件的进度条颜色
     */
    private var audioProgressColor = 0

    /**
     * 音频 删除颜色
     */
    private var audioDeleteColor = 0

    /**
     * 音频 播放按钮的颜色
     */
    private var audioPlayColor = 0

    /**
     * 点击事件(这里只针对音频)
     */
    var maskProgressLayoutListener: MaskProgressLayoutListener? = null
        set(value) {
            field = value
            mPhotoAdapter.listener = value
        }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initView(attrs!!)
    }

    /**
     * 初始化view
     */
    private fun initView(attrs: AttributeSet) {
        // 自定义View中如果重写了onDraw()即自定义了绘制，那么就应该在构造函数中调用view的setWillNotDraw(false).
        setWillNotDraw(false)

        // 获取系统颜色
        val defaultColor = -0x1000000
        val attrsArray = intArrayOf(R.color.colorPrimary, R.color.colorPrimaryDark, R.color.colorAccent)
        val typedArray = context.obtainStyledAttributes(attrsArray)
        val colorPrimary = typedArray.getColor(0, defaultColor)

        // 获取自定义属性。
        val maskProgressLayoutStyle = context.obtainStyledAttributes(attrs, R.styleable.MaskProgressLayoutZhongjh)
        // 是否允许操作
        isOperation = maskProgressLayoutStyle.getBoolean(R.styleable.MaskProgressLayoutZhongjh_isOperation, true)
        // 一行多少列
        val columnNumber = maskProgressLayoutStyle.getInteger(R.styleable.MaskProgressLayoutZhongjh_columnNumber, 4)
        // 列与列之间多少间隔px单位
        val columnSpace = maskProgressLayoutStyle.getInteger(R.styleable.MaskProgressLayoutZhongjh_columnSpace, 10)
        // 获取默认图片
        var drawable = maskProgressLayoutStyle.getDrawable(R.styleable.MaskProgressLayoutZhongjh_album_thumbnail_placeholder)
        // 获取添加图片
        val imageAddDrawable = maskProgressLayoutStyle.getDrawable(R.styleable.MaskProgressLayoutZhongjh_imageAddDrawable)
        // 获取显示图片的类
        val imageEngineStr = maskProgressLayoutStyle.getString(R.styleable.MaskProgressLayoutZhongjh_imageEngine)
        // provider的authorities,用于提供给外部的file
        val authority = maskProgressLayoutStyle.getString(R.styleable.MaskProgressLayoutZhongjh_authority)
        val saveStrategy = SaveStrategy(true, authority, "")
        mMediaStoreCompat = MediaStoreCompat(context, saveStrategy)
        // 获取最多显示多少个方框
        val maxCount = maskProgressLayoutStyle.getInteger(R.styleable.MaskProgressLayoutZhongjh_maxCount, 5)
        val imageDeleteColor = maskProgressLayoutStyle.getColor(R.styleable.MaskProgressLayoutZhongjh_imageDeleteColor, colorPrimary)
        val imageDeleteDrawable = maskProgressLayoutStyle.getDrawable(R.styleable.MaskProgressLayoutZhongjh_imageDeleteDrawable)

        // region 音频
        // 音频，删除按钮的颜色
        audioDeleteColor = maskProgressLayoutStyle.getColor(R.styleable.MaskProgressLayoutZhongjh_audioDeleteColor, colorPrimary)
        // 音频 文件的进度条颜色
        audioProgressColor = maskProgressLayoutStyle.getColor(R.styleable.MaskProgressLayoutZhongjh_audioProgressColor, colorPrimary)
        // 音频 播放按钮的颜色
        audioPlayColor = maskProgressLayoutStyle.getColor(R.styleable.MaskProgressLayoutZhongjh_audioPlayColor, colorPrimary)
        // endregion 音频

        // region 遮罩层相关属性

        val maskingColor = maskProgressLayoutStyle.getColor(R.styleable.MaskProgressLayoutZhongjh_maskingColor, colorPrimary)
        val maskingTextSize = maskProgressLayoutStyle.getInteger(R.styleable.MaskProgressLayoutZhongjh_maskingTextSize, 12)

        val maskingTextColor = maskProgressLayoutStyle.getColor(R.styleable.MaskProgressLayoutZhongjh_maskingTextColor, ContextCompat.getColor(context, R.color.z_thumbnail_placeholder))
        var maskingTextContent = maskProgressLayoutStyle.getString(R.styleable.MaskProgressLayoutZhongjh_maskingTextContent)

        // endregion 遮罩层相关属性

        if (imageEngineStr == null) {
            // 必须定义image_engine属性，指定某个显示图片类
            throw RuntimeException("The image_engine attribute must be defined to specify a class for displaying images")
        } else {
            val imageEngineClass: Class<*> //完整类名
            try {
                imageEngineClass = Class.forName(imageEngineStr)
                mImageEngine = imageEngineClass.newInstance() as ImageEngine
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (mImageEngine == null) {
                // image_engine找不到相关类
                throw RuntimeException("Image_engine could not find the related class $imageEngineStr")
            }
        }

        if (drawable == null) {
            drawable = ContextCompat.getDrawable(context, R.color.z_thumbnail_placeholder)
        }
        if (maskingTextContent == null) {
            maskingTextContent = ""
        }

        val view = inflate(context, R.layout.layout_mask_progress_zjh, this)
        mViewHolder = ViewHolder(view)

        // 初始化九宫格的控件
        mViewHolder.rlGrid.layoutManager = GridLayoutManager(context, columnNumber)
        mPhotoAdapter = PhotoAdapter(context, (mViewHolder.rlGrid.layoutManager as GridLayoutManager?)!!, this,
                mImageEngine!!, drawable!!, isOperation, maxCount,
                maskingColor, maskingTextSize, maskingTextColor, maskingTextContent,
                imageDeleteColor, imageDeleteDrawable, imageAddDrawable)
        mViewHolder.rlGrid.adapter = mPhotoAdapter

        maskProgressLayoutStyle.recycle()
        typedArray.recycle()
    }

    override fun setAuthority(authority: String) {
        val saveStrategy = SaveStrategy(true, authority, "")
        mMediaStoreCompat.saveStrategy = saveStrategy
    }

    override fun addLocalFileStartUpload(localFiles: List<LocalFile>) {
        isAuthority()
        // 图片的
        val multiMediaViewImages = ArrayList<MultiMediaView>()
        // 视频的
        val multiMediaViewVideos = ArrayList<MultiMediaView>()
        for (localFile in localFiles) {
            val multiMediaView = MultiMediaView(localFile)
            multiMediaView.isUploading = true
            // 直接处理音频
            if (multiMediaView.isAudio()) {
                // 检测添加多媒体上限
                mPhotoAdapter.notifyDataSetChanged()
                return
            }
            // 处理图片
            if (multiMediaView.isImageOrGif()) {
                multiMediaViewImages.add(multiMediaView)
            }
            // 处理视频
            if (multiMediaView.isVideo()) {
                multiMediaViewVideos.add(multiMediaView)
            }
        }
        mPhotoAdapter.addImageData(multiMediaViewImages)
        mPhotoAdapter.addVideoData(multiMediaViewVideos)
    }

    override fun addImagesUriStartUpload(uris: List<Uri>) {
        isAuthority()
        val multiMediaViews = ArrayList<MultiMediaView>()
        for (uri in uris) {
            val multiMediaView = MultiMediaView(MimeType.JPEG.mimeTypeName)
            multiMediaView.uri = uri
            multiMediaView.isUploading = true
            multiMediaViews.add(multiMediaView)
        }
        mPhotoAdapter.addImageData(multiMediaViews)
    }

    override fun addImagesPathStartUpload(imagePaths: List<String>) {
        isAuthority()
        val multiMediaViews = ArrayList<MultiMediaView>()
        for (string in imagePaths) {
            val multiMediaView = MultiMediaView(MimeType.JPEG.mimeTypeName)
            multiMediaView.path = string
            multiMediaView.uri = mMediaStoreCompat.getUri(string)
            multiMediaView.isUploading = true
            multiMediaViews.add(multiMediaView)
        }
        mPhotoAdapter.addImageData(multiMediaViews)
    }

    override fun setImageUrls(imagesUrls: List<String>) {
        val multiMediaViews = ArrayList<MultiMediaView>()
        for (string in imagesUrls) {
            val multiMediaView = MultiMediaView(MimeType.JPEG.mimeTypeName)
            multiMediaView.url = string
            multiMediaViews.add(multiMediaView)
        }
        mPhotoAdapter.setImageData(multiMediaViews)
        maskProgressLayoutListener?.onAddDataSuccess(multiMediaViews)
    }

    override fun addVideoStartUpload(videoUris: List<Uri>) {
        addVideo(videoUris, icClean = false, isUploading = true)
    }

    override fun setVideoCover(multiMediaView: MultiMediaView, videoPath: String) {
        multiMediaView.path = videoPath
    }

    override fun setVideoUrls(videoUrls: List<String>) {
        val multiMediaViews = ArrayList<MultiMediaView>()
        for (i in videoUrls.indices) {
            val multiMediaView = MultiMediaView(MimeType.MP4.mimeTypeName)
            multiMediaView.isUploading = false
            multiMediaView.url = videoUrls[i]
            multiMediaViews.add(multiMediaView)
        }
        mPhotoAdapter.setVideoData(multiMediaViews)
        maskProgressLayoutListener?.onAddDataSuccess(multiMediaViews)
    }

    override fun addAudioStartUpload(filePath: String, length: Long) {
        isAuthority()
        val multiMediaView = MultiMediaView(MimeType.AAC.mimeTypeName)
        multiMediaView.path = filePath
        multiMediaView.uri = mMediaStoreCompat.getUri(filePath)
        multiMediaView.duration = length
        // 检测添加多媒体上限
        mPhotoAdapter.notifyDataSetChanged()
    }

    override fun setAudioUrls(audioUrls: List<String>) {
        val multiMediaViews: ArrayList<MultiMediaView> = ArrayList()
        for (item in audioUrls) {
            val multiMediaView = MultiMediaView(MimeType.AAC.mimeTypeName)
            multiMediaView.url = item
            audioList.add(multiMediaView)
            multiMediaViews.add(multiMediaView)
        }
    }

    override fun setAudioCover(view: View, file: String) {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(file)

        // ms,时长
        val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
                ?: -1
        val multiMediaView = MultiMediaView(MimeType.AAC.mimeTypeName)
        multiMediaView.path = file
        multiMediaView.uri = mMediaStoreCompat.getUri(file)
        multiMediaView.duration = duration
        multiMediaView.mimeType = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)

        // 显示音频播放控件，当点击播放的时候，才正式下载并且进行播放
        view.visibility = View.VISIBLE
        val recordingItem = RecordingItem()
        recordingItem.path = file
        recordingItem.duration = duration
    }

    override fun reset() {
        audioList.clear()
        // 清空view
        mViewHolder.llContent.removeAllViews()

        // 清空数据和view
        mPhotoAdapter.clearAll()
    }

    override fun getImagesAndVideos(): ArrayList<MultiMediaView> {
        return mPhotoAdapter.getData()
    }

    override fun getImages(): ArrayList<MultiMediaView> {
        return mPhotoAdapter.getImageData()
    }

    override fun getVideos(): ArrayList<MultiMediaView> {
        return mPhotoAdapter.getVideoData()
    }

    override fun getAudios(): ArrayList<MultiMediaView> {
        return audioList
    }

    override fun onAudioClick(view: View) {
    }

    override fun removePosition(position: Int) {
        mPhotoAdapter.removePosition(position)
    }

    override fun setOperation(isOperation: Boolean) {
        this.isOperation = isOperation
        mPhotoAdapter.isOperation = isOperation
    }

    override fun onDestroy() {
        mPhotoAdapter.listener = null
        this.maskProgressLayoutListener = null
    }


    /**
     * 添加视频地址
     *
     * @param videoUris   视频列表
     * @param icClean     是否清除
     * @param isUploading 是否触发上传事件
     */
    private fun addVideo(videoUris: List<Uri>, icClean: Boolean, isUploading: Boolean) {
        isAuthority()
        val multiMediaViews = ArrayList<MultiMediaView>()
        for (i in videoUris.indices) {
            val multiMediaView = MultiMediaView(MimeType.MP4.mimeTypeName)
            multiMediaView.uri = videoUris[i]
            multiMediaView.isUploading = isUploading
            multiMediaViews.add(multiMediaView)
        }
        if (icClean) {
            mPhotoAdapter.setVideoData(multiMediaViews)
        } else {
            mPhotoAdapter.addVideoData(multiMediaViews)
        }
    }

    /**
     * 检测属性
     */
    private fun isAuthority() {
        if (mMediaStoreCompat.saveStrategy.authority == null) {
            // 必须定义authority属性，指定provider的authorities,用于提供给外部的file,否则Android7.0以上报错。也可以代码设置setAuthority
            throw java.lang.RuntimeException("You must define the authority attribute, which specifies the provider's authorities, to serve to external files. Otherwise, Android7.0 will report an error.You can also set setAuthority in code")
        }
    }

    class ViewHolder(rootView: View) {
        val rlGrid: RecyclerView = rootView.findViewById(R.id.rlGrid)
        val llContent: LinearLayout = rootView.findViewById(R.id.llContent)
    }

}