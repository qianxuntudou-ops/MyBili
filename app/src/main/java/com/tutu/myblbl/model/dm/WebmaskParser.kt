package com.tutu.myblbl.model.dm

import android.graphics.Path
import android.util.Base64
import com.tutu.myblbl.core.common.log.AppLog
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream

object WebmaskParser {

    private const val TAG = "WebmaskParser"

    fun parse(data: ByteArray, fps: Int = 0): DmMaskData? {
        if (data.size < 16) return null
        val buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)

        val header = ByteArray(4)
        buf.get(header)
        if (!header.contentEquals("MASK".toByteArray())) {
            AppLog.e(TAG, "Invalid webmask header")
            return null
        }

        val version = buf.int
        buf.int // vU, skip
        val segmentCount = buf.int
        if (segmentCount <= 0 || segmentCount > 1000) {
            AppLog.e(TAG, "Invalid segment count: $segmentCount")
            return null
        }

        // 只读 segment 索引表，不解析帧数据
        val segments = mutableListOf<LazyMaskSegment>()
        for (i in 0 until segmentCount) {
            val timeMs = buf.long
            val offset = buf.long.toInt()
            val endOffset = if (i + 1 < segmentCount) {
                // 需要知道下一个 segment 的 offset，先存临时值
                offset // placeholder
            } else {
                data.size
            }
            segments.add(LazyMaskSegment(timeMs = timeMs, startOffset = offset, endOffset = 0))
        }

        // 回填 endOffset
        for (i in segments.indices) {
            val endOff = if (i + 1 < segments.size) segments[i + 1].startOffset else data.size
            segments[i] = segments[i].copy(endOffset = endOff, rawData = data)
        }

        if (segments.isEmpty()) return null

        // 诊断日志
        AppLog.d(TAG, "Lazy parse done: ${segments.size} segments indexed")
        segments.take(3).forEach {
            AppLog.d(TAG, "seg: timeMs=${it.timeMs}, bytes=${it.endOffset - it.startOffset}")
        }

