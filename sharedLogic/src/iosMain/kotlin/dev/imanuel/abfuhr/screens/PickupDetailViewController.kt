@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package dev.imanuel.abfuhr.screens

import dev.imanuel.abfuhr.AbfuhrNavDestination
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.database.AbfuhrLocation
import dev.imanuel.abfuhr.database.AbfuhrPickup
import dev.imanuel.abfuhr.notifications.IosNotificationManager
import dev.imanuel.abfuhr.notifications.dequeuePickups
import dev.imanuel.abfuhr.notifications.enqueueNextPickups
import dev.imanuel.abfuhr.uikit.dsl.listView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.coroutines.*
import kotlinx.datetime.toNSDate
import org.koin.mp.KoinPlatformTools
import platform.Foundation.*
import platform.UIKit.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class PickupDetailViewController(
    private val streetId: Long
) : UIViewController(nibName = null, bundle = null) {

    init {
        tabBarItem = UITabBarItem(
            title = AbfuhrNavDestination.WasteAbc.title,
            image = AbfuhrNavDestination.WasteAbc.createIcon(),
            tag = AbfuhrNavDestination.WasteAbc.ordinal.toLong()
        )
    }

    private val database: AbfallDatabase
        get() = KoinPlatformTools.defaultContext().get().get()

    private lateinit var location: AbfuhrLocation
    private lateinit var nextPickups: List<AbfuhrPickup>

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun viewDidLoad() {
        super.viewDidLoad()

        location = database.abfuhrQueries.getLocationByStreetId(streetId).executeAsOne()

        title = if (location.locality == "Hildesheim") {
            "${location.street} Hildesheim"
        } else {
            "${location.street} ${location.district}"
        }

        nextPickups = database.abfuhrQueries.getPickupsByStreetId(streetId).executeAsList()
            .filter { it.date >= Clock.System.now().toEpochMilliseconds() }

        val pickupsListView = listView(UITableViewStyle.UITableViewStyleGrouped) {
            backgroundColor = UIColor.systemBackgroundColor()
            separatorStyle = UITableViewCellSeparatorStyle.UITableViewCellSeparatorStyleSingleLine
            rowHeight = UITableViewAutomaticDimension
            estimatedRowHeight = 72.0

            val formatter = NSDateFormatter().apply {
                dateStyle = NSDateFormatterMediumStyle
                timeStyle = NSDateFormatterNoStyle
                locale = NSLocale.currentLocale
                timeZone = NSTimeZone.localTimeZone
            }

            for (pickup in nextPickups) {
                val date = formatter.stringFromDate(
                    Instant.fromEpochMilliseconds(pickup.date).toNSDate()
                )
                val trashCan = when (pickup.type) {
                    "B" -> "Biotonne"
                    "R" -> "Restmülltonne"
                    "G" -> "Gelbe Tonne"
                    "P" -> "Papiertonne"
                    else -> continue
                }
                val tintColor = when (pickup.type) {
                    "B" -> UIColor.systemGreenColor
                    "R" -> UIColor.darkGrayColor
                    "G" -> UIColor.systemYellowColor
                    "P" -> UIColor.systemBlueColor
                    else -> continue
                }
                item(
                    trashCan,
                    subtitle = date,
                    icon = UIImage.imageNamed("Trashcan")
                ) {
                    iconTintColor = tintColor
                    accessoryType = UITableViewCellAccessoryType.UITableViewCellAccessoryNone
                }
            }
        }
        pickupsListView.setTranslatesAutoresizingMaskIntoConstraints(false)

        view.addSubview(pickupsListView)

        NSLayoutConstraint.activateConstraints(
            listOf(
                pickupsListView.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor),
                pickupsListView.bottomAnchor.constraintEqualToAnchor(view.bottomAnchor),
                pickupsListView.leadingAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.leadingAnchor),
                pickupsListView.trailingAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.trailingAnchor),
            )
        )

        renderReminderButton()
    }

    @ObjCAction
    fun onEnableReminder() {
        ioScope.launch {
            database.abfuhrQueries.setReminder(streetId).await()
            location = database.abfuhrQueries.getLocationByStreetId(streetId).executeAsOne()

            mainScope.launch {
                IosNotificationManager.requestAuthorization(completion = { granted, error ->
                    if (granted) {
                        mainScope.launch {
                            renderReminderButton()
                        }
                        enqueueNextPickups(streetId)
                    }
                })
            }
        }
    }

    @ObjCAction
    fun onDisableReminder() {
        ioScope.launch {
            database.abfuhrQueries.unsetReminder(streetId).await()
            location = database.abfuhrQueries.getLocationByStreetId(streetId).executeAsOne()

            mainScope.launch {
                renderReminderButton()
                dequeuePickups(streetId)
            }
        }
    }

    fun renderReminderButton() {
        if (location.hasReminder == 0L) {
            navigationItem.rightBarButtonItem = UIBarButtonItem(
                UIImage.systemImageNamed("bell.fill"),
                UIBarButtonItemStyle.UIBarButtonItemStylePlain,
                this,
                NSSelectorFromString("onEnableReminder")
            )
        } else {
            navigationItem.rightBarButtonItem = UIBarButtonItem(
                UIImage.systemImageNamed("bell.slash.fill"),
                UIBarButtonItemStyle.UIBarButtonItemStylePlain,
                this,
                NSSelectorFromString("onDisableReminder")
            )
        }
    }
}

fun createPickupDetailViewController(streetId: Long) = PickupDetailViewController(streetId)
