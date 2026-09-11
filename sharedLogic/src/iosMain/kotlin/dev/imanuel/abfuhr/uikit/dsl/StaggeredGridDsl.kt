@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.*

class StaggeredGridView : UIScrollView {

    var columnsCount: Int = 2
        set(value) {
            field = maxOf(1, value)
            setNeedsLayout()
        }

    var horizontalSpacing: Double = 8.0
        set(value) {
            field = value
            setNeedsLayout()
        }

    var verticalSpacing: Double = 8.0
        set(value) {
            field = value
            setNeedsLayout()
        }

    var paddingTop: Double = 0.0
        set(value) {
            field = value
            setNeedsLayout()
        }

    var paddingLeft: Double = 0.0
        set(value) {
            field = value
            setNeedsLayout()
        }

    var paddingBottom: Double = 0.0
        set(value) {
            field = value
            setNeedsLayout()
        }

    var paddingRight: Double = 0.0
        set(value) {
            field = value
            setNeedsLayout()
        }

    val gridItems = mutableListOf<UIView>()

    constructor() : super(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))

    fun addGridItem(view: UIView) {
        gridItems.add(view)
        addSubview(view)
        setNeedsLayout()
    }

    fun removeGridItem(view: UIView) {
        gridItems.remove(view)
        view.removeFromSuperview()
        setNeedsLayout()
    }

    fun clearGridItems() {
        for (item in gridItems) {
            item.removeFromSuperview()
        }
        gridItems.clear()
        setNeedsLayout()
    }

    fun calculateContentHeight(forWidth: Double): Double {
        if (forWidth <= 0.0 || columnsCount <= 0 || gridItems.isEmpty()) {
            return paddingTop + paddingBottom
        }

        val effectiveCols = maxOf(1, columnsCount)
        val totalHorizontalSpacing = (effectiveCols - 1) * horizontalSpacing
        val availableWidth = forWidth - paddingLeft - paddingRight - totalHorizontalSpacing
        val columnWidth = if (availableWidth > 0.0) availableWidth / effectiveCols else 0.0
        val columnHeights = DoubleArray(effectiveCols) { paddingTop }

        for (item in gridItems) {
            val fitting = item.sizeThatFits(CGSizeMake(columnWidth, 100000.0))
            val measuredHeight = fitting.useContents { height }
            val explicitHeight = item.bounds.useContents { size.height }
            val itemHeight = when {
                measuredHeight > 0.0 -> measuredHeight
                explicitHeight > 0.0 -> explicitHeight
                else -> 60.0
            }

            var minCol = 0
            var minHeight = columnHeights[0]
            for (i in 1 until effectiveCols) {
                if (columnHeights[i] < minHeight) {
                    minHeight = columnHeights[i]
                    minCol = i
                }
            }
            columnHeights[minCol] += itemHeight + verticalSpacing
        }

        val maxHeight = (columnHeights.maxOrNull() ?: paddingTop) + paddingBottom
        return if (gridItems.isNotEmpty() && maxHeight > paddingBottom + verticalSpacing) {
            maxHeight - verticalSpacing
        } else {
            maxHeight
        }
    }

    override fun layoutSubviews() {
        super.layoutSubviews()

        val currentWidth = bounds.useContents { size.width }
        if (currentWidth <= 0.0 || columnsCount <= 0 || gridItems.isEmpty()) return

        val effectiveCols = maxOf(1, columnsCount)
        val totalHorizontalSpacing = (effectiveCols - 1) * horizontalSpacing
        val availableWidth = currentWidth - paddingLeft - paddingRight - totalHorizontalSpacing
        val columnWidth = if (availableWidth > 0.0) availableWidth / effectiveCols else 0.0

        val columnHeights = DoubleArray(effectiveCols) { paddingTop }

        for (item in gridItems) {
            if (item.superview != this) {
                addSubview(item)
            }

            var minCol = 0
            var minHeight = columnHeights[0]
            for (i in 1 until effectiveCols) {
                if (columnHeights[i] < minHeight) {
                    minHeight = columnHeights[i]
                    minCol = i
                }
            }

            val x = paddingLeft + minCol * (columnWidth + horizontalSpacing)
            val y = columnHeights[minCol]

            val fitting = item.sizeThatFits(CGSizeMake(columnWidth, 100000.0))
            val measuredHeight = fitting.useContents { height }
            val explicitHeight = item.bounds.useContents { size.height }
            val itemHeight = when {
                measuredHeight > 0.0 -> measuredHeight
                explicitHeight > 0.0 -> explicitHeight
                else -> 60.0
            }

            item.setFrame(CGRectMake(x, y, columnWidth, itemHeight))
            columnHeights[minCol] += itemHeight + verticalSpacing
        }

        val maxHeight = (columnHeights.maxOrNull() ?: paddingTop) + paddingBottom
        val contentHeight = if (gridItems.isNotEmpty() && maxHeight > paddingBottom + verticalSpacing) {
            maxHeight - verticalSpacing
        } else {
            maxHeight
        }

        setContentSize(CGSizeMake(currentWidth, contentHeight))
    }

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        val width = size.useContents { width }
        val height = calculateContentHeight(width)
        return CGSizeMake(width, height)
    }
}

