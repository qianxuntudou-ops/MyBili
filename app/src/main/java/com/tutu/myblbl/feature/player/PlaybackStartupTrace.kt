package com.tutu.myblbl.feature.player

import android.os.SystemClock
import com.tutu.myblbl.core.common.log.AppLog
import java.util.concurrent.atomic.AtomicLong

object PlaybackStartupTrace {
    const val TAG = "PlaybackTrace"
    const val NO_TRACE = ""
    private val sequence = AtomicLong(0L)

    fun newTraceId(): String = "play-${System.currentTimeMillis()}-${sequence.incrementAndGet()}"

    fun nowMs(): Long = SystemClock.elapsedRealtime()

    fun log(
        traceId: String?,
        startElapsedMs: Long,
        step: String,
        message: String = ""
    ) {
        if (traceId.isNullOrBlank() || startElapsedMs <= 0L) return
        val elapsed = nowMs() - startElapsedMs
        val suffix = if (message.isBlank()) "" else " $message"
        AppLog.i(TAG, "PLAYBACK_TRACE trace=$traceId step=$step +${elapsed}ms$suffix")
    }
}
