package dev.imanuel.abfuhr.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.sync.SyncClient
import dev.imanuel.abfuhr.utils.clearAbfuhrNotifications
import dev.imanuel.abfuhr.utils.createAbfuhrNotifications
import dev.imanuel.abfuhr.utils.enqueueRefreshDataWorker

class RefreshDataWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val abfallDatabase: AbfallDatabase,
    private val syncClient: SyncClient
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            syncClient.sync()
            val reminderLocations =
                abfallDatabase.abfuhrQueries.getLocationsWithReminder().executeAsList()
            applicationContext.clearAbfuhrNotifications(reminderLocations)
            applicationContext.createAbfuhrNotifications(abfallDatabase, reminderLocations)

            return Result.success()
        } catch (e: Throwable) {
            return Result.failure()
        } finally {
            applicationContext.enqueueRefreshDataWorker()
        }
    }
}