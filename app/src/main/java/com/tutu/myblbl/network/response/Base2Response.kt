package com.tutu.myblbl.network.response

import com.google.gson.annotations.SerializedName

data class Base2Response<T>(
    @SerializedName("code")
    val code: Int = 0,
    @SerializedName("message")
    val message: String = "",
    @SerializedName("result")
    val result: T? = null
) {
    val isSuccess: Boolean
        get() = code == 0

    val errorMessage: String
        get() = message
}
