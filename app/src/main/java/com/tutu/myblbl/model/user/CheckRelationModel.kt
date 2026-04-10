package com.tutu.myblbl.model.user

import com.google.gson.annotations.SerializedName

data class CheckRelationModel(
    @SerializedName("attribute")
    val attribute: Int = 0,
    @SerializedName("mid")
    val mid: Long = 0,
    @SerializedName("special")
    val special: Int = 0
) {
    val isMutualFollow: Boolean
        get() = attribute == 6
}
