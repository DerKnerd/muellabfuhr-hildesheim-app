package dev.imanuel.abfuhr.worker

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.content.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.imanuel.abfuhr.R
import dev.imanuel.abfuhr.database.AbfallDatabase

class PickupReminderWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val abfallDatabase: AbfallDatabase
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val streetId = inputData.getLong("streetId", -1)
        val date = inputData.getLong("date", -1)
        val type = inputData.getString("type") ?: ""

        val trashCan = when (type) {
            "B" -> "Biotonne"
            "R" -> "Restmülltonne"
            "P" -> "Papiertonne"
            "G" -> "Gelbe Tonne"
            else -> ""
        }
        val trashCanIcon = when (type) {
            "B" -> R.drawable.trashcan_b
            "R" -> R.drawable.trashcan_r
            "P" -> R.drawable.trashcan_p
            "G" -> R.drawable.trashcan_g
            else -> 0
        }

        if (streetId == -1L || date == -1L || trashCan.isEmpty()) return Result.success()

        val notificationManager = applicationContext.getSystemService<NotificationManager>()
        val notification = Notification
            .Builder(applicationContext, "trash-reminder")
            .setContentTitle("Erinnerung an die $trashCan")
            .setContentText("Morgen ist die $trashCan dran, denk daran sie bis um 6 Uhr morgens rauszustellen.")
            .setSmallIcon(trashCanIcon)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    applicationContext.resources,
                    trashCanIcon
                )
            )
            .setAutoCancel(true)
            .build()

        val locationPickup =
            abfallDatabase.abfuhrQueries.getPickupByStreetDateAndType(streetId, date, type)

        notificationManager?.notify(locationPickup.hashCode(), notification)

        return Result.success()
    }
}