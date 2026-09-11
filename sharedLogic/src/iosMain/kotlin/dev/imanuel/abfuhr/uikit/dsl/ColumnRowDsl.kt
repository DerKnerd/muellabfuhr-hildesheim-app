@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.UIKit.*

@UIKitDsl
enum class LayoutAlignment {
    Fill,
    Start,
    Center,
    End
}

@UIKitDsl
enum class LayoutDistribution {
    Fill,
    FillEqually,
    FillProportionally,
    EqualSpacing
}

private fun LayoutAlignment.toStackViewAlignment(axis: UILayoutConstraintAxis): UIStackViewAlignment {
    return when (this) {
        LayoutAlignment.Fill -> UIStackViewAlignmentFill
        LayoutAlignment.Start -> UIStackViewAlignmentLeading
        LayoutAlignment.Center -> UIStackViewAlignmentCenter
        LayoutAlignment.End -> UIStackViewAlignmentTrailing
    }
}

private fun LayoutDistribution.toStackViewDistribution(): UIStackViewDistribution {
    return when (this) {
        LayoutDistribution.Fill -> UIStackViewDistributionFill
        LayoutDistribution.FillEqually -> UIStackViewDistributionFillEqually
        LayoutDistribution.FillProportionally -> UIStackViewDistributionFillProportionally
        LayoutDistribution.EqualSpacing -> UIStackViewDistributionEqualSpacing
    }
}

