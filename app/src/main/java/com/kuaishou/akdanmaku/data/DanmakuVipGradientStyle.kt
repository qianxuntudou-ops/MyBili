package com.kuaishou.akdanmaku.data

import java.io.Serializable

data class DanmakuVipGradientStyle(
  val fillTextureUrl: String = "",
  val strokeTextureUrl: String = ""
) : Serializable {

  val hasTexture: Boolean
    get() = fillTextureUrl.isNotBlank() || strokeTextureUrl.isNotBlank()

  companion object {
    val NONE = DanmakuVipGradientStyle()
  }
}
