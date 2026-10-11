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
    var tintColor: UIColor? = null,
    var iconTintColor: UIColor? = null,
    var onSelect: (() -> Unit)? = null
)

@UIKitDsl
class ListItemCellBuilder {
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
            title = title,
            subtitle = subtitle,
            icon = icon,
            accessoryType = accessoryType,
            tintColor = tintColor,
            iconTintColor = iconTintColor,
            onSelect = selectAction
        )
    }

    fun buildCell(reuseIdentifier: String? = "ListItemCell"): UITableViewCell {
        val cell = UITableViewCell(
            style = UITableViewCellStyle.UITableViewCellStyleSubtitle,
            reuseIdentifier = reuseIdentifier
        )
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
        tintColor?.let { cell.setTintColor(it) }
        cell.setUserInteractionEnabled(true)
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

        val reuseId = "DefaultDslCell"
        val cell = tableView.dequeueReusableCellWithIdentifier(reuseId)
            ?: UITableViewCell(
                style = UITableViewCellStyle.UITableViewCellStyleSubtitle,
                reuseIdentifier = reuseId
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

    private val backgroundColor = UIColor.systemBackgroundColor
    private val separatorStyle = UITableViewCellSeparatorStyle.UITableViewCellSeparatorStyleSingleLine
    var isScrollEnabled = true
    var rowHeight = UITableViewAutomaticDimension
    var estimatedRowHeight = 44.0

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
        tableView.setBackgroundColor(backgroundColor)
        tableView.setSeparatorStyle(separatorStyle)
        tableView.setScrollEnabled(isScrollEnabled)
        tableView.setRowHeight(rowHeight)
        tableView.setEstimatedRowHeight(estimatedRowHeight)

        val bridge = TableViewBridge(allItems, sectionTitlesByFirstLetter)
        tableView.setDataSource(bridge)
        tableView.setDelegate(bridge)
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
