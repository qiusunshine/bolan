package androidx.leanback.app

import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.leanback.media.PlayerAdapter
import androidx.leanback.widget.BaseGridView.OnTouchInterceptListener
import androidx.leanback.widget.PlaybackControlsRow
import androidx.leanback.widget.PlaybackSeekDataProvider
import chuangyuan.ycj.videolibrary.video.GestureVideoPlayer
import chuangyuan.ycj.videolibrary.video.GestureVideoPlayer.DoubleTapArea
import chuangyuan.ycj.videolibrary.widget.VideoPlayerView
import com.alibaba.fastjson.JSON
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import com.google.android.exoplayer2.ui.SubtitleView
import com.google.android.exoplayer2.util.Util
import com.hd.tvpro.MainActivity
import com.hd.tvpro.R
import com.hd.tvpro.app.App
import com.hd.tvpro.event.PlayUrlChange
import com.hd.tvpro.event.SubChange
import com.hd.tvpro.event.SwitchUrlChange
import com.hd.tvpro.setting.LiveListHolder
import com.hd.tvpro.setting.SettingHolder
import com.hd.tvpro.util.*
import com.hd.tvpro.util.http.HttpListener
import com.hd.tvpro.util.http.HttpUtils
import com.hd.tvpro.video.MediaPlayerAdapter
import com.hd.tvpro.video.MyPlaybackTransportControlGlue
import com.hd.tvpro.video.VideoDataHelper
import com.hd.tvpro.video.model.DlanUrlDTO
import com.pngcui.skyworth.dlna.center.DLNAGenaEventBrocastFactory
import com.pngcui.skyworth.dlna.center.DlnaMediaModel
import com.pngcui.skyworth.dlna.center.MediaControlBrocastFactory
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import service.LiveModel
import service.model.LiveItem
import utils.FileUtil
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.abs
import kotlin.math.floor


