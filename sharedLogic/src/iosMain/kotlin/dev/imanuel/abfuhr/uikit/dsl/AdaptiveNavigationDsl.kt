@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import dev.imanuel.abfuhr.helper.isIos18
import kotlinx.cinterop.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.objc.OBJC_ASSOCIATION_RETAIN_NONATOMIC
import platform.objc.objc_setAssociatedObject

/**
 * Adaptation mode for adaptive navigation.
 */
enum class AdaptiveNavigationMode {
    /**
     * Automatically adapts based on the device user interface idiom.
     * Uses the system convertible TabBar / Sidebar on iPadOS where available.
     */
    Auto,

    /**
     * Forces standard iPhone layout (Bottom TabBar / UITabBarController).
     */
    PhoneTabBar,

    /**
     * Dynamically adapts when the trait collection / size class changes.
     */
    TraitBased
}

/**
 * Represents a single navigation destination within the adaptive navigation structure.
 */
class AdaptiveNavigationItem(
    val title: String,
    val image: UIImage? = null,
    val viewController: UIViewController? = null,
    val contentView: UIView? = null,
) {
    fun toTabBarItem() = UITabBarItem(title = title, image = image, tag = 0L)

    fun resolveViewController(): UIViewController {
        if (viewController != null) {
            viewController.title = title
            viewController.tabBarItem = toTabBarItem()
            return viewController
        }
        val vc = UIViewController()
        if (contentView != null) {
            vc.view.addSubview(contentView)
            contentView.setTranslatesAutoresizingMaskIntoConstraints(false)
            NSLayoutConstraint.activateConstraints(
                listOf(
                    contentView.topAnchor.constraintEqualToAnchor(vc.view.topAnchor),
                    contentView.bottomAnchor.constraintEqualToAnchor(vc.view.bottomAnchor),
                    contentView.leadingAnchor.constraintEqualToAnchor(vc.view.leadingAnchor),
                    contentView.trailingAnchor.constraintEqualToAnchor(vc.view.trailingAnchor)
                )
            )
        }
        vc.title = title
        vc.tabBarItem = toTabBarItem()
        return vc
    }
}

/**
 * Section for grouping adaptive navigation items.
 */
class AdaptiveNavigationSection(
    val title: String?,
    val items: List<AdaptiveNavigationItem>
)

@UIKitDsl
class AdaptiveNavigationItemBuilder {
    var title: String = ""
    var image: UIImage? = null
    var viewController: UIViewController? = null
    var contentView: UIView? = null

    fun content(viewProvider: () -> UIView) {
        this.contentView = viewProvider()
    }

    fun build(): AdaptiveNavigationItem {
        return AdaptiveNavigationItem(
            title = title,
            image = image,
            viewController = viewController,
            contentView = contentView,
        )
    }
}

// Fixed static pointer for ObjC runtime key
private val adaptiveTabBarDelegateKey = nativeHeap.alloc<ByteVar>().ptr

private class AdaptiveTabBarControllerDelegateBridge(
    private val items: List<AdaptiveNavigationItem>,
    private val onItemSelected: ((Int, AdaptiveNavigationItem) -> Unit)? = null
) : NSObject(), UITabBarControllerDelegateProtocol {

    @ObjCSignatureOverride
    override fun tabBarController(tabBarController: UITabBarController, didSelectViewController: UIViewController) {
        val viewControllers = tabBarController.viewControllers ?: return
        val index = viewControllers.indexOf(didSelectViewController)
        if (index >= 0 && index < items.size) {
            val item = items[index]
            onItemSelected?.invoke(index, item)
        }
    }
}

/**
 * Controller managing adaptive navigation across iPhone (TabBar) and iPad (convertible TabBar / Sidebar).
 */
