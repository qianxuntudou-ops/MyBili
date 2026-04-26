package com.tutu.myblbl.core.ui.layout

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tutu.myblbl.core.common.log.AppLog

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

    override fun onFocusSearchFailed(
        focused: View,
        direction: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): View? {
        val result = super.onFocusSearchFailed(focused, direction, recycler, state)
        return if (
            result == null &&
            (direction == View.FOCUS_DOWN || direction == View.FOCUS_UP ||
                direction == View.FOCUS_LEFT || direction == View.FOCUS_RIGHT)
        ) {
            focused
        } else {
            result
        }
    }
}
