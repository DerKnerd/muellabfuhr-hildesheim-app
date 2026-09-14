@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.screens

import dev.imanuel.abfuhr.AbfuhrNavDestination
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.database.Location
import dev.imanuel.abfuhr.uikit.dsl.AppColors
import dev.imanuel.abfuhr.uikit.dsl.button
import dev.imanuel.abfuhr.uikit.dsl.mapView
import dev.imanuel.abfuhr.uikit.dsl.showAlert
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.useContents
import kotlinx.coroutines.*
import org.koin.mp.KoinPlatformTools
import platform.CoreGraphics.CGSizeMake
import platform.CoreLocation.*
import platform.MapKit.*
import platform.UIKit.*
import platform.darwin.NSObject

/**
 * Builds the text content for the location detail dialog matching the Compose AlertDialog.
 */
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

/**
 * Validates whether the coordinates are finite numbers within valid WGS84 geographic bounds
 * and not uninitialized/zero values.
 */
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

/**
 * Default fallback coordinates centered on Hildesheim.
 */
private const val DEFAULT_HILDESHEIM_LATITUDE = 52.15
private const val DEFAULT_HILDESHEIM_LONGITUDE = 9.95

/**
 * Returns the appropriate marker glyph image for a given location type.
 * - "container": custom container vector icon
 * - "deponie", "dump": system default recycle icon ("arrow.3.trianglepath")
 * - "office": system default office building icon ("building.2" / "building")
 * - other: default pin icon ("mappin.and.ellipse")
 */
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

/**
 * Custom annotation associating an MKPointAnnotation with a database Location model.
 */
class LocationPointAnnotation(
    val location: Location
) : MKPointAnnotation() {
    init {
        val lat = if (isValidCoordinate(
                location.latitude, location.longitude
            )
        ) location.latitude else DEFAULT_HILDESHEIM_LATITUDE
        val lon = if (isValidCoordinate(
                location.latitude, location.longitude
            )
        ) location.longitude else DEFAULT_HILDESHEIM_LONGITUDE
        setCoordinate(CLLocationCoordinate2DMake(lat, lon))
        setTitle(location.name.trim())
        setSubtitle(location.description.trim())
    }
}

/**
 * MapView delegate bridge handling marker rendering, selection dialogs, and initial user location centering.
 */
