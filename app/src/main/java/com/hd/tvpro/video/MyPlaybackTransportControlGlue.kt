package com.hd.tvpro.video

import android.content.Context
import androidx.leanback.media.PlaybackBannerControlGlue
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.media.PlayerAdapter
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.PlaybackControlsRow.SkipNextAction
import androidx.leanback.widget.PlaybackControlsRow.SkipPreviousAction

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
}