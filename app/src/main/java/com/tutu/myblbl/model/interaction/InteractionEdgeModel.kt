package com.tutu.myblbl.model.interaction

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class InteractionEdgeModel(
    @SerializedName("dimension")
    val dimension: InteractionEdgeDimensionModel? = null,
    @SerializedName("questions")
    val questions: List<InteractionEdgeQuestionModel>? = null,
    @SerializedName("skin")
    val skin: InteractionEdgeSkinModel? = null
) : Serializable
