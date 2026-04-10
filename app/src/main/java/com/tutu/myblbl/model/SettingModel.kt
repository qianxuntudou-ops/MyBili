package com.tutu.myblbl.model

import java.io.Serializable

data class SettingModel(
    var title: String = "",
    var info: String = ""
) : Serializable
