package com.tutu.myblbl.model.proto

import java.io.Serializable

data class DanmakuFlagProto(
    val dmid: Long = 0,
    val flag: Int = 0
) : Serializable

data class DanmakuAIFlagProto(
    val dmFlags: List<DanmakuFlagProto> = emptyList()
) : Serializable

data class DmColorfulProto(
    val type: Int = 0,
    val src: String = ""
) : Serializable

data class DmSegMobileReplyProto(
    val elems: List<DanmakuElemProto> = emptyList(),
    val state: Int = 0,
    val aiFlag: DanmakuAIFlagProto = DanmakuAIFlagProto(),
    val colorfulSrc: List<DmColorfulProto> = emptyList()
) : Serializable
