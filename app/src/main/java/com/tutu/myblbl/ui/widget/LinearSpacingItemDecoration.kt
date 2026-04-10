package com.tutu.myblbl.ui.widget

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class LinearSpacingItemDecoration(
    private val spacing: Int,
    private val includeTop: Boolean = false,
    private val includeBottom: Boolean = false,
    private val orientation: Int = VERTICAL
) : RecyclerView.ItemDecoration() {
    
    companion object {
        const val VERTICAL = 0
        const val HORIZONTAL = 1
    }
    
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val itemCount = state.itemCount
        
        if (orientation == VERTICAL) {
            outRect.left = spacing
            outRect.right = spacing
            
            if (includeTop && position == 0) {
                outRect.top = spacing
            }
            
            if (includeBottom && position == itemCount - 1) {
                outRect.bottom = spacing
            } else if (position < itemCount - 1) {
                outRect.bottom = spacing
            }
        } else {
            outRect.top = spacing
            outRect.bottom = spacing
            
            if (includeTop && position == 0) {
                outRect.left = spacing
            }
            
            if (includeBottom && position == itemCount - 1) {
                outRect.right = spacing
            } else if (position < itemCount - 1) {
                outRect.right = spacing
            }
        }
    }
}
