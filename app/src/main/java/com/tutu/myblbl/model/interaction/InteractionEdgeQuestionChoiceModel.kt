package com.tutu.myblbl.model.interaction

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class InteractionEdgeQuestionChoiceModel(
    @SerializedName("id")
    val id: Long = 0,
    @SerializedName("cid")
    val cid: Long = 0,
    @SerializedName("option")
    val option: String = "",
    @SerializedName("condition")
    val condition: String = "",
    @SerializedName("native_action")
    val nativeAction: String = "",
    @SerializedName("platform_action")
    val platformAction: String = "",
    @SerializedName("x")
    val x: Int = 0,
    @SerializedName("y")
    val y: Int = 0,
    @SerializedName("text_align")
    val textAlign: Int = 0,
    @SerializedName("is_default")
    val isDefault: Int = 0,
    @SerializedName("is_hidden")
    val isHidden: Int = 0
) : Serializable
