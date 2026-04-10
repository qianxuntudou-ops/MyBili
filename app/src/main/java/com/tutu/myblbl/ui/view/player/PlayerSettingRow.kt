package com.tutu.myblbl.ui.view.player

internal sealed class PlayerSettingRow {
    data class Header(
        val title: String,
        val subTitle: String? = null
    ) : PlayerSettingRow()

    data class Item(
        val id: Int,
        val title: String,
        val value: String = "",
        val iconRes: Int? = null,
        val checked: Boolean = false,
        val showArrow: Boolean = true
    ) : PlayerSettingRow()
}
