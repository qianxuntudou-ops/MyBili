package com.tutu.myblbl.model.dm

data class SpecialDanmakuModel(
    val id: Long,
    val progress: Int,
    val content: String,
    val color: Int,
    val fontSize: Int,
    val x: Float,
    val y: Float,
    val anchorX: Float,
    val anchorY: Float,
    val alpha: Float,
    val bold: Boolean,
    val strokeColor: Int,
    val strokeWidth: Float,
    val durationMs: Long,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotation: Float = 0f,
    val animations: List<SpecialDanmakuAction> = emptyList()
)
