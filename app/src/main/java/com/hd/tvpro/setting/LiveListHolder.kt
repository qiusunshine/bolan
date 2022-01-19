package com.hd.tvpro.setting

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.DisplayMetrics
import android.view.*
import android.widget.AdapterView
import android.widget.PopupWindow
import com.alibaba.fastjson.JSON
import com.hd.tvpro.R
import com.hd.tvpro.app.App
import com.hd.tvpro.constants.AppConfig
import com.hd.tvpro.util.PreferenceMgr
import com.hd.tvpro.view.ChannelListView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import service.LiveModel
import service.model.LiveItem

/**
 * 作者：By 15968
 * 日期：On 2021/10/26
 * 时间：At 15:33
 */
class LiveListHolder constructor(
    private val context: Context,
    private val clickListener: (LiveItem) -> Unit
) {
    companion object {
        var liveData: ArrayList<LiveItem> = ArrayList()
        var settingArrayList: ArrayList<String> = ArrayList()

        fun loadBackground(context: Context) {
            try {
                val sub = PreferenceMgr.getString(context, "sub", "")
                if (!sub.isNullOrEmpty()) {
                    GlobalScope.launch(Dispatchers.IO) {
                        LiveModel.loadData(sub) { list ->
                            GlobalScope.launch(Dispatchers.Main) {
                                liveData.clear()
                                liveData.addAll(list)
                                settingArrayList.clear()
                                settingArrayList.addAll(liveData.map { it.name })
                            }
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private var popupWindow: PopupWindow? = null
    private var channelListView: ChannelListView? = null
    private var channelView: View? = null
    private var adapter: LiveListViewAdapter? = null
    var selected = -1
    var showSelected = 0


    fun show(anchor: View) {
        val sub = PreferenceMgr.getString(App.INSTANCE, "sub", "")
        if (sub.isNullOrEmpty()) {
            return
        }
        if (popupWindow != null) {
            popupWindow?.let {
                it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                it.isFocusable = true
                it.isOutsideTouchable = true
                it.update()
                it.showAtLocation(anchor, Gravity.START, 0, 0)
            }
            channelListView?.requestFocus()
            return
        }
        channelView = LayoutInflater.from(context).inflate(R.layout.layout_live, null)
        val dm = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        windowManager!!.defaultDisplay.getMetrics(dm)
        val width: Int = dm.widthPixels
        val height: Int = dm.heightPixels

        val fontSize = width / 42
        AppConfig.fontSize = fontSize
        try {
            channelListView = channelView!!.findViewById(R.id.lv_setting_right)
            adapter = LiveListViewAdapter(context, settingArrayList, 0)
            channelListView!!.adapter = adapter
            channelListView!!.setOnItemClickListener { parent, v, posval, id ->
                //单击项目时
                selected = posval
                showSelected = posval
                channelListView?.setSelection(posval)
                clickListener(liveData[posval])
            }
            channelListView!!.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        pos: Int,
                        id: Long
                    ) {
                        showSelected = pos
                        channelListView?.setSelect(
                            pos,
                            (pos - channelListView!!.getFirstVisiblePosition()) * AppConfig.liveHeight
                        )
                        adapter?.notifyDataSetChanged()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {

                    }

                }
            channelListView?.setOnKeyListener { v, keycode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (keycode) {
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (showSelected == channelListView!!.lastVisiblePosition - 1) {
                                if (channelListView!!.lastVisiblePosition != channelListView!!.count - 1) {
                                    channelListView!!.smoothScrollBy(
                                        AppConfig.liveHeight,
                                        0
                                    )
                                }
                            }
                            println(showSelected)
                            println(channelListView!!.lastVisiblePosition)
                            println(channelListView!!.count - 1)
                            if (showSelected == channelListView!!.lastVisiblePosition) {
                                if (channelListView!!.lastVisiblePosition == channelListView!!.count - 1) {
                                    channelListView!!.setSelection(0)
                                }
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (showSelected == channelListView!!.firstVisiblePosition + 1) {
                                if (channelListView!!.firstVisiblePosition != 0) {
                                    channelListView!!.smoothScrollBy(
                                        -AppConfig.liveHeight,
                                        0
                                    )
                                }
                            }
                            if (showSelected == channelListView!!.firstVisiblePosition) {
                                if (channelListView!!.firstVisiblePosition == 0) {
                                    channelListView!!.setSelection(channelListView!!.count - 1)
                                }
                            }
                        }
                        else -> {
                        }
                    }
                }
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        AppConfig.liveHeight = (height - 12) / 8 - channelListView!!.dividerHeight + 1
        //展示window
        popupWindow = PopupWindow(channelView, width / 4, ViewGroup.LayoutParams.WRAP_CONTENT)
        popupWindow?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.isFocusable = true
            it.isOutsideTouchable = true
            it.update()
            it.showAtLocation(anchor, Gravity.START, 0, 0)
        }

        channelListView?.requestFocus()
        GlobalScope.launch(Dispatchers.IO) {
            LiveModel.loadData(sub) { list ->
                GlobalScope.launch(Dispatchers.Main) {
                    if (!JSON.toJSONString(list).equals(JSON.toJSONString(liveData))) {
                        liveData.clear()
                        liveData.addAll(list)
                        settingArrayList.clear()
                        settingArrayList.addAll(liveData.map { it.name })
                        notifySelect(true)
                    } else {
                        notifySelect(false)
                    }
                    channelListView?.requestFocus()
                }
            }
        }
    }

    private fun notifySelect(updateForce: Boolean) {
        if (selected >= 0) {
            if (selected >= settingArrayList.size) {
                selected = 0
            }
            if (selected < settingArrayList.size) {
                adapter?.setSelection(selected)
            } else if (updateForce) {
                adapter?.notifyDataSetChanged()
            }
        } else if (updateForce) {
            adapter?.notifyDataSetChanged()
        }
    }

    fun isShowing(): Boolean {
        if (popupWindow != null && popupWindow!!.isShowing) {
            return true
        }
        return false
    }

    fun hide() {
        if (popupWindow != null && popupWindow!!.isShowing) {
            popupWindow!!.dismiss()
        }
    }
}