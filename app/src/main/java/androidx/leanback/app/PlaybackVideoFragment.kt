package androidx.leanback.app

import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import androidx.leanback.media.PlayerAdapter
import androidx.leanback.widget.BaseGridView.OnTouchInterceptListener
import androidx.leanback.widget.PlaybackControlsRow
import androidx.leanback.widget.PlaybackSeekDataProvider
import chuangyuan.ycj.videolibrary.video.GestureVideoPlayer
import chuangyuan.ycj.videolibrary.video.GestureVideoPlayer.DoubleTapArea
import chuangyuan.ycj.videolibrary.widget.VideoPlayerView
import com.alibaba.fastjson.JSON
import com.hd.tvpro.MainActivity
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
import service.model.LiveItem
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.EmptyCoroutineContext
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
        if (isControlsOverlayVisible) {
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
        })
        //定时器
        scope.launch {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //简单支持手势操作
        gestureDetector = GestureDetector(context, object :
            GestureDetector.SimpleOnGestureListener() {
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
            url = media.url
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
        playData = DlanUrlDTO()
        playData?.apply {
            url = media.url
            title = media.url
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
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (keyAction == KeyEvent.ACTION_DOWN) {
                        fastPositionJump(-15)
                        val now = System.currentTimeMillis()
                        if (now - lastShowToastTime1 > 5 * 1000) {
                            ToastMgr.shortBottomCenter(context, "已快退15秒")
                        }
                        lastShowToastTime1 = now
                    }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (keyAction == KeyEvent.ACTION_DOWN) {
                        fastPositionJump(15)
                        val now = System.currentTimeMillis()
                        if (now - lastShowToastTime2 > 5 * 1000) {
                            ToastMgr.shortBottomCenter(context, "已快进15秒")
                        }
                        lastShowToastTime2 = now
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

    override fun onResume() {
        super.onResume()
        val mOnTouchInterceptListener =
            OnTouchInterceptListener { event ->
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
            mTransportControlGlue.title = "\n" + it.title
            mTransportControlGlue.subtitle = it.url
            playerAdapter.setDataSource(it.url, it.headers)
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
        LiveListHolder.settingArrayList = ArrayList()
        showLive()
    }

    fun showLive() {
        if (liveListHolder == null) {
            liveListHolder =
                LiveListHolder(requireContext()) { liveItem ->
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