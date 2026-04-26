package com.tutu.myblbl.core.ui.base

import android.os.Handler
import android.os.Looper
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView

abstract class BaseVideoViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {

    protected val handler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    protected var longPressTriggered = false

    protected open fun showCardMenu() {}

    protected fun startLongPress() {
        cancelLongPress()
        longPressTriggered = false
        longPressRunnable = Runnable {
            longPressTriggered = true
            showCardMenu()
        }
        handler.postDelayed(longPressRunnable!!, ViewConfiguration.getLongPressTimeout().toLong())
    }

    protected fun cancelLongPress() {
        longPressRunnable?.let { handler.removeCallbacks(it) }
        longPressRunnable = null
    }
}
