package com.tutu.myblbl.core.common.media

import com.tutu.myblbl.model.video.quality.VideoCodecEnum
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoCodecSupportTest {

    @Test
    fun keepsPreferredCodecFirstWhenHardwareSupportsIt() {
        assertEquals(
            listOf(VideoCodecEnum.HEVC, VideoCodecEnum.AV1, VideoCodecEnum.AVC),
            VideoCodecSupport.orderCandidates(
                availableCodecs = listOf(VideoCodecEnum.AVC, VideoCodecEnum.HEVC, VideoCodecEnum.AV1),
                preferredCodec = VideoCodecEnum.HEVC,
                hardwareSupportedCodecs = listOf(VideoCodecEnum.AVC, VideoCodecEnum.HEVC, VideoCodecEnum.AV1)
            )
        )
    }

    @Test
    fun fallsBackToAv1ThenAvcWhenHevcHardwareIsMissing() {
        assertEquals(
            listOf(VideoCodecEnum.AV1, VideoCodecEnum.AVC, VideoCodecEnum.HEVC),
            VideoCodecSupport.orderCandidates(
                availableCodecs = listOf(VideoCodecEnum.AVC, VideoCodecEnum.HEVC, VideoCodecEnum.AV1),
                preferredCodec = VideoCodecEnum.HEVC,
                hardwareSupportedCodecs = listOf(VideoCodecEnum.AVC, VideoCodecEnum.AV1)
            )
        )
    }

    @Test
    fun fallsBackToHevcThenAvcWhenAv1HardwareIsMissing() {
        assertEquals(
            listOf(VideoCodecEnum.HEVC, VideoCodecEnum.AVC, VideoCodecEnum.AV1),
            VideoCodecSupport.orderCandidates(
                availableCodecs = listOf(VideoCodecEnum.AVC, VideoCodecEnum.HEVC, VideoCodecEnum.AV1),
                preferredCodec = VideoCodecEnum.AV1,
                hardwareSupportedCodecs = listOf(VideoCodecEnum.AVC, VideoCodecEnum.HEVC)
            )
        )
    }

    @Test
    fun keepsSoftwareCodecAsLastResortWhenNoHardwareMatchExists() {
        assertEquals(
            listOf(VideoCodecEnum.AV1, VideoCodecEnum.HEVC),
            VideoCodecSupport.orderCandidates(
                availableCodecs = listOf(VideoCodecEnum.HEVC, VideoCodecEnum.AV1),
                preferredCodec = VideoCodecEnum.AV1,
                hardwareSupportedCodecs = emptyList()
            )
        )
    }
}
