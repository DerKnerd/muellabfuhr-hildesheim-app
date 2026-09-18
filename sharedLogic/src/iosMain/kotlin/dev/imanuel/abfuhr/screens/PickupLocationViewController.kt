@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package dev.imanuel.abfuhr.screens

import dev.imanuel.abfuhr.AbfuhrNavDestination
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.geo.checkIfLocationInHildesheim
import dev.imanuel.abfuhr.models.AbfuhrLocation
import dev.imanuel.abfuhr.models.AbfuhrPickup
import dev.imanuel.abfuhr.search.SearchClient
import dev.imanuel.abfuhr.uikit.dsl.ProgressIndicatorStyle
import dev.imanuel.abfuhr.uikit.dsl.activityIndicator
import dev.imanuel.abfuhr.uikit.dsl.column
import dev.imanuel.abfuhr.uikit.dsl.listView
import dev.imanuel.abfuhr.uikit.dsl.showAlert
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.datetime.toNSDate
import org.koin.mp.KoinPlatformTools
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject
import kotlin.time.Clock
import kotlin.time.Instant

data class NextPickup(
    val date: Long,
    val type: String
)

data class LocationWithNextPickups(
    val streetId: Long,
    val street: String,
    val locality: String,
    val localityId: Long,
    val district: String,
    val districtId: Long,
    val streetLatitude: Double,
    val streetLongitude: Double,
    val hasReminder: Long,
    val nextPickups: List<NextPickup>
)

