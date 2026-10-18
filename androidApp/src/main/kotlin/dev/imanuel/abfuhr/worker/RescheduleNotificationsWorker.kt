package dev.imanuel.abfuhr.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.utils.clearAbfuhrNotifications
import dev.imanuel.abfuhr.utils.createAbfuhrNotifications

class RescheduleNotificationsWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val abfallDatabase: AbfallDatabase
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        return try {

            val locations = abfallDatabase
                .abfuhrQueries
                .getLocationsWithReminder()
                .executeAsList()

            applicationContext.clearAbfuhrNotifications(locations)
            applicationContext.createAbfuhrNotifications(abfallDatabase, locations)

            Result.success()
        } catch (exception: Exception) {
            Result.retry()
        }
    }
}