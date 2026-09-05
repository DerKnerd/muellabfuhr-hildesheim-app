package dev.imanuel.abfuhr.ui.dsl

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIBarButtonItem
import platform.UIKit.UIBarButtonItemStyle
import platform.UIKit.UIBarButtonItemStylePlain
import platform.UIKit.UIButton
import platform.UIKit.UIButtonTypeSystem
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIControlStateNormal
import platform.UIKit.UIImage
import platform.UIKit.UILabel
import platform.UIKit.UILayoutConstraintAxisHorizontal
import platform.UIKit.UILayoutConstraintAxisVertical
import platform.UIKit.UINavigationBar
import platform.UIKit.UINavigationItem
import platform.UIKit.UIStackView
import platform.UIKit.UIStackViewAlignment
import platform.UIKit.UIStackViewAlignmentCenter
import platform.UIKit.UIStackViewAlignmentFill
import platform.UIKit.UIStackViewAlignmentLeading
import platform.UIKit.UIStackViewDistribution
import platform.UIKit.UIStackViewDistributionEqualSpacing
import platform.UIKit.UIStackViewDistributionFill
import platform.UIKit.UIView
import platform.UIKit.UIViewController

// =============================================================================
// 1. NAVIGATION BAR DSL
// =============================================================================

/**
 * Builder for creating and configuring a [UIBarButtonItem].
 */
@OptIn(ExperimentalForeignApi::class)
@UIKitDslMarker
class BarButtonItemBuilder {
    var title: String? = null
    var image: UIImage? = null
    var style: UIBarButtonItemStyle = UIBarButtonItemStylePlain
    var tintColor: UIColor? = null
    private var onClickAction: (() -> Unit)? = null

    fun onClick(action: () -> Unit) {
        onClickAction = action
    }

    fun build(): UIBarButtonItem {
        val action = onClickAction ?: {}
        val target = DslActionTarget(action)
        val selector = NSSelectorFromString("trigger")

        val item = if (image != null) {
            UIBarButtonItem(image = image, style = style, target = target, action = selector)
        } else {
            UIBarButtonItem(title = title ?: "", style = style, target = target, action = selector)
        }

        if (tintColor != null) {
            item.tintColor = tintColor
        }

        DslActionRegistry.retain(item, target)
        return item
    }
}

/**
 * Builder for configuring a [UINavigationItem].
 */
@UIKitDslMarker
class NavigationItemBuilder(
    val navigationItem: UINavigationItem = UINavigationItem()
) {
    var title: String?
        get() = navigationItem.title
        set(value) {
            navigationItem.title = value
        }

    var prompt: String?
        get() = navigationItem.prompt
        set(value) {
            navigationItem.prompt = value
        }

    var titleView: UIView?
        get() = navigationItem.titleView
        set(value) {
            navigationItem.titleView = value
        }

    fun leftButton(
        title: String,
        style: UIBarButtonItemStyle = UIBarButtonItemStylePlain,
        init: BarButtonItemBuilder.() -> Unit = {}
    ): UIBarButtonItem {
        val item = BarButtonItemBuilder().apply {
            this.title = title
            this.style = style
            init()
        }.build()
        navigationItem.leftBarButtonItem = item
        return item
    }

    fun rightButton(
        title: String,
        style: UIBarButtonItemStyle = UIBarButtonItemStylePlain,
        init: BarButtonItemBuilder.() -> Unit = {}
    ): UIBarButtonItem {
        val item = BarButtonItemBuilder().apply {
            this.title = title
            this.style = style
            init()
        }.build()
        navigationItem.rightBarButtonItem = item
        return item
    }

    fun customTitleView(init: ContainerBuilder.() -> Unit): UIView {
        val container = ContainerBuilder().apply(init).build()
        navigationItem.titleView = container
        return container
    }

    fun build(): UINavigationItem = navigationItem
}

/**
 * Builder for creating and configuring a [UINavigationBar].
 */
