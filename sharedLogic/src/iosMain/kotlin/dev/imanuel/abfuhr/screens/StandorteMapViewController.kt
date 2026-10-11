@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.screens

import dev.imanuel.abfuhr.AbfuhrNavDestination
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.database.Location
import dev.imanuel.abfuhr.uikit.dsl.AppColors
import dev.imanuel.abfuhr.uikit.dsl.iconButton
import dev.imanuel.abfuhr.uikit.dsl.mapView
import dev.imanuel.abfuhr.uikit.dsl.showAlert
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.useContents
import kotlinx.coroutines.*
import org.koin.mp.KoinPlatformTools
import platform.CoreLocation.*
import platform.MapKit.*
import platform.UIKit.*
import platform.darwin.NSObject

val Location.dialogText: String
    get() = buildString {
        val desc = description.trim()
        if (desc.isNotBlank()) {
            append(desc)
        }

        val hours = openingHours.trim()
        if (hours.isNotBlank()) {
            if (isNotEmpty()) append("\n\n")
            append("Öffnungszeiten\n")
            append(hours)
        }

        val hasContact = fax.isNotBlank() || mail.isNotBlank() || www.isNotBlank() || tel.isNotBlank()
        if (hasContact) {
            if (isNotEmpty()) append("\n\n")
            append("Kontaktdaten")
            if (tel.isNotBlank()) {
                append("\nTelefon: ")
                append(tel.trim())
            }
            if (mail.isNotBlank()) {
                append("\nMail: ")
                append(mail.trim())
            }
            if (fax.isNotBlank()) {
                append("\nFax: ")
                append(fax.trim())
            }
            if (www.isNotBlank()) {
                append("\nWebseite: ")
                append(www.trim())
            }
        }
    }


private fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
    if (latitude.isNaN() || longitude.isNaN() || latitude.isInfinite() || longitude.isInfinite()) {
        return false
    }
    if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) {
        return false
    }
    if (latitude == 0.0 && longitude == 0.0) {
        return false
    }
    return true
}

private const val DEFAULT_HILDESHEIM_LATITUDE = 52.15
private const val DEFAULT_HILDESHEIM_LONGITUDE = 9.95

val String.markerGlyph: UIImage?
    get() {
        val config = UIImageSymbolConfiguration.configurationWithPointSize(16.0)
        return when (lowercase()) {
            "deponie", "dump" -> {
                UIImage.systemImageNamed(
                    "arrow.3.trianglepath", config
                )
            }

            "office" -> {
                UIImage.systemImageNamed("building.2", config)
            }

            else -> {
                UIImage.imageNamed("ContainerMarker")
            }
        }
    }

class LocationPointAnnotation(val location: Location) : MKPointAnnotation() {
    init {
        val lat = if (isValidCoordinate(location.latitude, location.longitude)) {
            location.latitude
        } else {
            DEFAULT_HILDESHEIM_LATITUDE
        }
        val lon = if (isValidCoordinate(location.latitude, location.longitude)) {
            location.longitude
        } else {
            DEFAULT_HILDESHEIM_LONGITUDE
        }
        setCoordinate(CLLocationCoordinate2DMake(lat, lon))
        setTitle(location.name.trim())
        setSubtitle(location.description.trim())
    }
}

