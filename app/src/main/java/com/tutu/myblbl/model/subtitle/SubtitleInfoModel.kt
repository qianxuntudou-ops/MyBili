package com.tutu.myblbl.model.subtitle

import com.google.gson.annotations.SerializedName

data class SubtitleInfoModel(
    @SerializedName("id")
    val id: Long = 0,
    
    @SerializedName("lan")
    val lan: String = "",
    
    @SerializedName("lan_doc")
    val lanDoc: String = "",
    
    @SerializedName("is_lock")
    val isLock: Boolean = false,
    
    @SerializedName("subtitle_url")
    val subtitleUrl: String = "",
    
    @SerializedName("author")
    val author: SubtitleAuthor? = null
)

data class SubtitleAuthor(
    @SerializedName("mid")
    val mid: Long = 0,
    
    @SerializedName("name")
    val name: String = "",
    
    @SerializedName("sex")
    val sex: String = "",
    
    @SerializedName("face")
    val face: String = "",
    
    @SerializedName("sign")
    val sign: String = ""
)

data class SubtitleData(
    @SerializedName("body")
    val body: List<SubtitleItem>? = null
)

data class SubtitleItem(
    @SerializedName("from")
    val from: Float = 0f,
    
    @SerializedName("to")
    val to: Float = 0f,
    
    @SerializedName("location")
    val location: Int = 2,
    
    @SerializedName("content")
    val content: String = ""
)
