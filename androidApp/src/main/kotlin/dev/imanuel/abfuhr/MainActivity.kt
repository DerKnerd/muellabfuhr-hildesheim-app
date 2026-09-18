package dev.imanuel.abfuhr

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import dev.imanuel.abfuhr.composables.LocationsScreen
import dev.imanuel.abfuhr.composables.PickupScreen
import dev.imanuel.abfuhr.composables.ReportWasteScreen
import dev.imanuel.abfuhr.composables.WasteAbcScreen
import dev.imanuel.abfuhr.sync.SyncClient
import dev.imanuel.abfuhr.theme.AppTheme
import dev.imanuel.abfuhr.utils.enqueueRefreshDataWorker
import dev.imanuel.abfuhr.utils.firstSyncHappened
import dev.imanuel.abfuhr.utils.isLocationEnabled
import dev.imanuel.abfuhr.utils.markFirstSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    val syncClient by inject<SyncClient>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val notificationManager = getSystemService<NotificationManager>()
        notificationManager?.createNotificationChannel(
            NotificationChannel(
                "trash-reminder",
                "Erinnerungen für Abfuhrtermine",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        if (!firstSyncHappened() ) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    syncClient.sync()
                    if (syncClient.isSuccess.value) {
                        markFirstSync()
                        enqueueRefreshDataWorker()
                    }
                }
            }
        }

        setContent {
            MainComposable()
        }
    }
}

enum class Screens {
    Pickup,
    WasteAbc,
    Locations,
    ReportWaste
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainComposable(
    syncClient: SyncClient = koinInject(),
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val syncClientSyncing by syncClient.isSyncing.collectAsState()
    var syncing by remember { mutableStateOf(!context.firstSyncHappened() && syncClientSyncing) }

    var activeScreen by remember { mutableStateOf(Screens.Pickup) }

    val locationEnabled = remember {
        context.isLocationEnabled()
    }

    LaunchedEffect(syncClientSyncing) {
        if (!syncClientSyncing) syncing = false
    }

    AppTheme {
        if (syncing) {
            Surface(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.CenterVertically
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LoadingIndicator()
                        Text(
                            "Bitte warte einen Moment, die Müllabfuhrdaten werden gerade auf dein Gerät runtergeladen, danach kannst du die App benutzen. Das geht sogar ohne Internet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        } else {
            NavigationSuiteScaffold(
                navigationItems = {
                    NavigationSuiteItem(
                        selected = activeScreen == Screens.Pickup,
                        onClick = { activeScreen = Screens.Pickup },
                        icon = {
                            Icon(
                                imageVector = if (activeScreen == Screens.Pickup) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                                contentDescription = null
                            )
                        },
                        label = { Text("Abfuhrtermine") }
                    )
                    NavigationSuiteItem(
                        selected = activeScreen == Screens.WasteAbc,
                        onClick = { activeScreen = Screens.WasteAbc },
                        icon = {
                            Icon(
                                imageVector = if (activeScreen == Screens.WasteAbc) Icons.Filled.Recycling else Icons.Outlined.Recycling,
                                contentDescription = "Sortierhilfe"
                            )
                        },
                        label = {
                            Text("Sortierhilfe")
                        }
                    )
                    NavigationSuiteItem(
                        selected = activeScreen == Screens.Locations,
                        onClick = { activeScreen = Screens.Locations },
                        icon = {
                            Icon(
                                imageVector = if (activeScreen == Screens.Locations) Icons.Filled.LocationOn else Icons.Outlined.LocationOn,
                                contentDescription = "Standorte"
                            )
                        },
                        label = {
                            Text("Standorte")
                        }
                    )
                    if (locationEnabled) {
                        NavigationSuiteItem(
                            selected = activeScreen == Screens.ReportWaste,
                            onClick = { activeScreen = Screens.ReportWaste },
                            icon = {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.broom),
                                    contentDescription = "Müll melden"
                                )
                            },
                            label = {
                                Text("Müll melden")
                            }
                        )
                    }
                },
                navigationItemVerticalArrangement = Arrangement.Center
            ) {
                when (activeScreen) {
                    Screens.Pickup -> PickupScreen(
                        navController = navController
                    )

                    Screens.WasteAbc -> WasteAbcScreen(
                        navController = navController
                    )

                    Screens.Locations -> LocationsScreen()

                    Screens.ReportWaste -> ReportWasteScreen()
                }
            }
        }
    }
}