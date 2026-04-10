package com.tutu.myblbl.model.video.quality

enum class VideoCodecEnum(val id: Int, val displayName: String) {
    AVC(7, "H.264/AVC"),
    HEVC(12, "H.265/HEVC"),
    AV1(13, "AV1");
    
    companion object {
        fun fromId(id: Int): VideoCodecEnum {
            return values().find { it.id == id } ?: AVC
        }
    }
}
