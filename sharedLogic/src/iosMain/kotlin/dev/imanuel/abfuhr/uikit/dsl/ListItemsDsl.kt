@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import kotlinx.cinterop.*
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSIndexPath
import platform.UIKit.*
import platform.darwin.NSInteger
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
    var iconTintColor: UIColor? = null,
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
    var iconTintColor: UIColor? = null
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
            iconTintColor = iconTintColor,
            isEnabled = isEnabled,
            onSelect = selectAction
        )
    }

    fun buildCell(reuseIdentifier: String? = "ListItemCell"): UITableViewCell {
        val cell = UITableViewCell(style = cellStyle, reuseIdentifier = reuseIdentifier)
        cell.textLabel?.setText(title)
        subtitle?.let { cell.detailTextLabel?.setText(it) }
        icon?.let {
            cell.imageView?.apply {
                if (iconTintColor != null) {
                    image = icon?.imageWithRenderingMode(
                        UIImageRenderingMode.UIImageRenderingModeAlwaysTemplate
                    )
                    tintColor = iconTintColor!!
                } else {
                    image = icon
                }
            }
        }
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

class TableViewBridge(
    items: List<ListItemModel>,
    sectionTitlesByFirstLetter: Boolean
) : NSObject(), UITableViewDataSourceProtocol, UITableViewDelegateProtocol {

    private val groupedSections =
        if (sectionTitlesByFirstLetter) {
            items
                .groupBy { it.title.firstOrNull()?.uppercase() ?: "#" }
                .toList()
                .sortedBy { it.first }
                .map { (letter, entries) ->
                    letter to entries.sortedBy { it.title }
                }
        } else {
            listOf("" to items)
        }

    private val sectionTitles = if (sectionTitlesByFirstLetter) {
        groupedSections.map { it.first }
    } else {
        emptyList()
    }

    private fun itemAt(indexPath: NSIndexPath) = groupedSections
        .getOrNull(indexPath.section.toInt())
        ?.second
        ?.getOrNull(indexPath.row.toInt())

    override fun numberOfSectionsInTableView(tableView: UITableView) = groupedSections.size.toLong()

    @ObjCSignatureOverride
    override fun tableView(tableView: UITableView, numberOfRowsInSection: Long) = groupedSections
        .getOrNull(numberOfRowsInSection.toInt())
        ?.second
        ?.size
        ?.toLong() ?: 0L

    override fun sectionIndexTitlesForTableView(tableView: UITableView) = sectionTitles

    @ObjCSignatureOverride
    override fun tableView(tableView: UITableView, titleForHeaderInSection: Long) =
        sectionTitles.getOrNull(titleForHeaderInSection.toInt())

    @ObjCSignatureOverride
    override fun tableView(tableView: UITableView, sectionForSectionIndexTitle: String, atIndex: NSInteger) =
        sectionTitles
            .indexOf(sectionForSectionIndexTitle)
            .takeIf { it >= 0 }
            ?.toLong() ?: 0L

    @ObjCSignatureOverride
    override fun tableView(tableView: UITableView, cellForRowAtIndexPath: NSIndexPath): UITableViewCell {
        val item = itemAt(cellForRowAtIndexPath) ?: return UITableViewCell()

        item.customCellProvider?.let {
            return it(tableView, cellForRowAtIndexPath)
        }

        val reuseId = "DefaultDslCell"
        val cell = tableView.dequeueReusableCellWithIdentifier(reuseId)
            ?: UITableViewCell(
                style = UITableViewCellStyle.UITableViewCellStyleSubtitle,
                reuseIdentifier = reuseId
            )

        cell.textLabel?.apply {
            text = item.title
            textColor = item.titleColor ?: UIColor.labelColor
            font = item.titleFont ?: UIFont.preferredFontForTextStyle(
                UIFontTextStyleBody
            )
        }

        cell.detailTextLabel?.apply {
            text = item.subtitle
            textColor = item.subtitleColor ?: UIColor.secondaryLabelColor
            font = item.subtitleFont ?: UIFont.preferredFontForTextStyle(
                UIFontTextStyleSubheadline
            )
        }

        cell.imageView?.apply {
            image = if (item.iconTintColor != null) {
                item.icon?.imageWithRenderingMode(
                    UIImageRenderingMode.UIImageRenderingModeAlwaysTemplate
                )
            } else {
                item.icon
            }
            tintColor = item.iconTintColor ?: UIColor.labelColor
        }

        cell.apply {
            accessoryType = item.accessoryType
            backgroundColor = item.backgroundColor ?: UIColor.clearColor
            tintColor = item.tintColor ?: UIColor.systemBlueColor
            userInteractionEnabled = item.isEnabled
        }

        return cell
    }

    @ObjCSignatureOverride
    override fun tableView(tableView: UITableView, didSelectRowAtIndexPath: NSIndexPath) {
        tableView.deselectRowAtIndexPath(
            didSelectRowAtIndexPath,
            animated = true
        )

        itemAt(didSelectRowAtIndexPath)?.onSelect?.invoke()
    }
}

private val tableViewBridgeKey: COpaquePointer = nativeHeap.alloc<ByteVar>().ptr

@UIKitDsl
class ListItemsBuilder(
    val style: UITableViewStyle = UITableViewStyle.UITableViewStylePlain,
    val sectionTitlesByFirstLetter: Boolean,
) {
    val tableView: UITableView = UITableView(frame = CGRectZero.readValue(), style = style)

    var backgroundColor: UIColor? = null
    var separatorStyle: UITableViewCellSeparatorStyle? = null
    var separatorColor: UIColor? = null
    var isScrollEnabled: Boolean = true
    var rowHeight: Double = UITableViewAutomaticDimension
    var estimatedRowHeight: Double = 44.0
    var tableHeaderView: UIView? = null
    var tableFooterView: UIView? = null
    var height: Double? = null

    private var allItems = mutableListOf<ListItemModel>()

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
        allItems.add(b.buildModel())
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

        val bridge = TableViewBridge(allItems, sectionTitlesByFirstLetter)
        tableView.setDataSource(bridge)
        tableView.setDelegate(bridge)
        objc_setAssociatedObject(tableView, tableViewBridgeKey, bridge, OBJC_ASSOCIATION_RETAIN_NONATOMIC)

        val totalItems = allItems.size
        val effectiveHeight = height ?: if (!isScrollEnabled) {
            val itemHeight =
                if (rowHeight > 0.0 && rowHeight != UITableViewAutomaticDimension) rowHeight else (if (estimatedRowHeight > 0.0) estimatedRowHeight else 44.0)
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
    sectionTitlesByFirstLetter: Boolean = false,
    builder: ListItemsBuilder.() -> Unit
): UITableView {
    val b = ListItemsBuilder(style, sectionTitlesByFirstLetter)
    b.builder()
    return b.build()
}
