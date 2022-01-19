package service

import okhttp3.OkHttpClient
import okhttp3.Request
import service.model.LiveItem
import utils.FileUtil
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.set

object LiveModel {
    var okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(2000, TimeUnit.MILLISECONDS)
        .writeTimeout(2000, TimeUnit.MILLISECONDS)
        .connectTimeout(2000, TimeUnit.MILLISECONDS)
        .build()
    var cache: ArrayList<LiveItem>? = null
    var cacheUrl: String? = null
    private const val cacheFile: String = "hiker-live.txt"

    suspend fun loadData(url: String?, consumer: (ArrayList<LiveItem>) -> Unit) {
        if (cacheUrl != null && cacheUrl != url) {
            cache = null
        }
        if (cache != null && cache!!.isNotEmpty()) {
            consumer(cache!!)
            return
        }
        if (url == null || url.isEmpty()) {
            consumer(ArrayList())
            return
        } else {
            cacheUrl = url
        }
        val request = Request.Builder()
            .get()
            .url(cacheUrl!!)
            .build()
        try {
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body().use { body ->
                    val s = body!!.string()
                    if (s.isNotEmpty()) {
//                        FileUtil.stringToFile(s, cacheFile)
                        parseContent(s, consumer)
                    }
                }
                return
            } else {
                try {
                    response.close()
                } catch (e: Throwable) {
                }
            }
        } catch (e: Throwable) {
        }
        val s = FileUtil.fileToString(cacheFile)
        if (s.isNotEmpty()) {
            parseContent(s, consumer)
        }
    }

    private fun parseContent(s: String, consumer: (ArrayList<LiveItem>) -> Unit) {
        val map = HashMap<String, Int>()
        val list: ArrayList<LiveItem> = ArrayList()
        val data = s.split("\n")
        for (line in data) {
            val item = line.split(",")
            if (item.size < 2) {
                continue
            }
            if (!map.containsKey(item[0])) {
                val liveItem = LiveItem(item[0], ArrayList())
                val index = list.size
                list.add(liveItem)
                map[liveItem.name] = index
            }
            list[map[item[0]]!!].urls.add(item[1])
        }
        cache = list
        consumer(list)
    }
}