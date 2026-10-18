package dev.imanuel.abfuhr

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.imanuel.abfuhr.worker.RescheduleNotificationsWorker

class AppUpdatedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val workRequest = OneTimeWorkRequestBuilder<RescheduleNotificationsWorker>().build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "reschedule-notifications", ExistingWorkPolicy.REPLACE, workRequest
            )
    }
}