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
import com.hd.tvpro.R
import com.hd.tvpro.constants.AppConfig
import com.hd.tvpro.event.SwitchUrlChange
import com.hd.tvpro.util.PreferenceMgr
import com.hd.tvpro.util.ShareUtil
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

    enum class Option {
        SCREEN, SPEED, RESET, FINISH
    }

    interface SettingUpdateListener {
        fun update(option: Option)
    }

    private fun setSettingText(index: Int, nowUrl: String?, liveItem: LiveItem?) {
        try {
            settingArrayList.clear()
            if (liveItem != null && liveItem.urls.isNotEmpty()) {
                val switchOption = SettingOption("线路切换")
                for (item in liveItem.urls.withIndex()) {
                    switchOption.mRightList.add("线路${item.index + 1}")
                }
                settingArrayList.add(switchOption)
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
                "线路切换" -> {
                    for (item in liveItem!!.urls.withIndex()) {
                        if (item.value == nowUrl) {
                            mAdapterSettingValue!!.setSelection(item.index)
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
                    "线路切换" -> {
                        val url = liveItem!!.urls[posval]
                        EventBus.getDefault().post(SwitchUrlChange(url))
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

    fun show(anchor: View, nowUrl: String? = null, liveItem: LiveItem? = null) {
        settingView = LayoutInflater.from(context).inflate(R.layout.layout_setting, null)
        val dm = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        windowManager!!.defaultDisplay.getMetrics(dm)
        val width: Int = dm.widthPixels
        val height: Int = dm.heightPixels


        val fontSize = width / 42
        AppConfig.fontSize = fontSize
        mSettingList = settingView!!.findViewById<View>(R.id.lv_setting_left) as SelectListView
        setSettingText(0, nowUrl, liveItem)
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
                setSettingText(position, nowUrl, liveItem)
                mSettingPos = position
                mAdapterSetting!!.setSelection(position)
                mSettingList.setSelect(position, 0)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        mSettingList.setOnItemClickListener { adapter, v, pos, id ->
            setSettingText(pos, nowUrl, liveItem)
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
}