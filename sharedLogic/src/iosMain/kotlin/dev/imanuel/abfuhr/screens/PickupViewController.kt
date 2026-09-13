@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package dev.imanuel.abfuhr.screens

import dev.imanuel.abfuhr.AbfuhrNavDestination
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSSelectorFromString
import platform.UIKit.*
import platform.darwin.NSObject

class PickupViewController : UIViewController(nibName = null, bundle = null) {

    init {
        tabBarItem = UITabBarItem(
            title = AbfuhrNavDestination.WasteAbc.title,
            image = AbfuhrNavDestination.WasteAbc.createIcon(),
            tag = AbfuhrNavDestination.WasteAbc.ordinal.toLong()
        )
    }

    private lateinit var searchController: UISearchController
    private lateinit var searchUpdater: PickupSearchUpdaterBridge

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.setBackgroundColor(UIColor.systemBackgroundColor())

        setupSearchBar()
    }

    @ObjCAction
    fun onLocationTapped() {}

    private fun setupSearchBar() {
        // Bridge to receive text updates (plug in your filtering here later)
        searchUpdater = PickupSearchUpdaterBridge { query ->
            // TODO: filter your list using 'query'
        }

        searchController = UISearchController(searchResultsController = null).apply {
            setSearchResultsUpdater(searchUpdater)
            setObscuresBackgroundDuringPresentation(false)
            searchBar.placeholder = "Adresse finden"
        }

        // Show the search bar in the navigation bar area
        navigationItem.searchController = searchController
        navigationItem.hidesSearchBarWhenScrolling = true
        navigationItem.rightBarButtonItem = UIBarButtonItem(
            image = UIImage.systemImageNamed("location"),
            style = UIBarButtonItemStyle.UIBarButtonItemStylePlain,
            target = this,
            action = NSSelectorFromString("onLocationTapped"),
        )

        // Ensure presentation context is confined to this VC
        definesPresentationContext = true
    }
}

fun createPickupViewController() = PickupViewController()

private class PickupSearchUpdaterBridge(
    private val onQueryChanged: (String) -> Unit
) : NSObject(), UISearchResultsUpdatingProtocol {
    override fun updateSearchResultsForSearchController(searchController: UISearchController) {
        val text = searchController.searchBar.text ?: ""
        onQueryChanged(text)
    }
}