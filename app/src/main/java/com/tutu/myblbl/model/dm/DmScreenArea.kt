package com.tutu.myblbl.model.dm

enum class DmScreenArea(
    val area: Int,
    val showName: String
) {
    OneEighth(-1, "1/8"),
    OneSixth(0, "1/6"),
    Quarter(1, "1/4"),
    Half(3, "1/2"),
    ThreeQuarter(7, "3/4"),
    Full(15, "全屏")
}