@UIKitDslMarker
class NavigationBarBuilder(
    val navigationBar: UINavigationBar = UINavigationBar()
) {
    private val item = UINavigationItem()

    init {
        navigationBar.translatesAutoresizingMaskIntoConstraints = false
        navigationBar.pushNavigationItem(item, animated = false)
    }

    var title: String?
        get() = item.title
        set(value) {
            item.title = value
        }

    var prompt: String?
        get() = item.prompt
        set(value) {
            item.prompt = value
        }

    var barTintColor: UIColor?
        get() = navigationBar.barTintColor
        set(value) {
            navigationBar.barTintColor = value
        }

    var tintColor: UIColor?
        get() = navigationBar.tintColor
        set(value) {
            navigationBar.tintColor = value
        }

    var isTranslucent: Boolean
        get() = navigationBar.translucent
        set(value) {
            navigationBar.translucent = value
        }

    var prefersLargeTitles: Boolean
        get() = navigationBar.prefersLargeTitles
        set(value) {
            navigationBar.prefersLargeTitles = value
        }

    var backgroundColor: UIColor?
        get() = navigationBar.backgroundColor
        set(value) {
            navigationBar.backgroundColor = value
        }

    fun leftButton(
        title: String,
        style: UIBarButtonItemStyle = UIBarButtonItemStylePlain,
        init: BarButtonItemBuilder.() -> Unit = {}
    ): UIBarButtonItem {
        val button = BarButtonItemBuilder().apply {
            this.title = title
            this.style = style
            init()
        }.build()
        item.leftBarButtonItem = button
        return button
    }

    fun rightButton(
        title: String,
        style: UIBarButtonItemStyle = UIBarButtonItemStylePlain,
        init: BarButtonItemBuilder.() -> Unit = {}
    ): UIBarButtonItem {
        val button = BarButtonItemBuilder().apply {
            this.title = title
            this.style = style
            init()
        }.build()
        item.rightBarButtonItem = button
        return button
    }

    fun customTitleView(init: ContainerBuilder.() -> Unit): UIView {
        val container = ContainerBuilder().apply(init).build()
        item.titleView = container
        return container
    }

    fun build(): UINavigationBar = navigationBar
}

/**
 * Creates a navigation bar ([UINavigationBar]) inside the current container scope.
 */
fun ViewScope.navigationBar(
    init: NavigationBarBuilder.() -> Unit = {}
): UINavigationBar {
    val builder = NavigationBarBuilder().apply(init)
    val view = builder.build()
    add(view)
    return view
}

// =============================================================================
// 2. TITLE BAR (Custom Standalone Top Bar / Header Component)
// =============================================================================

/**
 * A standalone, customizable Title Bar / Header View.
 */
@OptIn(ExperimentalForeignApi::class)
class TitleBarView : UIView() {
    private val titleLabel = UILabel().apply {
        translatesAutoresizingMaskIntoConstraints = false
        font = DslTypography.title2
        textColor = DslColors.textPrimary
        textAlignment = platform.UIKit.NSTextAlignmentLeft
    }

    private val subtitleLabel = UILabel().apply {
        translatesAutoresizingMaskIntoConstraints = false
        font = DslTypography.subhead
        textColor = DslColors.textSecondary
        textAlignment = platform.UIKit.NSTextAlignmentLeft
        hidden = true
    }

    private val backButton = UIButton.buttonWithType(UIButtonTypeSystem).apply {
        translatesAutoresizingMaskIntoConstraints = false
        setTitle("‹ Back", forState = UIControlStateNormal)
        titleLabel?.font = DslTypography.body
        hidden = true
    }

    private val titlesStack = UIStackView().apply {
        translatesAutoresizingMaskIntoConstraints = false
        axis = UILayoutConstraintAxisVertical
        alignment = UIStackViewAlignmentLeading
        spacing = 2.0
    }

    private val leadingStack = UIStackView().apply {
        translatesAutoresizingMaskIntoConstraints = false
        axis = UILayoutConstraintAxisHorizontal
        alignment = UIStackViewAlignmentCenter
        spacing = 8.0
    }

    private val trailingStack = UIStackView().apply {
        translatesAutoresizingMaskIntoConstraints = false
        axis = UILayoutConstraintAxisHorizontal
        alignment = UIStackViewAlignmentCenter
        spacing = 12.0
    }

    private val rootStack = UIStackView().apply {
        translatesAutoresizingMaskIntoConstraints = false
        axis = UILayoutConstraintAxisHorizontal
        alignment = UIStackViewAlignmentCenter
        distribution = UIStackViewDistributionFill
        spacing = 12.0
    }

    init {
        translatesAutoresizingMaskIntoConstraints = false
        backgroundColor = DslColors.background

        titlesStack.addArrangedSubview(titleLabel)
        titlesStack.addArrangedSubview(subtitleLabel)

        leadingStack.addArrangedSubview(backButton)
        leadingStack.addArrangedSubview(titlesStack)

        rootStack.addArrangedSubview(leadingStack)
        rootStack.addArrangedSubview(trailingStack)

        addSubview(rootStack)
        rootStack.fillSuperview(top = 8.0, leading = 16.0, bottom = 8.0, trailing = 16.0)
    }

    var title: String?
        get() = titleLabel.text
        set(value) {
            titleLabel.text = value
        }

