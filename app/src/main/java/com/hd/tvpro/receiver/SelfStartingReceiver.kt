package com.hd.tvpro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hd.tvpro.MainActivity
import com.hd.tvpro.util.PreferenceMgr

/**
 * 作者：By 15968
 * 日期：On 2022/6/29
 * 时间：At 18:54
 */
class SelfStartingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val selfStart = PreferenceMgr.getBoolean(context, "selfStart", false)
        Log.d(Companion.TAG, "onReceive: ")
        if (selfStart) {
            val bootIntent = Intent(context, MainActivity::class.java)
            // 这里必须为FLAG_ACTIVITY_NEW_TASK
            bootIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(bootIntent)
        }
    }

    companion object {
        private const val TAG = "SelfStartingReceiver"
    }
}