package com.hd.tvpro.webserver

import com.alibaba.fastjson.JSON
import com.hd.tvpro.app.App
import com.hd.tvpro.event.PlayUrlChange
import com.hd.tvpro.event.SubChange
import com.hd.tvpro.util.PreferenceMgr
import com.hd.tvpro.util.StringUtil
import com.yanzhenjie.andserver.annotation.Controller
import com.yanzhenjie.andserver.annotation.GetMapping
import com.yanzhenjie.andserver.annotation.PostMapping
import com.yanzhenjie.andserver.annotation.ResponseBody
import com.yanzhenjie.andserver.http.RequestBody
import org.greenrobot.eventbus.EventBus
import service.LiveModel

/**
 * 作者：By 15968
 * 日期：On 2022/1/14
 * 时间：At 16:26
 */
@Controller
class WebController {

//    @GetMapping(path = ["/"])
//    @ResponseBody
//    fun ok(): String {
//        return "请使用波澜助手来管理直播源：https://haikuo.lanzouq.com/u/GoldRiver"
//    }

    @GetMapping(path = ["/hello"])
    @ResponseBody
    fun hello(): String {
        return "hello"
    }

    @GetMapping(path = ["/api/sub/get"])
    @ResponseBody
    fun getSub(): String {
        return PreferenceMgr.getString(App.INSTANCE, "sub", "")
    }

    @PostMapping(path = ["/api/sub/set"])
    @ResponseBody
    fun updateSub(body: RequestBody): String {
        val data = body.string()
        return try {
            val sd = JSON.parseObject(data)
            val url = sd.getString("url")
            if (StringUtil.isNotEmpty(url)) {
                PreferenceMgr.put(App.INSTANCE, "sub", url)
                LiveModel.cache = null
                LiveModel.cacheUrl = null
                EventBus.getDefault().post(SubChange(url))
            }
            "ok"
        } catch (e: Exception) {
            e.printStackTrace()
            "error:" + e.message
        }
    }

    @PostMapping(path = ["/api/play"])
    @ResponseBody
    fun play(body: RequestBody): String {
        val data = body.string()
        return try {
            val sd = JSON.parseObject(data)
            val url = sd.getString("url")
            val subtitle: String? = sd.getString("subtitle")
            if (StringUtil.isNotEmpty(url)) {
                EventBus.getDefault().post(PlayUrlChange(url, subtitle))
            }
            "ok"
        } catch (e: Exception) {
            e.printStackTrace()
            "error:" + e.message
        }
    }
}