        return DmMaskData(fps = fps, rawSegments = segments)
    }

    fun parseSegmentFrames(segment: LazyMaskSegment, fps: Int, segDurationMs: Long = 0L): List<MaskFrame>? {
        val data = segment.rawData ?: return null
        val startOffset = segment.startOffset
        val endOffset = segment.endOffset
        if (startOffset < 0 || endOffset > data.size || startOffset >= endOffset) return null

        val segBytes = data.copyOfRange(startOffset, endOffset)
        val decompressed = try {
            GZIPInputStream(segBytes.inputStream()).use { gzip ->
                val out = ByteArrayOutputStream()
                gzip.copyTo(out)
                out.toByteArray()
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "GZIP decompress failed: ${e.message}")
            return null
        }

        val separator = "data:image/svg+xml;base64,".toByteArray()
        val parts = splitBy(decompressed, separator)
        if (parts.size <= 1) return null

        var diagLogged = false
        var emptyCount = 0
        val totalFrames = parts.size - 1
        val frames = mutableListOf<MaskFrame>()
        for (frameIdx in 1 until parts.size) {
            val localIdx = frameIdx - 1
            val ptsMs = if (totalFrames > 1 && segDurationMs > 0L) {
                segment.timeMs + (localIdx.toLong() * segDurationMs) / totalFrames
            } else if (fps > 0) {
                segment.timeMs + localIdx.toLong() * 1000L / fps
            } else {
                segment.timeMs
            }
            val b64Data = parts[frameIdx]
            val svgBytes = try {
                Base64.decode(b64Data, Base64.DEFAULT)
            } catch (e: Exception) {
                frames.add(MaskFrame(presentationTimeMs = ptsMs, paths = emptyList()))
                emptyCount++
                continue
            }
            val svgText = svgBytes.toString(Charsets.UTF_8)
            if (!diagLogged) {
                AppLog.d(TAG, "SVG frame[0] preview: ${svgText.take(300).replace('\n', ' ')}")
                diagLogged = true
            }
            val parsed = parseSvgPaths(svgText)
            if (parsed.paths.isEmpty()) emptyCount++
            frames.add(
                MaskFrame(
                    presentationTimeMs = ptsMs,
                    paths = parsed.paths,
                    svgWidth = parsed.width,
                    svgHeight = parsed.height,
                )
            )
        }

        // 前向填充：空帧用前一个有 path 的帧替代，避免遮罩冻结。
        var lastFrame: MaskFrame? = null
        for (i in frames.indices) {
            if (frames[i].paths.isNotEmpty()) {
                lastFrame = frames[i]
            } else if (lastFrame != null) {
                // 保留当前帧的 PTS，只继承 paths/svgWidth/svgHeight
                frames[i] = MaskFrame(
                    presentationTimeMs = frames[i].presentationTimeMs,
                    paths = lastFrame.paths,
                    svgWidth = lastFrame.svgWidth,
                    svgHeight = lastFrame.svgHeight,
                )
                emptyCount--
            }
        }

        // 用第一个非空帧的 svgSize 作诊断——首帧若为空，svgWidth/Height 都是 0，
        // 显示 0x0 会误导调试。
        val sampleSize = frames.firstOrNull { it.svgWidth > 0 }
            ?.let { "${it.svgWidth}x${it.svgHeight}" } ?: "?"
        AppLog.d(TAG, "parseSegmentFrames: total=${frames.size}, withPaths=${frames.size - emptyCount}, remainingEmpty=$emptyCount, svgSize=$sampleSize")
        return frames.takeIf { it.isNotEmpty() }
    }

    private fun splitBy(data: ByteArray, delimiter: ByteArray): List<ByteArray> {
        val result = mutableListOf<ByteArray>()
        var start = 0
        var i = 0
        while (i <= data.size - delimiter.size) {
            var match = true
            for (j in delimiter.indices) {
                if (data[i + j] != delimiter[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                result.add(data.copyOfRange(start, i))
                start = i + delimiter.size
                i = start
            } else {
                i++
            }
        }
        result.add(data.copyOfRange(start, data.size))
        return result
    }

    /**
     * 解析 SVG 文本，返回 path 列表与 SVG 标定尺寸。SVG 尺寸是渲染时缩放的关键——
     * 横屏视频常见 320×180、竖屏可能是 180×320，硬编码会导致严重错位。
     */
    private data class ParsedSvg(val paths: List<Path>, val width: Int, val height: Int)

    private fun parseSvgPaths(svgText: String): ParsedSvg {
        val viewWidth = extractFloat(svgText, """width="([\d.]+)px"""")
            ?: return ParsedSvg(emptyList(), 0, 0)
        val viewHeight = extractFloat(svgText, """height="([\d.]+)px"""")
            ?: return ParsedSvg(emptyList(), 0, 0)
        if (viewWidth <= 0f || viewHeight <= 0f) return ParsedSvg(emptyList(), 0, 0)

        val pathRegex = Regex("""<path\s+d="([^"]+)"""")
        val results = mutableListOf<Path>()

        for (match in pathRegex.findAll(svgText)) {
            val d = match.groupValues[1]
            val path = svgPathToAndroidPath(d, viewWidth, viewHeight)
            if (path != null) {
                results.add(path)
            }
        }
        return ParsedSvg(results, viewWidth.toInt(), viewHeight.toInt())
    }

    private fun svgPathToAndroidPath(d: String, viewWidth: Float, viewHeight: Float): Path? {
        try {
            // webmask 协议的 path 是「整画面减人物」的带洞填充，B 站编码端用 SVG fill-rule="evenodd"
            // 表达"外圈减内圈"——外轮廓 + 人物轮廓内圈相消才能让人物区域成为"洞"。
            // Android Path 默认 WINDING（非零环绕规则）会把内圈也算成 fill 内部，clipPath 时
            // 人物区域反而被算作"允许绘制"，弹幕从人物身上漏出来——必须显式改成 EVEN_ODD。
            val path = Path().apply { fillType = Path.FillType.EVEN_ODD }
            val tokens = tokenizeSvgPath(d.trim())
            var i = 0
            var currentCommand = 'M'

            while (i < tokens.size) {
                val token = tokens[i]
                when {
                    token.length == 1 && token[0] in "MLmlCcSsQqTtAaHhVv" -> {
                        currentCommand = token[0]
                        i++
                        continue
                    }
                    token == "z" || token == "Z" -> {
                        path.close()
                        i++
                        continue
                    }
                    token[0].isDigit() || token[0] == '-' || token[0] == '.' -> { /* keep currentCommand */ }
                    else -> { i++; continue }
                }

                when (currentCommand) {
                    'M' -> {
                        if (i + 1 >= tokens.size) break
                        path.moveTo(tokens[i].toFloat() * 0.1f, viewHeight - tokens[i + 1].toFloat() * 0.1f)
                        i += 2
                    }
                    'L' -> {
                        if (i + 1 >= tokens.size) break
                        path.lineTo(tokens[i].toFloat() * 0.1f, viewHeight - tokens[i + 1].toFloat() * 0.1f)
                        i += 2
                    }
                    'm' -> {
                        if (i + 1 >= tokens.size) break
                        path.rMoveTo(tokens[i].toFloat() * 0.1f, tokens[i + 1].toFloat() * -0.1f)
                        i += 2
                    }
                    'l' -> {
                        if (i + 1 >= tokens.size) break
                        path.rLineTo(tokens[i].toFloat() * 0.1f, tokens[i + 1].toFloat() * -0.1f)
                        i += 2
                    }
                    'C' -> {
                        if (i + 5 >= tokens.size) break
                        path.cubicTo(
                            tokens[i].toFloat() * 0.1f, viewHeight - tokens[i + 1].toFloat() * 0.1f,
                            tokens[i + 2].toFloat() * 0.1f, viewHeight - tokens[i + 3].toFloat() * 0.1f,
                            tokens[i + 4].toFloat() * 0.1f, viewHeight - tokens[i + 5].toFloat() * 0.1f
                        )
                        i += 6
                    }
                    'c' -> {
                        if (i + 5 >= tokens.size) break
                        path.rCubicTo(
                            tokens[i].toFloat() * 0.1f, tokens[i + 1].toFloat() * -0.1f,
                            tokens[i + 2].toFloat() * 0.1f, tokens[i + 3].toFloat() * -0.1f,
                            tokens[i + 4].toFloat() * 0.1f, tokens[i + 5].toFloat() * -0.1f
                        )
                        i += 6
                    }
                    else -> i++
                }
            }
            return path
        } catch (e: Exception) {
            AppLog.e(TAG, "SVG path parse error: ${e.message}")
            return null
        }
    }

    private fun tokenizeSvgPath(d: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        val commands = "MmLlCcSsQqTtAaHhVvZz"

        for (ch in d) {
            if (ch in commands) {
                if (sb.isNotEmpty()) {
                    tokens.add(sb.toString())
                    sb.clear()
                }
                tokens.add(ch.toString())
            } else if (ch.isWhitespace() || ch == ',') {
                if (sb.isNotEmpty()) {
                    tokens.add(sb.toString())
                    sb.clear()
                }
            } else {
                sb.append(ch)
            }
        }
        if (sb.isNotEmpty()) {
            tokens.add(sb.toString())
        }
        return tokens
    }

    private fun extractFloat(text: String, pattern: String): Float? {
        return Regex(pattern).find(text)?.groupValues?.get(1)?.toFloatOrNull()
    }
}
