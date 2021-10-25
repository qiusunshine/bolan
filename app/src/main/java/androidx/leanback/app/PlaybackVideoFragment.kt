package androidx.leanback.app

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.BaseGridView.OnTouchInterceptListener
import androidx.leanback.widget.PlaybackControlsRow
import androidx.leanback.widget.PlaybackSeekDataProvider
import chuangyuan.ycj.videolibrary.video.GestureVideoPlayer
import chuangyuan.ycj.videolibrary.video.GestureVideoPlayer.DoubleTapArea
import chuangyuan.ycj.videolibrary.widget.VideoPlayerView
import com.alibaba.fastjson.JSON
import com.hd.tvpro.util.PreferenceMgr
import com.hd.tvpro.util.ScanLiveTVUtils
import com.hd.tvpro.util.StringUtil
import com.hd.tvpro.util.ToastMgr
import com.hd.tvpro.util.async.ThreadTool
import com.hd.tvpro.util.http.HttpListener
import com.hd.tvpro.util.http.HttpUtils
import com.hd.tvpro.video.MediaPlayerAdapter
import com.hd.tvpro.video.MyPlaybackTransportControlGlue
import com.hd.tvpro.video.VideoDataHelper
import com.hd.tvpro.video.model.DlanUrlDTO
import java.util.*
import kotlin.math.floor

/** Handles video playback with media controls. */
class PlaybackVideoFragment : VideoSupportFragment() {

    private lateinit var mTransportControlGlue: PlaybackTransportControlGlue<MediaPlayerAdapter>
    private var playData: DlanUrlDTO? = null
    private lateinit var playerAdapter: MediaPlayerAdapter
    private lateinit var videoView: VideoPlayerView
    private lateinit var gestureDetector: GestureDetector

    fun dispatchTouchEvent(ev: MotionEvent): Boolean {
//        return gestureDetector.onTouchEvent(ev)
        return false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        videoView = VideoPlayerView(context)
        val glueHost = VideoSupportFragmentGlueHost(this@PlaybackVideoFragment)
        playerAdapter = MediaPlayerAdapter(requireActivity(), videoView, videoDataHelper)
        playerAdapter.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE)

        mTransportControlGlue = MyPlaybackTransportControlGlue(activity, playerAdapter)
        mTransportControlGlue.host = glueHost
        mTransportControlGlue.playWhenPrepared()
        mTransportControlGlue.isControlsOverlayAutoHideEnabled = true
        mTransportControlGlue.isSeekEnabled = true

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
        playerAdapter.setDataSource("file:///android_asset/test.jpg", null)

        val parent = mVideoSurface.parent as ViewGroup
        parent.removeView(mVideoSurface)
        parent.addView(videoView, 0)

        val scanLiveTVUtils = ScanLiveTVUtils()
        val lastMem = PreferenceMgr.getString(activity, "remote", null)
        if (StringUtil.isNotEmpty(lastMem)) {
            scanLiveTVUtils.checkLastMem(lastMem, {
                startCheckPlayUrl(lastMem)
            }, {
                ToastMgr.shortBottomCenter(activity, "开始扫描远程设备，请稍候")
                scanLiveTVUtils.scan(activity, {
                    PreferenceMgr.put(context, "remote", it)
                    startCheckPlayUrl(it)
                }, {
                    ToastMgr.longCenter(activity, "扫描远程设备失败，请确认在一个局域网内")
                    //再给上次记录的地址一次机会，可能忘记打开手机
                    startCheckPlayUrl(lastMem)
                })
            })
        } else {
            ToastMgr.shortBottomCenter(activity, "开始扫描远程设备，请稍候")
            scanLiveTVUtils.scan(activity, {
                PreferenceMgr.put(context, "remote", it)
                startCheckPlayUrl(it)
            }, {
                ToastMgr.longCenter(activity, "扫描远程设备失败，请确认在一个局域网内")
            })
        }
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
    }

    override fun onResume() {
        super.onResume()
        val mOnTouchInterceptListener =
            OnTouchInterceptListener { event -> gestureDetector.onTouchEvent(event) || onInterceptInputEvent(event) }
        verticalGridView.setOnTouchInterceptListener(mOnTouchInterceptListener)
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
        override fun next() {
            val lastMem = PreferenceMgr.getString(activity, "remote", null)
            lastMem?.let {
                ToastMgr.shortBottomCenter(context, "播放下一集")
                ThreadTool.executeNewTask {
                    Log.d(TAG, "playNext: ${it}/playNext")
                    HttpUtils.get("${it}/playNext", object : HttpListener {
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
        ThreadTool.executeNewTask {
            HttpUtils.get("$url/playUrl?enhance=true", object : HttpListener {
                override fun success(body: String?) {
                    activity?.runOnUiThread {
                        view?.postDelayed({
                            startCheckPlayUrl(url)
                        }, 1000)
                    }
                    if (playData == null) {
                        playData = JSON.parseObject(body, DlanUrlDTO::class.java)

                    } else if (JSON.toJSONString(playData).equals(body)) {
                        return
                    } else {
                        playData = JSON.parseObject(body, DlanUrlDTO::class.java)
                    }
                    playData?.let {
                        mTransportControlGlue.title = "\n" + it.title
                        mTransportControlGlue.subtitle = it.url
                        playerAdapter.setDataSource(it.url, it.headers)
                    }
                }

                override fun failed(msg: String?) {
                    //ignore
                    activity?.runOnUiThread {
                        view?.postDelayed({
                            startCheckPlayUrl(url)
                        }, 1000)
                    }
                }
            })
        }
    }

    override fun onPause() {
        super.onPause()
        mTransportControlGlue.pause()
    }

    override fun getSurfaceView(): SurfaceView {
        return videoView?.playerView?.videoSurfaceView as SurfaceView
    }

    companion object {
        private const val TAG = "PlaybackVideoFragment"
    }
}