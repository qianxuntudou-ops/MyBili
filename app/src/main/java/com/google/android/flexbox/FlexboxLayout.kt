package com.google.android.flexbox

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.tutu.myblbl.R
import kotlin.math.max

/**
 * Local FlexboxLayout implementation aligned with the reference project's search-chip usage.
 * It supports child ordering, wrapping, wrap-reverse, and basic cross-axis alignment.
 */
class FlexboxLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private var alignItems: Int = ALIGN_ITEMS_STRETCH
    private var flexWrap: Int = FLEX_WRAP_NOWRAP
    private val flexLines = mutableListOf<FlexLine>()

    init {
        context.obtainStyledAttributes(attrs, R.styleable.FlexboxLayout, defStyleAttr, 0).apply {
            try {
                alignItems = getInt(R.styleable.FlexboxLayout_alignItems, ALIGN_ITEMS_STRETCH)
                flexWrap = getInt(R.styleable.FlexboxLayout_flexWrap, FLEX_WRAP_NOWRAP)
            } finally {
                recycle()
            }
        }
    }

    class LayoutParams : MarginLayoutParams {
        var order: Int = ORDER_DEFAULT
        var alignSelf: Int = ALIGN_SELF_AUTO

        constructor(c: Context, attrs: AttributeSet?) : super(c, attrs) {
            c.obtainStyledAttributes(attrs, R.styleable.FlexboxLayout_Layout).apply {
                try {
                    order = getInt(R.styleable.FlexboxLayout_Layout_layout_order, ORDER_DEFAULT)
                    alignSelf = getInt(
                        R.styleable.FlexboxLayout_Layout_layout_alignSelf,
                        ALIGN_SELF_AUTO
                    )
                } finally {
                    recycle()
                }
            }
        }

        constructor(width: Int, height: Int) : super(width, height)

        constructor(source: ViewGroup.LayoutParams) : super(source)

        constructor(source: MarginLayoutParams) : super(source)
    }

    private data class OrderedChild(
        val child: View,
        val params: LayoutParams,
        val index: Int
    )

    private data class LineItem(
        val child: View,
        val params: LayoutParams,
        var measuredWidth: Int,
        var measuredHeight: Int
    ) {
        val totalWidth: Int
            get() = measuredWidth + params.leftMargin + params.rightMargin

        val totalHeight: Int
            get() = measuredHeight + params.topMargin + params.bottomMargin
    }

    private data class FlexLine(
        val items: MutableList<LineItem> = mutableListOf(),
        var width: Int = 0,
        var height: Int = 0
    )

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): ViewGroup.LayoutParams {
        return when (p) {
            is LayoutParams -> LayoutParams(p)
            is MarginLayoutParams -> LayoutParams(p)
            null -> generateDefaultLayoutParams()
            else -> LayoutParams(p)
        }
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is LayoutParams
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val maxWidth = if (widthMode == MeasureSpec.UNSPECIFIED) Int.MAX_VALUE else widthSize

        flexLines.clear()
        var currentLine = FlexLine(width = paddingLeft + paddingRight)
        var maxLineWidth = paddingLeft + paddingRight

        orderedChildren().forEach { orderedChild ->
            val child = orderedChild.child
            if (child.visibility == View.GONE) return@forEach

            val params = orderedChild.params
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
            val lineItem = LineItem(child, params, child.measuredWidth, child.measuredHeight)

            val shouldWrap = flexWrap != FLEX_WRAP_NOWRAP &&
                currentLine.items.isNotEmpty() &&
                currentLine.width + lineItem.totalWidth > maxWidth

            if (shouldWrap) {
                finalizeLine(currentLine, widthMeasureSpec)
                flexLines += currentLine
                maxLineWidth = max(maxLineWidth, currentLine.width)
                currentLine = FlexLine(width = paddingLeft + paddingRight)
            }

            currentLine.items += lineItem
            currentLine.width += lineItem.totalWidth
            currentLine.height = max(currentLine.height, lineItem.totalHeight)
        }

        if (currentLine.items.isNotEmpty() || flexLines.isEmpty()) {
            finalizeLine(currentLine, widthMeasureSpec)
            flexLines += currentLine
            maxLineWidth = max(maxLineWidth, currentLine.width)
        }

        val contentHeight = flexLines.sumOf { it.height }
        val measuredWidth = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> minOf(maxLineWidth, widthSize)
            else -> maxLineWidth
        }
        val measuredHeight = resolveSize(
            paddingTop + paddingBottom + contentHeight,
            heightMeasureSpec
        )
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    private fun finalizeLine(line: FlexLine, parentWidthMeasureSpec: Int) {
        line.items.forEach { item ->
            if (resolveAlignSelf(item.params) == ALIGN_ITEMS_STRETCH &&
                item.params.height == ViewGroup.LayoutParams.WRAP_CONTENT
            ) {
                val stretchedHeight =
                    (line.height - item.params.topMargin - item.params.bottomMargin).coerceAtLeast(0)
                val childWidthSpec = getChildMeasureSpec(
                    parentWidthMeasureSpec,
                    paddingLeft + paddingRight + item.params.leftMargin + item.params.rightMargin,
                    item.params.width
                )
                val childHeightSpec =
                    MeasureSpec.makeMeasureSpec(stretchedHeight, MeasureSpec.EXACTLY)
                item.child.measure(childWidthSpec, childHeightSpec)
                item.measuredWidth = item.child.measuredWidth
                item.measuredHeight = item.child.measuredHeight
            }
        }
        line.height = line.items.maxOfOrNull { it.totalHeight } ?: 0
        line.width = paddingLeft + paddingRight + line.items.sumOf { it.totalWidth }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (flexLines.isEmpty()) {
            return
        }

        if (flexWrap == FLEX_WRAP_WRAP_REVERSE) {
            var currentBottom = bottom - top - paddingBottom
            flexLines.forEach { line ->
                val lineTop = currentBottom - line.height
                layoutLine(line, lineTop)
                currentBottom = lineTop
            }
        } else {
            var currentTop = paddingTop
            flexLines.forEach { line ->
                layoutLine(line, currentTop)
                currentTop += line.height
            }
        }
    }

    private fun layoutLine(line: FlexLine, lineTop: Int) {
        var currentLeft = paddingLeft
        line.items.forEach { item ->
            val childLeft = currentLeft + item.params.leftMargin
            val topOffset = when (resolveAlignSelf(item.params)) {
                ALIGN_ITEMS_FLEX_END -> line.height - item.measuredHeight - item.params.bottomMargin
                ALIGN_ITEMS_CENTER -> ((line.height - item.totalHeight) / 2) + item.params.topMargin
                else -> item.params.topMargin
            }
            val childTop = lineTop + topOffset
            item.child.layout(
                childLeft,
                childTop,
                childLeft + item.measuredWidth,
                childTop + item.measuredHeight
            )
            currentLeft = childLeft + item.measuredWidth + item.params.rightMargin
        }
    }

    private fun orderedChildren(): List<OrderedChild> {
        return (0 until childCount)
            .map { index ->
                val child = getChildAt(index)
                val params = when (val source = child.layoutParams) {
                    is LayoutParams -> source
                    is MarginLayoutParams -> LayoutParams(source)
                    null -> generateDefaultLayoutParams()
                    else -> LayoutParams(source)
                }
                OrderedChild(child = child, params = params, index = index)
            }
            .sortedWith(compareBy<OrderedChild> { it.params.order }.thenBy { it.index })
    }

    private fun resolveAlignSelf(params: LayoutParams): Int {
        return if (params.alignSelf != ALIGN_SELF_AUTO) params.alignSelf else alignItems
    }

    companion object {
        private const val FLEX_WRAP_NOWRAP = 0
        private const val FLEX_WRAP_WRAP_REVERSE = 2

        private const val ALIGN_ITEMS_FLEX_END = 1
        private const val ALIGN_ITEMS_CENTER = 2
        private const val ALIGN_ITEMS_STRETCH = 4

        private const val ALIGN_SELF_AUTO = -1
        private const val ORDER_DEFAULT = 1
    }
}
