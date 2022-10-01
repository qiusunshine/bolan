package com.hd.tvpro.video.model

import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray

/**
 * 作者：By 15968
 * 日期：On 2022/10/1
 * 时间：At 15:07
 */
data class TrackHolder(
    val trackGroups: TrackGroupArray?,
    val trackSelections: TrackSelectionArray?,
    val trackProvider: () -> MappingTrackSelector.MappedTrackInfo?,
    val subtitle: () -> String?
)