class PickupManagerDelegateBridge(
    private val onLocationUpdated: (CLLocation) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {

    @ObjCSignatureOverride
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val firstLocation = didUpdateLocations.firstOrNull() as? CLLocation ?: return
        onLocationUpdated(firstLocation)
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

    private lateinit var searchController: UISearchController
    private lateinit var searchUpdater: PickupSearchUpdaterBridge
    private lateinit var locationManagerDelegateBridge: PickupManagerDelegateBridge

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

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        setupPageView()
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.setBackgroundColor(UIColor.systemBackgroundColor())

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
                    searchJob?.cancel()
                    searchJob = ioScope.launch {
                        filteredLocations = searchClient.searchAbfuhrByGeolocation(latitude, longitude)
                        mainScope.launch {
                            populateSearchList()
                        }
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

    private fun reloadReminder() {
        locationsWithReminder = database.abfuhrQueries.getLocationsWithReminder().executeAsList()
        searchJob?.cancel()
        searchJob = ioScope.launch {
            filteredLocations = searchClient.searchAbfuhr("")
        }
    }

    private fun populateSearchList() {
        val newResultsView = listView(UITableViewStyle.UITableViewStyleGrouped) {
            backgroundColor = UIColor.systemBackgroundColor()
            separatorStyle = UITableViewCellSeparatorStyle.UITableViewCellSeparatorStyleSingleLine
            rowHeight = UITableViewAutomaticDimension
            estimatedRowHeight = 72.0

            val formatter = NSDateFormatter().apply {
                dateStyle = NSDateFormatterMediumStyle
                timeStyle = NSDateFormatterNoStyle
                locale = NSLocale.currentLocale
                timeZone = NSTimeZone.localTimeZone
            }

            loop@ for (location in filteredLocations) {
                val nextPickups = location.pickups.filter { it.date >= Clock.System.now() }
                val date = formatter.stringFromDate(nextPickups.first().date.toNSDate())

                val nextPickupLine = if (nextPickups.size > 1) {
                    val cans = nextPickups.map {
                        when (it.type) {
                            "B" -> "die Biotonne"
                            "R", "S" -> "die Restmülltonne"
                            "G" -> "die gelbe Tonne"
                            "P" -> "die Papiertonne"
                            else -> return@map ""
                        }
                    }
                    "Als nächstes sind ${cans.joinToString(" und ")} am $date dran"
                } else {
                    val trashCan = when (nextPickups.first().type) {
                        "B" -> "Biotonne"
                        "R", "S" -> "Restmülltonne"
                        "G" -> "gelbe Tonne"
                        "P" -> "Papiertonne"
                        else -> ""
                    }
                    if (trashCan.isEmpty()) "" else "Als nächstes ist die $trashCan am $date dran"
                }
                item(
                    buildString {
                        append(location.street)
                        if (location.locality == "Hildesheim") {
                            append(", Hildesheim")
                        } else {
                            append(", ")
                            append(location.district)
                        }
                    },
                    subtitle = nextPickupLine
                ) {
                    accessoryType = UITableViewCellAccessoryType.UITableViewCellAccessoryDisclosureIndicator
                    onSelect {
                        navigationController?.pushViewController(
                            createPickupDetailViewController(location),
                            true
                        )
                    }
                }
            }
        }

        newResultsView.setTranslatesAutoresizingMaskIntoConstraints(false)

        resultsView?.removeFromSuperview()
        resultsView = newResultsView
        pageView?.addArrangedSubview(newResultsView)
        showSearchBar()
    }

    private fun populateReminderList() {
        val newResultsView = listView(UITableViewStyle.UITableViewStyleGrouped) {
            backgroundColor = UIColor.systemBackgroundColor()
            separatorStyle = UITableViewCellSeparatorStyle.UITableViewCellSeparatorStyleSingleLine
            rowHeight = UITableViewAutomaticDimension
            estimatedRowHeight = 72.0

            val formatter = NSDateFormatter().apply {
                dateStyle = NSDateFormatterMediumStyle
                timeStyle = NSDateFormatterNoStyle
                locale = NSLocale.currentLocale
                timeZone = NSTimeZone.localTimeZone
            }

            loop@ for (location in locationsWithReminder) {
                val nextPickups = database.abfuhrQueries.getPickupsByStreetId(location.streetId).executeAsList()
                    .filter { it.date >= Clock.System.now().toEpochMilliseconds() }

                val date = formatter.stringFromDate(
                    Instant.fromEpochMilliseconds(nextPickups.first().date).toNSDate()
                )

                val nextPickupLine = if (nextPickups.size > 1) {
                    val cans = nextPickups.map {
                        when (it.type) {
                            "B" -> "die Biotonne"
                            "R" -> "die Restmülltonne"
                            "G" -> "die gelbe Tonne"
                            "P" -> "die Papiertonne"
                            else -> return@map ""
                        }
                    }
                    "Als nächstes sind ${cans.joinToString(" und ")} am $date dran"
                } else {
                    val trashCan = when (nextPickups.first().type) {
                        "B" -> "Biotonne"
                        "R" -> "Restmülltonne"
                        "G" -> "gelbe Tonne"
                        "P" -> "Papiertonne"
                        else -> ""
                    }
                    if (trashCan.isEmpty()) "" else "Als nächstes ist die $trashCan am $date dran"
                }
                item(
                    buildString {
                        append(location.street)
                        if (location.locality == "Hildesheim") {
                            append(", Hildesheim")
                        } else {
                            append(", ")
                            append(location.district)
                        }
                    },
                    subtitle = nextPickupLine
                ) {
                    accessoryType = UITableViewCellAccessoryType.UITableViewCellAccessoryDisclosureIndicator
                    onSelect {
                        navigationController?.pushViewController(
                            createPickupDetailViewController(
                                AbfuhrLocation(
                                    street = location.street,
                                    streetId = location.streetId,
                                    locality = location.locality,
                                    localityId = location.localityId,
                                    district = location.district,
                                    districtId = location.districtId,
                                    streetLatitude = location.streetLatitude,
                                    streetLongitude = location.streetLongitude,
                                    pickups = nextPickups.map {
                                        AbfuhrPickup(
                                            streetId = it.streetId,
                                            date = Instant.fromEpochMilliseconds(it.date),
                                            isPostponed = it.isPostponed == 1L,
                                            type = it.type,
                                        )
                                    },
                                )
                            ),
                            true,
                        )
                    }
                }
            }
        }

        newResultsView.setTranslatesAutoresizingMaskIntoConstraints(false)

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
        val newPageView = column {
            if (locationsWithReminder.isNotEmpty()) {
                column {
                    padding(horizontal = 16.0, vertical = 0.0)
                    add(UISegmentedControl(listOf("Suche", "Gemerkte Adressen")).apply {
                        selectedSegmentIndex = this@PickupViewController.selectedSegmentIndex
                        addTarget(
                            target = this@PickupViewController,
                            action = NSSelectorFromString("onSegmentChanged:"),
                            forControlEvents = UIControlEventValueChanged
                        )
                    })
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
            searchJob?.cancel()
            searchJob = ioScope.launch {
                filteredLocations = searchClient.searchAbfuhr("")
                mainScope.launch {
                    loader.stopAnimating()
                    loader.removeFromSuperview()
                    populateSearchList()
                    showSearchBar()
                }
            }
        } else {
            populateReminderList()
            hideSearchBar()
        }
    }

    private fun hideSearchBar() {
        if (::searchController.isInitialized) {
            if (searchController.isActive()) {
                searchController.setActive(false)
            }
            searchController.searchBar.resignFirstResponder()
        }
        navigationItem.searchController = null
        navigationItem.rightBarButtonItem = null
    }

    private fun showSearchBar() {
        if (!::searchController.isInitialized) {
            searchUpdater = PickupSearchUpdaterBridge { query ->
                searchJob?.cancel()
                searchJob = ioScope.launch {
                    filteredLocations = searchClient.searchAbfuhr(query)
                    mainScope.launch {
                        populateSearchList()
                    }
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