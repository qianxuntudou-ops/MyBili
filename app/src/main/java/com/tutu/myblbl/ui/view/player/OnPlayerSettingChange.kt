package com.tutu.myblbl.ui.view.player

import com.tutu.myblbl.model.video.quality.VideoQuality
import com.tutu.myblbl.model.video.quality.AudioQuality
import com.tutu.myblbl.model.video.quality.VideoCodecEnum

interface OnPlayerSettingChange {
    fun onVideoQualityChange(quality: VideoQuality)
    fun onAudioQualityChange(quality: AudioQuality)
    fun onPlaybackSpeedChange(speed: Float)
    fun onSubtitleChange(position: Int)
    fun onVideoCodecChange(codec: VideoCodecEnum)
    fun onAspectRatioChange(ratio: Int)
}
