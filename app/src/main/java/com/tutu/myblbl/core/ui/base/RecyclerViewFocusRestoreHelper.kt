package com.tutu.myblbl.core.ui.base

import androidx.recyclerview.widget.RecyclerView

object RecyclerViewFocusRestoreHelper {

    data class RequestResult(
        val handled: Boolean,
        val deferred: Boolean
    )

    fun requestFocusAtPosition(
        recyclerView: RecyclerView,
        position: Int,
        scrollIfMissing: Boolean = true,
        focusRequester: (RecyclerView.ViewHolder) -> Boolean = { holder ->
            holder.itemView.requestFocus()
        }
    ): RequestResult {
        if (position == RecyclerView.NO_POSITION) {
            return RequestResult(handled = false, deferred = false)
        }

        val holder = recyclerView.findViewHolderForAdapterPosition(position)
        if (holder != null && focusRequester(holder)) {
            return RequestResult(handled = true, deferred = false)
        }

        if (!scrollIfMissing) {
            return RequestResult(handled = false, deferred = false)
        }

        recyclerView.scrollToPosition(position)
        recyclerView.post {
            val deferredHolder = recyclerView.findViewHolderForAdapterPosition(position) ?: return@post
            focusRequester(deferredHolder)
        }
        return RequestResult(handled = false, deferred = true)
    }
}
