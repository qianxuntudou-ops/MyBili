package com.tutu.myblbl.model.dm

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class DmColorfulStyle(
    val fillColorUrl: String = "",
    val strokeColorUrl: String = ""
) : Serializable {

    val hasTexture: Boolean
        get() = fillColorUrl.isNotBlank() || strokeColorUrl.isNotBlank()

    companion object {
        val NONE = DmColorfulStyle()
    }
}

object DmColorfulStyleParser {

    private val gson = Gson()

    fun parse(raw: String?): DmColorfulStyle {
        val normalized = raw.orEmpty().trim()
        if (normalized.isEmpty()) {
            return DmColorfulStyle.NONE
        }
        return runCatching {
            gson.fromJson(normalized, DmColorfulStylePayload::class.java)
        }.getOrNull()?.toStyle() ?: DmColorfulStyle.NONE
    }

    private fun DmColorfulStylePayload.toStyle(): DmColorfulStyle {
        return DmColorfulStyle(
            fillColorUrl = normalizeUrl(fillColor ?: fillColorUrl),
            strokeColorUrl = normalizeUrl(strokeColor ?: strokeColorUrl)
        )
    }

    private fun normalizeUrl(url: String?): String {
        val value = url.orEmpty().trim()
        if (value.isEmpty()) {
            return ""
        }
        return when {
            value.startsWith("https://", ignoreCase = true) -> value
            value.startsWith("http://", ignoreCase = true) -> "https://${value.removePrefix("http://")}"
            value.startsWith("//") -> "https:$value"
            else -> value
        }
    }

    private data class DmColorfulStylePayload(
        @SerializedName("fill_color")
        val fillColor: String? = null,
        @SerializedName("fillColor")
        val fillColorUrl: String? = null,
        @SerializedName("stroke_color")
        val strokeColor: String? = null,
        @SerializedName("strokeColor")
        val strokeColorUrl: String? = null
    )
}
