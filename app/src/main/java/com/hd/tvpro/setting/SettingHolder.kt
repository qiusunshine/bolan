package com.hd.tvpro.setting

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.*
import android.widget.AdapterView
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.util.MimeTypes
import com.hd.tvpro.R
import com.hd.tvpro.constants.AppConfig
import com.hd.tvpro.event.SwitchUrlChange
import com.hd.tvpro.util.PreferenceMgr
import com.hd.tvpro.util.ShareUtil
import com.hd.tvpro.util.StringUtil
import com.hd.tvpro.util.ToastMgr
import com.hd.tvpro.video.model.TrackHolder
import com.hd.tvpro.view.SelectListView
import org.greenrobot.eventbus.EventBus
import service.model.LiveItem

/**
 * 作者：By 15968
 * 日期：On 2021/10/26
 * 时间：At 15:33
 */
class SettingHolder constructor(
    private val context: Context,
    private val settingUpdateListener: SettingUpdateListener
) {

    private var settingWindow: PopupWindow? = null
    private lateinit var mSettingList: SelectListView
    private var mAdapterSetting: ListViewAdapterSettingLeft? = null

    private var mSettingValueList: ListView? = null
    private var settingArrayList: ArrayList<SettingOption> = ArrayList()
    private var mAdapterSettingValue: ListViewAdapterSettingRight? = null
    private var mSettingPos = 0
    private var settingView: View? = null
    private var audioFormatMap = HashMap<Int, Format>()
    private var subtitleFormatMap = HashMap<Int, Format>()

    enum class Option {
        SCREEN, SPEED, RESET, FINISH
    }

    interface SettingUpdateListener {
        fun update(option: Option)
    }

    private fun setSettingText(
        index: Int,
        nowUrl: String?,
        liveItem: LiveItem?,
        trackHolder: TrackHolder?
    ) {
        try {
            settingArrayList.clear()
            if (liveItem != null && liveItem.urls.isNotEmpty()) {
                val switchOption = SettingOption("线路切换")
                for (item in liveItem.urls.withIndex()) {
                    switchOption.mRightList.add("线路${item.index + 1}")
                }
                settingArrayList.add(switchOption)
            }
            audioFormatMap.clear()
            subtitleFormatMap.clear()
            if (trackHolder != null) {
                val mappedTrackInfo = trackHolder.trackProvider()
                if (mappedTrackInfo != null) {
                    val audioFormats: MutableList<Format> = java.util.ArrayList()
                    val subtitleFormats: MutableList<Format> = java.util.ArrayList()
                    for (i in 0 until mappedTrackInfo.rendererCount) {
                        val rendererTrackGroups = mappedTrackInfo.getTrackGroups(i)
                        if (C.TRACK_TYPE_AUDIO == mappedTrackInfo.getRendererType(i)) { //判断是否是音轨
                            for (groupIndex in 0 until rendererTrackGroups.length) {
                                val trackGroup = rendererTrackGroups[groupIndex]
                                audioFormats.add(trackGroup.getFormat(0))
                            }
                        } else if (C.TRACK_TYPE_TEXT == mappedTrackInfo.getRendererType(i)) { //判断是否是字幕
                            for (groupIndex in 0 until rendererTrackGroups.length) {
                                val trackGroup = rendererTrackGroups[groupIndex]
                                subtitleFormats.add(trackGroup.getFormat(0))
                            }
                        }
                    }
                    if (audioFormats.isNotEmpty()) {
                        val audioTrackOption = SettingOption("音频轨道")
                        for (audioFormat in audioFormats) {
                            var label =
                                if (StringUtil.isNotEmpty(audioFormat.label)) audioFormat.label else audioFormat.language
                            if ("zh" == label || "Chinese" == label) {
                                label = "国语"
                            } else if ("Cantonese" == label) {
                                label = "粤语"
                            } else if ("en" == label) {
                                label = "英语"
                            }
                            val channel: String? = buildAudioChannelString(audioFormat)
                            if (StringUtil.isNotEmpty(channel)) {
                                label = "$label, $channel"
                            }
                            audioTrackOption.mRightList.add(label)
                            audioFormatMap[audioTrackOption.mRightList.size - 1] = audioFormat
                        }
                        if (!audioTrackOption.mRightList.isNullOrEmpty() && audioTrackOption.mRightList.size > 1) {
                            settingArrayList.add(audioTrackOption)
                        }
                    }
                    if (subtitleFormats.isNotEmpty()) {
                        val subtitleTrackOption = SettingOption("字幕轨道")
                        for (subtitleFormat in subtitleFormats) {
                            var label =
                                if (StringUtil.isNotEmpty(subtitleFormat.label)) subtitleFormat.label else subtitleFormat.language
                            if ("Chinese Simplified" == label || "Simplified" == label) {
                                label = "中文(简体)"
                            } else if ("Chinese Traditional" == label || "Traditional" == label) {
                                label = "中文(繁体)"
                            } else if ("en" == label) {
                                label = "英语"
                            } else if ("Chinese" == label) {
                                label = "中文"
                            }
                            if (label == null || label.isEmpty()) {
                                label = if (StringUtil.isNotEmpty(trackHolder.subtitle())) {
                                    "外挂字幕"
                                } else {
                                    "无名"
                                }
                            }
                            subtitleTrackOption.mRightList.add(label)
                            subtitleFormatMap[subtitleTrackOption.mRightList.size - 1] = subtitleFormat
                        }
                        if (!subtitleTrackOption.mRightList.isNullOrEmpty() && subtitleTrackOption.mRightList.size > 1) {
                            settingArrayList.add(subtitleTrackOption)
                        }
                    }
                }
            }
            val screenOption = SettingOption("屏幕比例")
            screenOption.mRightList.add("自适应")
            screenOption.mRightList.add("充满屏幕")
            screenOption.mRightList.add("拉伸宽度")
            screenOption.mRightList.add("拉伸高度")
            settingArrayList.add(screenOption)

            val speedOption = SettingOption("倍速播放")
            val speeds: List<Float> = getSpeedList()
            for (speed in speeds) {
                speedOption.mRightList.add(String.format("X %.1f", speed))
            }
            settingArrayList.add(speedOption)

            val dataOption = SettingOption("数据管理")
            dataOption.mRightList.add("重新扫描设备")
            settingArrayList.add(dataOption)

            val startOption = SettingOption("开机启动")
            startOption.mRightList.add("关闭")
            startOption.mRightList.add("开启")
            settingArrayList.add(startOption)

            val aboutOption = SettingOption("关于软件")
            aboutOption.mRightList.add("新版下载地址")
            aboutOption.mRightList.add("新方圆小棉袄")
            val versionName = try {
                val pkName = context.packageName
                context.packageManager.getPackageInfo(
                    pkName, 0
                ).versionName
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                ""
            }
            aboutOption.mRightList.add("版本：$versionName")
            aboutOption.mRightList.add("退出软件")
            settingArrayList.add(aboutOption)

            var pos = index
            if (index >= settingArrayList.size) {
                pos = 0
            }

            val rightList: ArrayList<String> = settingArrayList[pos].mRightList
            mSettingValueList = settingView!!.findViewById(R.id.lv_setting_right)
            mAdapterSettingValue = ListViewAdapterSettingRight(context, rightList, 0)
            mSettingValueList!!.adapter = mAdapterSettingValue
            when (settingArrayList[pos].mLeftValue) {
                "倍速播放" -> {
                    val speed = PreferenceMgr.getFloat(context, "speed", 1f)
                    val speeds2: List<Float> = getSpeedList()
                    var i = 0
                    while (i < speeds2.size) {
                        if (speed == speeds2[i]) {
                            mAdapterSettingValue!!.setSelection(i)
                            break
                        }
                        i++
                    }
                }
                "屏幕比例" -> {
                    val screenScale = PreferenceMgr.getInt(context, "screen", 0)
                    mAdapterSettingValue!!.setSelection(screenScale)
                }
                "开机启动" -> {
                    val selfStart = PreferenceMgr.getBoolean(context, "selfStart", false)
                    mAdapterSettingValue!!.setSelection(if (selfStart) 1 else 0)
                }
                "线路切换" -> {
                    for (item in liveItem!!.urls.withIndex()) {
                        if (item.value == nowUrl) {
                            mAdapterSettingValue!!.setSelection(item.index)
                            break
                        }
                    }
                }
                "音频轨道" -> {
                    var audio: String? = null
                    val trackSelections = trackHolder?.trackSelections
                    if (trackSelections != null) {
                        for (i in 0 until trackSelections.length) {
                            if (trackSelections.get(i) != null) {
                                for (j in 0 until trackSelections.get(i)!!.length()) {
                                    if (MimeTypes.isAudio(
                                            trackSelections.get(i)!!.getFormat(j).sampleMimeType
                                        )
                                        || trackSelections.get(i)!!
                                            .getFormat(j).channelCount != Format.NO_VALUE
                                    ) {
                                        audio = trackSelections.get(i)!!.getFormat(j).id
                                    }
                                }
                            }
                        }
                    }
                    for (entry in audioFormatMap) {
                        if (audio == entry.value.id) {
                            mAdapterSettingValue!!.setSelection(entry.key)
                            break
                        }
                    }
                }
                "字幕轨道" -> {
                    var text: String? = null
                    val trackSelections = trackHolder?.trackSelections
                    if (trackSelections != null) {
                        for (i in 0 until trackSelections.length) {
                            if (trackSelections.get(i) != null) {
                                for (j in 0 until trackSelections.get(i)!!.length()) {
                                    if (MimeTypes.isText(
                                            trackSelections.get(i)!!.getFormat(j).sampleMimeType
                                        )
                                    ) {
                                        text = trackSelections.get(i)!!.getFormat(j).id
                                    }
                                }
                            }
                        }
                    }
                    for (entry in subtitleFormatMap) {
                        if (text == entry.value.id) {
                            mAdapterSettingValue!!.setSelection(entry.key)
                            break
                        }
                    }
                }
                else -> {
                }
            }
            mSettingValueList!!.setOnItemClickListener { parent, v, posval, id ->
                //单击项目时
                when (settingArrayList.get(pos).mLeftValue) {
                    "屏幕比例" -> {
                        PreferenceMgr.put(context, "screen", posval)
                        settingUpdateListener.update(Option.SCREEN)
                        mAdapterSettingValue!!.setSelection(posval)
                    }
                    "倍速播放" -> {
                        val sp = getSpeedList()[posval]
                        PreferenceMgr.put(context, "speed", sp)
                        settingUpdateListener.update(Option.SPEED)
                        mAdapterSettingValue!!.setSelection(posval)
                    }
                    "数据管理" -> {
                        settingUpdateListener.update(Option.RESET)
                        hide()
                    }
                    "开机启动" -> {
                        PreferenceMgr.put(context, "selfStart", posval == 1)
                        mAdapterSettingValue!!.setSelection(posval)
                        ToastMgr.shortBottomCenter(
                            context,
                            "已" + (if (posval == 1) "开启" else "关闭") + "开机自启动"
                        )
                    }
                    "线路切换" -> {
                        val url = liveItem!!.urls[posval]
                        EventBus.getDefault().post(SwitchUrlChange(url))
                        hide()
                    }
                    "音频轨道" -> {
                        if (audioFormatMap.containsKey(posval)) {
                            val format = audioFormatMap[posval]
                            if (format != null) {
                                EventBus.getDefault().post(format)
                            }
                        }
                        hide()
                    }
                    "字幕轨道" -> {
                        if (subtitleFormatMap.containsKey(posval)) {
                            val format = subtitleFormatMap[posval]
                            if (format != null) {
                                EventBus.getDefault().post(format)
                            }
                        }
                        hide()
                    }
                    "关于软件" -> {
                        if (settingArrayList[pos].mRightList[posval] == "退出软件") {
                            settingUpdateListener.update(Option.FINISH)
                        } else {
                            ShareUtil.startUrl(context, "https://haikuo.lanzoui.com/u/GoldRiver")
                        }
                    }
                    else -> {
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSpeedList(): List<Float> {
        return ArrayList(listOf(1f, 1.2f, 1.5f, 2f, 3f, 4f))
    }

    fun show(
        anchor: View,
        nowUrl: String? = null,
        liveItem: LiveItem? = null,
        trackHolder: TrackHolder? = null
    ) {
        settingView = LayoutInflater.from(context).inflate(R.layout.layout_setting, null)
        val dm = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        windowManager!!.defaultDisplay.getMetrics(dm)
        val width: Int = dm.widthPixels
        val height: Int = dm.heightPixels


        val fontSize = width / 42
        AppConfig.fontSize = fontSize
        mSettingList = settingView!!.findViewById<View>(R.id.lv_setting_left) as SelectListView
        setSettingText(0, nowUrl, liveItem, trackHolder)
        mSettingList.requestFocus()
        mAdapterSetting = ListViewAdapterSettingLeft(context, settingArrayList!!, 0)
        mSettingList.pos = 0
        mSettingList.adapter = mAdapterSetting
        mSettingList.setSelection(0)

        val tvSettingTitle = settingView!!.findViewById<View>(R.id.tv_setting) as TextView
        tvSettingTitle.text = "通用设置"
        tvSettingTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, (fontSize + 3).toFloat())
        tvSettingTitle.visibility = View.GONE

        mSettingList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View,
                position: Int, id: Long
            ) {
                setSettingText(position, nowUrl, liveItem, trackHolder)
                mSettingPos = position
                mAdapterSetting!!.setSelection(position)
                mSettingList.setSelect(position, 0)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        mSettingList.setOnItemClickListener { adapter, v, pos, id ->
            setSettingText(pos, nowUrl, liveItem, trackHolder)
            mAdapterSetting!!.setSelection(pos)
            mSettingList.setSelect(pos, 0)
        }

        AppConfig.settingHeight = height / 12 - mSettingList.dividerHeight

        //展示window
        settingWindow = PopupWindow(settingView, width / 2, ViewGroup.LayoutParams.WRAP_CONTENT)
        settingWindow?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.isFocusable = true
            it.isOutsideTouchable = true
            it.update()
            it.showAtLocation(anchor, Gravity.CENTER, 0, 0)
        }
    }

    fun isShowing(): Boolean {
        if (settingWindow != null && settingWindow!!.isShowing) {
            return true
        }
        return false
    }

    fun hide() {
        if (settingWindow != null && settingWindow!!.isShowing) {
            settingWindow!!.dismiss()
        }
    }

    private fun buildAudioChannelString(format: Format): String? {
        val channelCount = format.channelCount
        return if (channelCount < 1) {
            null
        } else when (channelCount) {
            1 -> "单声道"
            2 -> "立体声"
            6, 7 -> "5.1 环绕声"
            8 -> "7.1 环绕声"
            else -> "环绕声"
        }
    }
}