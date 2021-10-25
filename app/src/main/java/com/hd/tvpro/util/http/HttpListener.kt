package com.hd.tvpro.util.http

/**
 * 作者：By 15968
 * 日期：On 2021/10/24
 * 时间：At 20:45
 */
interface HttpListener {
    fun success(body: String?)

    fun failed(msg: String?)
}