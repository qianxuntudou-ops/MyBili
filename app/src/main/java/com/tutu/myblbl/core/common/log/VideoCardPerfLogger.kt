package com.tutu.myblbl.core.common.log

import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object VideoCardPerfLogger {

    private const val TAG = "VideoCardPerf"
    private val inflateCounts = ConcurrentHashMap<String, AtomicInteger>()

    fun <T> measureInflate(source: String, block: () -> T): T {
        val startMs = SystemClock.elapsedRealtime()
        return block().also {
            val elapsed = SystemClock.elapsedRealtime() - startMs
            val count = inflateCounts.getOrPut(source) { AtomicInteger(0) }.incrementAndGet()
            if (count <= 12 || count % 20 == 0 || elapsed >= 8) {
                AppLog.i(TAG, "cell_video inflate source=$source count=$count elapsed=${elapsed}ms")
            }
        }
    }
}
