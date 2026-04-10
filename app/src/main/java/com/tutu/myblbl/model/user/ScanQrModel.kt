package com.tutu.myblbl.model.user

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ScanQrModel(
    @SerializedName("url")
    var url: String = "",
    @SerializedName("qrcode_key")
    var qrcodeKey: String = ""
) : Serializable