@UIKitDsl
abstract class BaseFlexLayoutBuilder(
    val axis: UILayoutConstraintAxis
) {
    val stackView: UIStackView = UIStackView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)).apply {
        setAxis(this@BaseFlexLayoutBuilder.axis)
        setAlignment(UIStackViewAlignmentFill)
        setDistribution(UIStackViewDistributionFill)
        setTranslatesAutoresizingMaskIntoConstraints(false)
    }

    var spacing: Double
        get() = stackView.spacing
        set(value) {
            stackView.setSpacing(value)
        }

    var alignment: UIStackViewAlignment
        get() = stackView.alignment
        set(value) {
            stackView.setAlignment(value)
        }

    var distribution: UIStackViewDistribution
        get() = stackView.distribution
        set(value) {
            stackView.setDistribution(value)
        }

    var backgroundColor: UIColor? = null
    var cornerRadius: Double? = null
    var borderWidth: Double? = null
    var borderColor: UIColor? = null

    fun align(alignment: LayoutAlignment) {
        this.alignment = alignment.toStackViewAlignment(axis)
    }

    fun distribute(distribution: LayoutDistribution) {
        this.distribution = distribution.toStackViewDistribution()
    }

    fun padding(top: Double, left: Double, bottom: Double, right: Double) {
        stackView.setLayoutMargins(UIEdgeInsetsMake(top, left, bottom, right))
        stackView.setLayoutMarginsRelativeArrangement(true)
    }

    fun padding(all: Double) {
        padding(top = all, left = all, bottom = all, right = all)
    }

    fun padding(horizontal: Double, vertical: Double) {
        padding(top = vertical, left = horizontal, bottom = vertical, right = horizontal)
    }

    fun add(view: UIView) {
        stackView.addArrangedSubview(view)
    }

    fun spacer(size: Double? = null): UIView {
        val spacerView = UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))
        spacerView.setTranslatesAutoresizingMaskIntoConstraints(false)
        if (size != null) {
            if (axis == UILayoutConstraintAxisVertical) {
                spacerView.heightAnchor.constraintEqualToConstant(size).setActive(true)
            } else {
                spacerView.widthAnchor.constraintEqualToConstant(size).setActive(true)
            }
        } else {
            spacerView.setContentHuggingPriority(1f, forAxis = axis)
            spacerView.setContentCompressionResistancePriority(1f, forAxis = axis)
        }
        stackView.addArrangedSubview(spacerView)
        return spacerView
    }

    fun divider(color: UIColor = UIColor.lightGrayColor, thickness: Double = 1.0): UIView {
        val div = UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))
        div.setBackgroundColor(color)
        div.setTranslatesAutoresizingMaskIntoConstraints(false)
        if (axis == UILayoutConstraintAxisVertical) {
            div.heightAnchor.constraintEqualToConstant(thickness).setActive(true)
        } else {
            div.widthAnchor.constraintEqualToConstant(thickness).setActive(true)
        }
        stackView.addArrangedSubview(div)
        return div
    }

    fun button(
        title: String? = null,
        buttonType: Long = UIButtonTypeSystem,
        builder: ButtonBuilder.() -> Unit = {}
    ): UIButton {
        val btn = dev.imanuel.abfuhr.uikit.dsl.button(title, buttonType, builder)
        stackView.addArrangedSubview(btn)
        return btn
    }

    fun iconButton(
        systemName: String? = null,
        builder: IconButtonBuilder.() -> Unit = {}
    ): UIButton {
        val btn = dev.imanuel.abfuhr.uikit.dsl.iconButton(systemName, builder)
        stackView.addArrangedSubview(btn)
        return btn
    }

    fun singleLineTextField(
        placeholder: String? = null,
        text: String? = null,
        builder: TextFieldBuilder.() -> Unit = {}
    ): UITextField {
        val tf = dev.imanuel.abfuhr.uikit.dsl.singleLineTextField(placeholder, text, builder)
        stackView.addArrangedSubview(tf)
        return tf
    }

    fun multiLineTextField(
        placeholder: String? = null,
        text: String? = null,
        builder: TextFieldBuilder.() -> Unit = {}
    ): UITextView {
        val tf = dev.imanuel.abfuhr.uikit.dsl.multiLineTextField(placeholder, text, builder)
        stackView.addArrangedSubview(tf)
        return tf
    }

    fun searchField(
        placeholder: String? = "Search",
        text: String? = null,
        builder: SearchFieldBuilder.() -> Unit = {}
    ): UISearchTextField {
        val sf = dev.imanuel.abfuhr.uikit.dsl.searchField(placeholder, text, builder)
        stackView.addArrangedSubview(sf)
        return sf
    }

    fun textView(
        text: String? = null,
        placeholder: String? = null,
        builder: TextViewBuilder.() -> Unit = {}
    ): UITextView {
        val tv = dev.imanuel.abfuhr.uikit.dsl.textView(text, placeholder, builder)
        stackView.addArrangedSubview(tv)
        return tv
    }

    fun activityIndicator(
        style: UIActivityIndicatorViewStyle = UIActivityIndicatorViewStyleMedium,
        color: UIColor? = null,
        isAnimating: Boolean = true,
        builder: ActivityIndicatorBuilder.() -> Unit = {}
    ): UIActivityIndicatorView {
        val cpi = dev.imanuel.abfuhr.uikit.dsl.activityIndicator(style, color, isAnimating, builder)
        stackView.addArrangedSubview(cpi)
        return cpi
    }

    fun label(
        text: String? = null,
        builder: LabelBuilder.() -> Unit = {}
    ): UILabel {
        val lbl = dev.imanuel.abfuhr.uikit.dsl.label(text, builder)
        stackView.addArrangedSubview(lbl)
        return lbl
    }

    fun glassCard(
        blurStyle: UIBlurEffectStyle = UIBlurEffectStyle.UIBlurEffectStyleSystemUltraThinMaterial,
        cornerRadius: Double = 16.0,
        builder: LiquidGlassCardBuilder.() -> Unit = {}
    ): UIView {
        val card = dev.imanuel.abfuhr.uikit.dsl.glassCard(blurStyle, cornerRadius, builder)
        stackView.addArrangedSubview(card)
        return card
    }

    fun listView(
        style: UITableViewStyle = UITableViewStyle.UITableViewStylePlain,
        builder: ListItemsBuilder.() -> Unit
    ): UITableView {
        val lv = dev.imanuel.abfuhr.uikit.dsl.listView(style, builder)
        stackView.addArrangedSubview(lv)
        return lv
    }

    fun column(
        spacing: Double = 8.0,
        builder: ColumnBuilder.() -> Unit
    ): UIStackView {
        val col = dev.imanuel.abfuhr.uikit.dsl.column(spacing = spacing, builder = builder)
        stackView.addArrangedSubview(col)
        return col
    }

    fun scrollableColumn(
        spacing: Double = 8.0,
        builder: ColumnBuilder.() -> Unit
    ): UIScrollView {
        val col = dev.imanuel.abfuhr.uikit.dsl.scrollableColumn(spacing = spacing, builder = builder)
        stackView.addArrangedSubview(col)
        return col
    }

    fun scrollableRow(
        spacing: Double = 8.0,
        builder: RowBuilder.() -> Unit
    ): UIScrollView {
        val r = dev.imanuel.abfuhr.uikit.dsl.scrollableRow(spacing = spacing, builder = builder)
        stackView.addArrangedSubview(r)
        return r
    }

    fun staggeredGrid(
        columns: Int = 2,
        spacing: Double = 8.0,
        builder: StaggeredGridBuilder.() -> Unit
    ): UIView {
        val grid = dev.imanuel.abfuhr.uikit.dsl.staggeredGrid(columns = columns, spacing = spacing, builder = builder)
        stackView.addArrangedSubview(grid)
        return grid
    }

    fun buildStackView(): UIStackView {
        backgroundColor?.let { stackView.setBackgroundColor(it) }
        cornerRadius?.let {
            stackView.layer.cornerRadius = it
            stackView.layer.masksToBounds = true
        }
        borderWidth?.let { stackView.layer.borderWidth = it }
        borderColor?.let { stackView.layer.borderColor = it.CGColor }

        return stackView
    }

    fun buildScrollView(): UIScrollView {
        val scrollView = UIScrollView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))
        scrollView.setTranslatesAutoresizingMaskIntoConstraints(false)
        backgroundColor?.let { scrollView.setBackgroundColor(it) }
        scrollView.addSubview(buildStackView())

        if (axis == UILayoutConstraintAxisVertical) {
            NSLayoutConstraint.activateConstraints(
                listOf(
                    stackView.topAnchor.constraintEqualToAnchor(scrollView.topAnchor),
                    stackView.leadingAnchor.constraintEqualToAnchor(scrollView.leadingAnchor),
                    stackView.trailingAnchor.constraintEqualToAnchor(scrollView.trailingAnchor),
                    stackView.bottomAnchor.constraintEqualToAnchor(scrollView.bottomAnchor),
                    stackView.widthAnchor.constraintEqualToAnchor(scrollView.widthAnchor)
                )
            )
        } else {
            NSLayoutConstraint.activateConstraints(
                listOf(
                    stackView.topAnchor.constraintEqualToAnchor(scrollView.topAnchor),
                    stackView.leadingAnchor.constraintEqualToAnchor(scrollView.leadingAnchor),
                    stackView.trailingAnchor.constraintEqualToAnchor(scrollView.trailingAnchor),
                    stackView.bottomAnchor.constraintEqualToAnchor(scrollView.bottomAnchor),
                    stackView.heightAnchor.constraintEqualToAnchor(scrollView.heightAnchor)
                )
            )
        }
        return scrollView
    }
}

