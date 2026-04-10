package com.tutu.myblbl.model.dm

import kotlin.math.roundToInt

object SpecialDanmakuParser {

    private const val DEFAULT_FONT_SIZE = 25
    private const val DEFAULT_DURATION_MS = 4_000L
    private const val DEFAULT_SCALE = 1f

    private val definitionRegex = Regex(
        """def\s+text\s+([^\s{]+)\s*\{(.*?)\}""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    private val actionRegex = Regex(
        """(then\s+)?set\s+([^\s{]+)\s*\{(.*?)\}\s*([0-9.]+)\s*(ms|s)""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    private val contentRegex = Regex(
        "content\\s*=\\s*\"((?:\\\\.|[^\"])*)\"",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    private val fontSizeRegex = Regex(
        """fontSize\s*=\s*([0-9.]+%?)""",
        RegexOption.IGNORE_CASE
    )
    private val booleanRegexTemplate = """%s\s*=\s*("?)(true|false|1|0)\1"""

    fun parse(
        parentId: Long,
        progressMs: Int,
        fallbackColor: Int,
        script: String
    ): List<SpecialDanmakuModel> {
        if (script.isBlank()) {
            return emptyList()
        }
        val matches = definitionRegex.findAll(script).toList()
        if (matches.isEmpty()) {
            return script.extractFallbackText()?.let { text ->
                listOf(
                    SpecialDanmakuModel(
                        id = parentId,
                        progress = progressMs,
                        content = text,
                        color = fallbackColor.normalizeRgbColor(),
                        fontSize = DEFAULT_FONT_SIZE,
                        x = 0.5f,
                        y = 0.1f,
                        anchorX = 0.5f,
                        anchorY = 0f,
                        alpha = 1f,
                        bold = false,
                        strokeColor = 0,
                        strokeWidth = 0f,
                        durationMs = DEFAULT_DURATION_MS
                    )
                )
            }.orEmpty()
        }

        val actionsByTarget = actionRegex.findAll(script)
            .groupBy { it.groupValues[2] }

        return matches.mapIndexedNotNull { index, match ->
            val targetId = match.groupValues[1]
            val body = match.groupValues[2]
            val actions = actionsByTarget[targetId].orEmpty()
            val text = body.extractQuotedProperty(contentRegex)
                ?.cleanupRenderedText()
                ?.takeIf { it.isNotBlank() }
                ?: return@mapIndexedNotNull null
            val bodyColor = body.extractColor()
            val actionColor = actions.mapNotNull { it.groupValues[3].extractColorOrNull() }
                .lastOrNull { it.isMeaningfulColor() }
            val resolvedColor = when {
                bodyColor.isMeaningfulColor() -> bodyColor
                actionColor.isMeaningfulColor() -> actionColor ?: fallbackColor
                else -> fallbackColor
            }.normalizeRgbColor()
            val animations = actions.toAnimations(body.extractFontSize())

            val resolvedDurationMs = (
                animations.maxOfOrNull { it.startMs + it.durationMs }
                    ?: actions.extractDurationMs()
            ).takeIf { it > 0L } ?: DEFAULT_DURATION_MS

            SpecialDanmakuModel(
                id = parentId + index + 1L,
                progress = progressMs,
                content = text,
                color = resolvedColor,
                fontSize = body.extractFontSize(),
                x = body.extractCoordinate("x"),
                y = body.extractCoordinate("y"),
                anchorX = body.extractCoordinate("anchorX", defaultValue = 0f, treatLargeAsPercent = false),
                anchorY = body.extractCoordinate("anchorY", defaultValue = 0f, treatLargeAsPercent = false),
                alpha = body.extractFloat("alpha", 1f).coerceIn(0f, 1f),
                bold = body.extractBoolean("bold"),
                strokeColor = body.extractColorOrNull("strokeColor") ?: 0,
                strokeWidth = body.extractFloat("strokeWidth", 0f).coerceAtLeast(0f),
                durationMs = resolvedDurationMs,
                scaleX = body.extractFloat("scaleX", DEFAULT_SCALE).coerceAtLeast(0.1f),
                scaleY = body.extractFloat("scaleY", DEFAULT_SCALE).coerceAtLeast(0.1f),
                rotation = body.extractFloat("rotateZ", body.extractFloat("rotation", 0f)),
                animations = animations
            )
        }
    }

    private fun List<MatchResult>.toAnimations(baseFontSize: Int): List<SpecialDanmakuAction> {
        if (isEmpty()) {
            return emptyList()
        }
        var chainCursorMs = 0L
        return mapNotNull { match ->
            val isThen = match.groupValues[1].isNotBlank()
            val body = match.groupValues[3]
            val durationMs = match.groupValues[4].toDurationMs(match.groupValues[5])
                .coerceAtLeast(0L)
            val action = SpecialDanmakuAction(
                startMs = if (isThen) chainCursorMs else 0L,
                durationMs = durationMs,
                x = body.extractCoordinateOrNull("x"),
                y = body.extractCoordinateOrNull("y"),
                alpha = body.extractFloatOrNull("alpha")?.coerceIn(0f, 1f),
                color = body.extractColorOrNull(),
                scaleX = body.extractFloatOrNull("scaleX")?.coerceAtLeast(0.1f),
                scaleY = body.extractFloatOrNull("scaleY")?.coerceAtLeast(0.1f),
                rotation = body.extractFloatOrNull("rotateZ")
                    ?: body.extractFloatOrNull("rotation"),
                fontSize = body.extractActionFontSize(baseFontSize)
            ).takeIf { candidate ->
                candidate.x != null ||
                    candidate.y != null ||
                    candidate.alpha != null ||
                    candidate.color != null ||
                    candidate.scaleX != null ||
                    candidate.scaleY != null ||
                    candidate.rotation != null ||
                    candidate.fontSize != null
            }
            if (isThen) {
                chainCursorMs += durationMs
            } else {
                chainCursorMs = durationMs
            }
            action
        }
    }

    private fun List<MatchResult>.extractDurationMs(): Long {
        var chainCursorMs = 0L
        var maxDuration = 0L
        forEach { match ->
            val isThen = match.groupValues[1].isNotBlank()
            val durationMs = match.groupValues[4].toDurationMs(match.groupValues[5])
            val endMs = if (isThen) {
                chainCursorMs + durationMs
            } else {
                durationMs
            }
            chainCursorMs = endMs
            if (endMs > maxDuration) {
                maxDuration = endMs
            }
        }
        return maxDuration.takeIf { it > 0L } ?: DEFAULT_DURATION_MS
    }

    private fun String.extractFallbackText(): String? {
        return extractQuotedProperty(contentRegex)
            ?.cleanupRenderedText()
            ?.takeIf { it.isNotBlank() }
    }

    private fun String.extractQuotedProperty(regex: Regex): String? {
        val match = regex.find(this) ?: return null
        return match.groupValues[1]
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
    }

    private fun String.cleanupRenderedText(): String {
        return replace('\u3000', ' ')
            .replace('\n', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun String.extractColor(property: String = "color"): Int {
        return extractColorOrNull(property) ?: 0
    }

    private fun String.extractColorOrNull(property: String = "color"): Int? {
        val regex = Regex(
            """${Regex.escape(property)}\s*=\s*0x([0-9a-fA-F]{6,8})""",
            RegexOption.IGNORE_CASE
        )
        val value = regex.find(this)?.groupValues?.getOrNull(1) ?: return null
        return value.takeLast(6).toIntOrNull(16)
    }

    private fun Int?.isMeaningfulColor(): Boolean {
        return this != null && this != 0 && this != 0xFFFFFF
    }

    private fun Int.normalizeRgbColor(): Int {
        return takeIf { it != 0 } ?: 0xFFFFFF
    }

    private fun String.extractFontSize(): Int {
        val rawValue = fontSizeRegex.find(this)?.groupValues?.getOrNull(1) ?: return DEFAULT_FONT_SIZE
        return if (rawValue.endsWith("%")) {
            val percentage = rawValue.removeSuffix("%").toFloatOrNull() ?: return DEFAULT_FONT_SIZE
            (percentage * 10f).roundToInt().coerceIn(12, 64)
        } else {
            rawValue.toFloatOrNull()?.roundToInt()?.coerceIn(12, 64) ?: DEFAULT_FONT_SIZE
        }
    }

    private fun String.extractCoordinate(
        property: String,
        defaultValue: Float = 0.5f,
        treatLargeAsPercent: Boolean = true
    ): Float {
        val raw = extractRawNumericValue(property) ?: return defaultValue
        val value = raw.value
        val normalized = when {
            raw.isPercent -> value / 100f
            treatLargeAsPercent && value > 1f -> value / 100f
            else -> value
        }
        return normalized.coerceIn(0f, 1f)
    }

    private fun String.extractCoordinateOrNull(
        property: String,
        treatLargeAsPercent: Boolean = true
    ): Float? {
        val raw = extractRawNumericValue(property) ?: return null
        val normalized = when {
            raw.isPercent -> raw.value / 100f
            treatLargeAsPercent && raw.value > 1f -> raw.value / 100f
            else -> raw.value
        }
        return normalized.coerceIn(0f, 1f)
    }

    private fun String.extractFloat(property: String, defaultValue: Float): Float {
        return extractRawNumericValue(property)?.value ?: defaultValue
    }

    private fun String.extractFloatOrNull(property: String): Float? {
        return extractRawNumericValue(property)?.value
    }

    private fun String.extractActionFontSize(baseFontSize: Int): Int? {
        val rawValue = fontSizeRegex.find(this)?.groupValues?.getOrNull(1) ?: return null
        return if (rawValue.endsWith("%")) {
            val percentage = rawValue.removeSuffix("%").toFloatOrNull() ?: return null
            (percentage * 10f).roundToInt().coerceIn(12, 64)
        } else {
            rawValue.toFloatOrNull()?.roundToInt()?.coerceIn(12, 64) ?: baseFontSize
        }
    }

    private fun String.extractBoolean(property: String): Boolean {
        val regex = Regex(
            booleanRegexTemplate.format(Regex.escape(property)),
            RegexOption.IGNORE_CASE
        )
        return regex.find(this)?.groupValues?.getOrNull(2)
            ?.lowercase()
            ?.let { it == "1" || it == "true" }
            ?: false
    }

    private fun String.extractRawNumericValue(property: String): NumericValue? {
        val regex = Regex(
            """${Regex.escape(property)}\s*=\s*(-?[0-9.]+)(%)?""",
            RegexOption.IGNORE_CASE
        )
        val match = regex.find(this) ?: return null
        val value = match.groupValues[1].toFloatOrNull() ?: return null
        return NumericValue(
            value = value,
            isPercent = match.groupValues[2] == "%"
        )
    }

    private data class NumericValue(
        val value: Float,
        val isPercent: Boolean
    )

    private fun String.toDurationMs(unit: String): Long {
        val value = toDoubleOrNull() ?: return 0L
        return if (unit.equals("s", ignoreCase = true)) {
            (value * 1000.0).roundToInt().toLong()
        } else {
            value.roundToInt().toLong()
        }
    }
}
