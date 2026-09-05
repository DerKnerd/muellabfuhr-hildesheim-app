package dev.imanuel.abfuhr.ui.dsl

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.UIKit.NSDirectionalEdgeInsets
import platform.UIKit.NSDirectionalEdgeInsetsMake
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIColor
import platform.UIKit.UIEdgeInsets
import platform.UIKit.UIEdgeInsetsMake
import platform.UIKit.UILayoutConstraintAxisHorizontal
import platform.UIKit.UILayoutConstraintAxisVertical
import platform.UIKit.UILayoutPriority
import platform.UIKit.UILayoutPriorityDefaultHigh
import platform.UIKit.UILayoutPriorityDefaultLow
import platform.UIKit.UIScrollView
import platform.UIKit.UIScrollViewKeyboardDismissMode
import platform.UIKit.UIScrollViewKeyboardDismissModeOnDrag
import platform.UIKit.UIStackView
import platform.UIKit.UIStackViewAlignment
import platform.UIKit.UIStackViewAlignmentFill
import platform.UIKit.UIStackViewDistribution
import platform.UIKit.UIStackViewDistributionFill
import platform.UIKit.UIView

/**
 * Common builder for creating and configuring a [UIStackView].
 */
@OptIn(ExperimentalForeignApi::class)
@UIKitDslMarker
open class StackBuilder(
    val stackView: UIStackView = UIStackView()
) : ViewScope {

    init {
        stackView.translatesAutoresizingMaskIntoConstraints = false
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

    var backgroundColor: UIColor?
        get() = stackView.backgroundColor
        set(value) {
            stackView.backgroundColor = value
        }

    var cornerRadius: Double
        get() = stackView.layer.cornerRadius
        set(value) {
            stackView.layer.cornerRadius = value
            stackView.layer.masksToBounds = value > 0.0
        }

    fun padding(top: Double = 0.0, leading: Double = 0.0, bottom: Double = 0.0, trailing: Double = 0.0) {
        stackView.layoutMarginsRelativeArrangement = true
        stackView.directionalLayoutMargins = NSDirectionalEdgeInsetsMake(top, leading, bottom, trailing)
    }

    fun padding(all: Double) {
        padding(top = all, leading = all, bottom = all, trailing = all)
    }

    fun padding(horizontal: Double = 0.0, vertical: Double = 0.0) {
        padding(top = vertical, leading = horizontal, bottom = vertical, trailing = horizontal)
    }

    override fun add(view: UIView) {
        view.translatesAutoresizingMaskIntoConstraints = false
        stackView.addArrangedSubview(view)
    }

    fun build(): UIStackView = stackView
}

/**
 * Vertical Stack layout builder.
 */
class VStackBuilder(stackView: UIStackView = UIStackView()) : StackBuilder(stackView) {
    init {
        this.stackView.axis = UILayoutConstraintAxisVertical
        this.stackView.alignment = UIStackViewAlignmentFill
        this.stackView.distribution = UIStackViewDistributionFill
    }
}

/**
 * Horizontal Stack layout builder.
 */
class HStackBuilder(stackView: UIStackView = UIStackView()) : StackBuilder(stackView) {
    init {
        this.stackView.axis = UILayoutConstraintAxisHorizontal
        this.stackView.alignment = UIStackViewAlignmentFill
        this.stackView.distribution = UIStackViewDistributionFill
    }
}

/**
 * Generic container view builder.
 */
@UIKitDslMarker
class ContainerBuilder(
    val containerView: UIView = UIView()
) : ViewScope {

    init {
        containerView.translatesAutoresizingMaskIntoConstraints = false
    }

    var backgroundColor: UIColor?
        get() = containerView.backgroundColor
        set(value) {
            containerView.backgroundColor = value
        }

    var cornerRadius: Double
        get() = containerView.layer.cornerRadius
        set(value) {
            containerView.layer.cornerRadius = value
            containerView.layer.masksToBounds = value > 0.0
        }

    var alpha: Double
        get() = containerView.alpha
        set(value) {
            containerView.alpha = value
        }

    var isHidden: Boolean
        get() = containerView.hidden
        set(value) {
            containerView.hidden = value
        }

    override fun add(view: UIView) {
        view.translatesAutoresizingMaskIntoConstraints = false
        containerView.addSubview(view)
    }

    fun build(): UIView = containerView
}

/**
 * Scroll view builder.
 */
@UIKitDslMarker
class ScrollViewBuilder(
    val scrollView: UIScrollView = UIScrollView()
) : ViewScope {

    init {
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        scrollView.keyboardDismissMode = UIScrollViewKeyboardDismissModeOnDrag
    }

    var bounces: Boolean
        get() = scrollView.bounces
        set(value) {
            scrollView.bounces = value
        }

    var alwaysBounceVertical: Boolean
        get() = scrollView.alwaysBounceVertical
        set(value) {
            scrollView.alwaysBounceVertical = value
        }

    var showsVerticalScrollIndicator: Boolean
        get() = scrollView.showsVerticalScrollIndicator
        set(value) {
            scrollView.showsVerticalScrollIndicator = value
        }

    var showsHorizontalScrollIndicator: Boolean
        get() = scrollView.showsHorizontalScrollIndicator
        set(value) {
            scrollView.showsHorizontalScrollIndicator = value
        }

    var backgroundColor: UIColor?
        get() = scrollView.backgroundColor
        set(value) {
            scrollView.backgroundColor = value
        }

    override fun add(view: UIView) {
        view.translatesAutoresizingMaskIntoConstraints = false
        scrollView.addSubview(view)
    }

    /**
     * Convenience method to host a vertical content layout inside the scroll view,
     * pinning content to the scroll view frame width so vertical scrolling works automatically.
     */
    fun contentVStack(
        spacing: Double = 0.0,
        alignment: UIStackViewAlignment = UIStackViewAlignmentFill,
        init: VStackBuilder.() -> Unit
    ): UIStackView {
        val vstack = VStackBuilder().apply {
            this.spacing = spacing
            this.alignment = alignment
            init()
        }.build()

        add(vstack)
        NSLayoutConstraint.activateConstraints(
            listOf(
                vstack.topAnchor.constraintEqualToAnchor(scrollView.contentLayoutGuide.topAnchor),
                vstack.bottomAnchor.constraintEqualToAnchor(scrollView.contentLayoutGuide.bottomAnchor),
                vstack.leadingAnchor.constraintEqualToAnchor(scrollView.contentLayoutGuide.leadingAnchor),
                vstack.trailingAnchor.constraintEqualToAnchor(scrollView.contentLayoutGuide.trailingAnchor),
                vstack.widthAnchor.constraintEqualToAnchor(scrollView.frameLayoutGuide.widthAnchor)
            )
        )
        return vstack
    }

    fun build(): UIScrollView = scrollView
}

// -----------------------------------------------------------------------------
// Layout DSL Scope Functions
// -----------------------------------------------------------------------------

/**
 * Creates a vertical stack layout ([UIStackView]).
 */
fun ViewScope.vstack(
    spacing: Double = 8.0,
    alignment: UIStackViewAlignment = UIStackViewAlignmentFill,
    distribution: UIStackViewDistribution = UIStackViewDistributionFill,
    init: VStackBuilder.() -> Unit = {}
): UIStackView {
    val builder = VStackBuilder().apply {
        this.spacing = spacing
        this.alignment = alignment
        this.distribution = distribution
        init()
    }
    val view = builder.build()
    add(view)
    return view
}

/**
 * Alias for [vstack].
 */
fun ViewScope.verticalStack(
    spacing: Double = 8.0,
    alignment: UIStackViewAlignment = UIStackViewAlignmentFill,
    distribution: UIStackViewDistribution = UIStackViewDistributionFill,
    init: VStackBuilder.() -> Unit = {}
): UIStackView = vstack(spacing, alignment, distribution, init)

/**
 * Creates a horizontal stack layout ([UIStackView]).
 */
fun ViewScope.hstack(
    spacing: Double = 8.0,
    alignment: UIStackViewAlignment = UIStackViewAlignmentFill,
    distribution: UIStackViewDistribution = UIStackViewDistributionFill,
    init: HStackBuilder.() -> Unit = {}
): UIStackView {
    val builder = HStackBuilder().apply {
        this.spacing = spacing
        this.alignment = alignment
        this.distribution = distribution
        init()
    }
    val view = builder.build()
    add(view)
    return view
}

/**
 * Alias for [hstack].
 */
fun ViewScope.horizontalStack(
    spacing: Double = 8.0,
    alignment: UIStackViewAlignment = UIStackViewAlignmentFill,
    distribution: UIStackViewDistribution = UIStackViewDistributionFill,
    init: HStackBuilder.() -> Unit = {}
): UIStackView = hstack(spacing, alignment, distribution, init)

/**
 * Creates a generic container view ([UIView]).
 */
fun ViewScope.container(
    init: ContainerBuilder.() -> Unit = {}
): UIView {
    val builder = ContainerBuilder().apply(init)
    val view = builder.build()
    add(view)
    return view
}

/**
 * Alias for [container].
 */
fun ViewScope.box(init: ContainerBuilder.() -> Unit = {}): UIView = container(init)

/**
 * Creates a scrollable view ([UIScrollView]).
 */
fun ViewScope.scrollView(
    init: ScrollViewBuilder.() -> Unit = {}
): UIScrollView {
    val builder = ScrollViewBuilder().apply(init)
    val view = builder.build()
    add(view)
    return view
}

/**
 * Creates an expanding flexible spacer view that pushes surrounding views apart in stacks.
 */
fun ViewScope.spacer(minLength: Double = 0.0): UIView {
    val spacerView = UIView().apply {
        translatesAutoresizingMaskIntoConstraints = false
        setContentHuggingPriority(UILayoutPriorityDefaultLow, UILayoutConstraintAxisHorizontal)
        setContentHuggingPriority(UILayoutPriorityDefaultLow, UILayoutConstraintAxisVertical)
        setContentCompressionResistancePriority(UILayoutPriorityDefaultLow, UILayoutConstraintAxisHorizontal)
        setContentCompressionResistancePriority(UILayoutPriorityDefaultLow, UILayoutConstraintAxisVertical)
        if (minLength > 0.0) {
            NSLayoutConstraint.activateConstraints(
                listOf(
                    widthAnchor.constraintGreaterThanOrEqualToConstant(minLength),
                    heightAnchor.constraintGreaterThanOrEqualToConstant(minLength)
                )
            )
        }
    }
    add(spacerView)
    return spacerView
}

/**
 * Creates a thin divider line.
 */
fun ViewScope.divider(
    color: UIColor = DslColors.separator,
    thickness: Double = 1.0,
    horizontal: Boolean = true
): UIView {
    val divider = UIView().apply {
        translatesAutoresizingMaskIntoConstraints = false
        backgroundColor = color
        if (horizontal) {
            heightAnchor.constraintEqualToConstant(thickness).active = true
        } else {
            widthAnchor.constraintEqualToConstant(thickness).active = true
        }
    }
    add(divider)
    return divider
}

// -----------------------------------------------------------------------------
// Auto Layout Extensions & Modifiers
// -----------------------------------------------------------------------------

/**
 * Pins this view to fill its superview with optional padding.
 */
fun UIView.fillSuperview(
    top: Double = 0.0,
    leading: Double = 0.0,
    bottom: Double = 0.0,
    trailing: Double = 0.0
) {
    val parent = superview ?: return
    translatesAutoresizingMaskIntoConstraints = false
    NSLayoutConstraint.activateConstraints(
        listOf(
            topAnchor.constraintEqualToAnchor(parent.topAnchor, constant = top),
            leadingAnchor.constraintEqualToAnchor(parent.leadingAnchor, constant = leading),
            bottomAnchor.constraintEqualToAnchor(parent.bottomAnchor, constant = -bottom),
            trailingAnchor.constraintEqualToAnchor(parent.trailingAnchor, constant = -trailing)
        )
    )
}

/**
 * Pins this view to fill its superview with equal padding on all sides.
 */
fun UIView.fillSuperview(padding: Double) {
    fillSuperview(top = padding, leading = padding, bottom = padding, trailing = padding)
}

/**
 * Pins this view to the safe area layout guide of its superview.
 */
fun UIView.pinToSafeArea(
    top: Double = 0.0,
    leading: Double = 0.0,
    bottom: Double = 0.0,
    trailing: Double = 0.0
) {
    val parent = superview ?: return
    translatesAutoresizingMaskIntoConstraints = false
    NSLayoutConstraint.activateConstraints(
        listOf(
            topAnchor.constraintEqualToAnchor(parent.safeAreaLayoutGuide.topAnchor, constant = top),
            leadingAnchor.constraintEqualToAnchor(parent.safeAreaLayoutGuide.leadingAnchor, constant = leading),
            bottomAnchor.constraintEqualToAnchor(parent.safeAreaLayoutGuide.bottomAnchor, constant = -bottom),
            trailingAnchor.constraintEqualToAnchor(parent.safeAreaLayoutGuide.trailingAnchor, constant = -trailing)
        )
    )
}

/**
 * Centers this view within its superview.
 */
fun UIView.centerInSuperview() {
    val parent = superview ?: return
    translatesAutoresizingMaskIntoConstraints = false
    NSLayoutConstraint.activateConstraints(
        listOf(
            centerXAnchor.constraintEqualToAnchor(parent.centerXAnchor),
            centerYAnchor.constraintEqualToAnchor(parent.centerYAnchor)
        )
    )
}

/**
 * Sets explicit width constraint.
 */
fun UIView.width(width: Double): UIView {
    translatesAutoresizingMaskIntoConstraints = false
    widthAnchor.constraintEqualToConstant(width).active = true
    return this
}

/**
 * Sets explicit height constraint.
 */
fun UIView.height(height: Double): UIView {
    translatesAutoresizingMaskIntoConstraints = false
    heightAnchor.constraintEqualToConstant(height).active = true
    return this
}

/**
 * Sets explicit width and height constraints.
 */
fun UIView.size(width: Double, height: Double): UIView {
    this.width(width)
    this.height(height)
    return this
}
