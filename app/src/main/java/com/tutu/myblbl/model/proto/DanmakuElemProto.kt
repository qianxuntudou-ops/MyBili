package com.tutu.myblbl.model.proto

import java.io.Serializable

data class DanmakuElemProto(
    val id: Long = 0,
    val progress: Int = 0,
    val mode: Int = 1,
    val fontSize: Int = 25,
    val color: Int = 0xFFFFFFFF.toInt(),
    val colorful: Int = 0,
    val midHash: String = "",
    val content: String = "",
    val ctime: Long = 0L,
    val weight: Int = 0,
    val pool: Int = 0,
    val action: String = "",
    val attr: Int = 0,
    val idStr: String = "",
    val animation: String = ""
) : Serializable
