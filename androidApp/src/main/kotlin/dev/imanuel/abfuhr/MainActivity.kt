package dev.imanuel.abfuhr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.imanuel.abfuhr.composables.PickupCalendarDialog
import dev.imanuel.abfuhr.composables.PickupScreen
import dev.imanuel.abfuhr.sync.SyncClient
import dev.imanuel.abfuhr.theme.AppTheme
import dev.imanuel.abfuhr.utils.firstSyncHappened
import dev.imanuel.abfuhr.utils.markFirstSync
import dev.imanuel.abfuhr.utils.markLastSync
import dev.imanuel.abfuhr.utils.syncDue
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

        if (!firstSyncHappened() || syncDue()) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    syncClient.sync()
                    if (syncClient.isSuccess.value) {
                        markFirstSync()
                        markLastSync()
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
    Locations
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

    LaunchedEffect(syncClientSyncing) {
        if (!syncClientSyncing) syncing = false
    }

    AppTheme {
        if (syncing) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LoadingIndicator()
                    Text(
                        "Bitte warte einen Moment, die Müllabfuhrdaten werden gerade auf dein Gerät runtergeladen, danach kannst du die App benutzen. Das geht sogar ohne Internet.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            NavHost(
                navController = navController,
                startDestination = Screens.Pickup.name
            ) {
                composable(Screens.Pickup.name) {
                    PickupScreen(
                        navController = navController
                    )
                }
            }
        }
    }
}