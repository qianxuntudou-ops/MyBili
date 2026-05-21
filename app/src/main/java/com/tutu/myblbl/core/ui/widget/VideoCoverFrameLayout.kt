package com.tutu.myblbl.core.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

class VideoCoverFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        if (width > 0) {
            val height = width * 9 / 16
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            )
            return
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
