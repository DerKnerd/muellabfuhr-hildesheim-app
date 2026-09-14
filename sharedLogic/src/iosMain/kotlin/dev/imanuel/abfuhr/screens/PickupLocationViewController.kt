@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package dev.imanuel.abfuhr.screens

import dev.imanuel.abfuhr.AbfuhrNavDestination
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.database.AbfuhrLocation
import dev.imanuel.abfuhr.geo.checkIfLocationInHildesheim
import dev.imanuel.abfuhr.uikit.dsl.listView
import dev.imanuel.abfuhr.uikit.dsl.showAlert
import kotlinx.cinterop.*
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

    private lateinit var searchController: UISearchController
    private lateinit var searchUpdater: PickupSearchUpdaterBridge
    private lateinit var locationManagerDelegateBridge: PickupManagerDelegateBridge

    private val database: AbfallDatabase
        get() = KoinPlatformTools.defaultContext().get().get()

    private var locationsWithReminder: List<AbfuhrLocation> = emptyList()
    private var filteredLocations: List<LocationWithNextPickups> = emptyList()

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.setBackgroundColor(UIColor.systemBackgroundColor())

        locationsWithReminder = database.abfuhrQueries.getLocationsWithReminder().executeAsList()
        filteredLocations = database.abfuhrQueries
            .getAllLocationWithNextPickups(Clock.System.now().epochSeconds)
            .executeAsList()
            .groupBy {
                it.streetId
            }
            .map { (key, value) ->
                val locations = value.groupBy { it.date }.minBy { it.key ?: Long.MAX_VALUE }.value
                val first = value.first()
                LocationWithNextPickups(
                    streetId = first.streetId,
                    street = first.street,
                    locality = first.locality,
                    localityId = first.localityId,
                    district = first.district,
                    districtId = first.districtId,
                    streetLatitude = first.streetLatitude,
                    streetLongitude = first.streetLongitude,
                    hasReminder = first.hasReminder,
                    nextPickups = locations.map { NextPickup(it.date!!, it.type) }
                )
            }

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
                    filteredLocations =
                        database
                            .abfuhrQueries
                            .searchLocationWithNextPickupsByGeolocation(
                                latitude,
                                longitude,
                                Clock.System.now().toEpochMilliseconds()
                            )
                            .executeAsList()
                            .groupBy {
                                it.streetId
                            }
                            .map { (key, value) ->
                                val locations = value.groupBy { it.date }.minBy { it.key ?: Long.MAX_VALUE }.value
                                val first = value.first()
                                LocationWithNextPickups(
                                    streetId = first.streetId,
                                    street = first.street,
                                    locality = first.locality,
                                    localityId = first.localityId,
                                    district = first.district,
                                    districtId = first.districtId,
                                    streetLatitude = first.streetLatitude,
                                    streetLongitude = first.streetLongitude,
                                    hasReminder = first.hasReminder,
                                    nextPickups = locations.map { NextPickup(it.date!!, it.type) }
                                )
                            }
                    populateSearchList()
                }
            }
        }
        setupSearchBar()
        populateSearchList()
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
                val date = formatter.stringFromDate(
                    Instant.fromEpochMilliseconds(location.nextPickups.first().date).toNSDate()
                )

                val nextPickupLine = if (location.nextPickups.size > 1) {
                    val cans = location.nextPickups.map {
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
                    val trashCan = when (location.nextPickups.first().type) {
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
                            createPickupDetailViewController(location.streetId),
                            true
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

    private fun setupSearchBar() {
        // Bridge to receive text updates (plug in your filtering here later)
        searchUpdater = PickupSearchUpdaterBridge { query ->
            filteredLocations = if (query.isNotBlank()) {
                database.abfuhrQueries.searchLocationWithNextPickupsByKeyword(
                    query,
                    Clock.System.now().toEpochMilliseconds()
                ).executeAsList()
                    .groupBy {
                        it.streetId
                    }
                    .map { (key, value) ->
                        val locations = value.groupBy { it.date }.minBy { it.key ?: Long.MAX_VALUE }.value
                        val first = value.first()
                        LocationWithNextPickups(
                            streetId = first.streetId,
                            street = first.street,
                            locality = first.locality,
                            localityId = first.localityId,
                            district = first.district,
                            districtId = first.districtId,
                            streetLatitude = first.streetLatitude,
                            streetLongitude = first.streetLongitude,
                            hasReminder = first.hasReminder,
                            nextPickups = locations.map { NextPickup(it.date!!, it.type) }
                        )
                    }
            } else {
                database.abfuhrQueries.getAllLocationWithNextPickups(Clock.System.now().toEpochMilliseconds())
                    .executeAsList()
                    .groupBy {
                        it.streetId
                    }
                    .map { (key, value) ->
                        val locations = value.groupBy { it.date }.minBy { it.key ?: Long.MAX_VALUE }.value
                        val first = value.first()
                        LocationWithNextPickups(
                            streetId = first.streetId,
                            street = first.street,
                            locality = first.locality,
                            localityId = first.localityId,
                            district = first.district,
                            districtId = first.districtId,
                            streetLatitude = first.streetLatitude,
                            streetLongitude = first.streetLongitude,
                            hasReminder = first.hasReminder,
                            nextPickups = locations.map { NextPickup(it.date!!, it.type) }
                        )
                    }
            }
            populateSearchList()
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