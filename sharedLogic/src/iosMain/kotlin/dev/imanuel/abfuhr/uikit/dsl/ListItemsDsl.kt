@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import kotlinx.cinterop.*
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSIndexPath
import platform.UIKit.*
import platform.darwin.NSObject
import platform.objc.OBJC_ASSOCIATION_RETAIN_NONATOMIC
import platform.objc.objc_setAssociatedObject

@UIKitDsl
class ListItemModel(
    var title: String,
    var subtitle: String? = null,
    var icon: UIImage? = null,
    var accessoryType: UITableViewCellAccessoryType = UITableViewCellAccessoryType.UITableViewCellAccessoryNone,
    var titleColor: UIColor? = null,
    var subtitleColor: UIColor? = null,
    var titleFont: UIFont? = null,
    var subtitleFont: UIFont? = null,
    var backgroundColor: UIColor? = null,
    var tintColor: UIColor? = null,
    var isEnabled: Boolean = true,
    var customCellProvider: ((UITableView, NSIndexPath) -> UITableViewCell)? = null,
    var onSelect: (() -> Unit)? = null
)

@UIKitDsl
class ListItemCellBuilder {
    var title: String = ""
    var subtitle: String? = null
    var icon: UIImage? = null
    var accessoryType: UITableViewCellAccessoryType = UITableViewCellAccessoryType.UITableViewCellAccessoryNone
    var titleColor: UIColor? = null
    var subtitleColor: UIColor? = null
    var titleFont: UIFont? = null
    var subtitleFont: UIFont? = null
    var backgroundColor: UIColor? = null
    var tintColor: UIColor? = null
    var isEnabled: Boolean = true
    var cellStyle: UITableViewCellStyle = UITableViewCellStyle.UITableViewCellStyleSubtitle

    private var selectAction: (() -> Unit)? = null

    fun systemIcon(name: String, pointSize: Double? = null) {
        val config = pointSize?.let { UIImageSymbolConfiguration.configurationWithPointSize(it) }
        icon = if (config != null) UIImage.systemImageNamed(name, config) else UIImage.systemImageNamed(name)
    }

    fun onSelect(action: () -> Unit) {
        this.selectAction = action
    }

    fun buildModel(): ListItemModel {
        return ListItemModel(
            title = title,
            subtitle = subtitle,
            icon = icon,
            accessoryType = accessoryType,
            titleColor = titleColor,
            subtitleColor = subtitleColor,
            titleFont = titleFont,
            subtitleFont = subtitleFont,
            backgroundColor = backgroundColor,
            tintColor = tintColor,
            isEnabled = isEnabled,
            onSelect = selectAction
        )
    }

    fun buildCell(reuseIdentifier: String? = "ListItemCell"): UITableViewCell {
        val cell = UITableViewCell(style = cellStyle, reuseIdentifier = reuseIdentifier)
        cell.textLabel?.setText(title)
        subtitle?.let { cell.detailTextLabel?.setText(it) }
        icon?.let { cell.imageView?.setImage(it) }
        cell.setAccessoryType(accessoryType)
        titleColor?.let { cell.textLabel?.setTextColor(it) }
        subtitleColor?.let { cell.detailTextLabel?.setTextColor(it) }
        titleFont?.let { cell.textLabel?.setFont(it) }
        subtitleFont?.let { cell.detailTextLabel?.setFont(it) }
        backgroundColor?.let { cell.setBackgroundColor(it) }
        tintColor?.let { cell.setTintColor(it) }
        cell.setUserInteractionEnabled(isEnabled)
        return cell
    }
}

