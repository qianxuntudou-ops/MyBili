package com.kuaishou.akdanmaku.ext

import android.util.Log
import com.tutu.myblbl.BuildConfig

object AkLog {

  fun d(tag: String, message: String) {
    if (!BuildConfig.DEBUG) return
    Log.d(tag, message)
  }

  fun e(tag: String, message: String, throwable: Throwable? = null) {
    if (!BuildConfig.DEBUG) return
    Log.e(tag, message, throwable)
  }

  fun i(tag: String, message: String) {
    if (!BuildConfig.DEBUG) return
    Log.i(tag, message)
  }

  fun v(tag: String, message: String) {
    if (!BuildConfig.DEBUG) return
    Log.v(tag, message)
  }

  fun w(tag: String, message: String, throwable: Throwable? = null) {
    if (!BuildConfig.DEBUG) return
    if (throwable == null) {
      Log.w(tag, message)
    } else {
      Log.w(tag, message, throwable)
    }
  }
}
