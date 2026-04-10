package com.tutu.myblbl.model.dm

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import kotlin.math.roundToInt

object AdvancedDanmakuParser {

    fun parse(
        id: Long,
        progressMs: Int,
        color: Int,
        fontSize: Int,
        rawContent: String
    ): SpecialDanmakuModel? {
        val payload = runCatching {
            JsonParser.parseString(rawContent).asJsonArray
        }.getOrNull() ?: return null
        val content = payload.optString(4).trim()
        if (content.isBlank()) {
            return null
        }

        val startX = payload.optCoordinate(0)
        val startY = payload.optCoordinate(1)
        val (alphaFrom, alphaTo) = payload.optAlphaRange(2)
        val lifetimeMs = payload.optDurationMs(3).coerceAtLeast(500L)
        val rotation = payload.optFloat(5)
        val endX = payload.optCoordinateOrNull(7)
        val endY = payload.optCoordinateOrNull(8)
        val moveDurationMs = payload.optDurationMs(9).takeIf { it > 0L } ?: lifetimeMs
        val moveDelayMs = payload.optDurationMs(10).coerceAtLeast(0L)
        val shadowEnabled = payload.optShadowEnabled(11)
        val pathAnimations = payload.optPathAnimations(moveDurationMs, moveDelayMs)

        val animations = buildList {
            if (alphaFrom != alphaTo) {
                add(
                    SpecialDanmakuAction(
                        startMs = 0L,
                        durationMs = lifetimeMs,
                        alpha = alphaTo
                    )
                )
            }
            if (pathAnimations.isNotEmpty()) {
                addAll(pathAnimations)
            } else if (endX != null || endY != null) {
                add(
                    SpecialDanmakuAction(
                        startMs = moveDelayMs,
                        durationMs = moveDurationMs,
                        x = endX ?: startX,
                        y = endY ?: startY
                    )
                )
            }
        }

        return SpecialDanmakuModel(
            id = id,
            progress = progressMs,
            content = content,
            color = color.takeIf { it != 0 } ?: 0xFFFFFF,
            fontSize = fontSize.coerceAtLeast(12),
            x = startX,
            y = startY,
            anchorX = 0f,
            anchorY = 0f,
            alpha = alphaFrom,
            bold = false,
            strokeColor = if (shadowEnabled) 0 else 0xFF000000.toInt(),
            strokeWidth = if (shadowEnabled) 0f else 2f,
            durationMs = maxOf(
                lifetimeMs,
                animations.maxOfOrNull { it.startMs + it.durationMs } ?: 0L
            ),
            rotation = rotation,
            animations = animations
        )
    }

    private fun JsonArray.optCoordinate(index: Int): Float {
        return optCoordinateOrNull(index) ?: 0f
    }

    private fun JsonArray.optCoordinateOrNull(index: Int): Float? {
        if (index >= size() || this[index].isJsonNull) {
            return null
        }
        val value = optString(index).takeIf { it.isNotBlank() }?.toFloatOrNull()
            ?: return null
        return when {
            value > 100f -> (value / 1000f).coerceIn(0f, 1f)
            value > 1f -> (value / 100f).coerceIn(0f, 1f)
            else -> value.coerceIn(0f, 1f)
        }
    }

    private fun JsonArray.optAlphaRange(index: Int): Pair<Float, Float> {
        val raw = optString(index).takeIf { it.isNotBlank() } ?: return 1f to 1f
        val parts = raw.split("-")
        val start = parts.getOrNull(0)?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f
        val end = parts.getOrNull(1)?.toFloatOrNull()?.coerceIn(0f, 1f) ?: start
        return start to end
    }

    private fun JsonArray.optDurationMs(index: Int): Long {
        return optString(index)
            .takeIf { it.isNotBlank() }
            ?.toDoubleOrNull()
            ?.let { (it * 1000.0).roundToInt().toLong() }
            ?: 0L
    }

    private fun JsonArray.optFloat(index: Int): Float {
        return optString(index)
            .takeIf { it.isNotBlank() }
            ?.toFloatOrNull()
            ?: 0f
    }

    private fun JsonArray.optShadowEnabled(index: Int): Boolean {
        val raw = optString(index).trim()
        if (raw.isBlank()) {
            return false
        }
        return raw != "false" && raw != "0"
    }

    private fun JsonArray.optPathAnimations(durationMs: Long, delayMs: Long): List<SpecialDanmakuAction> {
        val path = optString(14).trim()
        if (path.isBlank()) {
            return emptyList()
        }
        val points = Regex("""[-0-9.]+""")
            .findAll(path)
            .mapNotNull { it.value.toFloatOrNull() }
            .chunked(2)
            .mapNotNull { pair ->
                val x = pair.getOrNull(0) ?: return@mapNotNull null
                val y = pair.getOrNull(1) ?: return@mapNotNull null
                val nx = when {
                    x > 100f -> x / 1000f
                    x > 1f -> x / 100f
                    else -> x
                }
                val ny = when {
                    y > 100f -> y / 1000f
                    y > 1f -> y / 100f
                    else -> y
                }
                nx.coerceIn(0f, 1f) to ny.coerceIn(0f, 1f)
            }
            .toList()
        if (points.size < 2) {
            return emptyList()
        }
        val segmentDuration = (durationMs / (points.size - 1)).coerceAtLeast(1L)
        return points.drop(1).mapIndexed { index, point ->
            SpecialDanmakuAction(
                startMs = delayMs + segmentDuration * index,
                durationMs = segmentDuration,
                x = point.first,
                y = point.second
            )
        }
    }

    private fun JsonArray.optString(index: Int): String {
        if (index >= size() || this[index].isJsonNull) {
            return ""
        }
        val element = this[index]
        return when {
            element.isJsonPrimitive -> element.asJsonPrimitive.asString
            else -> element.toString()
        }
    }
}
