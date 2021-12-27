package com.hd.tvpro.video

import android.content.Context
import android.view.KeyEvent
import android.view.View
import androidx.leanback.media.PlaybackBannerControlGlue
import androidx.leanback.media.PlaybackGlueHost
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.media.PlayerAdapter
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.PlaybackControlsRow.SkipNextAction
import androidx.leanback.widget.PlaybackControlsRow.SkipPreviousAction
import androidx.leanback.widget.PlaybackSeekDataProvider
import androidx.leanback.widget.PlaybackSeekUi

/**
 * 作者：By 15968
 * 日期：On 2021/10/25
 * 时间：At 12:58
 */
class MyPlaybackTransportControlGlue<T : PlayerAdapter?>(context: Context?, impl: T) :
    PlaybackTransportControlGlue<T>(
        context, impl
    ) {
    private var mSkipNextAction: SkipNextAction? = null
    private var mSkipPreviousAction: SkipPreviousAction? = null
    var onKeyInterceptor: OnKeyInterceptor? = null

    override fun onCreatePrimaryActions(primaryActionsAdapter: ArrayObjectAdapter) {
        super.onCreatePrimaryActions(primaryActionsAdapter)
        val supportedActions = supportedActions
        if (supportedActions and PlaybackBannerControlGlue.ACTION_SKIP_TO_PREVIOUS.toLong() != 0L && mSkipPreviousAction == null) {
            primaryActionsAdapter.add(SkipPreviousAction(context).also { mSkipPreviousAction = it })
        } else if (supportedActions and PlaybackBannerControlGlue.ACTION_SKIP_TO_PREVIOUS.toLong() == 0L && mSkipPreviousAction != null) {
            primaryActionsAdapter.remove(mSkipPreviousAction)
            mSkipPreviousAction = null
        }
        if (supportedActions and PlaybackBannerControlGlue.ACTION_SKIP_TO_NEXT.toLong() != 0L && mSkipNextAction == null) {
            primaryActionsAdapter.add(SkipNextAction(context).also { mSkipNextAction = it })
        } else if (supportedActions and PlaybackBannerControlGlue.ACTION_SKIP_TO_NEXT.toLong() == 0L && mSkipNextAction != null) {
            primaryActionsAdapter.remove(mSkipNextAction)
            mSkipNextAction = null
        }
    }

    /**
     * Handles key events and returns true if handled.  A subclass may override this to provide
     * additional support.
     */
    override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
        if (onKeyInterceptor != null && onKeyInterceptor!!.onKey(v, keyCode, event)) {
            return true
        }
        return super.onKey(v, keyCode, event)
    }

    interface OnKeyInterceptor {
        fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean
    }

    override fun onAttachedToHost(host: PlaybackGlueHost?) {
        super.onAttachedToHost(host)

        if (host is PlaybackSeekUi) {
            (host as PlaybackSeekUi).setPlaybackSeekUiClient(mySeekClient)
        }
    }

    override fun onUpdateProgress() {
        if (!mySeekClient.mIsSeek) {
            super.onUpdateProgress()
        }
    }

    val mySeekClient = MySeekClient()

    inner class MySeekClient : PlaybackSeekUi.Client() {
        var mPausedBeforeSeek = false
        var mPositionBeforeSeek: Long = 0
        var mLastUserPosition: Long = 0
        var mIsSeek = false
        override fun getPlaybackSeekDataProvider(): PlaybackSeekDataProvider? {
            return this@MyPlaybackTransportControlGlue.seekProvider
        }

        override fun isSeekEnabled(): Boolean {
            return this@MyPlaybackTransportControlGlue.seekProvider != null || this@MyPlaybackTransportControlGlue.isSeekEnabled
        }

        override fun onSeekStarted() {
            mIsSeek = true
            mPausedBeforeSeek = !isPlaying
            this@MyPlaybackTransportControlGlue.playerAdapter?.setProgressUpdatingEnabled(true)
            // if we seek thumbnails, we don't need save original position because current
            // position is not changed during seeking.
            // otherwise we will call seekTo() and may need to restore the original position.
            mPositionBeforeSeek =
                if (playbackSeekDataProvider == null) this@MyPlaybackTransportControlGlue.playerAdapter!!.currentPosition else -1
            mLastUserPosition = -1
        }

        override fun onSeekPositionChanged(pos: Long) {
            if (playbackSeekDataProvider == null) {
                this@MyPlaybackTransportControlGlue.playerAdapter!!.seekTo(pos)
            } else {
                mLastUserPosition = pos
            }
            if (this@MyPlaybackTransportControlGlue.controlsRow != null) {
                this@MyPlaybackTransportControlGlue.controlsRow.currentPosition = pos
            }
        }

        override fun onSeekFinished(cancelled: Boolean) {
            if (!cancelled) {
                if (mLastUserPosition >= 0) {
                    seekTo(mLastUserPosition)
                }
            } else {
                if (mPositionBeforeSeek >= 0) {
                    seekTo(mPositionBeforeSeek)
                }
            }
            mIsSeek = false
            if (!mPausedBeforeSeek) {
//                play()
            } else {
                this@MyPlaybackTransportControlGlue.playerAdapter!!.setProgressUpdatingEnabled(false)
                // we neeed update UI since PlaybackControlRow still saves previous position.
                onUpdateProgress()
            }
        }
    }
}