package com.tutu.myblbl.core.ui.system

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity

object ScreenUtils {

    private fun findActivity(context: Context): AppCompatActivity {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is AppCompatActivity) return ctx
            ctx = ctx.baseContext
        }
        throw IllegalStateException("Cannot find Activity from context")
    }

    fun getScreenWidth(context: Context): Int {
        val activity = findActivity(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.windowManager.currentWindowMetrics.bounds.width()
        } else {
            @Suppress("DEPRECATION")
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.widthPixels
        }
    }

    fun getScreenHeight(context: Context): Int {
        val activity = findActivity(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.windowManager.currentWindowMetrics.bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.heightPixels
        }
    }

    fun getScreenDensity(context: Context): Float {
        return context.resources.displayMetrics.density
    }

    fun pxToDp(context: Context, px: Float): Int {
        return (px / getScreenDensity(context) + 0.5f).toInt()
    }
}
