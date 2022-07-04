package service

import android.content.Context
import android.util.Log
import com.hd.tvpro.app.App
import com.hd.tvpro.util.PreferenceMgr
import com.hd.tvpro.util.ToastMgr
import com.hd.tvpro.util.async.ThreadTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import service.model.LiveItem
import utils.FileUtil
import java.util.concurrent.TimeUnit
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
    const val goodFile: String = "hiker-good.txt"

    suspend fun loadLocalData(consumer: (ArrayList<LiveItem>) -> Unit){
        val s = FileUtil.fileToString(cacheFile)
        if (s.isNotEmpty()) {
            parseContent(s, consumer)
        }
    }

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
        if (url.startsWith("file://") || url.startsWith("/")) {
            val s = FileUtil.fileToString(url.replace("file://", ""))
            if (s.isNotEmpty()) {
                parseContent(s, consumer)
            }
            return
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
                    if (s.isNotEmpty() &&
                        (s.contains("#EXTM3U") || s.contains("CCTV") || s.contains(".m3u8") || s.contains(
                            "#genre#"
                        ))
                    ) {
                        FileUtil.stringToFile(s, cacheFile)
                        parseContent(s, consumer)
                    } else {
                        val s1 = FileUtil.fileToString(cacheFile)
                        if (s1.isNotEmpty()) {
                            parseContent(s1, consumer)
                            ThreadTool.runOnUI {
                                ToastMgr.shortBottomCenter(
                                    App.INSTANCE.applicationContext,
                                    "远程直播源获取失败，已启用本地缓存"
                                )
                            }
                        }
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
            ThreadTool.runOnUI {
                ToastMgr.shortBottomCenter(App.INSTANCE.applicationContext, "远程直播源获取失败，已启用本地缓存")
            }
        }
    }

    private fun parseContent(s: String, consumer: (ArrayList<LiveItem>) -> Unit) {
        val list: ArrayList<LiveItem> = ArrayList()
        try {
            if (s.contains("#EXTM3U") || s.contains("#genre#")) {
                val mode = s.indexOf("#EXTM3U") == 0
                val re =
                    if (mode) Regex("(.*),(.*)\\s*?((?:http|rtmp)[^#\\s]*)") else Regex("()(.*),(#genre#|(?:http|rtmp)[^#\\s]*)");
                var group = "未分组"
                var i = re.find(s)
                val map = HashMap<String, HashMap<String, ArrayList<String>>>()
                val groups = ArrayList<String>()
                val channels = ArrayList<ArrayList<String>>()
                while (i != null) {
                    if (mode) {
                        group =
                            Regex("group-title=\"(.*?)\"").find(i.groupValues[1])?.groupValues?.get(
                                1
                            )
                                ?: ""
                    } else if (i.groupValues[3] == "#genre#") {
                        group = i.groupValues[2]
                        i = i.next()
                        continue
                    }
                    val title = i.groupValues[2]
                    val url = i.groupValues[3].trim()

                    if (!map.containsKey(group)) {
                        map[group] = HashMap()
                        groups.add(group)
                        channels.add(ArrayList())
                    }
                    if (map[group]?.containsKey(title) != true) {
                        map[group]?.put(title, ArrayList())
                    }
                    map[group]?.get(title)?.add(url)
                    val groupIndex = groups.indexOf(group)
                    val groupChannels = channels[groupIndex]
                    if (!groupChannels.contains(title)) {
                        groupChannels.add(title)
                    }
                    i = i.next()
                }
                channels.forEachIndexed { index, arrayList ->
                    val g = groups[index]
                    val liveItem = LiveItem(g, ArrayList(), ArrayList())
                    arrayList.forEach {
                        liveItem.children?.add(LiveItem(it, map[g]!![it]!!))
                    }
                    list.add(liveItem)
                }
            } else {
                val map = HashMap<String, Int>()
                val data = s.split("\n")
                val children = ArrayList<LiveItem>()
                val all = LiveItem("全部", ArrayList(), children)
                list.add(all)
                for (line in data) {
                    val item = line.split(",")
                    if (item.size < 2) {
                        continue
                    }
                    if (!map.containsKey(item[0])) {
                        val liveItem = LiveItem(item[0], ArrayList())
                        val index = children.size
                        children.add(liveItem)
                        map[liveItem.name] = index
                    }
                    children[map[item[0]]!!].urls.add(item[1])
                }
            }
        } catch (e: Exception) {
            Log.e("LiveModel", e.message, e)
        }
        reSortUrls(list)
        cache = list
        consumer(list)
    }

    /**
     * 把成功的放最前面
     */
    private fun reSortUrls(list: ArrayList<LiveItem>) {
        if (list.isNotEmpty()) {
            val s = FileUtil.fileToString(goodFile)
            if (s.isEmpty()) {
                return
            }
            val goods = s.split("\n").filter { it.isNotEmpty() }.map {
                val ss = it.split(",")
                ss[ss.size - 1]
            }
            if (goods.isEmpty()) {
                return
            }
            val map = HashMap<String, Int>()
            for (indexedValue in goods.withIndex()) {
                map[indexedValue.value] = indexedValue.index
            }
            reSortUrls(list, map)
        }
    }

    private fun reSortUrls(list: ArrayList<LiveItem>, goods: HashMap<String, Int>) {
        if (list.isNotEmpty()) {
            for (liveItem in list) {
                if (!liveItem.urls.isNullOrEmpty()) {
                    //重排序，成功的放前面
                    val goodList = ArrayList<String>()
                    val normalList = ArrayList<String>()
                    for (url in liveItem.urls) {
                        if (goods.containsKey(url)) {
                            goodList.add(url)
                        } else {
                            normalList.add(url)
                        }
                    }
                    //最近使用的排最前面
                    goodList.sortBy {
                        goods[it]
                    }
                    goodList.addAll(normalList)
                    liveItem.urls = goodList
                }
                if (!liveItem.children.isNullOrEmpty()) {
                    reSortUrls(liveItem.children!!, goods)
                }
            }
        }
    }

    /**
     * 切换频道的时候才换，保证当前线路顺序不变，但是换台回来保证能播放
     */
    fun reSortLastLiveItem(liveItem: LiveItem) {
        GlobalScope.launch(Dispatchers.IO) {
            val s = FileUtil.fileToString(goodFile)
            if (s.isEmpty()) {
                return@launch
            }
            val goods = s.split("\n").filter { it.isNotEmpty() }.map {
                val ss = it.split(",")
                ss[ss.size - 1]
            }
            val map = HashMap<String, Int>()
            for (indexedValue in goods.withIndex()) {
                map[indexedValue.value] = indexedValue.index
            }
            //重排序，成功的放前面
            val goodList = ArrayList<String>()
            val normalList = ArrayList<String>()
            for (url in liveItem.urls) {
                if (map.containsKey(url)) {
                    goodList.add(url)
                } else {
                    normalList.add(url)
                }
            }
            //最近使用的排最前面
            goodList.sortBy {
                map[it]
            }
            goodList.addAll(normalList)
            liveItem.urls = goodList
        }
    }

    /**
     * 加能播放的
     */
    fun addGoodUrl(context: Context, name: String, url: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val s = FileUtil.fileToString(goodFile)
            val ss = s.split("\n").filter { it.isNotEmpty() }.toMutableList()
            val line = "$name,$url"
            ss.remove(line)
            if (ss.size >= 10000) {
                ss.removeLast()
            }
            //当前播放成功的放最前面
            ss.add(0, line)
            //持久化
            FileUtil.stringToFile(ss.joinToString("\n"), goodFile)
        }
        val urls = PreferenceMgr.getString(context, "playUrls", "")
        if (urls.isNotEmpty() && urls.contains(url) && !urls.startsWith(url)) {
            val us = ArrayList(urls.split("|||"))
            //放最前面去
            us.remove(url)
            us.add(0, url)
            PreferenceMgr.put(context, "playUrls", us.joinToString("|||"))
        }
    }

    /**
     * 删除不能播放的
     */
    fun clearGoodUrl(context: Context, url: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val s = FileUtil.fileToString(goodFile)
            val ss = s.split("\n").filter { it.isNotEmpty() }.toMutableList()
            for ((index, v) in ss.withIndex()) {
                if (v.endsWith(url)) {
                    ss.removeAt(index)
                    FileUtil.stringToFile(ss.joinToString("\n"), goodFile)
                    return@launch
                }
            }
        }
        val urls = PreferenceMgr.getString(context, "playUrls", "")
        if (urls.isNotEmpty() && urls.contains(url) && !urls.startsWith(url)) {
            val us = ArrayList(urls.split("|||"))
            //放最后面去
            us.remove(url)
            us.add(url)
            PreferenceMgr.put(context, "playUrls", us.joinToString("|||"))
        }
    }
}