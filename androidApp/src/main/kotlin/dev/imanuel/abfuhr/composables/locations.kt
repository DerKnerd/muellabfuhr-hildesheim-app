package dev.imanuel.abfuhr.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberUpdatedMarkerState
import dev.imanuel.abfuhr.R
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.database.Location
import dev.imanuel.abfuhr.utils.fetchFineLocation
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

enum class ContainerFilter {
    Paper,
    Glass
}

fun LatLngBounds.containsLocation(lat: Double, lng: Double): Boolean {
    return lat >= southwest.latitude && lat <= northeast.latitude &&
            lng >= southwest.longitude && lng <= northeast.longitude
}

@OptIn(
    ExperimentalPermissionsApi::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun LocationsScreen(
    abfallDatabase: AbfallDatabase = koinInject()
) {
    val context = LocalContext.current

    var currentLocation by remember { mutableStateOf(LatLng(52.15, 9.95)) }

    var foundMe by remember { mutableStateOf(false) }
    var located by remember { mutableStateOf(false) }

    var cameraPositionState by remember {
        mutableStateOf(
            CameraPositionState(
                position = CameraPosition.fromLatLngZoom(
                    currentLocation,
                    12f
                )
            )
        )
    }

    val isZoomedInEnough by remember { derivedStateOf { cameraPositionState.position.zoom >= 16f } }

    var filters by remember {
        mutableStateOf(
            setOf<ContainerFilter>(
                ContainerFilter.Paper,
                ContainerFilter.Glass
            )
        )
    }

    var selectedLocation by remember { mutableStateOf<Location?>(null) }

    val allLocations = remember { abfallDatabase.locationQueries.getAllLocations().executeAsList() }

    val coroutineScope = rememberCoroutineScope()

    val geolocationPermission = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) { result ->
        if (result) {
            coroutineScope.launch {
                val loc = context.fetchFineLocation()
                if (loc != null) {
                    cameraPositionState.position =
                        CameraPosition.fromLatLngZoom(LatLng(loc.latitude, loc.longitude), 17f)
                    currentLocation = LatLng(loc.latitude, loc.longitude)
                    foundMe = true
                    located = true
                }
            }
        } else {
            located = true
        }
    }

    val visibleLocations by remember(allLocations, cameraPositionState.isMoving) {
        derivedStateOf {
            if (!isZoomedInEnough) {
                return@derivedStateOf allLocations.filter { it.type != "container" }
            }

            val bounds = cameraPositionState.projection?.visibleRegion?.latLngBounds
            if (bounds == null) {
                emptyList()
            } else {
                allLocations.filter { location ->
                    bounds.containsLocation(location.latitude, location.longitude)
                }.filter { location ->
                    if (filters.containsAll(setOf(ContainerFilter.Paper, ContainerFilter.Glass))) {
                        true
                    } else if (filters.contains(ContainerFilter.Paper)) {
                        location.description.lowercase().contains(", papier")
                    } else if (filters.contains(ContainerFilter.Glass)) {
                        location.description.lowercase().contains(", glas")
                    } else {
                        false
                    }
                }
            }
        }
    }

    val toggleFilter = { filter: ContainerFilter ->
        if (filters.contains(filter)) {
            filters -= filter
        } else {
            filters += filter
        }
        if (filters.isEmpty()) {
            filters += ContainerFilter.Paper
            filters += ContainerFilter.Glass
        }
    }

    LaunchedEffect(Unit) {
        geolocationPermission.launchPermissionRequest()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        if (!located) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Einen Moment, wir versuchen deine Position zu finden")
                LoadingIndicator()
            }
        } else {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                if (foundMe) {
                    MarkerComposable(
                        state = rememberUpdatedMarkerState(position = currentLocation),
                        contentDescription = "Deine Position",
                    ) {
                        Image(ImageVector.vectorResource(R.drawable.home_marker), "Deine Position")
                    }
                }
                visibleLocations.forEach { location ->
                    key(location.id) {
                        val icon = when (location.type) {
                            "container" -> R.drawable.container_marker
                            "deponie", "dump" -> R.drawable.dump_marker
                            "office" -> R.drawable.office_marker
                            else -> return@forEach
                        }
                        val position =
                            remember(location.id, location.latitude, location.longitude) {
                                LatLng(location.latitude, location.longitude)
                            }

                        val markerState = rememberUpdatedMarkerState(position = position)

                        MarkerComposable(
                            state = markerState,
                            contentDescription = location.description.trim(),
                            title = location.description.trim(),
                            onClick = {
                                selectedLocation = location
                                true
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                            ) {
                                Image(
                                    imageVector = ImageVector.vectorResource(icon),
                                    contentDescription = location.name.trim(),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
            if (located && !isZoomedInEnough) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 6.dp
                ) {
                    Text(
                        text = "Zoome heran, um Container zu sehen",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (located) {
                HorizontalFloatingToolbar(
                    expanded = false,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp),
                ) {
                    TooltipBox(
                        positionProvider =
                            TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Above
                            ),
                        tooltip = {
                            PlainTooltip {
                                Text("Papiercontainer anzeigen")
                            }
                        },
                        state = rememberTooltipState()
                    ) {
                        IconToggleButton(
                            checked = filters.contains(ContainerFilter.Paper),
                            onCheckedChange = {
                                toggleFilter(ContainerFilter.Paper)
                            }
                        ) {
                            Icon(
                                ImageVector.vectorResource(R.drawable.paper_container),
                                contentDescription = null,
                            )
                        }
                    }
                    TooltipBox(
                        positionProvider =
                            TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Above
                            ),
                        tooltip = {
                            PlainTooltip {
                                Text("Glasscontainer anzeigen")
                            }
                        },
                        state = rememberTooltipState()
                    ) {
                        IconToggleButton(
                            checked = filters.contains(ContainerFilter.Glass),
                            onCheckedChange = {
                                toggleFilter(ContainerFilter.Glass)
                            }
                        ) {
                            Icon(
                                ImageVector.vectorResource(R.drawable.glass_container),
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
            selectedLocation?.let { location ->
                AlertDialog(
                    onDismissRequest = { selectedLocation = null },
                    confirmButton = {
                        TextButton(onClick = { selectedLocation = null }) {
                            Text("Schließen")
                        }
                    },
                    title = { Text(location.name) },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(
                                rememberScrollState()
                            )
                        ) {
                            Text(location.description.trim())
                            Text(
                                "Öffnungszeiten", style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(location.openingHours.trim())
                            if (location.fax.isNotBlank() || location.mail.isNotBlank() || location.www.isNotBlank() || location.tel.isNotBlank()) {
                                Text(
                                    "Kontaktdaten",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                if (location.tel.isNotBlank()) {
                                    Text("Telefon: ${location.tel.trim()}")
                                }
                                if (location.mail.isNotBlank()) {
                                    Text("Mail: ${location.mail.trim()}")
                                }
                                if (location.fax.isNotBlank()) {
                                    Text("Fax: ${location.fax.trim()}")
                                }
                                if (location.www.isNotBlank()) {
                                    Text("Webseite: ${location.www.trim()}")
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
