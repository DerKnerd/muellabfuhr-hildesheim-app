package dev.imanuel.abfuhr.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.location.Location
import android.location.LocationManager
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.core.graphics.createBitmap
import androidx.core.location.LocationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.tasks.CancellationTokenSource
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.database.AbfuhrLocation
import dev.imanuel.abfuhr.worker.PickupReminderWorker
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

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

fun Context.clearAbfuhrNotifications(locations: List<AbfuhrLocation>) {
    val workManager = WorkManager.getInstance(this)
    locations.forEach { location -> workManager.cancelAllWorkByTag("trash-reminder-${location.streetId}") }
}

fun Context.createAbfuhrNotifications(
    abfallDatabase: AbfallDatabase,
    locations: List<AbfuhrLocation>
) {
    val workManager = WorkManager.getInstance(this)
    val today = Clock
        .System
        .now()
        .toLocalDateTime(
            TimeZone.currentSystemDefault()
        )
        .date
        .atTime(0, 0)
        .toInstant(TimeZone.currentSystemDefault())

    val workRequests = locations.flatMap { location ->
        val pickups =
            abfallDatabase.abfuhrQueries.getPickupsByStreetId(location.streetId).executeAsList()
        pickups
            .filter {
                it.date >= today.toEpochMilliseconds()
            }
            .map {
                val reminderTime =
                    it.date - 1.days.inWholeMilliseconds + 18.hours.inWholeMilliseconds
                val request = OneTimeWorkRequestBuilder<PickupReminderWorker>()
                    .setInitialDelay(reminderTime, TimeUnit.MILLISECONDS)
                    .setInputData(
                        workDataOf(
                            "streetId" to location.streetId,
                            "type" to it.type,
                            "date" to it.date
                        )
                    )
                    .addTag("trash-reminder-${location.streetId}")
                    .build()

                request
            }
    }
        .first()

    workManager.enqueue(workRequests)
}