@UIKitDsl
class StaggeredGridBuilder(
    var columns: Int = 2
) {
    val gridView: StaggeredGridView = StaggeredGridView().apply {
        columnsCount = this@StaggeredGridBuilder.columns
    }

    var spacing: Double = 8.0
        set(value) {
            field = value
            horizontalSpacing = value
            verticalSpacing = value
        }

    var horizontalSpacing: Double
        get() = gridView.horizontalSpacing
        set(value) {
            gridView.horizontalSpacing = value
        }

    var verticalSpacing: Double
        get() = gridView.verticalSpacing
        set(value) {
            gridView.verticalSpacing = value
        }

    var backgroundColor: UIColor? = null
    var cornerRadius: Double? = null

    private val pendingItems = mutableListOf<UIView>()

    fun padding(top: Double, left: Double, bottom: Double, right: Double) {
        gridView.paddingTop = top
        gridView.paddingLeft = left
        gridView.paddingBottom = bottom
        gridView.paddingRight = right
    }

    fun padding(all: Double) {
        padding(top = all, left = all, bottom = all, right = all)
    }

    fun padding(horizontal: Double, vertical: Double) {
        padding(top = vertical, left = horizontal, bottom = vertical, right = horizontal)
    }

    fun add(view: UIView) {
        pendingItems.add(view)
    }

    fun item(view: UIView) {
        pendingItems.add(view)
    }

    fun customItem(viewProvider: () -> UIView) {
        pendingItems.add(viewProvider())
    }

    fun <T> items(
        itemList: Iterable<T>,
        itemContent: StaggeredGridBuilder.(T) -> Unit
    ) {
        for (elem in itemList) {
            this.itemContent(elem)
        }
    }

    fun glassCard(
        blurStyle: UIBlurEffectStyle = UIBlurEffectStyle.UIBlurEffectStyleSystemUltraThinMaterial,
        cornerRadius: Double = 16.0,
        builder: LiquidGlassCardBuilder.() -> Unit = {}
    ): UIView {
        val card = dev.imanuel.abfuhr.uikit.dsl.glassCard(blurStyle, cornerRadius, builder)
        pendingItems.add(card)
        return card
    }

    fun button(
        title: String? = null,
        buttonType: Long = UIButtonTypeSystem,
        builder: ButtonBuilder.() -> Unit = {}
    ): UIButton {
        val btn = dev.imanuel.abfuhr.uikit.dsl.button(title, buttonType, builder)
        pendingItems.add(btn)
        return btn
    }

    fun label(
        text: String? = null,
        builder: LabelBuilder.() -> Unit = {}
    ): UILabel {
        val lbl = dev.imanuel.abfuhr.uikit.dsl.label(text, builder)
        pendingItems.add(lbl)
        return lbl
    }

    fun textField(
        placeholder: String? = null,
        text: String? = null,
        builder: TextFieldBuilder.() -> Unit = {}
    ): UITextField {
        val tf = dev.imanuel.abfuhr.uikit.dsl.singleLineTextField(placeholder, text, builder)
        pendingItems.add(tf)
        return tf
    }

    fun column(
        spacing: Double = 8.0,
        builder: ColumnBuilder.() -> Unit
    ): UIView {
        val col = dev.imanuel.abfuhr.uikit.dsl.column(spacing = spacing, builder = builder)
        pendingItems.add(col)
        return col
    }

    fun row(
        spacing: Double = 8.0,
        builder: RowBuilder.() -> Unit
    ): UIView {
        val r = dev.imanuel.abfuhr.uikit.dsl.row(spacing = spacing, builder = builder)
        pendingItems.add(r)
        return r
    }

    fun build(): StaggeredGridView {
        gridView.columnsCount = columns
        backgroundColor?.let { gridView.setBackgroundColor(it) }
        cornerRadius?.let {
            gridView.layer.cornerRadius = it
            gridView.layer.masksToBounds = true
        }

        for (item in pendingItems) {
            gridView.addGridItem(item)
        }

        return gridView
    }
}

inline fun staggeredGrid(
    columns: Int = 2,
    spacing: Double = 8.0,
    builder: StaggeredGridBuilder.() -> Unit
): StaggeredGridView {
    val b = StaggeredGridBuilder(columns = columns)
    b.spacing = spacing
    b.builder()
    return b.build()
}