@UIKitDsl
class ListSectionBuilder(
    var headerTitle: String? = null,
    var footerTitle: String? = null
) {
    val items = mutableListOf<ListItemModel>()

    fun item(
        title: String,
        subtitle: String? = null,
        icon: UIImage? = null,
        builder: ListItemCellBuilder.() -> Unit = {}
    ) {
        val b = ListItemCellBuilder()
        b.title = title
        b.subtitle = subtitle
        b.icon = icon
        b.builder()
        items.add(b.buildModel())
    }

    fun <T> items(
        itemList: Iterable<T>,
        itemContent: ListItemCellBuilder.(T) -> Unit
    ) {
        for (item in itemList) {
            val b = ListItemCellBuilder()
            b.itemContent(item)
            items.add(b.buildModel())
        }
    }

    fun customItem(
        onSelect: (() -> Unit)? = null,
        cellProvider: (UITableView, NSIndexPath) -> UITableViewCell
    ) {
        items.add(
            ListItemModel(
                title = "",
                customCellProvider = cellProvider,
                onSelect = onSelect
            )
        )
    }
}

class ListSection(
    val headerTitle: String?,
    val footerTitle: String?,
    val items: List<ListItemModel>
)

class TableViewBridge(
    private val sections: List<ListSection>
) : NSObject(), UITableViewDataSourceProtocol, UITableViewDelegateProtocol {

    override fun numberOfSectionsInTableView(tableView: UITableView): Long {
        return sections.size.toLong()
    }

    @ObjCSignatureOverride
    override fun tableView(tableView: UITableView, numberOfRowsInSection: Long): Long {
        val sec = sections.getOrNull(numberOfRowsInSection.toInt()) ?: return 0L
        return sec.items.size.toLong()
    }

    @ObjCSignatureOverride
    override fun tableView(tableView: UITableView, cellForRowAtIndexPath: NSIndexPath): UITableViewCell {
        val sectionIndex = cellForRowAtIndexPath.section.toInt()
        val rowIndex = cellForRowAtIndexPath.row.toInt()
        val item = sections.getOrNull(sectionIndex)?.items?.getOrNull(rowIndex)
            ?: return UITableViewCell()

        if (item.customCellProvider != null) {
            return item.customCellProvider!!.invoke(tableView, cellForRowAtIndexPath)
        }

        val reuseId = "DefaultDslCell"
        var cell = tableView.dequeueReusableCellWithIdentifier(reuseId)
        if (cell == null) {
            cell = UITableViewCell(style = UITableViewCellStyle.UITableViewCellStyleSubtitle, reuseIdentifier = reuseId)
        }

        cell.textLabel?.setText(item.title)
        cell.detailTextLabel?.setText(item.subtitle ?: "")
        cell.imageView?.setImage(item.icon)
        cell.setAccessoryType(item.accessoryType)
        item.titleColor?.let { cell.textLabel?.setTextColor(it) }
        item.subtitleColor?.let { cell.detailTextLabel?.setTextColor(it) }
        item.titleFont?.let { cell.textLabel?.setFont(it) }
        item.subtitleFont?.let { cell.detailTextLabel?.setFont(it) }
        item.backgroundColor?.let { cell.setBackgroundColor(it) }
        item.tintColor?.let { cell.setTintColor(it) }
        cell.setUserInteractionEnabled(item.isEnabled)

        return cell
    }

    @ObjCSignatureOverride
    override fun tableView(tableView: UITableView, titleForHeaderInSection: Long): String? {
        return sections.getOrNull(titleForHeaderInSection.toInt())?.headerTitle
    }

    @ObjCSignatureOverride
    override fun tableView(tableView: UITableView, titleForFooterInSection: Long): String? {
        return sections.getOrNull(titleForFooterInSection.toInt())?.footerTitle
    }

    @ObjCSignatureOverride
    override fun tableView(tableView: UITableView, didSelectRowAtIndexPath: NSIndexPath) {
        tableView.deselectRowAtIndexPath(didSelectRowAtIndexPath, animated = true)
        val sectionIndex = didSelectRowAtIndexPath.section.toInt()
        val rowIndex = didSelectRowAtIndexPath.row.toInt()
        val item = sections.getOrNull(sectionIndex)?.items?.getOrNull(rowIndex)
        item?.onSelect?.invoke()
    }
}

