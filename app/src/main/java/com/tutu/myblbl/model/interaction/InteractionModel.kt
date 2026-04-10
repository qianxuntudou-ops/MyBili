package com.tutu.myblbl.model.interaction

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class InteractionModel(
    @SerializedName("title")
    val title: String = "",
    @SerializedName("edge_id")
    val edgeId: String = "",
    @SerializedName("edges")
    val edges: InteractionEdgeModel? = null,
    @SerializedName("hidden_vars")
    val hiddenVars: List<InteractionVariableModel>? = null,
    @SerializedName("preload")
    val preload: InteractionPreloadVideoWrapper? = null,
    @SerializedName("story_list")
    val storyList: List<InteractionStoryModel>? = null
) : Serializable
