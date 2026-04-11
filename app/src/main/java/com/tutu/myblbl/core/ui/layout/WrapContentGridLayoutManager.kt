package com.tutu.myblbl.core.ui.layout

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tutu.myblbl.utils.AppLog

open class WrapContentGridLayoutManager(
    context: Context,
    spanCount: Int
) : GridLayoutManager(context, spanCount) {

    override fun supportsPredictiveItemAnimations(): Boolean = false

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (error: IndexOutOfBoundsException) {
            AppLog.e("WrapContentGrid", error.message.orEmpty(), error)
        }
    }
}