/** Handles video playback with media controls. */
class PlaybackVideoFragment : VideoSupportFragment(),
    MediaControlBrocastFactory.IMediaControlListener {

    private lateinit var mTransportControlGlue: MyPlaybackTransportControlGlue<MediaPlayerAdapter>
    private var playData: DlanUrlDTO? = null
    private lateinit var playerAdapter: MediaPlayerAdapter
    private lateinit var videoView: VideoPlayerView
    private lateinit var gestureDetector: GestureDetector
    private var settingHolder: SettingHolder? = null
    private val scanLiveTVUtils = ScanLiveTVUtils()
    private lateinit var mMediaControlBorcastFactory: MediaControlBrocastFactory
    private val scope = CoroutineScope(EmptyCoroutineContext)
    private var useDlan = false
    private var lastShowToastTime1: Long = 0
    private var lastShowToastTime2: Long = 0
    private var liveListHolder: LiveListHolder? = null
    private var liveItem: LiveItem? = null

    private var webDlanData: DlanUrlDTO? = null


    private var firstTouch = false
    private var volumeControl = false
    private var toSeek = false
    private var isNowVerticalFullScreen = false
    private var screeHeightPixels: Int = 1
    private var screeWidthPixels: Int = 1
    private var formatBuilder: java.lang.StringBuilder = StringBuilder()
    private var formatter: Formatter = Formatter(formatBuilder, Locale.getDefault())

    /***控制音频 */
    private var exoAudioLayout: android.view.View? = null

    private lateinit var dialogProLayout: View
    private lateinit var videoDialogProText: TextView

    /***亮度布局 */
    private lateinit var exoBrightnessLayout: android.view.View

    /***水印,封面图占位,显示音频和亮度布图 */
    private lateinit var videoAudioImg: ImageView

    /***水印,封面图占位,显示音频和亮度布图 */
    private lateinit var videoBrightnessImg: ImageView

    /***显示音频和亮度 */
    private lateinit var videoAudioPro: ProgressBar

    /***显示音频和亮度 */
    private lateinit var videoBrightnessPro: ProgressBar

    /***音量的最大值 */
    private var mMaxVolume = 0

    /*** 亮度值  */
    private var brightness = -1f

    /**** 当前音量   */
    private var volume = -1

    /*** 新的播放进度  */
    private var newPosition: Long = -1

    /*** 音量管理  */
    private var audioManager: AudioManager? = null

    fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        return false
    }

    fun onKeyDown(keyCode: Int, ev: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showSetting()
            return true
        }
        return false
    }

    fun onBackPressed(): Boolean {
        if (settingHolder != null && settingHolder!!.isShowing()) {
            if (isControlsOverlayVisible) {
                hideControlsOverlay(false)
            }
            settingHolder!!.hide()
            return true
        }
        if (liveListHolder != null && liveListHolder!!.isShowing()) {
            if (isControlsOverlayVisible) {
                hideControlsOverlay(false)
            }
            liveListHolder!!.hide()
            return true
        }
        if (isControlsOverlayVisible && view != null) {
            hideControlsOverlay(false)
            return true
        }
        return false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        videoView = VideoPlayerView(context)
        val glueHost = VideoSupportFragmentGlueHost(this@PlaybackVideoFragment)
        playerAdapter = MediaPlayerAdapter(requireActivity(), videoView, videoDataHelper)

        playerAdapter.playStartTask = Runnable {
            if (activity is MainActivity) {
                (activity as MainActivity).hideHelpDialog()
            }
            if (liveItem != null && !liveItem!!.urls.isNullOrEmpty() && playerAdapter.player?.player?.isCurrentWindowLive == true) {
                playerAdapter.mMediaSourceUri?.let {
                    LiveModel.addGoodUrl(requireContext(), liveItem!!.name, it)
                }
            }
        }
        playerAdapter.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE)

        mTransportControlGlue = MyPlaybackTransportControlGlue(activity, playerAdapter)
        mTransportControlGlue.host = glueHost
        mTransportControlGlue.playWhenPrepared()
        mTransportControlGlue.isControlsOverlayAutoHideEnabled = true
        mTransportControlGlue.isSeekEnabled = true
        mTransportControlGlue.onKeyInterceptor =
            object : MyPlaybackTransportControlGlue.OnKeyInterceptor {
                override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
                    if (keyCode == KeyEvent.KEYCODE_MENU) {
                        showSetting()
                        return true
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if (!isControlsOverlayVisible && (liveListHolder == null || !liveListHolder!!.isShowing())) {
                            showLive()
                            return true
                        }
                    }
                    return false
                }
            }

        /**
         * 支持左右键调节进度
         */
        mTransportControlGlue.seekProvider = object : PlaybackSeekDataProvider() {
            override fun getSeekPositions(): LongArray {
                /**
                 * 每隔十秒
                 */
                val seekGap = 10 * 1000
                val gap: Int = floor((playerAdapter.duration / seekGap).toDouble()).toInt()
                val positions = LongArray(gap)
                for (index in 0 until gap) {
                    positions[index] = (index * seekGap).toLong()
                }
                return positions
            }
        }

        //不加这句，控制条不会自动消失
        val initUrl = PreferenceMgr.getString(context, "playUrl", "file:///android_asset/test.jpg")
        val initTitle = PreferenceMgr.getString(context, "playTitle", null)
        playData = DlanUrlDTO()
        playData?.url = initUrl
        playData?.title = initTitle
        if (!initTitle.isNullOrEmpty()) {
            mTransportControlGlue.title = "\n" + initTitle
            mTransportControlGlue.subtitle = initUrl
            val urls = PreferenceMgr.getString(context, "playUrls", "")
            if (urls.isNotEmpty()) {
                liveItem = LiveItem(initTitle, ArrayList(urls.split("|||")))
            }
        }

        playerAdapter.setDataSource(initUrl, null)
        LiveListHolder.loadBackground(requireContext())
        val parent = mVideoSurface.parent as ViewGroup
        parent.removeView(mVideoSurface)
        parent.addView(videoView, 0)

        if (activity is MainActivity) {
            scope.launch(Dispatchers.Main) {
                delay(2000)
                if (StringUtil.isEmpty(mTransportControlGlue.title) && !playerAdapter.isPlaying) {
                    (activity as MainActivity).showHelpDialog()
                }
            }
        }
        val lastMem = PreferenceMgr.getString(activity, "remote", null)
        if (StringUtil.isNotEmpty(lastMem)) {
            scanLiveTVUtils.checkLastMem(lastMem, {
                startCheckPlayUrl(lastMem)
            }, {
                ToastMgr.shortBottomCenter(activity, "开始扫描远程设备，请稍候")
                scanLiveTVUtils.scan(activity, {
                    if (context != null) {
                        PreferenceMgr.put(context, "remote", it)
                    }
                    startCheckPlayUrl(it)
                }, {
                    if (!playerAdapter.isPlaying) {
                        ToastMgr.longCenter(activity, "扫描失败，请打开海阔视界的网页投屏")
                    }
                    //再给上次记录的地址一次机会，可能忘记打开手机
                    startCheckPlayUrl(lastMem)
                })
            })
        } else {
            startScan(false)
        }

        //字幕背景透明化
        val subtitleView: SubtitleView? = videoView.findViewById(R.id.exo_subtitles)
        subtitleView?.setStyle(
            CaptionStyleCompat(
                Color.WHITE,
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                CaptionStyleCompat.EDGE_TYPE_NONE,
                Color.WHITE,  /* typeface= */
                null
            )
        )
        initDlan()
    }

    private fun initDlan() {
        mMediaControlBorcastFactory = MediaControlBrocastFactory(App.INSTANCE)
        mMediaControlBorcastFactory.register(this)
        playerAdapter.addListener(object : PlayerAdapter.Callback() {
            override fun onPreparedStateChanged(adapter: PlayerAdapter?) {
                if (playerAdapter.duration > 0) {
                    DLNAGenaEventBrocastFactory.sendDurationEvent(
                        App.INSTANCE,
                        playerAdapter.duration.toInt()
                    )
                }
            }

            override fun onPlayStateChanged(adapter: PlayerAdapter?) {
                if (playerAdapter.isPlaying) {
                    DLNAGenaEventBrocastFactory.sendPlayStateEvent(App.INSTANCE)
                } else {
                    DLNAGenaEventBrocastFactory.sendPauseStateEvent(App.INSTANCE)
                }
            }

            override fun onError(adapter: PlayerAdapter?, errorCode: Int, errorMessage: String?) {
                if (liveItem != null && !liveItem!!.urls.isNullOrEmpty()) {
                    playerAdapter.mMediaSourceUri?.let {
                        LiveModel.clearGoodUrl(requireContext(), it)
                    }
                }
            }
        })
        //定时器
        scope.launch {
            var count = 1
            while (true) {
                try {
                    if (App.INSTANCE.getDevInfo()?.status == true) {
                        if (activity == null || activity?.isFinishing == true) {
                            break
                        }
                        withContext(Dispatchers.Main) {
                            DLNAGenaEventBrocastFactory.sendDurationEvent(
                                App.INSTANCE,
                                playerAdapter.duration.toInt()
                            )
                            DLNAGenaEventBrocastFactory.sendSeekEvent(
                                App.INSTANCE,
                                playerAdapter.currentPosition.toInt()
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (e is CancellationException) {
                        break
                    }
                }
                try {
                    if (count == 1) {
                        count++
                    } else {
                        count--
                        if (playerAdapter?.player != null && playerAdapter?.player?.isPlaying == true) {
                            playerAdapter?.memoryPosition()
                            Log.d(TAG, "initDlan: memoryPosition")
                        }
                    }
                } catch (e: Exception) {
                }
                delay(1000)
            }
        }
    }

    private fun startScan(notify: Boolean) {
        ToastMgr.shortBottomCenter(activity, "开始扫描远程设备，请稍候")
        scanLiveTVUtils.scan(activity, {
            PreferenceMgr.put(context, "remote", it)
            if (notify) {
                ToastMgr.shortBottomCenter(
                    context, "已扫描到设备：" +
                            it.replace(ScanLiveTVUtils.HTTP, "")
                                .replace(ScanLiveTVUtils.PORT, "")
                )
            }
            startCheckPlayUrl(it)
        }, {
            if (!playerAdapter.isPlaying) {
                ToastMgr.longCenter(activity, "扫描失败，请打开海阔视界的网页投屏")
            }
        })
    }

    /**
     * 手势结束
     */
    @Synchronized
    private fun endGesture() {
        if (exoAudioLayout == null) {
            return
        }
        volume = -1
        brightness = -1f
        showGestureView(View.GONE)
        if (newPosition >= 0) {
            if (playerAdapter.player?.player?.isCurrentWindowLive == true) {
                newPosition = -1
                return
            }
            playerAdapter.seekTo(newPosition)
            videoView.seekFromPlayer(newPosition)
            newPosition = -1
        }
    }

    /***
     * 显示隐藏手势布局
     *
     * @param visibility 状态
     */
    private fun showGestureView(visibility: Int) {
        if (exoAudioLayout == null) {
            return
        }
        exoAudioLayout!!.visibility = visibility
        exoBrightnessLayout.visibility = visibility
        dialogProLayout.visibility = visibility
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //简单支持手势操作
        gestureDetector = GestureDetector(context, object :
            GestureDetector.SimpleOnGestureListener() {

            init {
                audioManager = activity!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                mMaxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val displayMetrics = activity!!.resources.displayMetrics
                screeHeightPixels = displayMetrics.heightPixels
                screeWidthPixels = displayMetrics.widthPixels
                brightness = getScreenBrightness(activity!!) / 255.0f
            }

            /**
             * 1.获取系统默认屏幕亮度值 屏幕亮度值范围（0-255）
             */
            private fun getScreenBrightness(context: Context): Int {
                val contentResolver = context.contentResolver
                val defVal = 125
                return Settings.System.getInt(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS, defVal
                )
            }

            private fun checkView() {
                if (exoAudioLayout == null) {
                    val videoProgressId =
                        chuangyuan.ycj.videolibrary.R.layout.simple_exo_video_progress_dialog
                    val audioId: Int =
                        chuangyuan.ycj.videolibrary.R.layout.simple_video_audio_brightness_dialog
                    val brightnessId: Int =
                        chuangyuan.ycj.videolibrary.R.layout.simple_video_audio_brightness_dialog
                    dialogProLayout = FrameLayout.inflate(context, videoProgressId, null)
                    exoAudioLayout = FrameLayout.inflate(context, audioId, null)
                    exoBrightnessLayout = FrameLayout.inflate(context, brightnessId, null)
                    exoAudioLayout!!.visibility = FrameLayout.GONE
                    exoBrightnessLayout.visibility = FrameLayout.GONE
                    dialogProLayout.visibility = FrameLayout.GONE
                    videoView.addView(dialogProLayout, videoView.childCount)
                    videoView.addView(exoAudioLayout, videoView.childCount)
                    videoView.addView(exoBrightnessLayout, videoView.childCount)
                    videoDialogProText =
                        dialogProLayout.findViewById(chuangyuan.ycj.videolibrary.R.id.exo_video_dialog_pro_text)
                    videoAudioImg =
                        exoAudioLayout!!.findViewById(chuangyuan.ycj.videolibrary.R.id.exo_video_audio_brightness_img)
                    videoAudioPro =
                        exoAudioLayout!!.findViewById(chuangyuan.ycj.videolibrary.R.id.exo_video_audio_brightness_pro)
                    videoBrightnessImg =
                        exoBrightnessLayout.findViewById(chuangyuan.ycj.videolibrary.R.id.exo_video_audio_brightness_img)
                    videoBrightnessPro =
                        exoBrightnessLayout.findViewById(chuangyuan.ycj.videolibrary.R.id.exo_video_audio_brightness_pro)
                }
            }

            private fun setTimePosition(seekTime: SpannableString) {
                dialogProLayout.visibility = View.VISIBLE
                videoDialogProText.text = seekTime
            }

            private fun setVolumePosition(mMaxVolume: Int, currIndex: Int) {
                if (exoAudioLayout != null) {
                    if (exoAudioLayout!!.visibility != View.VISIBLE) {
                        videoAudioPro.max = mMaxVolume
                    }
                    exoAudioLayout!!.visibility = View.VISIBLE
                    videoAudioPro.progress = currIndex
                    videoAudioImg.setImageResource(if (currIndex == 0) chuangyuan.ycj.videolibrary.R.drawable.ic_volume_off_white_48px else chuangyuan.ycj.videolibrary.R.drawable.ic_volume_up_white_48px)
                }
            }

            private fun setBrightnessPosition(mMaxVolume: Int, currIndex: Int) {
                if (exoBrightnessLayout.visibility != View.VISIBLE) {
                    videoBrightnessPro.max = mMaxVolume
                    videoBrightnessImg.setImageResource(chuangyuan.ycj.videolibrary.R.drawable.ic_brightness_6_white_48px)
                }
                exoBrightnessLayout.visibility = View.VISIBLE
                videoBrightnessPro.progress = currIndex
            }


            /****
             * 滑动进度
             *
             * @param  seekTimePosition  滑动的时间
             * @param  duration         视频总长
             * @param  seekTime    滑动的时间 格式化00:00
             * @param  totalTime    视频总长 格式化00:00
             */
            private fun showProgressDialog(
                nowTime: String,
                seekTimePosition: Long,
                duration: Long,
                seekTime: String,
                totalTime: String
            ) {
                checkView()
                newPosition = seekTimePosition
                if (playerAdapter.player?.player?.isCurrentWindowLive == true) {
                    return
                }
                val stringBuilder = "$nowTime/$seekTime"
                val blueSpan = ForegroundColorSpan(
                    ContextCompat.getColor(
                        activity!!, R.color.simple_exo_style_color
                    )
                )
                val spannableString = SpannableString(stringBuilder)
                spannableString.setSpan(
                    blueSpan,
                    nowTime.length,
                    stringBuilder.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setTimePosition(spannableString)
            }

            /**
             * 滑动改变声音大小
             *
             * @param percent percent 滑动
             */
            private fun showVolumeDialog(percent: Float) {
                checkView()
                if (volume == -1) {
                    volume = audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
                    if (volume < 0) {
                        volume = 0
                    }
                }
                var index: Int = (percent * mMaxVolume).toInt() + volume
                if (index > mMaxVolume) {
                    index = mMaxVolume
                } else if (index < 0) {
                    index = 0
                }
                // 变更进度条 // int i = (int) (index * 1.5 / mMaxVolume * 100);
                //  String s = i + "%";  // 变更声音
                audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0)
                setVolumePosition(mMaxVolume, index)
            }

            /**
             * 滑动改变亮度
             *
             * @param percent 值大小
             */
            @Synchronized
            private fun showBrightnessDialog(percent: Float) {
                checkView()
                if (brightness < 0) {
                    brightness = activity!!.window.attributes.screenBrightness
                    if (brightness <= 0.00f) {
                        brightness = 0.50f
                    } else if (brightness < 0.01f) {
                        brightness = 0.01f
                    }
                }
                val lpa = activity!!.window.attributes
                lpa.screenBrightness = brightness + percent
                if (lpa.screenBrightness > 1.0) {
                    lpa.screenBrightness = 1.0f
                } else if (lpa.screenBrightness < 0.01f) {
                    lpa.screenBrightness = 0.01f
                }
                activity!!.window.attributes = lpa
                setBrightnessPosition(
                    100,
                    (lpa.screenBrightness * 100).toInt()
                )
            }

            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                if (isControlsOverlayVisible) {
//                    hideControlsOverlay(true)
                } else {
                    tickle()
                }
                return super.onSingleTapConfirmed(e)
            }

            override fun onLongPress(e: MotionEvent?) {
                if (isControlsOverlayVisible) {
                    hideControlsOverlay(false)
                }
                val dm = DisplayMetrics()
                val windowManager =
                    context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
                windowManager!!.defaultDisplay.getMetrics(dm)
                val width: Int = dm.widthPixels
                if (e != null && e.x < width / 2) {
                    showLive()
                } else {
                    showSetting()
                }
                super.onLongPress(e)
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                if (activity == null || activity!!.isFinishing) {
                    return true
                }
                val width: Int = videoView.playerView.width
                val gap = width / 6
                val right = width - gap
                val tapArea: DoubleTapArea = when {
                    e!!.x < gap -> {
                        DoubleTapArea.LEFT
                    }
                    e.x > right -> {
                        DoubleTapArea.RIGHT
                    }
                    else -> {
                        DoubleTapArea.CENTER
                    }
                }
                onDoubleTapListener.onDoubleTap(e, tapArea)
                return true
            }

            override fun onDown(e: MotionEvent?): Boolean {
                firstTouch = true
                return super.onDown(e)
            }

            /**
             * 滑动
             */
            override fun onScroll(
                e1: MotionEvent,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (e2.pointerCount > 1) {
                    return false
                }
                val mOldX = e1.x
                val mOldY = e1.y
                val deltaY = mOldY - e2.y
                var deltaX = mOldX - e2.x
                if (firstTouch) {
                    toSeek = abs(distanceX) >= abs(distanceY)
                    if (isNowVerticalFullScreen) {
                        volumeControl = mOldX > screeWidthPixels * 0.5f
                        if (mOldY < screeHeightPixels * 0.1f) {
                            return false
                        }
                    } else {
                        volumeControl = mOldX > screeHeightPixels * 0.5f
                        if (mOldY < screeWidthPixels * 0.1f) {
                            return false
                        }
                    }
                    firstTouch = false
                }
                if (toSeek) {
                    deltaX = -deltaX
                    val position: Long = playerAdapter.currentPosition
                    val duration: Long = playerAdapter.duration
                    var newPosition: Long =
                        (position + deltaX * duration / screeHeightPixels / 3).toLong()
                    if (newPosition > duration) {
                        newPosition = duration
                    } else if (newPosition <= 0) {
                        newPosition = 0
                    }
                    showProgressDialog(
                        Util.getStringForTime(formatBuilder, formatter, position),
                        newPosition,
                        duration,
                        Util.getStringForTime(formatBuilder, formatter, newPosition),
                        Util.getStringForTime(formatBuilder, formatter, duration)
                    )
                } else {
                    val percent: Float = deltaY / screeHeightPixels
                    if (volumeControl) {
                        showVolumeDialog(percent)
                    } else {
                        showBrightnessDialog(percent)
                    }
                }
                return super.onScroll(e1, e2, distanceX, distanceY)
            }
        })

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDlan(media: DlnaMediaModel) {
        Log.d(TAG, "onDlan: " + JSON.toJSONString(media))
        if (activity?.isFinishing == true) {
            return
        }
        useDlan = true
        playData = DlanUrlDTO()
        playData?.apply {
            url = media.url?.split("##\\|")?.get(0) ?: media.url
            headers = HttpParser.getEncodedHeaders(media.url)
            title = media.title
        }
        play()
        if (isControlsOverlayVisible) {
            hideControlsOverlay(false)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSwitch(media: SwitchUrlChange) {
        if (activity?.isFinishing == true) {
            return
        }
        playData = DlanUrlDTO()
        playData?.apply {
            url = media.url
        }
        PreferenceMgr.put(context, "playUrl", media.url)
        play(false)
        if (isControlsOverlayVisible) {
            hideControlsOverlay(false)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlay(media: PlayUrlChange) {
        if (activity?.isFinishing == true) {
            return
        }
        if (playData == null) {
            playData = DlanUrlDTO()
        }
        val urlEmpty = media.url.isNullOrEmpty()
        playData?.apply {
            if (!urlEmpty) {
                url = media.url
                title = if (media.name.isNullOrEmpty()) media.url else media.name
            }
            subtitle = media.subtitle
        }
        play(true)
        if (isControlsOverlayVisible) {
            hideControlsOverlay(false)
        }
    }

    override fun onInterceptInputEvent(event: InputEvent?): Boolean {
        var keyCode = KeyEvent.KEYCODE_UNKNOWN
        var keyAction = 0
        if (event is KeyEvent) {
            keyCode = event.keyCode
            keyAction = event.action
        }
        if (!isControlsOverlayVisible) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    val act = activity as MainActivity
                    if (act.dismissDialog()) {
                        return true
                    }
                    if (onBackPressed()) {
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (playerAdapter.player?.player?.isCurrentWindowLive == true) {
                        showLive()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (playerAdapter.player?.player?.isCurrentWindowLive == true) {
                        showLive()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (keyAction == KeyEvent.ACTION_DOWN) {
                        val liveIndex = findLiveIndex()
                        if (playerAdapter.player?.player?.isCurrentWindowLive == true
                            || liveIndex >= 0
                        ) {
                            if (liveIndex > 0) {
                                onSwitch(SwitchUrlChange(liveItem!!.urls[liveIndex - 1]))
                                ToastMgr.shortBottomCenter(context, "已切换线路${liveIndex}")
                            }
                        } else {
                            fastPositionJump(-15)
                            val now = System.currentTimeMillis()
                            if (now - lastShowToastTime1 > 5 * 1000) {
                                ToastMgr.shortBottomCenter(context, "已快退15秒")
                            }
                            lastShowToastTime1 = now
                        }
                    }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (keyAction == KeyEvent.ACTION_DOWN) {
                        val liveIndex = findLiveIndex()
                        if (playerAdapter.player?.player?.isCurrentWindowLive == true
                            || liveIndex >= 0
                        ) {
                            if (liveIndex >= 0 && liveIndex < liveItem!!.urls.size - 1) {
                                onSwitch(SwitchUrlChange(liveItem!!.urls[liveIndex + 1]))
                                ToastMgr.shortBottomCenter(context, "已切换线路${liveIndex + 2}")
                            }
                        } else {
                            fastPositionJump(15)
                            val now = System.currentTimeMillis()
                            if (now - lastShowToastTime2 > 5 * 1000) {
                                ToastMgr.shortBottomCenter(context, "已快进15秒")
                            }
                            lastShowToastTime2 = now
                        }
                    }
                    return true
                }
                KeyEvent.KEYCODE_MENU -> {
                    showSetting()
                    return true
                }
            }
        }
        return super.onInterceptInputEvent(event)
    }

    private fun findLiveIndex(): Int {
        var index = -1
        if (liveItem == null || liveItem!!.urls.isNullOrEmpty()) {
            return index
        }
        liveItem!!.urls.forEachIndexed { i, s ->
            if (s == playData?.url) {
                index = i
            }
        }
        return index
    }

    override fun onResume() {
        super.onResume()
        val mOnTouchInterceptListener =
            OnTouchInterceptListener { event ->
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_UP -> endGesture()
                    else -> {
                    }
                }
                gestureDetector.onTouchEvent(event) || onInterceptInputEvent(
                    event
                )
            }
        verticalGridView.setOnTouchInterceptListener(mOnTouchInterceptListener)
    }

    override fun onDestroy() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        super.onDestroy()
    }

    /**
     * 简单支持手势操作
     */
    private val onDoubleTapListener =
        GestureVideoPlayer.OnDoubleTapListener { e, tapArea ->
            run {
                if (tapArea == DoubleTapArea.CENTER) {
                    if (playerAdapter.isPlaying) {
                        playerAdapter.pause()
                    } else {
                        playerAdapter.play()
                    }
                } else if (tapArea == DoubleTapArea.LEFT) {
                    fastPositionJump(-10)
                } else {
                    fastPositionJump(10)
                }
            }
        }

    /**
     * 快进快退
     */
    private fun fastPositionJump(forward: Long) {
        var newPos: Long = playerAdapter.currentPosition + forward * 1000
        if (playerAdapter.duration < newPos) {
            newPos = playerAdapter.duration - 1000
        } else if (newPos < 0) {
            if (forward > 0) {
                newPos = forward * 1000
            } else {
                newPos = 0
            }
        }
        playerAdapter.seekTo(newPos)
    }

    private val videoDataHelper = object : VideoDataHelper {
        override fun next(autoEnd: Boolean) {
            if (useDlan) {
                if (autoEnd) {
                    ToastMgr.shortBottomCenter(context, "正在使用DLAN投屏，不支持自动下一集")
                } else {
                    ToastMgr.shortBottomCenter(context, "正在使用DLAN投屏，手机上操作吧~")
                }
                return
            }
            val lastMem = PreferenceMgr.getString(activity, "remote", null)
            lastMem?.let {
                ToastMgr.shortBottomCenter(context, "播放下一集")
                scope.launch(Dispatchers.IO) {
                    Log.d(TAG, "playNext: ${lastMem}/playNext")
                    HttpUtils.get("${lastMem}/playNext", object : HttpListener {
                        override fun success(body: String?) {
                            Log.d(TAG, "playNext success: $body")
                        }

                        override fun failed(msg: String?) {
                            //ignore
                        }
                    })
                }
            }
        }

        override fun previous() {

        }

        override fun fastForward() {
            fastPositionJump(10)
        }

        override fun rewind() {
            fastPositionJump(-10)
        }

    }

    private fun startCheckPlayUrl(url: String) {
        if (useDlan) {
            return
        }
        scope.launch(Dispatchers.IO) {
            HttpUtils.get("$url/playUrl?enhance=true", object : HttpListener {
                override fun success(body: String?) {
                    if (useDlan) {
                        return
                    }
                    restartCheck(url)
                    scope.launch(Dispatchers.Main) {
                        if (webDlanData == null) {
                            playData = JSON.parseObject(body, DlanUrlDTO::class.java)
                            webDlanData = playData
                            play()
                        } else if (!JSON.toJSONString(webDlanData).equals(body)) {
                            playData = JSON.parseObject(body, DlanUrlDTO::class.java)
                            webDlanData = playData
                            play()
                        }
                    }
                }

                override fun failed(msg: String?) {
                    //ignore
                    if (useDlan) {
                        return
                    }
                    restartCheck(url)
                }
            })
        }
    }

    private fun play(clearSwitch: Boolean = true) {
//        if (activity is MainActivity) {
//            if ((activity as MainActivity).isOnPause) {
//                WindowHelper.setTopApp(requireActivity())
//            }
//        }
        if (clearSwitch) {
            liveItem = null
        }
        if (isControlsOverlayVisible) {
            hideControlsOverlay(false)
        }
        if (activity is MainActivity) {
            (activity as MainActivity).hideHelpDialog()
        }
        playData?.let {
            val t =
                if (it.title.isNullOrEmpty() || it.title == it.url) FileUtil.getFileName(it.url) else it.title
            mTransportControlGlue.title = "\n" + t
            mTransportControlGlue.subtitle = it.subtitle
            playerAdapter.setDataSource(it.url, it.headers, it.subtitle)
        }
        hideControlsOverlay(true)
    }

    private fun restartCheck(url: String) {
        if (activity?.isFinishing == false) {
            scope.launch(Dispatchers.IO) {
                delay(1000)
                if (activity?.isFinishing == false) {
                    startCheckPlayUrl(url)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mTransportControlGlue.pause()
    }

    override fun getSurfaceView(): SurfaceView {
        return videoView?.playerView?.videoSurfaceView as SurfaceView
    }

    fun showSetting() {
        if (settingHolder == null) {
            settingHolder =
                SettingHolder(requireContext(), object : SettingHolder.SettingUpdateListener {
                    override fun update(option: SettingHolder.Option) {
                        when (option) {
                            SettingHolder.Option.SCREEN -> {
                                playerAdapter.loadResizeMode()
                            }
                            SettingHolder.Option.SPEED -> {
                                playerAdapter.loadSpeed()
                            }
                            SettingHolder.Option.RESET -> {
                                if (useDlan) {
                                    useDlan = false
                                }
                                PreferenceMgr.remove(activity, "remote")
                                startScan(true)
                            }
                            SettingHolder.Option.FINISH -> {
                                activity?.finish()
                            }
                        }
                    }
                })
        }
        if (settingHolder!!.isShowing()) {
            return
        }
        settingHolder!!.show(videoView, playData?.url, liveItem)
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLiveUrlChange(subChange: SubChange) {
        if (liveListHolder != null && liveListHolder!!.isShowing()) {
            liveListHolder?.hide()
        }
        liveListHolder = null
        LiveListHolder.liveData = ArrayList()
        LiveListHolder.channelNamesList = ArrayList()
        showLive()
    }

    fun showLive() {
        if (liveListHolder == null) {
            liveListHolder =
                LiveListHolder(requireContext()) { liveItem ->
                    if(this.liveItem != null){
                        //换台，换台回来保证能播放
                        LiveModel.reSortLastLiveItem(this.liveItem!!)
                    }
                    playData = DlanUrlDTO()
                    playData?.apply {
                        url = liveItem.urls[0]
                        title = liveItem.name
                    }
                    this.liveItem = liveItem
                    PreferenceMgr.put(context, "playUrl", liveItem.urls[0])
                    PreferenceMgr.put(context, "playTitle", liveItem.name)
                    PreferenceMgr.put(context, "playUrls", liveItem.urls.joinToString("|||"))
                    play(false)
                    if (isControlsOverlayVisible) {
                        hideControlsOverlay(false)
                    }
                    liveListHolder?.hide()
                }
        }
        if (liveListHolder!!.isShowing()) {
            return
        }
        liveListHolder!!.show(videoView)
    }

    companion object {
        private const val TAG = "PlaybackVideoFragment"
    }

    override fun onPlayCommand() {
        playerAdapter.play()
    }

    override fun onPauseCommand() {
        playerAdapter.pause()
    }

    override fun onStopCommand() {
        playerAdapter.pause()
    }

    override fun onSeekCommand(time: Int) {
        val pos: Long = when {
            time < 0 -> 0
            time > playerAdapter.duration -> playerAdapter.duration
            else -> {
                time.toLong()
            }
        }
        playerAdapter.seekTo(pos)
    }

    override fun onDestroyView() {
        scope.cancel()
        super.onDestroyView()
    }
}