package com.hd.tvpro.video

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.view.SurfaceHolder
import androidx.leanback.media.PlaybackBaseControlGlue
import androidx.leanback.media.PlaybackGlueHost
import androidx.leanback.media.PlayerAdapter
import androidx.leanback.media.SurfaceHolderGlueHost
import chuangyuan.ycj.videolibrary.listener.VideoInfoListener
import chuangyuan.ycj.videolibrary.video.ExoUserPlayer
import chuangyuan.ycj.videolibrary.video.ManualPlayer
import chuangyuan.ycj.videolibrary.video.VideoPlayerManager
import chuangyuan.ycj.videolibrary.widget.VideoPlayerView
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.AnimUtils
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.video.VideoSize
import com.hd.tvpro.util.PreferenceMgr
import com.hd.tvpro.util.StringUtil
import java.io.*
import java.util.*
import kotlin.collections.ArrayList


/**
 * 作者：By 15968
 * 日期：On 2021/10/24
 * 时间：At 18:02
 */
class MediaPlayerAdapter constructor(
    private var mContext: Context,
    private var videoView: VideoPlayerView,
    private val videoDataHelper: VideoDataHelper
) : PlayerAdapter() {
    var player: ManualPlayer? = null
    var mSurfaceHolderGlueHost: SurfaceHolderGlueHost? = null
    val mRunnable: Runnable = object : Runnable {
        override fun run() {
            //这里更新太频繁，外部监听器暂时不管
            callback.onCurrentPositionChanged(this@MediaPlayerAdapter)
            mHandler.postDelayed(this, getProgressUpdatingInterval().toLong())
        }
    }
    val mHandler = Handler()
    var mInitialized = false // true when the MediaPlayer is prepared/initialized

    var mMediaSourceUri: String? = null
    var headers: Map<String, String>? = null
    var subtitle: String? = null
    var mHasDisplay = false
    var mBufferedProgress: Long = 0
    var playStartTask: Runnable? = null

    private var listeners: MutableList<Callback> = ArrayList()

    private val videoPlayerSurfaceHolderCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
//            setDisplay(surfaceHolder)
        }

        override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {

        }

        override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
//            setDisplay(null)
        }
    }

    fun addListener(listener: Callback) {
        listeners.add(listener)
    }

    private fun initPlayer() {
        checkListener()
        if (player == null || player?.player == null) {
            player = VideoPlayerManager.Builder(VideoPlayerManager.TYPE_PLAY_MANUAL, videoView)
                .setTitle("")
                .create()
            val videoInfoListener = object : VideoInfoListener {
                override fun onPlayStart(currPosition: Long) {
                    mInitialized = true
                    listeners.forEach {
                        it.onBufferingStateChanged(this@MediaPlayerAdapter, false)
                        it.onPreparedStateChanged(this@MediaPlayerAdapter)
                    }
                    playStartTask?.run()
                }

                override fun onLoadingChanged() {
                    listeners.forEach {
                        it.onBufferingStateChanged(this@MediaPlayerAdapter, true)
                    }
                }

                override fun isPlaying(playWhenReady: Boolean) {
                    listeners.forEach {
                        it.onPlayStateChanged(this@MediaPlayerAdapter)
                    }
                }

                override fun onPlayerError(e: ExoPlaybackException?) {
                    onError(e)
                }

                override fun onPlayEnd() {
                    listeners.forEach {
                        it.onPlayCompleted(this@MediaPlayerAdapter)
                    }
                    videoDataHelper.next(true)
                }
            }

            player!!.player.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    onError(error)
                }
            })

            val listener = object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    super.onVideoSizeChanged(videoSize)
                    listeners.forEach {
                        it.onVideoSizeChanged(
                            this@MediaPlayerAdapter,
                            videoSize.width,
                            videoSize.height
                        )
                    }
                }

            }

            player?.let {
                it.addVideoInfoListener(videoInfoListener)
                it.player.addListener(listener)
            }
            val progressListener =
                AnimUtils.UpdateProgressListener { position, bufferedPosition, duration ->
                    mBufferedProgress = bufferedPosition
                    listeners.forEach {
                        it.onBufferedPositionChanged(this@MediaPlayerAdapter)
                        it.onDurationChanged(this@MediaPlayerAdapter)
                        it.onCurrentPositionChanged(this@MediaPlayerAdapter)
                    }
                }
            videoView.playbackControlView.addUpdateProgressListener(progressListener)
            videoView.isNetworkNotify = false
            loadResizeMode()
            loadSpeed()
