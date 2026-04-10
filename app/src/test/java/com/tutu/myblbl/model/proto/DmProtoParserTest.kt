package com.tutu.myblbl.model.proto

import com.google.protobuf.CodedOutputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DmProtoParserTest {

    @Test
    fun parseSegment_readsExtendedDanmakuFields() {
        val elemBytes = protoMessage(
            varintField(1, 123L),
            varintField(2, 456),
            varintField(3, 7),
            varintField(4, 32),
            uint32Field(5, 0x00FF99FF),
            stringField(6, "mid_hash"),
            stringField(7, "[\"0\",\"0\",\"1-1\",\"4.5\",\"高级弹幕\"]"),
            varintField(8, 1_717_171_717L),
            varintField(9, 11),
            stringField(10, "action"),
            varintField(11, 1),
            stringField(12, "id_str"),
            varintField(13, 2),
            stringField(14, "animation")
        )
        val aiFlagBytes = protoMessage(
            bytesField(
                1,
                protoMessage(
                    varintField(1, 123L),
                    uint32Field(2, 9)
                )
            )
        )
        val segmentBytes = protoMessage(
            bytesField(1, elemBytes),
            varintField(2, 3),
            bytesField(3, aiFlagBytes)
        )

        val result = DmProtoParser.parseSegment(segmentBytes)

        assertEquals(1, result.elems.size)
        val elem = result.elems.first()
        assertEquals(123L, elem.id)
        assertEquals(456, elem.progress)
        assertEquals(7, elem.mode)
        assertEquals(32, elem.fontSize)
        assertEquals(0x00FF99FF, elem.color)
        assertEquals("mid_hash", elem.midHash)
        assertEquals(1_717_171_717L, elem.ctime)
        assertEquals("action", elem.action)
        assertEquals(1, elem.pool)
        assertEquals(2, elem.attr)
        assertEquals("id_str", elem.idStr)
        assertEquals("animation", elem.animation)
        assertEquals(3, result.state)
        assertEquals(1, result.aiFlag.dmFlags.size)
        assertEquals(9, result.aiFlag.dmFlags.first().flag)
    }

    @Test
    fun parseView_readsSegmentAndSpecialInfo() {
        val viewBytes = protoMessage(
            bytesField(
                4,
                protoMessage(
                    varintField(1, 360000),
                    varintField(2, 6)
                )
            ),
            stringField(6, "http://example.com/special-1.bin"),
            stringField(6, "http://example.com/special-2.bin"),
            varintField(8, 6000L)
        )

        val result = DmProtoParser.parseView(viewBytes)

        assertEquals(360000, result.segmentDurationMs)
        assertEquals(6, result.totalSegments)
        assertEquals(6000L, result.totalCount)
        assertEquals(2, result.specialDanmakuUrls.size)
        assertTrue(result.specialDanmakuUrls.first().contains("special-1"))
    }

    private fun protoMessage(vararg fields: ByteArray): ByteArray {
        return ByteArrayOutputStream().use { output ->
            fields.forEach(output::write)
            output.toByteArray()
        }
    }

    private fun varintField(fieldNumber: Int, value: Long): ByteArray {
        return ByteArrayOutputStream().use { output ->
            val codedOutput = CodedOutputStream.newInstance(output)
            codedOutput.writeUInt32NoTag(fieldNumber shl 3)
            codedOutput.writeInt64NoTag(value)
            codedOutput.flush()
            output.toByteArray()
        }
    }

    private fun uint32Field(fieldNumber: Int, value: Int): ByteArray {
        return ByteArrayOutputStream().use { output ->
            val codedOutput = CodedOutputStream.newInstance(output)
            codedOutput.writeUInt32NoTag(fieldNumber shl 3)
            codedOutput.writeUInt32NoTag(value)
            codedOutput.flush()
            output.toByteArray()
        }
    }

    private fun stringField(fieldNumber: Int, value: String): ByteArray {
        return bytesField(fieldNumber, value.toByteArray(Charsets.UTF_8))
    }

    private fun bytesField(fieldNumber: Int, value: ByteArray): ByteArray {
        return ByteArrayOutputStream().use { output ->
            val codedOutput = CodedOutputStream.newInstance(output)
            codedOutput.writeUInt32NoTag((fieldNumber shl 3) or 2)
            codedOutput.writeByteArrayNoTag(value)
            codedOutput.flush()
            output.toByteArray()
        }
    }
}
