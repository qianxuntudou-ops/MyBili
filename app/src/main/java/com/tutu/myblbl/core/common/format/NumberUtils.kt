package com.tutu.myblbl.core.common.format

import java.text.DecimalFormat
import java.util.Locale

object NumberUtils {
    
    private val wanFormat = DecimalFormat("#.#万")
    private val yiFormat = DecimalFormat("#.#亿")
    private val commaFormat = DecimalFormat("#,###")
    
    fun formatCount(count: Long): String {
        return when {
            count >= 100000000 -> {
                yiFormat.format(count / 100000000.0)
            }
            count >= 10000 -> {
                wanFormat.format(count / 10000.0)
            }
            else -> {
                commaFormat.format(count)
            }
        }
    }
    
    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return when {
            hours > 0 -> String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs)
            else -> String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
        }
    }
    

}
