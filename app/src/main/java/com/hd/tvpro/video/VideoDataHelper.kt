package com.hd.tvpro.video

/**
 * 作者：By 15968
 * 日期：On 2021/10/25
 * 时间：At 11:05
 */
interface VideoDataHelper {

    fun next(autoEnd: Boolean)

    fun previous()

    fun fastForward()

    fun rewind()
}