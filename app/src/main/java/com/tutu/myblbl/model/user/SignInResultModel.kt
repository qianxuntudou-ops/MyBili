package com.tutu.myblbl.model.user

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class SignInResultModel(
    @SerializedName("code")
    var code: Int = 0,
    @SerializedName("url")
    var url: String = "",
    @SerializedName("refresh_token")
    var refreshToken: String = "",
    @SerializedName("timestamp")
    var timestamp: Long = 0,
    @SerializedName("message")
    var message: String = ""
) : Serializable {
    fun isSuccess(): Boolean = code == 0
}