private val tableViewBridgeKey: COpaquePointer = nativeHeap.alloc<ByteVar>().ptr

@UIKitDsl
class ListItemsBuilder(
    val style: UITableViewStyle = UITableViewStyle.UITableViewStylePlain
) {
    val tableView: UITableView = UITableView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), style = style)

    var backgroundColor: UIColor? = null
    var separatorStyle: UITableViewCellSeparatorStyle? = null
    var separatorColor: UIColor? = null
    var isScrollEnabled: Boolean = true
    var rowHeight: Double = UITableViewAutomaticDimension
    var estimatedRowHeight: Double = 44.0
    var tableHeaderView: UIView? = null
    var tableFooterView: UIView? = null
    var height: Double? = null

    private val sectionBuilders = mutableListOf<ListSectionBuilder>()
    private val defaultSection = ListSectionBuilder()

    fun section(
        header: String? = null,
        footer: String? = null,
        builder: ListSectionBuilder.() -> Unit
    ) {
        val s = ListSectionBuilder(header, footer)
        s.builder()
        sectionBuilders.add(s)
    }

    fun item(
        title: String,
        subtitle: String? = null,
        icon: UIImage? = null,
        builder: ListItemCellBuilder.() -> Unit = {}
    ) {
        defaultSection.item(title, subtitle, icon, builder)
    }

    fun <T> items(
        itemList: Iterable<T>,
        itemContent: ListItemCellBuilder.(T) -> Unit
    ) {
        defaultSection.items(itemList, itemContent)
    }

    fun customItem(
        onSelect: (() -> Unit)? = null,
        cellProvider: (UITableView, NSIndexPath) -> UITableViewCell
    ) {
        defaultSection.customItem(onSelect, cellProvider)
    }

    fun build(): UITableView {
        backgroundColor?.let { tableView.setBackgroundColor(it) }
        separatorStyle?.let { tableView.setSeparatorStyle(it) }
        separatorColor?.let { tableView.setSeparatorColor(it) }
        tableView.setScrollEnabled(isScrollEnabled)
        tableView.setRowHeight(rowHeight)
        tableView.setEstimatedRowHeight(estimatedRowHeight)
        tableHeaderView?.let { tableView.setTableHeaderView(it) }
        tableFooterView?.let { tableView.setTableFooterView(it) }

        val allSections = if (sectionBuilders.isNotEmpty()) {
            val list = mutableListOf<ListSection>()
            if (defaultSection.items.isNotEmpty()) {
                list.add(
                    ListSection(
                        defaultSection.headerTitle,
                        defaultSection.footerTitle,
                        defaultSection.items.toList()
                    )
                )
            }
            list.addAll(sectionBuilders.map { ListSection(it.headerTitle, it.footerTitle, it.items.toList()) })
            list
        } else {
            listOf(ListSection(defaultSection.headerTitle, defaultSection.footerTitle, defaultSection.items.toList()))
        }

        val bridge = TableViewBridge(allSections)
        tableView.setDataSource(bridge)
        tableView.setDelegate(bridge)
        objc_setAssociatedObject(tableView, tableViewBridgeKey, bridge, OBJC_ASSOCIATION_RETAIN_NONATOMIC)

        val totalItems = allSections.sumOf { it.items.size }
        val effectiveHeight = height ?: if (!isScrollEnabled) {
            val itemHeight = if (rowHeight > 0.0 && rowHeight != UITableViewAutomaticDimension) rowHeight else (if (estimatedRowHeight > 0.0) estimatedRowHeight else 44.0)
            (totalItems * itemHeight).coerceAtLeast(1.0)
        } else null

        effectiveHeight?.let {
            tableView.heightAnchor.constraintEqualToConstant(it).setActive(true)
        }

        return tableView
    }
}

inline fun listView(
    style: UITableViewStyle = UITableViewStyle.UITableViewStylePlain,
    builder: ListItemsBuilder.() -> Unit
): UITableView {
    val b = ListItemsBuilder(style)
    b.builder()
    return b.build()
}