class StandorteMapDelegateBridge(
    private val onMarkerSelected: (Location) -> Unit, private val onUserLocationFirstDetected: (Double, Double) -> Unit
) : NSObject(), MKMapViewDelegateProtocol {

    private var hasCenteredOnUser = false

    @ObjCSignatureOverride
    override fun mapView(
        mapView: MKMapView,
        viewForAnnotation: MKAnnotationProtocol
    ): MKAnnotationView? {
        if (viewForAnnotation is MKUserLocation) {
            return null
        }

        if (viewForAnnotation is MKClusterAnnotation) {
            val reuseId = "StandorteCluster"

            val marker = (mapView.dequeueReusableAnnotationViewWithIdentifier(reuseId) as? MKMarkerAnnotationView)
                ?: MKMarkerAnnotationView(
                    annotation = viewForAnnotation,
                    reuseIdentifier = reuseId
                )

            marker.annotation = viewForAnnotation
            marker.markerTintColor = AppColors.primary
            marker.glyphTintColor = UIColor.whiteColor()
            marker.glyphText = viewForAnnotation.memberAnnotations.size.toString()
            marker.glyphImage = null
            marker.clusteringIdentifier = null
            marker.canShowCallout = false

            return marker
        }

        val annotation = viewForAnnotation as? LocationPointAnnotation ?: return null

        val reuseId = "StandorteMarker"

        val marker = (mapView.dequeueReusableAnnotationViewWithIdentifier(reuseId) as? MKMarkerAnnotationView)
            ?: MKMarkerAnnotationView(
                annotation = annotation,
                reuseIdentifier = reuseId
            )

        marker.annotation = annotation
        marker.markerTintColor = AppColors.primary
        marker.glyphTintColor = UIColor.whiteColor()
        marker.glyphImage = annotation.location.type.markerGlyph
        marker.glyphText = null
        marker.clusteringIdentifier = annotation.location.type
        marker.canShowCallout = false
        marker.animatesWhenAdded = true

        marker.displayPriority = if (annotation.location.type == "container") {
            MKFeatureDisplayPriorityDefaultLow
        } else {
            MKFeatureDisplayPriorityRequired
        }

        return marker
    }

    @ObjCSignatureOverride
    override fun mapView(mapView: MKMapView, didSelectAnnotationView: MKAnnotationView) {
        val annotation = didSelectAnnotationView.annotation ?: return
        if (annotation is MKUserLocation) return

        mapView.deselectAnnotation(annotation, animated = false)

        val loc = (annotation as? LocationPointAnnotation)?.location
        if (loc != null) {
            onMarkerSelected(loc)
        }
    }

    @ObjCSignatureOverride
    override fun mapView(mapView: MKMapView, didUpdateUserLocation: MKUserLocation) {
        val loc = didUpdateUserLocation.location ?: return
        if (!hasCenteredOnUser) {
            loc.coordinate.useContents {
                if (isValidCoordinate(latitude, longitude)) {
                    hasCenteredOnUser = true
                    onUserLocationFirstDetected(latitude, longitude)
                }
            }
        }
    }
}

class StandorteLocationManagerDelegateBridge(private val onLocationUpdated: (CLLocation) -> Unit) : NSObject(),
    CLLocationManagerDelegateProtocol {

    @ObjCSignatureOverride
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val firstLocation = didUpdateLocations.firstOrNull() as? CLLocation ?: return
        onLocationUpdated(firstLocation)
    }
}

class StandorteMapViewController : UIViewController(nibName = null, bundle = null) {
    init {
        tabBarItem = UITabBarItem(
            title = AbfuhrNavDestination.Locations.title,
            image = AbfuhrNavDestination.Locations.createIcon(),
            tag = AbfuhrNavDestination.Locations.ordinal.toLong()
        )
    }

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val database: AbfallDatabase
        get() = KoinPlatformTools.defaultContext().get().get()

    private lateinit var mapView: MKMapView
    private lateinit var mapDelegateBridge: StandorteMapDelegateBridge
    private val locationManager = CLLocationManager()
    private var locationManagerDelegateBridge: StandorteLocationManagerDelegateBridge? = null

    private var hasCenteredOnUser = false
    private val annotations = mutableListOf<LocationPointAnnotation>()

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = AppColors.background

