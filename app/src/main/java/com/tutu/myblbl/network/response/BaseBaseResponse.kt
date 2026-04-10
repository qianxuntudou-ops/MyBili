package com.tutu.myblbl.network.response

import com.google.gson.annotations.SerializedName

data class BaseBaseResponse(
    @SerializedName("code")
    val code: Int = 0,
    @SerializedName("message")
    val message: String = "",
    @SerializedName("msg")
    val msg: String = ""
) {
    val isSuccess: Boolean
        get() = code == 0

    val errorMessage: String
        get() = message.ifEmpty { msg }
}
