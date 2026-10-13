@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package dev.imanuel.abfuhr.screens

import dev.imanuel.abfuhr.AbfuhrNavDestination
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.geo.checkIfLocationInHildesheim
import dev.imanuel.abfuhr.models.AbfuhrLocation
import dev.imanuel.abfuhr.models.AbfuhrPickup
import dev.imanuel.abfuhr.notifications.dequeuePickups
import dev.imanuel.abfuhr.search.SearchClient
import dev.imanuel.abfuhr.uikit.dsl.activityIndicator
import dev.imanuel.abfuhr.uikit.dsl.column
import dev.imanuel.abfuhr.uikit.dsl.listView
import dev.imanuel.abfuhr.uikit.dsl.showAlert
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.mp.KoinPlatformTools
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSSelectorFromString
import platform.UIKit.*
import platform.darwin.NSObject
import kotlin.time.Clock
import kotlin.time.Instant

class PickupManagerDelegateBridge(
    private val onLocationUpdated: (CLLocation) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {

    @ObjCSignatureOverride
    override fun locationManager(
        manager: CLLocationManager, didUpdateLocations: List<*>
    ) {
        val location = didUpdateLocations.filterIsInstance<CLLocation>().lastOrNull() ?: return

        if (location.horizontalAccuracy !in 0.0..50.0) {
            return
        }

        onLocationUpdated(location)
    }
}

class PickupViewController : UIViewController(nibName = null, bundle = null) {

    init {
        tabBarItem = UITabBarItem(
            title = AbfuhrNavDestination.WasteAbc.title,
            image = AbfuhrNavDestination.WasteAbc.createIcon(),
            tag = AbfuhrNavDestination.WasteAbc.ordinal.toLong()
        )
    }

    private val locationManager: CLLocationManager = CLLocationManager()
    private var resultsView: UIView? = null
    private var pageView: UIStackView? = null
    private var loader: UIActivityIndicatorView = activityIndicator {
        startAnimating()
        style = UIActivityIndicatorViewStyleLarge
    }

    private var searchController: UISearchController? = null
    private lateinit var searchUpdater: PickupSearchUpdaterBridge
    private lateinit var locationManagerDelegateBridge: PickupManagerDelegateBridge

    private enum class SearchMode { ADDRESS, GEOLOCATION }

    private var searchMode = SearchMode.ADDRESS
    private var lastSearchQuery = ""
    private var selectedSegmentIndex = 0L

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var searchJob: Job? = null
    private val searchClient: SearchClient
        get() = KoinPlatformTools.defaultContext().get().get()
    private val database: AbfallDatabase
        get() = KoinPlatformTools.defaultContext().get().get()

    private var locationsWithReminder: List<dev.imanuel.abfuhr.database.AbfuhrLocation> = emptyList()
    private var filteredLocations: List<AbfuhrLocation> = emptyList()
    private var isFiltered = false

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)

        if (pageView == null) {
            setupPageView()
        }
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.systemBackgroundColor

        locationManagerDelegateBridge = PickupManagerDelegateBridge { location ->
            locationManager.stopUpdatingLocation()
            location.coordinate.useContents {
                if (!checkIfLocationInHildesheim(latitude, longitude)) {
                    showAlert(
                        "Nicht in Hildesheim",
                        "Du scheinst nicht im Landkreis Hildesheim zu sein. Bitte suche eine Adresse über das Suchfeld."
                    ) {
                        okAction("Schließen")
                    }
                } else {
                    val lat = latitude
                    val lon = longitude
                    searchMode = SearchMode.GEOLOCATION

                    performSearch(mode = SearchMode.GEOLOCATION) {
                        searchClient.searchAbfuhrByGeolocation(lat, lon)
                    }
                }
            }
        }
        setupPageView()

        definesPresentationContext = true
    }

    @ObjCAction
    fun onLocationTapped() {
        locationManager.run {
            setDelegate(this@PickupViewController.locationManagerDelegateBridge)
            setDesiredAccuracy(kCLLocationAccuracyBest)
            requestWhenInUseAuthorization()
            startUpdatingLocation()
        }
    }

    @ObjCAction
    fun onSegmentChanged(sender: UISegmentedControl) {
        selectedSegmentIndex = sender.selectedSegmentIndex
        if (selectedSegmentIndex == 0L) {
            showSearchBar()
            populateSearchList()
        } else {
            populateReminderList()
        }
    }

    private fun performSearch(
        mode: SearchMode,
        search: suspend () -> List<AbfuhrLocation>
    ) {
        searchJob?.cancel()

        searchJob = mainScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    search()
                }

                ensureActive()
                searchMode = mode
                filteredLocations = results
                isFiltered = mode == SearchMode.GEOLOCATION || lastSearchQuery.isNotBlank()

                println("Search completed: mode=$mode, results=${results.map { it.street }}")
                populateSearchList()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("Pickup search failed: $e")
            }
        }
    }

    private fun reloadReminder() {
        locationsWithReminder = database.abfuhrQueries
            .getLocationsWithReminder()
            .executeAsList()
    }

    private fun nextPickupLine(nextPickups: List<AbfuhrPickup>): String {
        val date = nextPickups.first().date.toLocalDateTime(TimeZone.currentSystemDefault()).let {
            buildString {
                append(it.day + 1)
                append(". ")
                append(
                    when (it.month) {
                        Month.JANUARY -> "Jan"
                        Month.FEBRUARY -> "Feb"
                        Month.MARCH -> "Mär"
                        Month.APRIL -> "Apr"
                        Month.MAY -> "Mai"
                        Month.JUNE -> "Jun"
                        Month.JULY -> "Jul"
                        Month.AUGUST -> "Aug"
                        Month.SEPTEMBER -> "Sep"
                        Month.OCTOBER -> "Okt"
                        Month.NOVEMBER -> "Nov"
                        Month.DECEMBER -> "Dez"
                    }
                )
            }
        }

        val cans = if (nextPickups.size > 1) {
            nextPickups.map {
                when (it.type) {
                    "B" -> "Bio"
                    "R", "S" -> "Restmüll"
                    "G" -> "Gelbe Tonne"
                    "P" -> "Papier"
                    else -> ""
                }
            }
        } else {
            listOf(
                when (nextPickups.first().type) {
                    "B" -> "Bio"
                    "R", "S" -> "Restmüll"
                    "G" -> "Gelbe Tonne"
                    "P" -> "Papier"
                    else -> ""
                }
            )
        }.filter { it.isNotEmpty() }.distinct()

        return "$date: ${cans.joinToString(", ")}"
    }

    private fun populateSearchList() {
        val newResultsView = listView(UITableViewStyle.UITableViewStyleGrouped, !isFiltered) {
            rowHeight = UITableViewAutomaticDimension
            estimatedRowHeight = 72.0

            for (location in filteredLocations) {
                item(
                    buildString {
                        append(location.street)
                        if (location.locality == "Hildesheim") {
                            append(", Hildesheim")
                        } else {
                            append(", ")
                            append(location.district)
                        }
                    }, subtitle = nextPickupLine(location.pickups)
                ) {
                    accessoryType = UITableViewCellAccessoryType.UITableViewCellAccessoryDisclosureIndicator
                    onSelect {
                        navigationController?.pushViewController(
                            createPickupDetailViewController(location), true
                        )
                    }
                }
            }
        }

        newResultsView.translatesAutoresizingMaskIntoConstraints = false

        resultsView?.removeFromSuperview()
        resultsView = newResultsView
        pageView?.addArrangedSubview(newResultsView)
    }

    private fun populateReminderList() {
        val newResultsView = listView(UITableViewStyle.UITableViewStyleGrouped) {
            rowHeight = UITableViewAutomaticDimension
            estimatedRowHeight = 72.0

            deleteIcon = UIImage.systemImageNamed("bell.slash.fill")
            deleteTitle = "Erinnerung löschen"
            onDelete = { streetId ->
                if (streetId is Long) {
                    ioScope.launch {
                        database.abfuhrQueries.unsetReminder(streetId)
                        dequeuePickups(streetId)
                        reloadReminder()
                        mainScope.launch {
                            if (locationsWithReminder.isEmpty()) {
                                populateSearchList()
                                setupPageView()
                            }
                            populateReminderList()
                        }
                    }
                }
            }

            for ((streetId, street, locality, localityId, district, districtId, streetLatitude, streetLongitude) in locationsWithReminder) {
                val nextPickups =
                    database
                        .abfuhrQueries
                        .getPickupsByStreetId(streetId) { streetId, date, isPostponed, type ->
                            AbfuhrPickup(
                                streetId = streetId,
                                date = Instant.fromEpochMilliseconds(date),
                                isPostponed = isPostponed == 1L,
                                type = type,
                            )
                        }
                        .executeAsList()
                        .filter { it.date >= Clock.System.now() }

                item(
                    buildString {
                        append(street)
                        if (locality == "Hildesheim") {
                            append(", Hildesheim")
                        } else {
                            append(", ")
                            append(district)
                        }
                    }, subtitle = nextPickupLine(nextPickups), item = streetId
                ) {
                    accessoryType = UITableViewCellAccessoryType.UITableViewCellAccessoryDisclosureIndicator
                    onSelect {
                        navigationController?.pushViewController(
                            createPickupDetailViewController(
                                AbfuhrLocation(
                                    street = street,
                                    streetId = streetId,
                                    locality = locality,
                                    localityId = localityId,
                                    district = district,
                                    districtId = districtId,
                                    streetLatitude = streetLatitude,
                                    streetLongitude = streetLongitude,
                                    pickups = nextPickups,
                                )
                            ),
                            true,
                        )
                    }
                }
            }
        }

        newResultsView.translatesAutoresizingMaskIntoConstraints = false

        resultsView?.removeFromSuperview()
        resultsView = newResultsView
        pageView?.addArrangedSubview(newResultsView)
        hideSearchBar()
    }

    private fun setupPageView() {
        if (locationsWithReminder.isEmpty()) {
            selectedSegmentIndex = 0L
        }

        reloadReminder()
        var tmpSpacer: UIView? = null
        val newPageView = column {
            if (locationsWithReminder.isNotEmpty()) {
                column {
                    padding(horizontal = 16.0, vertical = 0.0)
                    add(UISegmentedControl(listOf("Suche", "Meine Adressen")).apply {
                        selectedSegmentIndex = this@PickupViewController.selectedSegmentIndex
                        addTarget(
                            target = this@PickupViewController,
                            action = NSSelectorFromString("onSegmentChanged:"),
                            forControlEvents = UIControlEventValueChanged
                        )
                    })
                    tmpSpacer = column { }
                }
            }
        }

        pageView?.removeFromSuperview()
        pageView = newPageView
        view.addSubview(newPageView)
        NSLayoutConstraint.activateConstraints(
            listOf(
                newPageView.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor),
                newPageView.bottomAnchor.constraintEqualToAnchor(view.bottomAnchor),
                newPageView.leadingAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.leadingAnchor),
                newPageView.trailingAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.trailingAnchor),
            )
        )

        pageView!!.addArrangedSubview(loader)
        NSLayoutConstraint.activateConstraints(
            listOf(
                loader.centerXAnchor.constraintEqualToAnchor(pageView!!.centerXAnchor),
                loader.centerYAnchor.constraintEqualToAnchor(pageView!!.centerYAnchor),
            )
        )

        if (selectedSegmentIndex == 0L || locationsWithReminder.isEmpty()) {
            if (searchMode == SearchMode.GEOLOCATION) {
                loader.stopAnimating()
                loader.removeFromSuperview()
                tmpSpacer?.removeFromSuperview()
                populateSearchList()
                showSearchBar()
            } else {
                searchJob?.cancel()
                searchJob = mainScope.launch {
                    try {
                        val results = withContext(Dispatchers.IO) {
                            searchClient.searchAbfuhr("")
                        }

                        ensureActive()
                        filteredLocations = results
                        isFiltered = false

                        loader.stopAnimating()
                        loader.removeFromSuperview()
                        tmpSpacer?.removeFromSuperview()

                        populateSearchList()
                        showSearchBar()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        println("Initial pickup search failed: $e")
                    }
                }
            }
        } else {
            tmpSpacer?.removeFromSuperview()
            populateReminderList()
            hideSearchBar()
        }
    }

    private fun hideSearchBar() {
        searchController?.run {
            if (active) {
                active = false
            }
            searchBar.resignFirstResponder()
        }
        navigationItem.searchController = null
        navigationItem.rightBarButtonItem = null
    }

    private fun showSearchBar() {
        if (searchController == null) {
            searchUpdater = PickupSearchUpdaterBridge { query ->
                if (query == lastSearchQuery) {
                    return@PickupSearchUpdaterBridge
                }

                lastSearchQuery = query
                searchMode = SearchMode.ADDRESS

                performSearch(mode = SearchMode.ADDRESS) {
                    searchClient.searchAbfuhr(query)
                }
            }

            searchController = UISearchController(searchResultsController = null).apply {
                setSearchResultsUpdater(searchUpdater)
                setObscuresBackgroundDuringPresentation(false)
                searchBar.placeholder = "Adresse finden"
            }
        }

        navigationItem.searchController = searchController
        navigationItem.hidesSearchBarWhenScrolling = true
        navigationItem.rightBarButtonItem = UIBarButtonItem(
            image = UIImage.systemImageNamed("location"),
            style = UIBarButtonItemStyle.UIBarButtonItemStylePlain,
            target = this,
            action = NSSelectorFromString("onLocationTapped"),
        )
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