        setupMapView()
        setupLocationTracking()
        setupRecenterButton()
        loadLocations()
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        if (annotations.isEmpty()) {
            loadLocations()
        }
    }

    private fun setupMapView() {
        mapDelegateBridge = StandorteMapDelegateBridge(
            onMarkerSelected = { location ->
                showLocationDetailDialog(location)
            },
            onUserLocationFirstDetected = { lat, lon ->
                if (!hasCenteredOnUser) {
                    centerMapOnCoordinate(lat, lon)
                }
            }
        )
        mapView = mapView {
            delegate = mapDelegateBridge
        }

        view.addSubview(mapView)

        NSLayoutConstraint.activateConstraints(
            listOf(
                mapView.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor),
                mapView.bottomAnchor.constraintEqualToAnchor(view.bottomAnchor),
                mapView.leadingAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.leadingAnchor),
                mapView.trailingAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.trailingAnchor),
            )
        )
    }

    private fun setupLocationTracking() {
        this.locationManagerDelegateBridge = StandorteLocationManagerDelegateBridge { location ->
            if (!hasCenteredOnUser) {
                location.coordinate.useContents {
                    if (isValidCoordinate(latitude, longitude)) {
                        centerMapOnCoordinate(latitude, longitude)
                    }
                }
            }
        }

        locationManager.run {
            setDelegate(this@StandorteMapViewController.locationManagerDelegateBridge)
            setDesiredAccuracy(kCLLocationAccuracyBest)
            requestWhenInUseAuthorization()
            startUpdatingLocation()
        }
    }

    private fun setupRecenterButton() {
        val button = iconButton("location.fill") {
            backgroundColor = AppColors.background.colorWithAlphaComponent(0.9)
            tintColor = AppColors.primary

            onClick {
                recenterOnUserLocation()
            }
        }

        view.addSubview(button)
        NSLayoutConstraint.activateConstraints(
            listOf(
                button.trailingAnchor.constraintEqualToAnchor(
                    view.safeAreaLayoutGuide.trailingAnchor,
                    constant = -16.0
                ),
                button.bottomAnchor.constraintEqualToAnchor(
                    view.safeAreaLayoutGuide.bottomAnchor,
                    constant = -24.0,
                ),
                button.widthAnchor.constraintEqualToConstant(48.0),
                button.heightAnchor.constraintEqualToConstant(48.0)
            )
        )
    }

    private fun recenterOnUserLocation() {
        val userLoc = mapView.userLocation.location
        val lastLoc = locationManager.location
        if (userLoc != null) {
            userLoc.coordinate.useContents {
                if (isValidCoordinate(latitude, longitude)) {
                    centerMapOnCoordinate(latitude, longitude, animated = true)
                }
            }
        } else if (lastLoc != null) {
            lastLoc.coordinate.useContents {
                if (isValidCoordinate(latitude, longitude)) {
                    centerMapOnCoordinate(latitude, longitude, animated = true)
                }
            }
        } else {
            locationManager.startUpdatingLocation()
        }
    }

    private fun centerMapOnCoordinate(
        latitude: Double,
        longitude: Double,
        latitudinalMeters: Double = 1500.0,
        longitudinalMeters: Double = 1500.0,
        animated: Boolean = true
    ) {
        hasCenteredOnUser = true
        val targetLat = if (isValidCoordinate(latitude, longitude)) latitude else DEFAULT_HILDESHEIM_LATITUDE
        val targetLon = if (isValidCoordinate(latitude, longitude)) longitude else DEFAULT_HILDESHEIM_LONGITUDE
        val targetLatMeters = if (latitudinalMeters > 0.0) latitudinalMeters else 1500.0
        val targetLonMeters = if (longitudinalMeters > 0.0) longitudinalMeters else 1500.0
        val coord = CLLocationCoordinate2DMake(targetLat, targetLon)
        val region = MKCoordinateRegionMakeWithDistance(coord, targetLatMeters, targetLonMeters)
        mapView.setRegion(region, animated = animated)
    }

    private fun loadLocations() {
        ioScope.launch {
            val locationsList = database.locationQueries.getAllLocations().executeAsList()
            setLocations(locationsList)
        }
    }

    private fun setLocations(locationsList: List<Location>) {
        if (annotations.isNotEmpty()) {
            mainScope.launch {
                mapView.removeAnnotations(annotations)
                annotations.clear()
            }
        }

        val validLocations = locationsList.filter { isValidCoordinate(it.latitude, it.longitude) }

        if (validLocations.isEmpty()) {
            if (!hasCenteredOnUser) {
                mainScope.launch {
                    centerMapOnCoordinate(
                        DEFAULT_HILDESHEIM_LATITUDE,
                        DEFAULT_HILDESHEIM_LONGITUDE,
                        latitudinalMeters = 8000.0,
                        longitudinalMeters = 8000.0,
                        animated = false
                    )
                }
            }
            return
        }

        for (loc in validLocations) {
            val annotation = LocationPointAnnotation(loc)
            mainScope.launch {
                annotations.add(annotation)
                mapView.addAnnotation(annotation)
            }
        }

        if (!hasCenteredOnUser) {
            val avgLat = validLocations.map { it.latitude }.average()
            val avgLon = validLocations.map { it.longitude }.average()
            mainScope.launch {
                if (isValidCoordinate(avgLat, avgLon)) {
                    centerMapOnCoordinate(
                        avgLat,
                        avgLon,
                        latitudinalMeters = 6000.0,
                        longitudinalMeters = 6000.0,
                        animated = false,
                    )
                } else {
                    centerMapOnCoordinate(
                        DEFAULT_HILDESHEIM_LATITUDE,
                        DEFAULT_HILDESHEIM_LONGITUDE,
                        latitudinalMeters = 8000.0,
                        longitudinalMeters = 8000.0,
                        animated = false,
                    )
                }
            }
        }
    }

    private fun showLocationDetailDialog(location: Location) {
        showAlert {
            title = location.name.trim().ifBlank { "Standort" }
            message = location.dialogText
            okAction("Schließen")
        }
    }
}

fun createStandorteMapViewController() = StandorteMapViewController()