class StandorteMapDelegateBridge(
    private val onMarkerSelected: (Location) -> Unit, private val onUserLocationFirstDetected: (Double, Double) -> Unit
) : NSObject(), MKMapViewDelegateProtocol {

    private var hasCenteredOnUser = false

    @ObjCSignatureOverride
    override fun mapView(mapView: MKMapView, viewForAnnotation: MKAnnotationProtocol): MKAnnotationView? {
        if (viewForAnnotation is MKUserLocation) {
            return null
        }

        val reuseId = "StandorteMarkerAnnotationView"
        var markerView = mapView.dequeueReusableAnnotationViewWithIdentifier(reuseId) as? MKMarkerAnnotationView
        if (markerView == null) {
            markerView = MKMarkerAnnotationView(annotation = viewForAnnotation, reuseIdentifier = reuseId)
        } else {
            markerView.annotation = viewForAnnotation
        }

        val locationAnno = viewForAnnotation as? LocationPointAnnotation
        val loc = locationAnno?.location

        markerView.setMarkerTintColor(AppColors.primary)
        markerView.setGlyphTintColor(UIColor.whiteColor())
        markerView.canShowCallout = false
        markerView.animatesWhenAdded = true

        markerView.glyphImage = loc?.type?.markerGlyph

        return markerView
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

/**
 * Location manager delegate bridge for tracking user coordinates and permissions.
 */
class StandorteLocationManagerDelegateBridge(
    private val onLocationUpdated: (CLLocation) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {

    @ObjCSignatureOverride
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val firstLocation = didUpdateLocations.firstOrNull() as? CLLocation ?: return
        onLocationUpdated(firstLocation)
    }
}

/**
 * UIViewController for the "Standorte" navigation point displaying an MKMapView with markers
 * for all locations from the database and centered on the user's current location.
 */
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
    private val locationManager: CLLocationManager = CLLocationManager()
    private var locationManagerDelegateBridge: StandorteLocationManagerDelegateBridge? = null

    private var hasCenteredOnUser: Boolean = false
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
        mapView = mapView {
            mapType = MKMapTypeStandard
            showsUserLocation = true
            isZoomEnabled = true
            isScrollEnabled = true
            isRotateEnabled = true
            isPitchEnabled = true
        }

        val delegate = StandorteMapDelegateBridge(onMarkerSelected = { location ->
            showLocationDetailDialog(location)
        }, onUserLocationFirstDetected = { lat, lon ->
            if (!hasCenteredOnUser) {
                centerMapOnCoordinate(lat, lon)
            }
        })
        mapDelegateBridge = delegate
        mapView.setDelegate(delegate)

        mapView.setTranslatesAutoresizingMaskIntoConstraints(false)
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
        val button = button {
            button.setTranslatesAutoresizingMaskIntoConstraints(false)
            backgroundColor = AppColors.background.colorWithAlphaComponent(0.9)
            button.layer.run {
                cornerRadius = 24.0
                shadowColor = UIColor.blackColor.CGColor
                shadowOpacity = 0.2f
                shadowOffset = CGSizeMake(0.0, 2.0)
                shadowRadius = 4.0
            }

            image = UIImage.systemImageNamed(
                "location.fill", withConfiguration = UIImageSymbolConfiguration.configurationWithPointSize(20.0)
            )
            tintColor = AppColors.primary

            onClick {
                recenterOnUserLocation()
            }
        }

        view.addSubview(button)
        NSLayoutConstraint.activateConstraints(
            listOf(
                button.trailingAnchor.constraintEqualToAnchor(
                    view.safeAreaLayoutGuide.trailingAnchor, constant = -16.0
                ),
                button.bottomAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.bottomAnchor, constant = -24.0),
                button.widthAnchor.constraintEqualToConstant(48.0),
                button.heightAnchor.constraintEqualToConstant(48.0)
            )
        )
    }

    private fun recenterOnUserLocation() {
        val userLoc = mapView.userLocation.location
        if (userLoc != null) {
            userLoc.coordinate.useContents {
                if (isValidCoordinate(latitude, longitude)) {
                    centerMapOnCoordinate(latitude, longitude, animated = true)
                }
            }
        } else {
            val lastLoc = locationManager.location
            if (lastLoc != null) {
                lastLoc.coordinate.useContents {
                    if (isValidCoordinate(latitude, longitude)) {
                        centerMapOnCoordinate(latitude, longitude, animated = true)
                    }
                }
            } else {
                locationManager.startUpdatingLocation()
            }
        }
    }

    /**
     * Centers the map view on the specified coordinate.
     */
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

    /**
     * Loads all locations from the database and creates map markers.
     */
    private fun loadLocations() {
        ioScope.launch {
            val locationsList = database.locationQueries.getAllLocations().executeAsList()
            setLocations(locationsList)
        }
    }

    /**
     * Updates map annotations for the given list of locations.
     */
    private fun setLocations(locationsList: List<Location>) {
        if (annotations.isNotEmpty()) {
            mainScope.launch {
                mapView.removeAnnotations(annotations)
                annotations.clear()
            }
        }

        val validLocations = locationsList.filter { isValidCoordinate(it.latitude, it.longitude) }

        if (validLocations.isEmpty()) {
            // Default center around Hildesheim if no valid locations and no user position yet
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

        // If user location is not available yet, center on average location coordinates
        if (!hasCenteredOnUser) {
            val avgLat = validLocations.map { it.latitude }.average()
            val avgLon = validLocations.map { it.longitude }.average()
            mainScope.launch {
                if (isValidCoordinate(avgLat, avgLon)) {
                    centerMapOnCoordinate(
                        avgLat, avgLon, latitudinalMeters = 6000.0, longitudinalMeters = 6000.0, animated = false
                    )
                } else {
                    centerMapOnCoordinate(
                        DEFAULT_HILDESHEIM_LATITUDE,
                        DEFAULT_HILDESHEIM_LONGITUDE,
                        latitudinalMeters = 8000.0,
                        longitudinalMeters = 8000.0,
                        animated = false
                    )
                }
            }
        }
    }

    /**
     * Opens an alert dialog with the details of the selected location.
     */
    private fun showLocationDetailDialog(location: Location) {
        showAlert {
            title = location.name.trim().ifBlank { "Standort" }
            message = location.dialogText
            style = UIAlertControllerStyleAlert
            action(
                title = "Schließen", style = UIAlertActionStyleCancel, handler = null
            )
        }
    }
}

fun createStandorteMapViewController() = StandorteMapViewController()
