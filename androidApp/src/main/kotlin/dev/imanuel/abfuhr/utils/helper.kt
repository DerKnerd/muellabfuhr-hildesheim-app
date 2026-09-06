package dev.imanuel.abfuhr.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

fun Context.isLocationEnabled(): Boolean {
    val locationManager = this.getSystemService<LocationManager>()
    return LocationManagerCompat.isLocationEnabled(locationManager!!)
}

fun Context.getSharedPrefs(): SharedPreferences =
    getSharedPreferences("muellabfuhr", Context.MODE_PRIVATE)

fun Context.firstSyncHappened() = getSharedPrefs().getBoolean("firstSync", false)

fun Context.markFirstSync() = getSharedPrefs().edit { putBoolean("firstSync", true) }

fun Context.markLastSync() =
    getSharedPrefs().edit { putLong("lastSync", System.currentTimeMillis()) }

fun Context.syncDue() =
    System.currentTimeMillis() > getSharedPrefs().getLong(
        "lastSync",
        0
    ) + 28L * 24L * 60L * 60L * 1000L

@SuppressLint("MissingPermission")
suspend fun Context.fetchFineLocation(): Location? {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    val cancellationTokenSource = CancellationTokenSource()

    return try {
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            cancellationTokenSource.token
        ).await()
    } catch (e: Exception) {
        null
    }
}
