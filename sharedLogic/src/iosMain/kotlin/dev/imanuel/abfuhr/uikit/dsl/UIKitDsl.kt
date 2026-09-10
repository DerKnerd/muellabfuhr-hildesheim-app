@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.UIKit.*

@UIKitDsl
class LabelBuilder {
    val label: UILabel = UILabel()

    var text: String? = null
    var textColor: UIColor? = null
    var font: UIFont? = null
    var textAlignment: NSTextAlignment = NSTextAlignmentNatural
    var numberOfLines: Long = 1L
    var lineBreakMode: NSLineBreakMode = NSLineBreakByTruncatingTail
    var backgroundColor: UIColor? = null

    fun build(): UILabel {
        text?.let { label.setText(it) }
        textColor?.let { label.setTextColor(it) }
        font?.let { label.setFont(it) }
        label.setTextAlignment(textAlignment)
        label.setNumberOfLines(numberOfLines)
        label.setLineBreakMode(lineBreakMode)
        backgroundColor?.let { label.setBackgroundColor(it) }
        return label
    }
}

inline fun label(
    text: String? = null,
    builder: LabelBuilder.() -> Unit = {}
): UILabel {
    val b = LabelBuilder()
    if (text != null) b.text = text
    b.builder()
    return b.build()
}

@UIKitDsl
class StackViewBuilder(
    val axis: UILayoutConstraintAxis
) {
    val stackView: UIStackView = UIStackView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)).apply {
        setAxis(this@StackViewBuilder.axis)
        setAlignment(UIStackViewAlignmentFill)
        setDistribution(UIStackViewDistributionFill)
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

    fun textField(
        placeholder: String? = null,
        text: String? = null,
        builder: TextFieldBuilder.() -> Unit = {}
    ): UITextField {
        val tf = dev.imanuel.abfuhr.uikit.dsl.textField(placeholder, text, builder)
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

    fun pageShell(
        builder: PageShellBuilder.() -> Unit
    ): AdaptivePageShellView {
        val shell = dev.imanuel.abfuhr.uikit.dsl.pageShell(builder)
        stackView.addArrangedSubview(shell)
        return shell
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

    fun liquidGlassCard(
        blurStyle: UIBlurEffectStyle = UIBlurEffectStyle.UIBlurEffectStyleSystemUltraThinMaterial,
        cornerRadius: Double = 16.0,
        builder: LiquidGlassCardBuilder.() -> Unit = {}
    ): UIView {
        val card = dev.imanuel.abfuhr.uikit.dsl.glassCard(blurStyle, cornerRadius, builder)
        stackView.addArrangedSubview(card)
        return card
    }

    fun glassCard(
        blurStyle: UIBlurEffectStyle = UIBlurEffectStyle.UIBlurEffectStyleSystemUltraThinMaterial,
        cornerRadius: Double = 16.0,
        builder: LiquidGlassCardBuilder.() -> Unit = {}
    ): UIView = liquidGlassCard(blurStyle, cornerRadius, builder)

    fun listView(
        style: UITableViewStyle = UITableViewStyle.UITableViewStylePlain,
        builder: ListItemsBuilder.() -> Unit
    ): UITableView {
        val lv = dev.imanuel.abfuhr.uikit.dsl.listView(style, builder)
        stackView.addArrangedSubview(lv)
        return lv
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

    fun vStack(
        spacing: Double = 8.0,
        builder: StackViewBuilder.() -> Unit
    ): UIStackView {
        val childStack = dev.imanuel.abfuhr.uikit.dsl.vStack(spacing, builder)
        stackView.addArrangedSubview(childStack)
        return childStack
    }

    fun hStack(
        spacing: Double = 8.0,
        builder: StackViewBuilder.() -> Unit
    ): UIStackView {
        val childStack = dev.imanuel.abfuhr.uikit.dsl.hStack(spacing, builder)
        stackView.addArrangedSubview(childStack)
        return childStack
    }

    fun build(): UIStackView = stackView
}

inline fun vStack(
    spacing: Double = 8.0,
    builder: StackViewBuilder.() -> Unit
): UIStackView {
    val b = StackViewBuilder(UILayoutConstraintAxisVertical)
    b.spacing = spacing
    b.builder()
    return b.build()
}

inline fun hStack(
    spacing: Double = 8.0,
    builder: StackViewBuilder.() -> Unit
): UIStackView {
    val b = StackViewBuilder(UILayoutConstraintAxisHorizontal)
    b.spacing = spacing
    b.builder()
    return b.build()
}

fun UIView.pinToEdges(parent: UIView, insets: Double = 0.0) {
    setTranslatesAutoresizingMaskIntoConstraints(false)
    NSLayoutConstraint.activateConstraints(
        listOf(
            topAnchor.constraintEqualToAnchor(parent.topAnchor, constant = insets),
            leadingAnchor.constraintEqualToAnchor(parent.leadingAnchor, constant = insets),
            trailingAnchor.constraintEqualToAnchor(parent.trailingAnchor, constant = -insets),
            bottomAnchor.constraintEqualToAnchor(parent.bottomAnchor, constant = -insets)
        )
    )
}
