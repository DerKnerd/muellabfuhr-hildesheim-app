package dev.imanuel.abfuhr.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.core.location.LocationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.database.AbfuhrLocation
import dev.imanuel.abfuhr.worker.PickupReminderWorker
import dev.imanuel.abfuhr.worker.RefreshDataWorker
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

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
                val targetInstant = Instant.ofEpochMilli(it.date)
                    .atZone(ZoneId.systemDefault())
                    .minusDays(1)
                    .withHour(18)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0)
                    .toInstant()

                val delayMillis =
                    (targetInstant.toEpochMilli() - System.currentTimeMillis()).coerceAtLeast(0)

                val request = OneTimeWorkRequestBuilder<PickupReminderWorker>()
                    .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
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

fun Context.enqueueRefreshDataWorker() {
    val now = LocalDateTime.now()
    val firstOfNextMonth = now.plusMonths(1)
        .with(TemporalAdjusters.firstDayOfMonth())
        .withHour(0).withMinute(0).withSecond(0).withNano(0)

    val workManager = WorkManager.getInstance(this)
    val delayInSeconds = Duration.between(now, firstOfNextMonth).seconds

    val workRequest = OneTimeWorkRequestBuilder<RefreshDataWorker>()
        .setInitialDelay(delayInSeconds, TimeUnit.SECONDS)
        .build()

    workManager.enqueue(workRequest)
}
