package com.tutu.myblbl.model.dm

import java.io.Serializable

data class DmModel(
    val id: Long = 0L,
    val color: Int = 0,
    val colorful: Int = 0,
    val colorfulSrc: String = "",
    val colorfulStyle: DmColorfulStyle = DmColorfulStyle.NONE,
    val content: String = "",
    val mode: Int = 0,
    val progress: Int = 0,
    val fontSize: Int = 25,
    val weight: Int = 0,
    val pool: Int = 0,
    val attr: Int = 0,
    val aiFlagScore: Int = 0,
    val midHash: String = "",
    val ctime: Long = 0L,
    val action: String = "",
    val idStr: String = "",
    val animation: String = ""
) : Serializable
