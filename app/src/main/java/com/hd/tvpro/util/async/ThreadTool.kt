package com.hd.tvpro.util.async

import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

/**
 * 作者：By 15968
 * 日期：On 2021/10/24
 * 时间：At 20:29
 */
object ThreadTool {

    //这里的代码是拿的AsyncTask的源码，作用是创建合理可用的线程池容量
    private val CPU_COUNT = Runtime.getRuntime().availableProcessors()
    private val CORE_POOL_SIZE = max(2, min(CPU_COUNT - 1, 3)) + 1
    private val taskQueue = LinkedBlockingDeque<Runnable>(8192)
    private val executorService: ExecutorService = ThreadPoolExecutor(
        CORE_POOL_SIZE, 6,
        10L, TimeUnit.SECONDS, taskQueue
    )

    fun executeNewTask(command: Runnable?) {
        executorService.execute(command)
    }
}