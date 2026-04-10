package com.tutu.myblbl.model.dm

data class SpecialDanmakuAction(
    val startMs: Long,
    val durationMs: Long,
    val x: Float? = null,
    val y: Float? = null,
    val alpha: Float? = null,
    val color: Int? = null,
    val scaleX: Float? = null,
    val scaleY: Float? = null,
    val rotation: Float? = null,
    val fontSize: Int? = null
)