//            videoView.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
        }
    }

    private fun onError(e: Exception?) {
        e?.let {
            e.printStackTrace()
            listeners.forEach {
                it.onError(this@MediaPlayerAdapter, 0, e.message)
            }
        }
    }

    private fun checkListener() {
        if (listeners.isEmpty() || (callback != null && !listeners.contains(callback))) {
            listeners.add(callback)
        }
    }

    fun loadResizeMode() {
        when (PreferenceMgr.getInt(mContext, "screen", 0)) {
            0 -> {
                videoView.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
            1 -> {
                videoView.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
            2 -> {
                videoView.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
            }
            3 -> {
                videoView.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
            }
        }
    }

    fun loadSpeed() {
        val speed = PreferenceMgr.getFloat(mContext, "speed", 1f)
        player!!.setPlaybackParameters(speed, 1f)
    }

    /**
     * Constructor.
     */
    fun MediaPlayerAdapter(context: Context) {
        mContext = context
    }

    override fun onAttachedToHost(host: PlaybackGlueHost?) {
        if (host is SurfaceHolderGlueHost) {
            mSurfaceHolderGlueHost = host
            mSurfaceHolderGlueHost!!.setSurfaceHolderCallback(videoPlayerSurfaceHolderCallback)
        }
    }

    /**
     * Will reset the [MediaPlayer] and the glue such that a new file can be played. You are
     * not required to call this method before playing the first file. However you have to call it
     * before playing a second one.
     */
    private fun reset() {
        player?.let {
            changeToUnitialized()
            try {
                player!!.reset()
            } catch (e: Exception) {
            }
        }
    }

    private fun changeToUnitialized() {
        if (player != null) {
            if (mHasDisplay) {
                listeners.forEach {
                    it.onPreparedStateChanged(this@MediaPlayerAdapter)
                }
            }
        }
    }

    /**
     * Release internal MediaPlayer. Should not use the object after call release().
     */
    private fun release() {
        player?.let {
            changeToUnitialized()
            mHasDisplay = false
            player!!.releasePlayers()
        }
    }

    override fun onDetachedFromHost() {
        player?.let {
            if (player?.player?.isCurrentWindowLive != true) {
                PreferenceMgr.put(mContext, memUrlKey, mMediaSourceUri)
                var pos = 0L
                if (it.duration - it.currentPosition > 3 * 60 * 1000 && it.duration > 10 * 60 * 1000 && it.currentPosition > 3 * 60 * 1000) {
                    //最后还剩3分钟不管
                    //总时长不超过10分钟不管
                    //播放时长没超过3分钟不管
                    pos = it.currentPosition
                }
                PreferenceMgr.put(mContext, memPosKey, pos)
            }
        }
        if (mSurfaceHolderGlueHost != null) {
            mSurfaceHolderGlueHost!!.setSurfaceHolderCallback(null)
            mSurfaceHolderGlueHost = null
        }

        reset()
        release()
    }

    protected fun onError(what: Int, extra: Int): Boolean {
        return false
    }

    protected fun onSeekComplete() {}

    protected fun onInfo(what: Int, extra: Int): Boolean {
        return false
    }

    /**
     * @see MediaPlayer.setDisplay
     */
    fun setDisplay(surfaceHolder: SurfaceHolder?) {
        val hadDisplay = mHasDisplay
        mHasDisplay = surfaceHolder != null
        if (hadDisplay == mHasDisplay) {
            return
        }
        player?.let {
            it.player.setVideoSurfaceHolder(surfaceHolder)
        }
        listeners.forEach {
            if (mHasDisplay) {
                if (player != null) {
                    it.onPreparedStateChanged(this@MediaPlayerAdapter)
                }
            } else {
                if (player != null) {
                    it.onPreparedStateChanged(this@MediaPlayerAdapter)
                }
            }
        }
    }

    override fun setProgressUpdatingEnabled(enabled: Boolean) {
        mHandler.removeCallbacks(mRunnable)
        if (!enabled) {
            return
        }
        mHandler.postDelayed(mRunnable, getProgressUpdatingInterval().toLong())
    }

    /**
     * Return updating interval of progress UI in milliseconds. Subclass may override.
     * @return Update interval of progress UI in milliseconds.
     */
    fun getProgressUpdatingInterval(): Int {
        return 16
    }

    override fun isPlaying(): Boolean {
        return if (player != null) {
            player!!.isPlaying
        } else false
    }

    override fun getDuration(): Long {
        return if (player != null) player!!.duration else -1
    }

    override fun getCurrentPosition(): Long {
        return if (player != null) player!!.currentPosition else -1
    }

    override fun play() {
        player?.let {
            if (it.isPlaying) {
                return
            }
            it.setStartOrPause(true)
            listeners.forEach {
                it.onPlayStateChanged(this@MediaPlayerAdapter)
                it.onCurrentPositionChanged(this@MediaPlayerAdapter)
            }
        }
    }

    override fun pause() {
        if (isPlaying && player != null) {
            player!!.setStartOrPause(false)
            listeners.forEach {
                it.onPlayStateChanged(this@MediaPlayerAdapter)
            }
        }
    }

    override fun next() {
        videoDataHelper.next(false)
    }

    override fun fastForward() {
        videoDataHelper.fastForward()
    }

    override fun rewind() {
        videoDataHelper.rewind()
    }

    override fun getSupportedActions(): Long {
        return (PlaybackBaseControlGlue.ACTION_PLAY_PAUSE or
                PlaybackBaseControlGlue.ACTION_FAST_FORWARD or
                PlaybackBaseControlGlue.ACTION_REWIND or
                PlaybackBaseControlGlue.ACTION_SKIP_TO_NEXT).toLong()

    }

    override fun seekTo(newPosition: Long) {
        if (player == null) {
            return
        }
        player!!.seekTo(newPosition)
    }

    override fun getBufferedPosition(): Long {
        return mBufferedProgress
    }

    /**
     * Sets the media source of the player witha given URI.
     *
     * @return Returns `true` if uri represents a new media; `false`
     * otherwise.
     * @see MediaPlayer.setDataSource
     */
    fun setDataSource(
        uri: String?,
        headers: Map<String, String>?,
        subtitle: String? = null
    ): Boolean {
        if (if (uri != null) mMediaSourceUri == uri else true) {
            return false
        }
        mMediaSourceUri = uri
        this.headers = headers
        this.subtitle = subtitle
        prepareMediaForPlaying()
        return true
    }

    private fun prepareMediaForPlaying() {
        reset()
        initPlayer()
        try {
            if (mMediaSourceUri != null) {
                player?.let {
                    try {
                        it.setPlayUri(
                            mMediaSourceUri!!,
                            headers,
                            subtitle
                        )
                        val memUrl = PreferenceMgr.getString(mContext, memUrlKey, "")
                        if (StringUtil.isNotEmpty(memUrl) && memUrl.equals(mMediaSourceUri)) {
                            val memPos = PreferenceMgr.getLong(mContext, memPosKey, 0L)
                            if (memPos > 0) {
                                it.setPosition(memPos)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                return
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        //直接播放
        player!!.startPlayer<ExoUserPlayer>()
    }

    /**
     * @return True if MediaPlayer OnPreparedListener is invoked and got a SurfaceHolder if
     * [PlaybackGlueHost] provides SurfaceHolder.
     */
    override fun isPrepared(): Boolean {
        return player != null
    }

    companion object {
        const val memUrlKey = "url"
        const val memPosKey = "memPos"
    }
}