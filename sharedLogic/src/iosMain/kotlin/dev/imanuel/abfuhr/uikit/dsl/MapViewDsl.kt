@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.MapKit.*

@UIKitDsl
class MapViewBuilder {
    val mapView: MKMapView = MKMapView(frame = CGRectMake(0.0, 0.0, 320.0, 480.0))

    var mapType: MKMapType = MKMapTypeStandard
    var showsUserLocation: Boolean = false
    var isZoomEnabled: Boolean = true
    var isScrollEnabled: Boolean = true
    var isRotateEnabled: Boolean = true
    var isPitchEnabled: Boolean = true

    private var initialRegionAction: (() -> Unit)? = null

    fun build(): MKMapView {
        mapView.setMapType(mapType)
        mapView.setShowsUserLocation(showsUserLocation)
        mapView.setZoomEnabled(isZoomEnabled)
        mapView.setScrollEnabled(isScrollEnabled)
        mapView.setRotateEnabled(isRotateEnabled)
        mapView.setPitchEnabled(isPitchEnabled)

        initialRegionAction?.invoke()

        return mapView
    }
}

inline fun mapView(
    builder: MapViewBuilder.() -> Unit = {}
): MKMapView {
    val b = MapViewBuilder()
    b.builder()
    return b.build()
}