@UIKitDsl
class ColumnBuilder : BaseFlexLayoutBuilder(UILayoutConstraintAxisVertical)

@UIKitDsl
class RowBuilder : BaseFlexLayoutBuilder(UILayoutConstraintAxisHorizontal)

inline fun column(
    spacing: Double = 8.0,
    alignment: UIStackViewAlignment = UIStackViewAlignmentFill,
    distribution: UIStackViewDistribution = UIStackViewDistributionFill,
    builder: ColumnBuilder.() -> Unit
): UIStackView {
    val b = ColumnBuilder()
    b.spacing = spacing
    b.alignment = alignment
    b.distribution = distribution
    b.builder()

    return b.buildStackView()
}

inline fun scrollableColumn(
    spacing: Double = 8.0,
    alignment: UIStackViewAlignment = UIStackViewAlignmentFill,
    distribution: UIStackViewDistribution = UIStackViewDistributionFill,
    builder: ColumnBuilder.() -> Unit
): UIScrollView {
    val b = ColumnBuilder()
    b.spacing = spacing
    b.alignment = alignment
    b.distribution = distribution
    b.builder()

    return b.buildScrollView()
}

inline fun row(
    spacing: Double = 8.0,
    alignment: UIStackViewAlignment = UIStackViewAlignmentFill,
    distribution: UIStackViewDistribution = UIStackViewDistributionFill,
    builder: RowBuilder.() -> Unit
): UIStackView {
    val b = RowBuilder()
    b.spacing = spacing
    b.alignment = alignment
    b.distribution = distribution
    b.builder()

    return b.buildStackView()
}

inline fun scrollableRow(
    spacing: Double = 8.0,
    alignment: UIStackViewAlignment = UIStackViewAlignmentFill,
    distribution: UIStackViewDistribution = UIStackViewDistributionFill,
    builder: RowBuilder.() -> Unit
): UIScrollView {
    val b = RowBuilder()
    b.spacing = spacing
    b.alignment = alignment
    b.distribution = distribution
    b.builder()

    return b.buildScrollView()
}
