package com.hd.tvpro.webserver

import android.content.Context
import com.yanzhenjie.andserver.annotation.Config
import com.yanzhenjie.andserver.framework.config.WebConfig
import com.yanzhenjie.andserver.framework.website.AssetsWebsite

/**
 * 作者：By 15968
 * 日期：On 2021/10/20
 * 时间：At 20:55
 */
@Config
class MyWebConfig : WebConfig {
    override fun onConfig(context: Context, delegate: WebConfig.Delegate) {
        // 增加一个静态网站
        delegate.addWebsite(AssetsWebsite(context, "/live", "sub.html"))
    }
}