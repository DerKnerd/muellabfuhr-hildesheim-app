@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.MapKit.MKMapTypeStandard
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol

@UIKitDsl
class MapViewBuilder {
    val mapView = MKMapView(frame = CGRectMake(0.0, 0.0, 320.0, 480.0))

    var mapType = MKMapTypeStandard
    var showsUserLocation = false
    var isZoomEnabled = true
    var isScrollEnabled = true
    var isRotateEnabled = true
    var isPitchEnabled = true

    var delegate: MKMapViewDelegateProtocol? = null

    private var initialRegionAction: (() -> Unit)? = null

    fun build(): MKMapView {
        mapView.mapType = mapType
        mapView.showsUserLocation = showsUserLocation
        mapView.zoomEnabled = isZoomEnabled
        mapView.scrollEnabled = isScrollEnabled
        mapView.rotateEnabled = isRotateEnabled
        mapView.pitchEnabled = isPitchEnabled
        mapView.delegate = delegate
        mapView.translatesAutoresizingMaskIntoConstraints = false

        initialRegionAction?.invoke()

        return mapView
    }
}

inline fun mapView(builder: MapViewBuilder.() -> Unit = {}): MKMapView {
    val b = MapViewBuilder()
    b.builder()
    return b.build()
}