class AdaptiveNavigationController(
    val mode: AdaptiveNavigationMode = AdaptiveNavigationMode.Auto,
    val sections: List<AdaptiveNavigationSection>,
    val tabBarTintColor: UIColor? = null,
    val tabBarBackgroundColor: UIColor? = null,
    val tabBarUnselectedItemTintColor: UIColor? = null,
    var onItemSelectedCallback: ((Int, AdaptiveNavigationItem) -> Unit)? = null
) : UIViewController(nibName = null, bundle = null) {

    val allItems: List<AdaptiveNavigationItem> = sections.flatMap { it.items }

    var selectedIndex: Int = 0
        private set

    private var tabBarController: UITabBarController? = null
    private var currentChildViewController: UIViewController? = null

    private val isPadLayout: Boolean
        get() = when (mode) {
            AdaptiveNavigationMode.PhoneTabBar -> false
            AdaptiveNavigationMode.Auto -> UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad
            AdaptiveNavigationMode.TraitBased -> traitCollection.horizontalSizeClass != UIUserInterfaceSizeClassCompact
        }

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.systemBackgroundColor
        setupChildNavigation()
    }

    override fun traitCollectionDidChange(previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        if (mode == AdaptiveNavigationMode.TraitBased) {
            val prevClass = previousTraitCollection?.horizontalSizeClass
            val currentClass = traitCollection.horizontalSizeClass
            if (prevClass != currentClass) {
                setupChildNavigation()
            }
        }
    }

    fun setupChildNavigation() {
        currentChildViewController?.let {
            it.willMoveToParentViewController(null)
            it.view.removeFromSuperview()
            it.removeFromParentViewController()
        }

        setupTabBarController(prefersConvertibleSidebar = isPadLayout)
    }

    private fun setupTabBarController(prefersConvertibleSidebar: Boolean = false) {
        val tabController = UITabBarController()

        if (prefersConvertibleSidebar) {
            tabController.configureAsConvertibleTabBar()
        }

        val appearance = UITabBarAppearance().apply {
            configureWithDefaultBackground()
            tabBarBackgroundColor?.let { backgroundColor = it }
        }
        tabController.tabBar.standardAppearance = appearance
        tabController.tabBar.scrollEdgeAppearance = appearance

        tabBarTintColor?.let {
            tabController.tabBar.tintColor = it
        }
        tabBarBackgroundColor?.let {
            tabController.tabBar.backgroundColor = it
        }
        tabBarUnselectedItemTintColor?.let {
            tabController.tabBar.unselectedItemTintColor = it
        }

        val viewControllers = allItems.map { item ->
            val rootVc = item.resolveViewController()

            UINavigationController(rootViewController = rootVc).apply {
                tabBarItem = item.toTabBarItem()

                navigationBar.standardAppearance =
                    UINavigationBarAppearance().apply {
                        configureWithDefaultBackground()
                    }

                navigationBar.scrollEdgeAppearance =
                    navigationBar.standardAppearance
            }
        }

        tabController.setViewControllers(viewControllers, animated = false)

        val bridge = AdaptiveTabBarControllerDelegateBridge(allItems) { idx, item ->
            this.selectedIndex = idx
            onItemSelectedCallback?.invoke(idx, item)
        }

        tabController.setDelegate(bridge)

        objc_setAssociatedObject(
            tabController,
            adaptiveTabBarDelegateKey,
            bridge,
            OBJC_ASSOCIATION_RETAIN_NONATOMIC
        )

        if (allItems.isNotEmpty()) {
            val targetIndex = selectedIndex.coerceIn(0, allItems.size - 1)

            tabController.setSelectedIndex(targetIndex.toULong())
        }

        this.tabBarController = tabController
        embedChild(tabController)
    }

    private fun UITabBarController.configureAsConvertibleTabBar() {
        if (UIDevice.currentDevice.userInterfaceIdiom != UIUserInterfaceIdiomPad || !isIos18()) {
            return
        }

        mode = UITabBarControllerModeTabSidebar
        tabBarMinimizeBehavior = UITabBarMinimizeBehaviorNever
    }

    private fun embedChild(child: UIViewController) {
        addChildViewController(child)
        view.addSubview(child.view)
        child.view.setTranslatesAutoresizingMaskIntoConstraints(false)
        NSLayoutConstraint.activateConstraints(
            listOf(
                child.view.topAnchor.constraintEqualToAnchor(view.topAnchor),
                child.view.bottomAnchor.constraintEqualToAnchor(view.bottomAnchor),
                child.view.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor),
                child.view.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor)
            )
        )
        child.didMoveToParentViewController(this)
        currentChildViewController = child
    }

    fun selectItem(index: Int, notify: Boolean = true) {
        if (index !in allItems.indices) return
        this.selectedIndex = index
        val item = allItems[index]

        tabBarController?.setSelectedIndex(index.toULong())

        if (notify) {
            onItemSelectedCallback?.invoke(index, item)
        }
    }
}

@UIKitDsl
class AdaptiveNavigationBuilder {
    var mode: AdaptiveNavigationMode = AdaptiveNavigationMode.Auto
    var selectedIndex: Int = 0

    private val defaultItems = mutableListOf<AdaptiveNavigationItem>()
    private var itemSelectedListener: ((Int, AdaptiveNavigationItem) -> Unit)? = null

    fun item(
        title: String,
        image: UIImage,
        builder: AdaptiveNavigationItemBuilder.() -> Unit = {}
    ) {
        val b = AdaptiveNavigationItemBuilder()
        b.title = title
        b.image = image
        b.builder()
        defaultItems.add(b.build())
    }

    fun buildController(): AdaptiveNavigationController {
        val controller = AdaptiveNavigationController(
            mode = mode,
            sections = listOf(
                AdaptiveNavigationSection(
                    title = null,
                    items = defaultItems.toList()
                )
            ),
            onItemSelectedCallback = itemSelectedListener
        )

        if (selectedIndex != 0) {
            controller.selectItem(selectedIndex, notify = false)
        }

        return controller
    }
}

/**
 * Creates an adaptive navigation controller that automatically switches between iPhone TabBar and iPad convertible TabBar.
 */
inline fun adaptiveNavigation(
    builder: AdaptiveNavigationBuilder.() -> Unit
): AdaptiveNavigationController {
    val b = AdaptiveNavigationBuilder()
    b.builder()
    return b.buildController()
}
