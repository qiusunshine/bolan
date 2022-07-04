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
import com.hd.tvpro.util.ToastMgr
import com.hd.tvpro.view.ChannelListView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import service.LiveModel
import service.model.LiveItem
import kotlin.math.max
import kotlin.math.min

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
        var channelNamesList: ArrayList<String> = ArrayList()
        var groupNamesList: ArrayList<String> = ArrayList()
        var selectGroup = 0

        fun loadBackground(context: Context) {
            try {
                val sub = PreferenceMgr.getString(context, "sub", "")
                if (!sub.isNullOrEmpty()) {
                    GlobalScope.launch(Dispatchers.IO) {
                        LiveModel.loadData(sub) { list ->
                            GlobalScope.launch(Dispatchers.Main) {
                                liveData.clear()
                                liveData.addAll(list)
                                groupNamesList.clear()
                                channelNamesList.clear()
                                if (list.isNotEmpty() && !list[0].children.isNullOrEmpty()) {
                                    groupNamesList.addAll(liveData.map { it.name })
                                    if (selectGroup >= liveData.size) {
                                        selectGroup = 0
                                    }
                                    channelNamesList.addAll(liveData[selectGroup].children!!.map { it.name })
                                } else {
                                    channelNamesList.addAll(liveData.map { it.name })
                                }
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
    private var lv_live_group: ChannelListView? = null
    private var channelView: View? = null
    private var adapter: LiveListViewChannelAdapter? = null
    private var groupAdapter: LiveListViewAdapter? = null
    var selected = -1
    var showSelected = 0
    var showGroupSelected = 0


    fun show(anchor: View) {
        val sub = PreferenceMgr.getString(App.INSTANCE, "sub", "")
        if (sub.isNullOrEmpty()) {
            ToastMgr.shortBottomCenter(anchor.context, "还没有设置直播源地址哦~")
            return
        }
        if (channelView != null) {
            popupWindow?.dismiss()
            val dm = DisplayMetrics()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
            windowManager!!.defaultDisplay.getMetrics(dm)
            val width: Int = max(dm.widthPixels, dm.heightPixels)
            popupWindow = PopupWindow(channelView, width / 2, ViewGroup.LayoutParams.WRAP_CONTENT)
            popupWindow?.let {
                it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                it.isFocusable = true
                it.isOutsideTouchable = true
                it.update()
                it.showAtLocation(anchor, Gravity.START, 0, 0)
            }
            channelListView?.requestFocus()
            if (liveData.isEmpty()) {
                refreshData(sub)
            }
            return
        }
        channelView = LayoutInflater.from(context).inflate(R.layout.layout_live, null)
        val dm = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        windowManager!!.defaultDisplay.getMetrics(dm)
        val width: Int = max(dm.widthPixels, dm.heightPixels)
        val height: Int = min(dm.widthPixels, dm.heightPixels)

        val fontSize = width / 42
        AppConfig.fontSize = fontSize
        try {
            channelListView = channelView!!.findViewById(R.id.lv_setting_right)
            lv_live_group = channelView!!.findViewById(R.id.lv_live_group)
            adapter = LiveListViewChannelAdapter(context, channelNamesList, 0)
            groupAdapter = LiveListViewAdapter(context, groupNamesList, 0)
            channelListView!!.adapter = adapter
            lv_live_group!!.adapter = groupAdapter

            lv_live_group!!.setOnItemClickListener { parent, v, posval, id ->
                //单击项目时
                selectGroup = posval
                showGroupSelected = posval
                groupAdapter?.setSelection(posval)
                lv_live_group?.setSelection(posval)

                channelNamesList.clear()
                channelNamesList.addAll(liveData[showGroupSelected].children!!.map { it.name })
                adapter!!.notifyDataSetChanged()
                channelListView!!.setSelection(0)
                adapter?.setSelection(0)
                channelListView!!.smoothScrollToPosition(0)
            }
            lv_live_group!!.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        pos: Int,
                        id: Long
                    ) {
                        showGroupSelected = pos
                        lv_live_group?.setSelect(
                            pos,
                            (pos - lv_live_group!!.getFirstVisiblePosition()) * AppConfig.liveHeight
                        )
                        groupAdapter?.setSelection(pos)

                        channelNamesList.clear()
                        channelNamesList.addAll(liveData[showGroupSelected].children!!.map { it.name })
                        adapter!!.notifyDataSetChanged()
                        channelListView!!.setSelection(0)
                        channelListView!!.smoothScrollToPosition(0)
                        adapter?.setSelection(0)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            lv_live_group?.setOnKeyListener { v, keycode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (keycode) {
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (showGroupSelected == lv_live_group!!.lastVisiblePosition - 1) {
                                if (lv_live_group!!.lastVisiblePosition != lv_live_group!!.count - 1) {
                                    lv_live_group!!.smoothScrollBy(
                                        AppConfig.liveHeight,
                                        0
                                    )
                                }
                            }
                            println(showGroupSelected)
                            println(lv_live_group!!.lastVisiblePosition)
                            println(lv_live_group!!.count - 1)
                            if (showGroupSelected == lv_live_group!!.lastVisiblePosition) {
                                if (lv_live_group!!.lastVisiblePosition == lv_live_group!!.count - 1) {
                                    lv_live_group!!.setSelection(0)
                                }
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (showGroupSelected == lv_live_group!!.firstVisiblePosition + 1) {
                                if (lv_live_group!!.firstVisiblePosition != 0) {
                                    lv_live_group!!.smoothScrollBy(
                                        -AppConfig.liveHeight,
                                        0
                                    )
                                }
                            }
                            if (showGroupSelected == lv_live_group!!.firstVisiblePosition) {
                                if (lv_live_group!!.firstVisiblePosition == 0) {
                                    lv_live_group!!.setSelection(lv_live_group!!.count - 1)
                                }
                            }
                        }
                        else -> {
                        }
                    }
                }
                false
            }

            channelListView!!.setOnItemClickListener { parent, v, posval, id ->
                //单击项目时
                selected = posval
                showSelected = posval
                channelListView?.setSelection(posval)
                adapter?.setSelection(posval)
                if (groupNamesList.isNotEmpty()) {
                    clickListener(liveData[showGroupSelected].children!![posval])
                } else {
                    clickListener(liveData[posval])
                }
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
                        adapter?.setSelection(pos)
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
        popupWindow = PopupWindow(channelView, width / 2, ViewGroup.LayoutParams.WRAP_CONTENT)
        popupWindow?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.isFocusable = true
            it.isOutsideTouchable = true
            it.update()
            it.showAtLocation(anchor, Gravity.START, 0, 0)
        }

        channelListView?.requestFocus()
        //先尝试加载本地的
        GlobalScope.launch(Dispatchers.IO) {
            LiveModel.loadLocalData { list ->
                loadFinish(list)
            }
        }
        refreshData(sub)
    }

    private fun loadFinish(list: ArrayList<LiveItem>){
        GlobalScope.launch(Dispatchers.Main) {
            if (!JSON.toJSONString(list).equals(JSON.toJSONString(liveData))) {
                liveData.clear()
                liveData.addAll(list)
                groupNamesList.clear()
                channelNamesList.clear()
                if (list.isNotEmpty() && !list[0].children.isNullOrEmpty()) {
                    groupNamesList.addAll(liveData.map { it.name })
                    if (selectGroup >= liveData.size) {
                        selectGroup = 0
                    }
                    channelNamesList.addAll(liveData[selectGroup].children!!.map { it.name })
                } else {
                    channelNamesList.addAll(liveData.map { it.name })
                }
                notifySelect(true)
            } else {
                notifySelect(false)
            }
            channelListView?.requestFocus()
        }
    }

    private fun refreshData(sub: String) {
        GlobalScope.launch(Dispatchers.IO) {
            LiveModel.loadData(sub) { list ->
                loadFinish(list)
            }
        }
    }

    private fun notifySelect(updateForce: Boolean) {
        if (selectGroup >= 0 && selected > 0) {
            if (selectGroup >= groupNamesList.size) {
                selectGroup = 0
            }
            if (selectGroup < groupNamesList.size) {
                showGroupSelected = selectGroup
                groupAdapter?.setSelection(selectGroup)
            } else if (updateForce) {
                showGroupSelected = 0
                groupAdapter?.notifyDataSetChanged()
            }
            if (selected >= channelNamesList.size) {
                selected = 0
            }
            if (selected < channelNamesList.size) {
                adapter?.setSelection(selected)
            } else if (updateForce) {
                adapter?.notifyDataSetChanged()
            }
        } else if (selectGroup >= 0) {
            if (selectGroup >= groupNamesList.size) {
                selectGroup = 0
            }
            if (selectGroup < groupNamesList.size) {
                showGroupSelected = selectGroup
                groupAdapter?.setSelection(selectGroup)
            } else if (updateForce) {
                showGroupSelected = 0
                groupAdapter?.notifyDataSetChanged()
            }
        } else if (selected >= 0) {
            if (selected >= channelNamesList.size) {
                selected = 0
            }
            if (selected < channelNamesList.size) {
                adapter?.setSelection(selected)
            } else if (updateForce) {
                adapter?.notifyDataSetChanged()
            }
        } else if (updateForce) {
            adapter?.notifyDataSetChanged()
            groupAdapter?.notifyDataSetChanged()
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