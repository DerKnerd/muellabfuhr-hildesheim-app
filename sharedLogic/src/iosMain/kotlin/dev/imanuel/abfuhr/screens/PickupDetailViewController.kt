@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package dev.imanuel.abfuhr.screens

import dev.imanuel.abfuhr.AbfuhrNavDestination
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.models.AbfuhrLocation
import dev.imanuel.abfuhr.models.AbfuhrPickup
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

class PickupDetailViewController(
    private val location: AbfuhrLocation
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

    private lateinit var nextPickups: List<AbfuhrPickup>
    private var databaseLocation: dev.imanuel.abfuhr.database.AbfuhrLocation? = null

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun viewDidLoad() {
        super.viewDidLoad()

        databaseLocation = database.abfuhrQueries.getLocationByStreetId(location.streetId).executeAsOneOrNull()

        title = if (location.locality == "Hildesheim") {
            "${location.street} Hildesheim"
        } else {
            "${location.street} ${location.district}"
        }

        nextPickups = location.pickups.filter { it.date >= Clock.System.now() }

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
                    pickup.date.toNSDate()
                )
                val trashCan = when (pickup.type) {
                    "B" -> "Biotonne"
                    "R" -> "Restmülltonne"
                    "S" -> "Restmülltonne (14-tägige Abfuhr)"
                    "G" -> "Gelbe Tonne"
                    "P" -> "Papiertonne"
                    else -> continue
                }
                val tintColor = when (pickup.type) {
                    "B" -> UIColor.systemGreenColor
                    "R", "S" -> UIColor.darkGrayColor
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

        if (databaseLocation != null) {
            renderReminderButton()
        }
    }

    @ObjCAction
    fun onEnableReminder() {
        ioScope.launch {
            database.abfuhrQueries.setReminder(location.streetId).await()
            databaseLocation = database.abfuhrQueries.getLocationByStreetId(location.streetId).executeAsOne()

            mainScope.launch {
                IosNotificationManager.requestAuthorization(completion = { granted, error ->
                    if (granted) {
                        mainScope.launch {
                            renderReminderButton()
                        }
                        enqueueNextPickups(location.streetId)
                    }
                })
            }
        }
    }

    @ObjCAction
    fun onDisableReminder() {
        ioScope.launch {
            database.abfuhrQueries.unsetReminder(location.streetId).await()
            databaseLocation = database.abfuhrQueries.getLocationByStreetId(location.streetId).executeAsOne()

            mainScope.launch {
                renderReminderButton()
                dequeuePickups(location.streetId)
            }
        }
    }

    fun renderReminderButton() {
        if (databaseLocation != null && databaseLocation!!.hasReminder == 0L) {
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

fun createPickupDetailViewController(location: AbfuhrLocation) =
    PickupDetailViewController(location)
