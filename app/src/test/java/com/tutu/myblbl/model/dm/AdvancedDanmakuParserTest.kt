package com.tutu.myblbl.model.dm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedDanmakuParserTest {

    @Test
    fun parse_extractsMotionAndAlphaTimeline() {
        val result = AdvancedDanmakuParser.parse(
            id = 1L,
            progressMs = 1500,
            color = 0x33CCFF,
            fontSize = 36,
            rawContent = """["10","20","0.2-0.8","4.5","高级弹幕","15","0","80","60","2","0","false"]"""
        )

        assertNotNull(result)
        result ?: return
        assertEquals("高级弹幕", result.content)
        assertEquals(0.1f, result.x, 0.001f)
        assertEquals(0.2f, result.y, 0.001f)
        assertEquals(0.2f, result.alpha, 0.001f)
        assertEquals(15f, result.rotation, 0.001f)
        assertEquals(4500L, result.durationMs)
        assertEquals(2, result.animations.size)
        assertEquals(0.8f, result.animations[0].alpha ?: 0f, 0.001f)
        assertEquals(0.8f, result.animations[1].x ?: 0f, 0.001f)
        assertEquals(0.6f, result.animations[1].y ?: 0f, 0.001f)
    }

    @Test
    fun parse_supportsPathMotion() {
        val result = AdvancedDanmakuParser.parse(
            id = 2L,
            progressMs = 0,
            color = 0xFFFFFF,
            fontSize = 25,
            rawContent = """["0","0","1-1","3","路径","0","0","","","3","0","true","","","M 0,0 L 50,50 L 100,0"]"""
        )

        assertNotNull(result)
        result ?: return
        assertTrue(result.animations.size >= 2)
        assertEquals(0.5f, result.animations.first().x ?: 0f, 0.001f)
        assertEquals(0.5f, result.animations.first().y ?: 0f, 0.001f)
    }
}
