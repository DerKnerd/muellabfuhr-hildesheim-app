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

        if (streetId == -1L || date == -1L) return Result.success()

        val pickups =
            abfallDatabase.abfuhrQueries.getPickupByStreetAndDate(streetId, date).executeAsList()

        val notificationManager = applicationContext.getSystemService<NotificationManager>()
        val has14DayTrash = pickups.any { it.type == "R" }
        for (pickup in pickups) {
            if (has14DayTrash && pickup.type == "S") {
                continue
            }
            val trashCan = when (pickup.type) {
                "B" -> "Biotonne"
                "R" -> "Restmülltonne"
                "S" -> "Restmülltonne (14-tägige Abfuhr)"
                "P" -> "Papiertonne"
                "G" -> "Gelbe Tonne"
                else -> ""
            }
            val trashCanIcon = when (pickup.type) {
                "B" -> R.drawable.trashcan_b
                "R" -> R.drawable.trashcan_r
                "S" -> R.drawable.trashcan_r
                "P" -> R.drawable.trashcan_p
                "G" -> R.drawable.trashcan_g
                else -> 0
            }
            val trashCanColor = when (pickup.type) {
                "B" -> R.color.trashcan_b
                "R" -> R.color.trashcan_r
                "S" -> R.color.trashcan_r
                "P" -> R.color.trashcan_p
                "G" -> R.color.trashcan_g
                else -> 0
            }

            if (trashCan.isEmpty()) {
                continue
            }

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
                .setColor(trashCanColor)
                .setColorized(true)
                .setAutoCancel(true)
                .build()

            notificationManager?.notify(pickup.hashCode(), notification)
        }

        return Result.success()
    }
}