    var subtitle: String?
        get() = subtitleLabel.text
        set(value) {
            subtitleLabel.text = value
            subtitleLabel.hidden = value.isNullOrEmpty()
        }

    var titleColor: UIColor?
        get() = titleLabel.textColor
        set(value) {
            titleLabel.textColor = value
        }

    var subtitleColor: UIColor?
        get() = subtitleLabel.textColor
        set(value) {
            subtitleLabel.textColor = value
        }

    var showBackButton: Boolean
        get() = !backButton.hidden
        set(value) {
            backButton.hidden = !value
        }

    var backButtonTitle: String?
        get() = backButton.titleForState(UIControlStateNormal)
        set(value) {
            backButton.setTitle(value ?: "‹ Back", forState = UIControlStateNormal)
        }

    fun onBackClick(action: () -> Unit) {
        showBackButton = true
        backButton.onEvent(UIControlEventTouchUpInside, action)
    }

    fun addAction(title: String, onClick: () -> Unit): UIButton {
        val btn = UIButton.buttonWithType(UIButtonTypeSystem).apply {
            translatesAutoresizingMaskIntoConstraints = false
            setTitle(title, forState = UIControlStateNormal)
            titleLabel?.font = DslTypography.headline
            onEvent(UIControlEventTouchUpInside, onClick)
        }
        trailingStack.addArrangedSubview(btn)
        return btn
    }

    fun addCustomTrailing(view: UIView) {
        view.translatesAutoresizingMaskIntoConstraints = false
        trailingStack.addArrangedSubview(view)
    }
}

/**
 * DSL Builder for configuring a [TitleBarView].
 */
@UIKitDslMarker
class TitleBarBuilder(
    val titleBar: TitleBarView = TitleBarView()
) {
    var title: String?
        get() = titleBar.title
        set(value) {
            titleBar.title = value
        }

    var subtitle: String?
        get() = titleBar.subtitle
        set(value) {
            titleBar.subtitle = value
        }

    var titleColor: UIColor?
        get() = titleBar.titleColor
        set(value) {
            titleBar.titleColor = value
        }

    var subtitleColor: UIColor?
        get() = titleBar.subtitleColor
        set(value) {
            titleBar.subtitleColor = value
        }

    var backgroundColor: UIColor?
        get() = titleBar.backgroundColor
        set(value) {
            titleBar.backgroundColor = value
        }

    var showBackButton: Boolean
        get() = titleBar.showBackButton
        set(value) {
            titleBar.showBackButton = value
        }

    var backButtonTitle: String?
        get() = titleBar.backButtonTitle
        set(value) {
            titleBar.backButtonTitle = value
        }

    fun onBackClick(action: () -> Unit) {
        titleBar.onBackClick(action)
    }

    fun action(title: String, onClick: () -> Unit): UIButton {
        return titleBar.addAction(title, onClick)
    }

    fun customTrailing(view: UIView) {
        titleBar.addCustomTrailing(view)
    }

    fun build(): TitleBarView = titleBar
}

/**
 * Creates a standalone Title Bar ([TitleBarView]) in the current container scope.
 */
fun ViewScope.titleBar(
    title: String = "",
    subtitle: String? = null,
    init: TitleBarBuilder.() -> Unit = {}
): TitleBarView {
    val builder = TitleBarBuilder().apply {
        this.title = title
        if (subtitle != null) this.subtitle = subtitle
        init()
    }
    val view = builder.build()
    add(view)
    return view
}

// =============================================================================
// 3. VIEW CONTROLLER INTEGRATION
// =============================================================================

/**
 * Root View DSL Builder for constructing screens.
 */
@UIKitDslMarker
class RootViewBuilder : ViewScope {
    val rootView: UIView = UIView().apply {
        backgroundColor = DslColors.background
    }

    override fun add(view: UIView) {
        view.translatesAutoresizingMaskIntoConstraints = false
        rootView.addSubview(view)
    }

    fun build(): UIView = rootView
}

/**
 * Configures the view hierarchy of a [UIViewController] using the UIKit DSL.
 */
fun UIViewController.setContent(init: RootViewBuilder.() -> Unit): UIView {
    val builder = RootViewBuilder().apply(init)
    val root = builder.build()
    this.view = root
    return root
}

/**
 * Configures the navigation bar / item of this [UIViewController] using the DSL.
 */
fun UIViewController.setupNavigationBar(init: NavigationItemBuilder.() -> Unit) {
    NavigationItemBuilder(this.navigationItem).apply(init)
}

/**
 * Creates a [UIView] using the DSL.
 */
fun uiView(init: RootViewBuilder.() -> Unit): UIView {
    return RootViewBuilder().apply(init).build()
}
