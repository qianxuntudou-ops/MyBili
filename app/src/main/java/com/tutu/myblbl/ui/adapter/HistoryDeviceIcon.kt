package com.tutu.myblbl.ui.adapter

import com.tutu.myblbl.R

internal object HistoryDeviceIcon {
    data class DeviceIcon(
        val drawableRes: Int,
        val contentDescription: String
    )

    fun resolve(dt: Int): DeviceIcon? = when (dt) {
        1, 3, 5, 7 -> DeviceIcon(R.drawable.ic_history_device_mobile, "手机观看")
        2 -> DeviceIcon(R.drawable.ic_history_device_pc, "电脑观看")
        4, 6 -> DeviceIcon(R.drawable.ic_history_device_pad, "平板观看")
        33 -> DeviceIcon(R.drawable.ic_history_device_tv, "电视观看")
        else -> null
    }
}
