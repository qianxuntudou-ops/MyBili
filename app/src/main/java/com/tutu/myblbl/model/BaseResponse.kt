package com.tutu.myblbl.model

import com.google.gson.annotations.SerializedName

data class BaseResponse<T>(
    @SerializedName("code")
    val code: Int = 0,
    
    @SerializedName("message")
    val message: String = "",
    
    @SerializedName("msg")
    val msg: String = "",
    
    @SerializedName("data")
    val data: T? = null
) {
    val isSuccess: Boolean
        get() = code == 0
    
    val errorMessage: String
        get() = message.ifEmpty { msg }
}
