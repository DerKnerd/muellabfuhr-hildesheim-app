@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.sync

import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.notifications.enqueueNextPickups
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.runBlocking
import org.koin.mp.KoinPlatformTools
import platform.BackgroundTasks.BGProcessingTask
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow

private const val SYNC_TASK_ID = "dev.imanuel.abfuhr.sync"

fun initializeBackgroundTasks() {
    BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
        identifier = SYNC_TASK_ID,
        usingQueue = null
    ) { task ->
        handleSyncTask(task as BGProcessingTask)
    }

    scheduleSync()
}

private fun handleSyncTask(task: BGProcessingTask) {
    scheduleSync()

    try {
        val syncClient =
            KoinPlatformTools
                .defaultContext()
                .get()
                .get<SyncClient>()
        val database = KoinPlatformTools
            .defaultContext()
            .get()
            .get<AbfallDatabase>()

        runBlocking {
            syncClient.sync()
            val withReminder = database.abfuhrQueries.getLocationsWithReminder().executeAsList().map { it.streetId }
            for (streetId in withReminder) {
                enqueueNextPickups(streetId)
            }
        }

        task.setTaskCompletedWithSuccess(true)
    } catch (e: Throwable) {
        task.setTaskCompletedWithSuccess(false)
    }
}

private fun scheduleSync() {
    val request = BGProcessingTaskRequest(
        identifier = SYNC_TASK_ID
    )

    request.earliestBeginDate =
        NSDate.dateWithTimeIntervalSinceNow(
            30.0 * 24.0 * 60.0 * 60.0
        )

    BGTaskScheduler.sharedScheduler.submitTaskRequest(
        request,
        error = null
    )
}