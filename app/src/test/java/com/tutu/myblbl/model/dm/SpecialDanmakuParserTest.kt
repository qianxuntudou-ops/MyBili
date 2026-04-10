package com.tutu.myblbl.model.dm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpecialDanmakuParserTest {

    @Test
    fun parse_extractsColoredTextsFromBasScript() {
        val script = """
            def text t1_1 {
                content = "嗚呼　華のように鮮やかに　さあ"
                fontSize = 2.8%
                x = 50%
                y = 0%
                alpha = 1
                color = 0xff99ff
                anchorX = 0.5
                anchorY = 0
            }
            set t1_1 {alpha = 1} 10200ms
            then set t1_1 {alpha = 0} 1000ms
            set t1_1 {color = 0x3300ff} 11200ms

            def text t1_11 {
                content = "啊　繁花盛放　雍容绚丽　呵"
                fontSize = 2.3%
                x = 50%
                y = 6%
                alpha = 1
                color = 0xff99ff
                anchorX = 0.5
                anchorY = 0
            }
            set t1_11 {alpha = 1} 10200ms
        """.trimIndent()

        val result = SpecialDanmakuParser.parse(
            parentId = 100L,
            progressMs = 5200,
            fallbackColor = 0xFFFFFF,
            script = script
        )

        assertEquals(2, result.size)
        assertEquals("嗚呼 華のように鮮やかに さあ", result[0].content)
        assertEquals(0xFF99FF, result[0].color)
        assertEquals(0.5f, result[0].x, 0.001f)
        assertEquals(0f, result[0].y, 0.001f)
        assertEquals(0.5f, result[0].anchorX, 0.001f)
        assertEquals(0f, result[0].anchorY, 0.001f)
        assertEquals(1f, result[0].alpha, 0.001f)
        assertTrue(result[0].fontSize in 12..48)
        assertEquals(11200L, result[0].durationMs)
        assertEquals(3, result[0].animations.size)
        assertEquals(10200L, result[0].animations[1].startMs)
        assertEquals(0f, result[0].animations[1].alpha ?: 1f, 0.001f)
    }

    @Test
    fun parse_usesActionColorWhenBodyIsWhite() {
        val script = """
            def text a {
                content = "Hello Bilibili!"
                fontSize = 5%
                x = 0
                y = 90%
                color = 0xFFFFFF
            }
            set a {
                color = 0xFF0000
            } 3s
        """.trimIndent()

        val result = SpecialDanmakuParser.parse(
            parentId = 200L,
            progressMs = 8000,
            fallbackColor = 0xFFFFFF,
            script = script
        )

        assertEquals(1, result.size)
        assertEquals(0xFF0000, result.first().color)
        assertEquals(0f, result.first().x, 0.001f)
        assertEquals(0.9f, result.first().y, 0.001f)
        assertEquals(3000L, result.first().durationMs)
        assertEquals(1, result.first().animations.size)
    }

    @Test
    fun parse_extractsAnchorAlphaAndStroke() {
        val script = """
            def text title {
                content = "彩色测试"
                fontSize = 3.5%
                x = 25%
                y = 40%
                anchorX = 0.3
                anchorY = 0.6
                alpha = 0.85
                color = 0xabcdef
                strokeWidth = 1.5
                strokeColor = 0x112233
                bold = 1
            }
            set title { alpha = 1 } 5s
        """.trimIndent()

        val result = SpecialDanmakuParser.parse(
            parentId = 300L,
            progressMs = 1200,
            fallbackColor = 0xFFFFFF,
            script = script
        )

        assertEquals(1, result.size)
        assertEquals(0.25f, result.first().x, 0.001f)
        assertEquals(0.4f, result.first().y, 0.001f)
        assertEquals(0.3f, result.first().anchorX, 0.001f)
        assertEquals(0.6f, result.first().anchorY, 0.001f)
        assertEquals(0.85f, result.first().alpha, 0.001f)
        assertEquals(0x112233, result.first().strokeColor)
        assertEquals(1.5f, result.first().strokeWidth, 0.001f)
        assertTrue(result.first().bold)
        assertFalse(result.first().content.isBlank())
    }
}
