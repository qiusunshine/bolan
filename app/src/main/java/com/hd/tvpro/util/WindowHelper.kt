package com.hd.tvpro.util

import android.app.ActivityManager
import android.content.Context

/**
 * 作者：By 15968
 * 日期：On 2021/11/4
 * 时间：At 13:23
 */
object WindowHelper {

    private fun isRunningForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcessInfoList = activityManager.runningAppProcesses
        for (appProcessInfo in appProcessInfoList) {
            if (appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                && appProcessInfo.processName == context.applicationInfo.processName
            ) {
                return true
            }
        }
        return false
    }

    fun setTopApp(context: Context) {
        if (isRunningForeground(context)) {
            return
        }
        //获取ActivityManager
        val activityManager: ActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        //获得当前运行的task(任务)
        val taskInfoList: List<ActivityManager.RunningTaskInfo> =
            activityManager.getRunningTasks(100)
        for (taskInfo in taskInfoList) {
            //找到本应用的 task，并将它切换到前台
            if (taskInfo.topActivity?.packageName.equals(context.packageName)) {
                //返回启动它的根任务（home 或者 MainActivity）
                activityManager.moveTaskToFront(
                    taskInfo.id,
                    ActivityManager.MOVE_TASK_NO_USER_ACTION
                )
                break
            }
        }
    }
}