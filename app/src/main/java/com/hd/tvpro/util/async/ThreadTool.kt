package com.hd.tvpro.util.async

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * 作者：By 15968
 * 日期：On 2021/10/24
 * 时间：At 20:29
 */
object ThreadTool {

    fun executeNewTask(command: Runnable?) {
        GlobalScope.launch(Dispatchers.IO) {
            command?.run()
        }
    }
}