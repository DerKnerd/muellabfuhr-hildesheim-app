package dev.imanuel.abfuhr.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextDefaults
import dev.imanuel.abfuhr.R
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.database.AbfuhrLocation
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaZoneId
import org.koin.core.context.GlobalContext
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class NextPickupsFrontScreenWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val abfallDatabase = GlobalContext.get().get<AbfallDatabase>()
        val locations = abfallDatabase.abfuhrQueries.getLocationsWithReminder().executeAsList()

        provideContent {
            NextPickupUi(abfallDatabase, locations)
        }
    }

    @Composable
    private fun NextPickupUi(abfallDatabase: AbfallDatabase, locations: List<AbfuhrLocation>) {
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current

        val pickups by remember {
            derivedStateOf {
                locations
                    .associateWith {
                        val nextPickups =
                            abfallDatabase.abfuhrQueries.getPickupsByStreetId(it.streetId)
                                .executeAsList()
                                .filter { it.date >= Instant.now().toEpochMilli() }
                                .groupBy { pickup -> pickup.date }
                        nextPickups
                            .values
                            .firstOrNull()
                    }
                    .filter {
                        it.value != null
                    }
                    .map {
                        Pair(it.key, it.value!!)
                    }
                    .toMap()
            }
        }

        GlanceTheme {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start
            ) {
                if (locations.isEmpty()) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .cornerRadius(28.dp)
                            .background(GlanceTheme.colors.surface)
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                    ) {
                        Text(
                            "Du hast noch keine Erinnerungen eingerichtet",
                            style = TextDefaults.defaultTextStyle.copy(
                                fontSize = 16.sp,
                                color = GlanceTheme.colors.onSurface
                            )
                        )
                    }
                }
                if (locations.isNotEmpty() && pickups.isEmpty()) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .cornerRadius(28.dp)
                            .background(GlanceTheme.colors.surface)
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                    ) {
                        Text(
                            "Es stehen keine Abfuhrtermine an",
                            style = TextDefaults.defaultTextStyle.copy(
                                fontSize = 16.sp,
                                color = GlanceTheme.colors.onSurface
                            )
                        )
                    }
                }
                LazyColumn(
                    modifier = GlanceModifier.padding(4.dp).fillMaxSize()
                ) {
                    for ((location, pickups) in pickups) {
                        item {
                            Column(
                                modifier = GlanceModifier
                                    .padding(4.dp)
                            ) {
                                Column(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .cornerRadius(18.dp)
                                        .background(GlanceTheme.colors.surface)
                                ) {
                                    Row(
                                        modifier = GlanceModifier
                                            .fillMaxWidth()
                                            .background(GlanceTheme.colors.primary)
                                            .padding(horizontal = 14.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            location.street,
                                            style = TextDefaults.defaultTextStyle.copy(
                                                fontSize = 16.sp,
                                                color = GlanceTheme.colors.onPrimary
                                            )
                                        )
                                    }
                                    for ((streetId, date, isPostponed, type) in pickups) {
                                        val trashCanIcon = when (type) {
                                            "B" -> R.drawable.trashcan_b
                                            "R" -> R.drawable.trashcan_r
                                            "P" -> R.drawable.trashcan_p
                                            "G" -> R.drawable.trashcan_g
                                            else -> continue
                                        }
                                        val trashCan = when (type) {
                                            "B" -> "Biotonne"
                                            "R" -> "Restmülltonne"
                                            "P" -> "Papiertonne"
                                            "G" -> "gelbe Tonne"
                                            else -> continue
                                        }

                                        Row(
                                            modifier = GlanceModifier
                                                .fillMaxWidth()
                                                .cornerRadius(18.dp)
                                                .background(GlanceTheme.colors.surface)
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Image(
                                                ImageProvider(trashCanIcon),
                                                contentDescription = null,
                                                modifier = GlanceModifier
                                                    .padding(end = 16.dp)
                                                    .size(54.dp)
                                            )
                                            Column(modifier = GlanceModifier.defaultWeight()) {
                                                Text(
                                                    Instant
                                                        .ofEpochMilli(date)
                                                        .atZone(
                                                            TimeZone.currentSystemDefault()
                                                                .toJavaZoneId()
                                                        )
                                                        .toLocalDateTime()
                                                        .toLocalDate()
                                                        .format(
                                                            DateTimeFormatter.ofLocalizedDate(
                                                                FormatStyle.MEDIUM
                                                            )
                                                        ),
                                                    style = TextDefaults.defaultTextStyle.copy(
                                                        fontSize = 10.sp,
                                                        color = GlanceTheme.colors.onSurface
                                                    )
                                                )
                                                Text(
                                                    trashCan,
                                                    style = TextDefaults.defaultTextStyle.copy(
                                                        fontSize = 14.sp,
                                                        color = GlanceTheme.colors.onSurface
                                                    )
                                                )
                                                Text(
                                                    if (isPostponed == 1L) {
                                                        "Verschobene Abfuhr"
                                                    } else {
                                                        "Reguläre Abfuhr"
                                                    },
                                                    style = TextDefaults.defaultTextStyle.copy(
                                                        fontSize = 10.sp,
                                                        color = GlanceTheme.colors.onSurface
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class NextPickupsFrontScreenWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextPickupsFrontScreenWidget()
}
