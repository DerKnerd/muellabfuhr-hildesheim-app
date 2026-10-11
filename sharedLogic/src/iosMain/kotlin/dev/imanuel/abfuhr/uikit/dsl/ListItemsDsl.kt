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
    var item: Any? = null,
    var title: String,
    var subtitle: String? = null,
    var icon: UIImage? = null,
    var accessoryType: UITableViewCellAccessoryType = UITableViewCellAccessoryType.UITableViewCellAccessoryNone,
    var tintColor: UIColor? = null,
    var iconTintColor: UIColor? = null,
    var onSelect: (() -> Unit)? = null
)

@UIKitDsl
class ListItemCellBuilder {
    var item: Any? = null
    var title: String = ""
    var subtitle: String? = null
    var icon: UIImage? = null
    var accessoryType: UITableViewCellAccessoryType = UITableViewCellAccessoryType.UITableViewCellAccessoryNone
    var tintColor: UIColor? = null
    var iconTintColor: UIColor? = null

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
            item = item,
            title = title,
            subtitle = subtitle,
            icon = icon,
            accessoryType = accessoryType,
            tintColor = tintColor,
            iconTintColor = iconTintColor,
            onSelect = selectAction
        )
    }
}

class TableViewBridge(
    items: List<ListItemModel>,
    sectionTitlesByFirstLetter: Boolean,
    private val deleteTitle: String? = null,
    private val deleteIcon: UIImage? = null,
    private val onDelete: ((Any?) -> Unit)? = null
) : NSObject(), UITableViewDataSourceProtocol, UITableViewDelegateProtocol {

    private val groupedSections = if (sectionTitlesByFirstLetter) {
        items.groupBy { it.title.firstOrNull()?.uppercase() ?: "#" }.toList().sortedBy { it.first }
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

    private fun itemAt(indexPath: NSIndexPath) =
        groupedSections.getOrNull(indexPath.section.toInt())?.second?.getOrNull(indexPath.row.toInt())

    override fun numberOfSectionsInTableView(tableView: UITableView) = groupedSections.size.toLong()

    @ObjCSignatureOverride
    override fun tableView(tableView: UITableView, numberOfRowsInSection: Long) =
        groupedSections.getOrNull(numberOfRowsInSection.toInt())?.second?.size?.toLong() ?: 0L

    override fun sectionIndexTitlesForTableView(tableView: UITableView) = sectionTitles

    @ObjCSignatureOverride
    override fun tableView(tableView: UITableView, titleForHeaderInSection: Long) =
        sectionTitles.getOrNull(titleForHeaderInSection.toInt())

    @ObjCSignatureOverride
    override fun tableView(tableView: UITableView, sectionForSectionIndexTitle: String, atIndex: NSInteger) =
        sectionTitles.indexOf(sectionForSectionIndexTitle).takeIf { it >= 0 }?.toLong() ?: 0L

    @ObjCSignatureOverride
    override fun tableView(tableView: UITableView, cellForRowAtIndexPath: NSIndexPath): UITableViewCell {
        val item = itemAt(cellForRowAtIndexPath) ?: return UITableViewCell()

        val reuseId = "DefaultDslCell"
        val cell = tableView.dequeueReusableCellWithIdentifier(reuseId) ?: UITableViewCell(
            style = UITableViewCellStyle.UITableViewCellStyleSubtitle, reuseIdentifier = reuseId
        )

        cell.textLabel?.apply {
            text = item.title
            textColor = UIColor.labelColor
            font = UIFont.preferredFontForTextStyle(UIFontTextStyleBody)
        }

        cell.detailTextLabel?.apply {
            text = item.subtitle
            textColor = UIColor.secondaryLabelColor
            font = UIFont.preferredFontForTextStyle(UIFontTextStyleSubheadline)
        }

        cell.imageView?.apply {
            image = if (item.iconTintColor != null) {
                item.icon?.imageWithRenderingMode(UIImageRenderingMode.UIImageRenderingModeAlwaysTemplate)
            } else {
                item.icon
            }
            tintColor = item.iconTintColor ?: UIColor.labelColor
        }

        cell.apply {
            accessoryType = item.accessoryType
            backgroundColor = UIColor.clearColor
            tintColor = item.tintColor ?: UIColor.systemBlueColor
            userInteractionEnabled = true
        }

        return cell
    }

    @ObjCSignatureOverride
    override fun tableView(tableView: UITableView, didSelectRowAtIndexPath: NSIndexPath) {
        tableView.deselectRowAtIndexPath(
            didSelectRowAtIndexPath, animated = true
        )

        itemAt(didSelectRowAtIndexPath)?.onSelect?.invoke()
    }

    @ObjCSignatureOverride
    override fun tableView(
        tableView: UITableView, trailingSwipeActionsConfigurationForRowAtIndexPath: NSIndexPath
    ): UISwipeActionsConfiguration? {
        val item = itemAt(trailingSwipeActionsConfigurationForRowAtIndexPath) ?: return null
        val deleteAction = onDelete ?: return null

        val action = UIContextualAction.contextualActionWithStyle(
            style = UIContextualActionStyle.UIContextualActionStyleDestructive, title = deleteTitle ?: "Löschen"
        ) { _, _, completionHandler ->
            onDelete(item.item)
            completionHandler?.invoke(true)
        }

        action.image = deleteIcon ?: UIImage.systemImageNamed("trash")
        action.backgroundColor = UIColor.systemRedColor

        return UISwipeActionsConfiguration.configurationWithActions(listOf(action)).apply {
            performsFirstActionWithFullSwipe = true
        }
    }
}

private val tableViewBridgeKey: COpaquePointer = nativeHeap.alloc<ByteVar>().ptr

@UIKitDsl
class ListItemsBuilder(
    val style: UITableViewStyle = UITableViewStyle.UITableViewStylePlain,
    val sectionTitlesByFirstLetter: Boolean,
) {
    val tableView: UITableView = UITableView(frame = CGRectZero.readValue(), style = style)

    private val backgroundColor = UIColor.systemBackgroundColor
    private val separatorStyle = UITableViewCellSeparatorStyle.UITableViewCellSeparatorStyleSingleLine
    var isScrollEnabled = true
    var rowHeight = UITableViewAutomaticDimension
    var estimatedRowHeight = 44.0

    var deleteTitle: String? = null
    var deleteIcon: UIImage? = null
    var onDelete: ((Any?) -> Unit)? = null

    private var allItems = mutableListOf<ListItemModel>()

    fun item(
        title: String,
        subtitle: String? = null,
        icon: UIImage? = null,
        item: Any? = null,
        builder: ListItemCellBuilder.() -> Unit = {}
    ) {
        val b = ListItemCellBuilder()
        b.item = item
        b.title = title
        b.subtitle = subtitle
        b.icon = icon
        b.builder()
        allItems.add(b.buildModel())
    }

    fun build(): UITableView {
        tableView.backgroundColor = backgroundColor
        tableView.separatorStyle = separatorStyle
        tableView.scrollEnabled = isScrollEnabled
        tableView.rowHeight = rowHeight
        tableView.estimatedRowHeight = estimatedRowHeight

        val bridge = TableViewBridge(allItems, sectionTitlesByFirstLetter, deleteTitle, deleteIcon, onDelete)
        tableView.dataSource = bridge
        tableView.delegate = bridge
        objc_setAssociatedObject(tableView, tableViewBridgeKey, bridge, OBJC_ASSOCIATION_RETAIN_NONATOMIC)

        val totalItems = allItems.size
        val effectiveHeight = if (!isScrollEnabled) {
            val itemHeight = if (rowHeight > 0.0 && rowHeight != UITableViewAutomaticDimension) {
                rowHeight
            } else if (estimatedRowHeight > 0.0) {
                estimatedRowHeight
            } else {
                44.0
            }
            (totalItems * itemHeight).coerceAtLeast(1.0)
        } else null

        effectiveHeight?.let {
            tableView.heightAnchor.constraintEqualToConstant(it).active = true
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
