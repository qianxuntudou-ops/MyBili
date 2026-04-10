package com.tutu.myblbl.model.video.quality

data class VideoQuality(
    val id: Int,
    val name: String,
    val resolution: String = "",
    val codecId: Int = 0,
    val bandwidth: Long = 0,
    val baseUrl: String = "",
    val backupUrls: List<String>? = null
) {
    companion object {
        val QUALITY_8K = VideoQuality(127, "8K 超高清", "4320P")
        val QUALITY_DOLBY_VISION = VideoQuality(126, "杜比视界", "DolbyVision")
        val QUALITY_HDR_VIVID = VideoQuality(129, "HDR Vivid", "HDRVivid")
        val QUALITY_HDR = VideoQuality(125, "HDR 真彩色", "HDR")
        val QUALITY_4K = VideoQuality(120, "4K 超清", "2160P")
        val QUALITY_1080P_60 = VideoQuality(116, "1080P 60帧", "1080P60")
        val QUALITY_1080P_PLUS = VideoQuality(112, "1080P+ 高码率", "1080P+")
        val QUALITY_SUPER_RESOLUTION = VideoQuality(100, "智能修复", "SuperResolution")
        val QUALITY_1080P = VideoQuality(80, "1080P 高清", "1080P")
        val QUALITY_720P_60 = VideoQuality(74, "720P 60帧", "720P60")
        val QUALITY_720P = VideoQuality(64, "720P 高清", "720P")
        val QUALITY_480P = VideoQuality(32, "480P 清晰", "480P")
        val QUALITY_360P = VideoQuality(16, "360P 流畅", "360P")
        val QUALITY_240P = VideoQuality(6, "240P 极速", "240P")
        
        fun fromId(id: Int): VideoQuality {
            return when (id) {
                129 -> QUALITY_HDR_VIVID
                127 -> QUALITY_8K
                126 -> QUALITY_DOLBY_VISION
                125 -> QUALITY_HDR
                120 -> QUALITY_4K
                116 -> QUALITY_1080P_60
                112 -> QUALITY_1080P_PLUS
                100 -> QUALITY_SUPER_RESOLUTION
                80 -> QUALITY_1080P
                74 -> QUALITY_720P_60
                64 -> QUALITY_720P
                32 -> QUALITY_480P
                16 -> QUALITY_360P
                6 -> QUALITY_240P
                else -> VideoQuality(id, "画质 $id")
            }
        }
    }
}
