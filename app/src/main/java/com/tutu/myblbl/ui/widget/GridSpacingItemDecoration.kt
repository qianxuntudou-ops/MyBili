package com.tutu.myblbl.ui.widget

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean = true
) : RecyclerView.ItemDecoration() {
    
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) {
            return
        }
        val gridLayoutManager = parent.layoutManager as? GridLayoutManager
        val spanSizeLookup = gridLayoutManager?.spanSizeLookup
        val resolvedSpanCount = gridLayoutManager?.spanCount ?: spanCount
        val spanSize = spanSizeLookup?.getSpanSize(position) ?: 1
        val column = spanSizeLookup?.getSpanIndex(position, resolvedSpanCount) ?: (position % resolvedSpanCount)
        val row = spanSizeLookup?.getSpanGroupIndex(position, resolvedSpanCount)
            ?: (position / resolvedSpanCount)

        if (position == 0 && spanSize == resolvedSpanCount) {
            outRect.set(0, 0, 0, 0)
            return
        }

        if (spanSize == resolvedSpanCount) {
            outRect.left = spacing
            outRect.right = spacing
            outRect.top = if (includeEdge || row > 0) spacing else 0
            outRect.bottom = spacing
            return
        }
        
        if (includeEdge) {
            outRect.left = spacing - column * spacing / resolvedSpanCount
            outRect.right = (column + 1) * spacing / resolvedSpanCount
            
            if (row == 0) {
                outRect.top = spacing
            }
            outRect.bottom = spacing
        } else {
            outRect.left = column * spacing / resolvedSpanCount
            outRect.right = spacing - (column + 1) * spacing / resolvedSpanCount
            if (row > 0) {
                outRect.top = spacing
            }
        }
    }
}
