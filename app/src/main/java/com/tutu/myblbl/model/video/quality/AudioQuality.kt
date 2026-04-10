package com.tutu.myblbl.model.video.quality

data class AudioQuality(
    val id: Int,
    val name: String,
    val bandwidth: Long = 0,
    val codecId: Int = 0,
    val baseUrl: String = "",
    val backupUrls: List<String>? = null
) {
    companion object {
        val AUDIO_192K = AudioQuality(30280, "192Kbps")
        val AUDIO_132K = AudioQuality(30232, "132Kbps")
        val AUDIO_64K = AudioQuality(30216, "64Kbps")
        val AUDIO_DOLBY = AudioQuality(30250, "杜比全景声")
        val AUDIO_HIRES = AudioQuality(30251, "Hi-Res无损")
        
        fun fromId(id: Int): AudioQuality {
            return when (id) {
                30280 -> AUDIO_192K
                30232 -> AUDIO_132K
                30216 -> AUDIO_64K
                30250 -> AUDIO_DOLBY
                30251 -> AUDIO_HIRES
                else -> AudioQuality(id, "音轨 $id")
            }
        }
    }
}
