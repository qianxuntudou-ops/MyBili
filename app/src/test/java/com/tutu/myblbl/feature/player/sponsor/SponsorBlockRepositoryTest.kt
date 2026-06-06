package com.tutu.myblbl.feature.player.sponsor

import org.junit.Assert.assertEquals
import org.junit.Test

class SponsorBlockRepositoryTest {

    @Test
    fun selectsSegmentsForMatchingBvidAndCidFromHashResponse() {
        val videos = listOf(
            SponsorBlockRepository.HashVideoResponse(
                videoID = "BV-other",
                segments = listOf(segment(uuid = "other", cid = "100"))
            ),
            SponsorBlockRepository.HashVideoResponse(
                videoID = "BV-target",
                segments = listOf(
                    segment(uuid = "first-p", cid = "100"),
                    segment(uuid = "second-p", cid = "200")
                )
            )
        )

        val selected = SponsorBlockRepository.selectSegments(videos, "BV-target", 200L)

        assertEquals(listOf("second-p"), selected.map { it.UUID })
    }

    @Test
    fun keepsAllCidsWhenCidIsMissing() {
        val videos = listOf(
            SponsorBlockRepository.HashVideoResponse(
                videoID = "BV-target",
                segments = listOf(
                    segment(uuid = "first-p", cid = "100"),
                    segment(uuid = "second-p", cid = "200")
                )
            )
        )

        val selected = SponsorBlockRepository.selectSegments(videos, "BV-target", 0L)

        assertEquals(listOf("first-p", "second-p"), selected.map { it.UUID })
    }

    private fun segment(uuid: String, cid: String): SponsorSegment {
        return SponsorSegment(
            segment = listOf(1f, 2f),
            UUID = uuid,
            category = SponsorSegment.CATEGORY_SPONSOR,
            cid = cid
        )
    }
}
