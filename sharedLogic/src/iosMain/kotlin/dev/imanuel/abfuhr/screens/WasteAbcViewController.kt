package dev.imanuel.abfuhr.screens

import dev.imanuel.abfuhr.AbfuhrNavDestination
import dev.imanuel.abfuhr.models.AbfallAbcWaste
import dev.imanuel.abfuhr.search.SearchClient
import dev.imanuel.abfuhr.uikit.dsl.listView
import kotlinx.coroutines.*
import org.koin.mp.KoinPlatformTools
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.UIKit.*
import platform.darwin.NSObject

private val validLanguages = listOf(
    "de",
    "en",
    "fr",
    "ar",
    "ku",
)

private class WasteAbcSearchUpdaterBridge(
    private val onQueryChanged: (String) -> Unit
) : NSObject(), UISearchResultsUpdatingProtocol {
    override fun updateSearchResultsForSearchController(searchController: UISearchController) {
        val text = searchController.searchBar.text ?: ""
        onQueryChanged(text)
    }
}

class WasteAbcViewController : UIViewController(nibName = null, bundle = null) {
    init {
        tabBarItem = UITabBarItem(
            title = AbfuhrNavDestination.WasteAbc.title,
            image = AbfuhrNavDestination.WasteAbc.createIcon(),
            tag = AbfuhrNavDestination.WasteAbc.ordinal.toLong()
        )
    }

    private lateinit var searchController: UISearchController
    private lateinit var searchUpdater: WasteAbcSearchUpdaterBridge
    private var resultsView: UIView? = null

    private var searchJob: Job? = null
    private var searchResults: List<AbfallAbcWaste> = emptyList()

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val currentLanguage: String
        get() = if (validLanguages.contains(NSLocale.currentLocale.languageCode)) NSLocale.currentLocale.languageCode else "de"

    private val searchClient: SearchClient
        get() = KoinPlatformTools.defaultContext().get().get()

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.setBackgroundColor(UIColor.systemBackgroundColor())

        setupSearchBar()
        updateSearchResults("")
        populateList()
    }

    private fun populateList() {
        val newResultsView = listView(UITableViewStyle.UITableViewStyleGrouped) {
            backgroundColor = UIColor.systemBackgroundColor()
            separatorStyle = UITableViewCellSeparatorStyle.UITableViewCellSeparatorStyleSingleLine
            rowHeight = UITableViewAutomaticDimension
            estimatedRowHeight = 72.0

            for (waste in searchResults.map { it.copy(title = it.title.trim()) }) {
                item(waste.title) {
                    accessoryType = UITableViewCellAccessoryType.UITableViewCellAccessoryDisclosureIndicator
                    onSelect {
                        navigationController?.pushViewController(
                            createWasteAbcDetailViewController(waste), animated = true
                        )
                    }
                }
            }
        }

        newResultsView.setTranslatesAutoresizingMaskIntoConstraints(false)

        resultsView?.removeFromSuperview()
        resultsView = newResultsView
        view.addSubview(newResultsView)

        NSLayoutConstraint.activateConstraints(
            listOf(
                newResultsView.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor),
                newResultsView.bottomAnchor.constraintEqualToAnchor(view.bottomAnchor),
                newResultsView.leadingAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.leadingAnchor),
                newResultsView.trailingAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.trailingAnchor),
            )
        )
    }

    private fun updateSearchResults(query: String) {
        searchJob?.cancel()
        searchJob = ioScope.launch {
            val trimmed = query.trim()
            searchResults = searchClient.searchAbfallAbc(trimmed, currentLanguage)
            mainScope.launch {
                populateList()
            }
        }
    }

    private fun setupSearchBar() {
        searchUpdater = WasteAbcSearchUpdaterBridge { query ->
            updateSearchResults(query)
        }

        searchController = UISearchController(searchResultsController = null).apply {
            setSearchResultsUpdater(searchUpdater)
            setObscuresBackgroundDuringPresentation(false)
            searchBar.placeholder = "Suchen"
        }

        navigationItem.searchController = searchController
        navigationItem.hidesSearchBarWhenScrolling = true

        definesPresentationContext = true
    }
}

fun createWasteAbcViewController() = WasteAbcViewController()
