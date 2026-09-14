package dev.imanuel.abfuhr.screens

import dev.imanuel.abfuhr.AbfuhrNavDestination
import dev.imanuel.abfuhr.database.AbfallAbcWaste
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.uikit.dsl.scrollableColumn
import org.koin.mp.KoinPlatformTools
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

    private val database: AbfallDatabase
        get() = KoinPlatformTools.defaultContext().get().get()

    override fun viewDidLoad() {
        super.viewDidLoad()
        val routes =
            database
                .abfallAbcQueries
                .getDisposalRouteByWasteId(waste.id, waste.language)
                .executeAsList()
                .distinct()

        val tips = database
            .abfallAbcQueries
            .getWasteTipsByWasteId(waste.id, waste.language)
            .executeAsList()

        view.backgroundColor = UIColor.systemBackgroundColor()

        val detailsView = scrollableColumn {
            textView(waste.description.trim()) {
                padding(8.0, 16.0)
                isSelectable = true
                font = UIFont.systemFontOfSize(UIFont.systemFontSize)
            }
            if (tips.isNotEmpty()) {
                textView("Tipps") {
                    padding(16.0, 8.0, 0.0, 8.0)
                    font = UIFont.systemFontOfSize(UIFont.labelFontSize)
                }
                textView(tips.joinToString("\n")) {
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

                for (route in routes) {
                    item(route.title.trim()) {
                        accessoryType = UITableViewCellAccessoryType.UITableViewCellAccessoryDisclosureIndicator

                        onSelect {
                            navigationController?.pushViewController(
                                createWasteAbcRouteViewController(route, waste.language),
                                animated = true
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