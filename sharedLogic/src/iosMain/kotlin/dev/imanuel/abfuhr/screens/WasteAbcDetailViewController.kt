package dev.imanuel.abfuhr.screens

import dev.imanuel.abfuhr.AbfuhrNavDestination
import dev.imanuel.abfuhr.models.AbfallAbcWaste
import dev.imanuel.abfuhr.uikit.dsl.scrollableColumn
import platform.UIKit.*

class WasteAbcDetailViewController(
    private val waste: AbfallAbcWaste
) : UIViewController(nibName = null, bundle = null) {
    init {
        tabBarItem = UITabBarItem(
            title = AbfuhrNavDestination.WasteAbc.title,
            image = AbfuhrNavDestination.WasteAbc.createIcon(),
            tag = AbfuhrNavDestination.WasteAbc.ordinal.toLong()
        )
        title = waste.title
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.systemBackgroundColor()

        val detailsView = scrollableColumn {
            textView(waste.description.trim()) {
                padding(8.0, 16.0)
                isSelectable = true
                font = UIFont.systemFontOfSize(UIFont.systemFontSize)
            }
            if (waste.tips.isNotEmpty()) {
                textView("Tipps") {
                    padding(16.0, 8.0, 0.0, 8.0)
                    font = UIFont.systemFontOfSize(UIFont.labelFontSize)
                }
                textView(waste.tips.joinToString("\n")) {
                    padding(8.0, 0.0)
                    isSelectable = true
                    font = UIFont.systemFontOfSize(UIFont.systemFontSize)
                }
            }
            textView("Entsorgung") {
                padding(16.0, 8.0, 0.0, 8.0)
                font = UIFont.systemFontOfSize(UIFont.labelFontSize)
            }
            listView {
                isScrollEnabled = false
                rowHeight = 44.0

                val routes = waste.routes.flatMap {
                    listOfNotNull(it.alternativeRoute, it.collection, it.dischargePoint)
                }
                for (route in routes) {
                    item(route.title.trim()) {
                        accessoryType = UITableViewCellAccessoryType.UITableViewCellAccessoryDisclosureIndicator

                        onSelect {
                            navigationController?.pushViewController(
                                createWasteAbcRouteViewController(route, waste.language), animated = true
                            )
                        }
                    }
                }
            }
        }

        view.addSubview(detailsView)

        NSLayoutConstraint.activateConstraints(
            listOf(
                detailsView.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor),
                detailsView.bottomAnchor.constraintEqualToAnchor(view.bottomAnchor),
                detailsView.leadingAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.leadingAnchor),
                detailsView.trailingAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.trailingAnchor),
            )
        )
    }
}

fun createWasteAbcDetailViewController(waste: AbfallAbcWaste) = WasteAbcDetailViewController(waste)