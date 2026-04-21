package com.tutu.myblbl.feature.player

import com.tutu.myblbl.core.model.id.Aid
import com.tutu.myblbl.core.model.id.Bvid
import com.tutu.myblbl.core.model.id.Cid
import com.tutu.myblbl.core.model.id.EpId

data class PlayerLaunchContext(
    val aid: Long = 0L,
    val bvid: String = "",
    val cid: Long = 0L,
    val epId: Long = 0L,
    val seasonId: Long = 0L,
    val seekPositionMs: Long = 0L,
    val startEpisodeIndex: Int = -1
) {

    val typedAid: Aid get() = Aid(aid)
    val typedBvid: Bvid get() = Bvid(bvid)
    val typedCid: Cid get() = Cid(cid)
    val typedEpId: EpId get() = EpId(epId)

    companion object {
        fun create(
            aid: Long = 0L,
            bvid: String = "",
            cid: Long = 0L,
            epId: Long = 0L,
            seasonId: Long = 0L,
            seekPositionMs: Long = 0L,
            startEpisodeIndex: Int = -1
        ): PlayerLaunchContext {
            return PlayerLaunchContext(
                aid = aid,
                bvid = bvid,
                cid = cid,
                epId = epId,
                seasonId = seasonId,
                seekPositionMs = seekPositionMs.coerceAtLeast(0L),
                startEpisodeIndex = startEpisodeIndex
            )
        }
    }
}
