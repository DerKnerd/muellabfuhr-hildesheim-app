package dev.imanuel.abfuhr.notifications

import dev.imanuel.abfuhr.database.AbfallDatabase
import kotlinx.datetime.*
import org.koin.mp.KoinPlatformTools
import kotlin.time.Clock
import kotlin.time.Instant

fun enqueueNextPickups(streetId: Long) {
    val database = KoinPlatformTools
        .defaultContext()
        .get()
        .get<AbfallDatabase>()
    val pickups = database.abfuhrQueries.getPickupsByStreetId(streetId).executeAsList().filter {
        it.date >= Clock.System.now().toEpochMilliseconds()
    }

    val timeZone = TimeZone.currentSystemDefault()

    for ((_, date, _, type) in pickups) {
        val trashCan = when (type) {
            "B" -> "Biotonne"
            "R" -> "Restmülltonne"
            "P" -> "Papiertonne"
            "G" -> "Gelbe Tonne"
            else -> ""
        }
        val pickupInstant = Instant.fromEpochMilliseconds(date)
        val pickupDate = pickupInstant.toLocalDateTime(timeZone).date
        val dayBefore = pickupDate.minus(1, DateTimeUnit.DAY)
        val reminderDateTime = LocalDateTime(dayBefore.year, dayBefore.month, dayBefore.day, 18, 0, 0)
        val reminderTimestamp = reminderDateTime.toInstant(timeZone).toEpochMilliseconds()

        IosNotificationManager.enqueueNotification(
            reminderTimestamp,
            "Erinnerung an die $trashCan",
            "Morgen ist die $trashCan dran, denk daran sie bis um 6 Uhr morgens rauszustellen.",
            setOf("trash-reminder-$streetId"),
        )
    }
}

fun dequeuePickups(streetId: Long) {
    IosNotificationManager.cancelNotificationsByTags(setOf("trash-reminder-$streetId"))
}
