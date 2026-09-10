@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import kotlinx.cinterop.*
import platform.Foundation.NSIndexPath
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
     */
    Auto,

    /**
     * Forces standard iPhone layout (Bottom TabBar / UITabBarController).
     */
    PhoneTabBar,

    /**
     * Forces iPadOS layout (Sidebar / UISplitViewController).
     */
    PadSidebar,

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
    val subtitle: String? = null,
    val image: UIImage? = null,
    val selectedImage: UIImage? = null,
    val badgeValue: String? = null,
    val badgeColor: UIColor? = null,
    val tag: Long = 0L,
    val viewController: UIViewController? = null,
    val contentView: UIView? = null,
    val onSelect: (() -> Unit)? = null
) {
    fun toTabBarItem(): UITabBarItem {
        val item = UITabBarItem(title = title, image = image, tag = tag)
        item.selectedImage = selectedImage ?: image
        item.badgeValue = badgeValue
        badgeColor?.let { item.badgeColor = it }
        return item
    }

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
 * Section for grouping items in iPad sidebar navigation.
 */
class AdaptiveNavigationSection(
    val title: String?,
    val items: List<AdaptiveNavigationItem>
)

@UIKitDsl
class AdaptiveNavigationItemBuilder {
    var title: String = ""
    var subtitle: String? = null
    var systemImageName: String? = null
    var image: UIImage? = null
    var selectedImage: UIImage? = null
    var selectedSystemImageName: String? = null
    var badgeValue: String? = null
    var badgeColor: UIColor? = null
    var tag: Long = 0L
    var viewController: UIViewController? = null
    var contentView: UIView? = null

    private var selectAction: (() -> Unit)? = null

    fun systemIcon(name: String, pointSize: Double? = null) {
        val config = pointSize?.let { UIImageSymbolConfiguration.configurationWithPointSize(it) }
        image = if (config != null) UIImage.systemImageNamed(name, config) else UIImage.systemImageNamed(name)
    }

    fun selectedSystemIcon(name: String, pointSize: Double? = null) {
        val config = pointSize?.let { UIImageSymbolConfiguration.configurationWithPointSize(it) }
        selectedImage = if (config != null) UIImage.systemImageNamed(name, config) else UIImage.systemImageNamed(name)
    }

    fun onSelect(action: () -> Unit) {
        this.selectAction = action
    }

    fun content(viewProvider: () -> UIView) {
        this.contentView = viewProvider()
    }

    fun viewController(controllerProvider: () -> UIViewController) {
        this.viewController = controllerProvider()
    }

    fun build(): AdaptiveNavigationItem {
        val finalImage = image ?: systemImageName?.let { UIImage.systemImageNamed(it) }
        val finalSelectedImage = selectedImage ?: selectedSystemImageName?.let { UIImage.systemImageNamed(it) }

        return AdaptiveNavigationItem(
            title = title,
            subtitle = subtitle,
            image = finalImage,
            selectedImage = finalSelectedImage,
            badgeValue = badgeValue,
            badgeColor = badgeColor,
            tag = tag,
            viewController = viewController,
            contentView = contentView,
            onSelect = selectAction
        )
    }
}

@UIKitDsl
class AdaptiveNavigationSectionBuilder(
    var title: String? = null,
) {
    private val itemsList = mutableListOf<AdaptiveNavigationItem>()

    fun item(
        title: String,
        systemImageName: String? = null,
        tag: Long = 0L,
        builder: AdaptiveNavigationItemBuilder.() -> Unit = {}
    ) {
        val b = AdaptiveNavigationItemBuilder()
        b.title = title
        b.systemImageName = systemImageName
        b.tag = tag
        b.builder()
        itemsList.add(b.build())
    }

    fun item(
        title: String,
        image: UIImage,
        selectedImage: UIImage? = null,
        tag: Long = 0L,
        builder: AdaptiveNavigationItemBuilder.() -> Unit = {}
    ) {
        val b = AdaptiveNavigationItemBuilder()
        b.title = title
        b.image = image
        b.selectedImage = selectedImage
        b.tag = tag
        b.builder()
        itemsList.add(b.build())
    }

    fun build(): AdaptiveNavigationSection {
        return AdaptiveNavigationSection(
            title = title,
            items = itemsList.toList()
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
            item.onSelect?.invoke()
            onItemSelected?.invoke(index, item)
        }
    }
}

// Native iPadOS Sidebar Controller using UICollectionView + UICollectionLayoutListAppearanceSidebar
private class SidebarCollectionViewController(
    private val sections: List<AdaptiveNavigationSection>,
    private val headerTitle: String?,
    private val onItemSelected: (Int, AdaptiveNavigationItem) -> Unit
) : UIViewController(nibName = null, bundle = null) {

    private var collectionView: UICollectionView? = null

    override fun viewDidLoad() {
        super.viewDidLoad()
        title = headerTitle ?: ""

        val config =
            UICollectionLayoutListConfiguration(UICollectionLayoutListAppearance.UICollectionLayoutListAppearanceSidebar)
        val layout = UICollectionViewCompositionalLayout.layoutWithListConfiguration(config)

        collectionView = UICollectionView(frame = view.bounds, collectionViewLayout = layout).apply {
            autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
            registerClass(UICollectionViewListCell.`class`(), forCellWithReuseIdentifier = "SidebarCell")
            delegate = SidebarDelegate()
            dataSource = SidebarDataSource()
        }

        view.addSubview(collectionView!!)
    }

    fun selectItem(index: Int) {
        var count = 0
        for (secIdx in sections.indices) {
            val sec = sections[secIdx]
            if (index < count + sec.items.size) {
                val itemIdx = index - count
                val indexPath = NSIndexPath.indexPathForItem(itemIdx.toLong(), inSection = secIdx.toLong())
                collectionView?.selectItemAtIndexPath(
                    indexPath,
                    animated = false,
                    scrollPosition = UICollectionViewScrollPositionNone
                )
                break
            }
            count += sec.items.size
        }
    }

    private inner class SidebarDataSource : NSObject(), UICollectionViewDataSourceProtocol {
        override fun numberOfSectionsInCollectionView(collectionView: UICollectionView): Long {
            return sections.size.toLong()
        }

        override fun collectionView(collectionView: UICollectionView, numberOfItemsInSection: Long): Long {
            return sections[numberOfItemsInSection.toInt()].items.size.toLong()
        }

        override fun collectionView(
            collectionView: UICollectionView,
            cellForItemAtIndexPath: NSIndexPath
        ): UICollectionViewCell {
            val item = sections[cellForItemAtIndexPath.section.toInt()].items[cellForItemAtIndexPath.item.toInt()]

            val cell = collectionView.dequeueReusableCellWithReuseIdentifier(
                identifier = "SidebarCell",
                forIndexPath = cellForItemAtIndexPath
            )

            val contentConfig = UIListContentConfiguration.sidebarCellConfiguration().apply {
                setText(item.title)
                setSecondaryText(item.subtitle)
                setImage(item.image)
            }

            cell.setContentConfiguration(contentConfig)
            return cell
        }
    }

    private inner class SidebarDelegate : NSObject(), UICollectionViewDelegateProtocol {
        override fun collectionView(collectionView: UICollectionView, didSelectItemAtIndexPath: NSIndexPath) {
            var globalIndex = 0
            val targetSection = didSelectItemAtIndexPath.section.toInt()
            val targetItem = didSelectItemAtIndexPath.item.toInt()

            for (s in 0 until targetSection) {
                globalIndex += sections[s].items.size
            }
            globalIndex += targetItem

            val item = sections[targetSection].items[targetItem]
            onItemSelected(globalIndex, item)
        }
    }
}

/**
 * Controller managing adaptive navigation across iPhone (TabBar) and iPad (Sidebar / SplitView).
 */
class AdaptiveNavigationController(
    val mode: AdaptiveNavigationMode = AdaptiveNavigationMode.Auto,
    val sections: List<AdaptiveNavigationSection>,
    val headerTitle: String? = "Menu",
    val tabBarTintColor: UIColor? = null,
    val tabBarBackgroundColor: UIColor? = null,
    val tabBarUnselectedItemTintColor: UIColor? = null,
    val showsDisplayModeButtonItem: Boolean = true,
    var onItemSelectedCallback: ((Int, AdaptiveNavigationItem) -> Unit)? = null
) : UIViewController(nibName = null, bundle = null) {

    val allItems: List<AdaptiveNavigationItem> = sections.flatMap { it.items }

    var selectedIndex: Int = 0
        private set

    var tabBarController: UITabBarController? = null
        private set

    var splitViewController: UISplitViewController? = null
        private set

    private var sidebarController: SidebarCollectionViewController? = null
    var currentChildViewController: UIViewController? = null
        private set

    val isPadLayout: Boolean
        get() = when (mode) {
            AdaptiveNavigationMode.PadSidebar -> true
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

        if (isPadLayout) {
            setupSplitViewController()
        } else {
            setupTabBarController()
        }
    }

    private fun setupTabBarController() {
        val tabController = UITabBarController()
        tabBarTintColor?.let { tabController.tabBar.setTintColor(it) }
        tabBarBackgroundColor?.let { tabController.tabBar.setBackgroundColor(it) }
        tabBarUnselectedItemTintColor?.let { tabController.tabBar.setUnselectedItemTintColor(it) }

        val viewControllers = allItems.map { item ->
            val rootVc = item.resolveViewController()
            UINavigationController(rootViewController = rootVc).apply {
                this.tabBarItem = item.toTabBarItem()
            }
        }
        tabController.setViewControllers(viewControllers, animated = false)

        val bridge = AdaptiveTabBarControllerDelegateBridge(allItems) { idx, item ->
            this.selectedIndex = idx
            item.onSelect?.invoke()
            onItemSelectedCallback?.invoke(idx, item)
        }
        tabController.setDelegate(bridge)
        objc_setAssociatedObject(tabController, adaptiveTabBarDelegateKey, bridge, OBJC_ASSOCIATION_RETAIN_NONATOMIC)

        if (allItems.isNotEmpty()) {
            val targetIndex = selectedIndex.coerceIn(0, allItems.size - 1)
            tabController.setSelectedIndex(targetIndex.toULong())
        }

        this.tabBarController = tabController
        this.splitViewController = null
        embedChild(tabController)
    }

    private fun setupSplitViewController() {
        val splitVc = UISplitViewController(style = UISplitViewControllerStyle.UISplitViewControllerStyleDoubleColumn)

        val sidebarVc = SidebarCollectionViewController(
            sections = sections,
            headerTitle = headerTitle,
            onItemSelected = { idx, _ ->
                this.selectedIndex = idx
                selectItem(idx, notify = true)
            }
        )
        this.sidebarController = sidebarVc

        val sidebarNav = UINavigationController(rootViewController = sidebarVc)
        val targetItem = allItems.getOrNull(selectedIndex) ?: allItems.firstOrNull()

        val initialDetailVc = targetItem?.resolveViewController() ?: UIViewController()
        val detailNav = UINavigationController(rootViewController = initialDetailVc)

        if (showsDisplayModeButtonItem) {
            detailNav.topViewController?.navigationItem?.leftBarButtonItem = splitVc.displayModeButtonItem()
        }

        splitVc.setViewController(
            sidebarNav,
            forColumn = UISplitViewControllerColumn.UISplitViewControllerColumnPrimary
        )
        splitVc.setViewController(
            detailNav,
            forColumn = UISplitViewControllerColumn.UISplitViewControllerColumnSecondary
        )
        splitVc.setPrimaryBackgroundStyle(UISplitViewControllerBackgroundStyle.UISplitViewControllerBackgroundStyleSidebar)

        this.splitViewController = splitVc
        this.tabBarController = null
        embedChild(splitVc)

        sidebarVc.selectItem(selectedIndex)
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

        splitViewController?.let { splitVc ->
            val resolved = item.resolveViewController()
            val secondaryNav = UINavigationController(rootViewController = resolved)
            if (showsDisplayModeButtonItem) {
                secondaryNav.topViewController?.navigationItem?.leftBarButtonItem = splitVc.displayModeButtonItem()
            }
            splitVc.setViewController(
                secondaryNav,
                forColumn = UISplitViewControllerColumn.UISplitViewControllerColumnSecondary
            )
            sidebarController?.selectItem(index)
        }

        if (notify) {
            item.onSelect?.invoke()
            onItemSelectedCallback?.invoke(index, item)
        }
    }

    fun selectItemByTag(tag: Long, notify: Boolean = true) {
        val index = allItems.indexOfFirst { it.tag == tag }
        if (index >= 0) {
            selectItem(index, notify)
        }
    }
}

@UIKitDsl
class AdaptiveNavigationBuilder {
    var mode: AdaptiveNavigationMode = AdaptiveNavigationMode.Auto
    var headerTitle: String? = "Menu"
    var selectedIndex: Int = 0
    var tabBarTintColor: UIColor? = null
    var tabBarBackgroundColor: UIColor? = null
    var tabBarUnselectedItemTintColor: UIColor? = null
    var showsDisplayModeButtonItem: Boolean = true

    private val sectionsList = mutableListOf<AdaptiveNavigationSection>()
    private val defaultItems = mutableListOf<AdaptiveNavigationItem>()
    private var itemSelectedListener: ((Int, AdaptiveNavigationItem) -> Unit)? = null

    fun item(
        title: String,
        systemImageName: String? = null,
        tag: Long = 0L,
        builder: AdaptiveNavigationItemBuilder.() -> Unit = {}
    ) {
        val b = AdaptiveNavigationItemBuilder()
        b.title = title
        b.systemImageName = systemImageName
        b.tag = tag
        b.builder()
        defaultItems.add(b.build())
    }

    fun item(
        title: String,
        image: UIImage,
        selectedImage: UIImage? = null,
        tag: Long = 0L,
        builder: AdaptiveNavigationItemBuilder.() -> Unit = {}
    ) {
        val b = AdaptiveNavigationItemBuilder()
        b.title = title
        b.image = image
        b.selectedImage = selectedImage
        b.tag = tag
        b.builder()
        defaultItems.add(b.build())
    }

    fun section(
        title: String? = null,
        builder: AdaptiveNavigationSectionBuilder.() -> Unit
    ) {
        val b = AdaptiveNavigationSectionBuilder(title = title)
        b.builder()
        sectionsList.add(b.build())
    }

    fun onItemSelected(action: (Int, AdaptiveNavigationItem) -> Unit) {
        this.itemSelectedListener = action
    }

    fun buildController(): AdaptiveNavigationController {
        val allSections = mutableListOf<AdaptiveNavigationSection>()
        if (defaultItems.isNotEmpty()) {
            allSections.add(
                AdaptiveNavigationSection(
                    title = null,
                    items = defaultItems.toList()
                )
            )
        }
        allSections.addAll(sectionsList)

        val controller = AdaptiveNavigationController(
            mode = mode,
            sections = allSections,
            headerTitle = headerTitle,
            tabBarTintColor = tabBarTintColor,
            tabBarBackgroundColor = tabBarBackgroundColor,
            tabBarUnselectedItemTintColor = tabBarUnselectedItemTintColor,
            showsDisplayModeButtonItem = showsDisplayModeButtonItem,
            onItemSelectedCallback = itemSelectedListener
        )

        if (selectedIndex != 0) {
            controller.selectItem(selectedIndex, notify = false)
        }

        return controller
    }
}

/**
 * Creates an adaptive navigation controller that automatically switches between iPhone TabBar and iPad Sidebar.
 */
inline fun adaptiveNavigation(
    builder: AdaptiveNavigationBuilder.() -> Unit
): AdaptiveNavigationController {
    val b = AdaptiveNavigationBuilder()
    b.builder()
    return b.buildController()
}