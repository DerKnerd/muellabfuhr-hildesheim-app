@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
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
    val stackView: UIStackView = UIStackView(frame = CGRectZero.readValue()).apply {
        setAxis(this@BaseFlexLayoutBuilder.axis)
        setAlignment(UIStackViewAlignmentFill)
        setDistribution(UIStackViewDistributionFill)
        setTranslatesAutoresizingMaskIntoConstraints(false)
    }

    var spacing: Double
        get() = stackView.spacing
        set(value) {
            stackView.spacing = value
        }

    var alignment: UIStackViewAlignment
        get() = stackView.alignment
        set(value) {
            stackView.alignment = value
        }

    var distribution: UIStackViewDistribution
        get() = stackView.distribution
        set(value) {
            stackView.distribution = value
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
        stackView.layoutMargins = UIEdgeInsetsMake(top, left, bottom, right)
        stackView.layoutMarginsRelativeArrangement = true
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

    fun button(
        title: String? = null,
        buttonType: Long = UIButtonTypeSystem,
        builder: ButtonBuilder.() -> Unit = {}
    ): UIButton {
        val btn = dev.imanuel.abfuhr.uikit.dsl.button(title, buttonType, builder)
        stackView.addArrangedSubview(btn)
        return btn
    }

    fun singleLineTextField(
        placeholder: String? = null,
        builder: TextFieldBuilder.() -> Unit = {}
    ): UITextField {
        val tf = dev.imanuel.abfuhr.uikit.dsl.singleLineTextField(placeholder, builder)
        stackView.addArrangedSubview(tf)
        return tf
    }

    fun multiLineTextField(
        builder: TextFieldBuilder.() -> Unit = {}
    ): UITextView {
        val tf = dev.imanuel.abfuhr.uikit.dsl.multiLineTextField(  builder)
        stackView.addArrangedSubview(tf)
        return tf
    }

    fun textView(
        text: String? = null,
        builder: TextViewBuilder.() -> Unit = {}
    ): UITextView {
        val tv = dev.imanuel.abfuhr.uikit.dsl.textView(text,  builder)
        stackView.addArrangedSubview(tv)
        return tv
    }

    fun label(
        text: String? = null,
        builder: LabelBuilder.() -> Unit = {}
    ): UILabel {
        val lbl = dev.imanuel.abfuhr.uikit.dsl.label(text, builder)
        stackView.addArrangedSubview(lbl)
        return lbl
    }

    fun listView(
        style: UITableViewStyle = UITableViewStyle.UITableViewStylePlain,
        sectionTitlesByFirstLetter: Boolean = false,
        builder: ListItemsBuilder.() -> Unit
    ): UITableView {
        val lv = dev.imanuel.abfuhr.uikit.dsl.listView(style, sectionTitlesByFirstLetter, builder)
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

    fun buildStackView(): UIStackView {
        backgroundColor?.let { stackView.backgroundColor = it }
        cornerRadius?.let {
            stackView.layer.cornerRadius = it
            stackView.layer.masksToBounds = true
        }
        borderWidth?.let { stackView.layer.borderWidth = it }
        borderColor?.let { stackView.layer.borderColor = it.CGColor }

        return stackView
    }

    fun buildScrollView(): UIScrollView {
        val scrollView = UIScrollView(frame = CGRectZero.readValue())
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        backgroundColor?.let { scrollView.backgroundColor = it }
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
