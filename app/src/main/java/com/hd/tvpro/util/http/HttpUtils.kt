package com.hd.tvpro.util.http

import com.lzy.okgo.OkGo
import com.lzy.okgo.model.Response

/**
 * 作者：By 15968
 * 日期：On 2021/10/24
 * 时间：At 20:44
 */
object HttpUtils {
    fun get(url:String, listener: HttpListener) {
        OkGo.get<String>(url)
            .execute(object : CharsetStringCallback("UTF-8") {
                override fun onSuccess(response: Response<String>) {
                    listener.success(response.body())
                }

                override fun onError(response: Response<String>) {
                    listener.failed(response.body())
                    super.onError(response)
                }
            })
    }
}