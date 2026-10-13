@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.imanuel.abfuhr.composables

import android.content.Context
import android.location.Location
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.glance.appwidget.updateAll
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.imanuel.abfuhr.R
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.database.AbfuhrPickup
import dev.imanuel.abfuhr.geo.checkIfLocationInHildesheim
import dev.imanuel.abfuhr.models.AbfuhrLocation
import dev.imanuel.abfuhr.search.SearchClient
import dev.imanuel.abfuhr.ui.SimpleTopSearchBar
import dev.imanuel.abfuhr.utils.*
import dev.imanuel.abfuhr.widgets.NextPickupsFrontScreenWidget
import dev.imanuel.abfuhr.widgets.NextPickupsWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import org.koin.compose.koinInject
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.toJavaInstant

private suspend fun Context.updateAllWidgets() {
    NextPickupsWidget().updateAll(this)
    NextPickupsFrontScreenWidget().updateAll(this)
}

private fun nextPickupLine(nextPickups: List<dev.imanuel.abfuhr.models.AbfuhrPickup>): String {
    val date = nextPickups.first().date.toLocalDateTime(TimeZone.currentSystemDefault()).let {
        buildString {
            append(it.day + 1)
            append(". ")
            append(
                when (it.month) {
                    Month.JANUARY -> "Jan"
                    Month.FEBRUARY -> "Feb"
                    Month.MARCH -> "Mär"
                    Month.APRIL -> "Apr"
                    Month.MAY -> "Mai"
                    Month.JUNE -> "Jun"
                    Month.JULY -> "Jul"
                    Month.AUGUST -> "Aug"
                    Month.SEPTEMBER -> "Sep"
                    Month.OCTOBER -> "Okt"
                    Month.NOVEMBER -> "Nov"
                    Month.DECEMBER -> "Dez"
                }
            )
        }
    }

    val cans = if (nextPickups.size > 1) {
        nextPickups.map {
            when (it.type) {
                "B" -> "Bio"
                "R", "S" -> "Restmüll"
                "G" -> "Gelbe Tonne"
                "P" -> "Papier"
                else -> ""
            }
        }
    } else {
        listOf(
            when (nextPickups.first().type) {
                "B" -> "Bio"
                "R", "S" -> "Restmüll"
                "G" -> "Gelbe Tonne"
                "P" -> "Papier"
                else -> ""
            }
        )
    }.filter { it.isNotEmpty() }.distinct()

    return "$date: ${cans.joinToString(", ")}"
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PickupCalendarDialog(
    canRemindMe: Boolean,
    pickups: List<AbfuhrPickup>,
    location: dev.imanuel.abfuhr.database.AbfuhrLocation,
    onDismissRequest: () -> Unit,
    abfallDatabase: AbfallDatabase = koinInject()
) {
    val context = LocalContext.current

    val today = remember {
        Clock
            .System
            .now()
            .toLocalDateTime(
                TimeZone.currentSystemDefault()
            )
            .date
            .atTime(0, 0)
            .toInstant(TimeZone.currentSystemDefault())
    }

    val coroutineScope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(false) }

    var pickups by remember { mutableStateOf(pickups) }
    var location by remember { mutableStateOf(location) }

    val snackbarHostState = remember { SnackbarHostState() }

    val agendaState = rememberLazyListState(pickups.indexOfFirst {
        it.date >= today.toEpochMilliseconds()
    })

    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }
    val enqueueReminder = {
        context.createAbfuhrNotifications(
            abfallDatabase,
            listOf(location)
        )
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                abfallDatabase.abfuhrQueries.setReminder(location.streetId)
                context.updateAllWidgets()
                loading = true
            }
        }
    }
    val remindMe = {
        if (notificationPermission?.status?.isGranted == false) {
            notificationPermission.launchPermissionRequest()
        }
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                enqueueReminder()
                val snackbarResult = snackbarHostState.showSnackbar(
                    "Erinnerung für ${location.street} eingerichtet",
                    actionLabel = "Doch nicht",
                    duration = SnackbarDuration.Short
                )
                if (snackbarResult == SnackbarResult.ActionPerformed) {
                    context.clearAbfuhrNotifications(listOf(location))
                    abfallDatabase.abfuhrQueries.unsetReminder(location.streetId)
                    context.updateAllWidgets()
                    loading = true
                }
            }
        }
    }
    val stopRemindingMe = {
        context.clearAbfuhrNotifications(listOf(location))
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                abfallDatabase.abfuhrQueries.unsetReminder(location.streetId)
                context.updateAllWidgets()
                loading = true
                val snackbarResult = snackbarHostState.showSnackbar(
                    "Erinnerung für ${location.street} wurden gelöscht",
                    actionLabel = "Rückgängig",
                    duration = SnackbarDuration.Short
                )
                if (snackbarResult == SnackbarResult.ActionPerformed) {
                    enqueueReminder()
                }
            }
        }
    }

    val isExpandedWindow =
        currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(840)

    LaunchedEffect(loading) {
        if (loading) {
            withContext(Dispatchers.IO) {
                pickups =
                    abfallDatabase
                        .abfuhrQueries
                        .getPickupsByStreetId(location.streetId)
                        .executeAsList()
                location = abfallDatabase
                    .abfuhrQueries
                    .getLocationByStreetId(location.streetId)
                    .executeAsOne()
                loading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = isExpandedWindow)
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(snackbarHostState)
            },
            topBar = {
                TopAppBar(
                    title = {
                        Text(location.street)
                    },
                    navigationIcon = {
                        if (!isExpandedWindow)
                            IconButton(onClick = { onDismissRequest() }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Schließen"
                                )
                            }
                    },
                    scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                    actions = {
                        if (isExpandedWindow)
                            IconButton(onClick = { onDismissRequest() }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Schließen"
                                )
                            }
                    }
                )
            },
            floatingActionButton = {
                if (canRemindMe && location.hasReminder == 1L) {
                    FloatingActionButton(
                        onClick = {
                            stopRemindingMe()
                        }
                    ) {
                        Icon(
                            Icons.Filled.NotificationsOff,
                            contentDescription = "Erinner mich nicht mehr an diese Adresse"
                        )
                    }
                } else if (canRemindMe && location.hasReminder == 0L) {
                    FloatingActionButton(
                        onClick = {
                            remindMe()
                        }
                    ) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "Erinner mich an diese Adresse"
                        )
                    }
                }
            },
            modifier = Modifier
                .graphicsLayer {
                    if (isExpandedWindow) {
                        shape = RoundedCornerShape(24.dp)
                        clip = true
                        shadowElevation = 8.0f
                    }
                }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                val pullToRefreshState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = loading,
                    onRefresh = { loading = true },
                    state = pullToRefreshState,
                    modifier = Modifier.fillMaxSize(),
                    enabled = canRemindMe,
                    indicator = {
                        PullToRefreshDefaults.LoadingIndicator(
                            modifier = Modifier.align(Alignment.TopCenter),
                            state = pullToRefreshState,
                            isRefreshing = loading
                        )
                    }
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = agendaState
                    ) {
                        items(pickups.size) {
                            val pickup = pickups[it]
                            val trashCan = when (pickup.type) {
                                "B" -> "Biotonne"
                                "R" -> "Restmülltonne"
                                "S" -> "Restmülltonne (14-tägige Abfuhr)"
                                "P" -> "Papiertonne"
                                "G" -> "Gelbe Tonne"
                                else -> ""
                            }
                            val trashCanColor =
                                if (pickup.date > today.toEpochMilliseconds()) {
                                    when (pickup.type) {
                                        "B" -> Color(0xFF388E3C)
                                        "R", "S" -> Color(0xFF2D2D2D)
                                        "P" -> Color(0xFF2196F3)
                                        "G" -> Color(0xFFFFEB3B)
                                        else -> Color(0x00000000)
                                    }
                                } else {
                                    when (pickup.type) {
                                        "B" -> Color(0x80388E3C)
                                        "R", "S" -> Color(0x802D2D2D)
                                        "P" -> Color(0x802196F3)
                                        "G" -> Color(0x80FFEB3B)
                                        else -> Color(0x00000000)
                                    }
                                }
                            ListItem(
                                supportingContent = {
                                    if (pickup.isPostponed == 1L) {
                                        Text("Verschobene Abfuhr")
                                    } else if (pickup.date < today.toEpochMilliseconds()) {
                                        Text("Vergangene Abfuhr")
                                    } else {
                                        Text("Reguläre Abfuhr")
                                    }
                                },
                                leadingContent = {
                                    Icon(
                                        ImageVector.vectorResource(R.drawable.trashcan),
                                        contentDescription = null,
                                        tint = trashCanColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                overlineContent = {
                                    Text(
                                        Instant.fromEpochMilliseconds(pickup.date).toJavaInstant()
                                            .atZone(
                                                TimeZone.currentSystemDefault().toJavaZoneId()
                                            ).toLocalDate().format(
                                                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                                            )
                                    )
                                }
                            ) {
                                Text(trashCan)
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class PickupTabs {
    Search,
    Active
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalPermissionsApi::class)
@Composable
fun PickupScreen(
    navController: NavController,
    searchClient: SearchClient = koinInject(),
    database: AbfallDatabase = koinInject()
) {
    val context = LocalContext.current

    val syncSuccessful = remember {
        context.firstSyncHappened()
    }

    val geolocationPermission = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    var locateMeNow by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var loadReminderLocations by remember { mutableStateOf(true) }
    var searched by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }

    var activeTab by remember { mutableStateOf(PickupTabs.Search) }

    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()

    val locationEnabled = remember {
        context.isLocationEnabled()
    }
    val today = remember {
        Clock.System.now()
    }

    var locations by remember { mutableStateOf(emptyList<AbfuhrLocation>()) }
    var locationsWithReminder by remember { mutableStateOf(emptyList<dev.imanuel.abfuhr.database.AbfuhrLocation>()) }

    var locationAddressKeyword by remember { mutableStateOf("") }
    var notInHildesheimMessageOpen by remember { mutableStateOf(false) }
    var coordinates by remember { mutableStateOf<Location?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val inHildesheim by remember {
        derivedStateOf {
            coordinates != null && checkIfLocationInHildesheim(
                coordinates!!.latitude,
                coordinates!!.longitude
            )
        }
    }

    LaunchedEffect(locateMeNow, geolocationPermission.status) {
        if (locateMeNow && geolocationPermission.status.isGranted) {
            loading = true
            val location = context.fetchFineLocation()
            coordinates = location
            if (location != null) {
                locations =
                    searchClient.searchAbfuhrByGeolocation(location.latitude, location.longitude)
            }
            notInHildesheimMessageOpen = !inHildesheim
            locateMeNow = false
            loading = false
            searched = true
        }
    }

    LaunchedEffect(loadReminderLocations) {
        if (loadReminderLocations) {
            locationsWithReminder =
                database.abfuhrQueries.getLocationsWithReminder().executeAsList()
            loadReminderLocations = false
        }
    }
    LaunchedEffect(locationAddressKeyword) {
        loading = true
        locations = searchClient.searchAbfuhr(locationAddressKeyword)
        loading = false
        searched = true
    }

    val locateMe = {
        locateMeNow = true
        if (!geolocationPermission.status.isGranted) {
            geolocationPermission.launchPermissionRequest()
        }
    }
    val searchByAddress = { query: String ->
        val search = query.ifBlank { locationAddressKeyword }
        if (search.isNotBlank()) {
            coroutineScope.launch {
                loading = true
                locations = searchClient.searchAbfuhr(search)
                loading = false
                searched = true
                searchOpen = false
            }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                TopAppBar(
                    title = {
                        if (searchOpen) {
                            SimpleTopSearchBar(
                                query = locationAddressKeyword,
                                onQueryChange = { locationAddressKeyword = it },
                                onSearchExecuted = { searchByAddress(it) },
                                onClose = {
                                    searchOpen = false
                                    locationAddressKeyword = ""
                                },
                                searchLabel = "Suchen",
                                closeLabel = "Suche schließen",
                                placeholder = "Finde eine Adresse",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Text("Abfuhrtermine")
                        }
                    },
                    actions = {
                        if (!searchOpen && activeTab == PickupTabs.Search) {
                            IconButton(
                                onClick = { searchOpen = true }
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Adresse suchen")
                            }
                        }
                    }
                )
                if (!searchOpen) {
                    PrimaryTabRow(
                        selectedTabIndex = activeTab.ordinal,
                    ) {
                        Tab(
                            selected = activeTab == PickupTabs.Search,
                            onClick = { activeTab = PickupTabs.Search }
                        ) {
                            Text(
                                "Adresssuche",
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        Tab(
                            selected = activeTab == PickupTabs.Active,
                            onClick = { activeTab = PickupTabs.Active },
                        ) {
                            Text(
                                "Meine Adressen",
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (syncSuccessful && locationEnabled && activeTab == PickupTabs.Search) {
                ExtendedFloatingActionButton(
                    onClick = { locateMe() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationSearching,
                            contentDescription = null
                        )
                        Text("Finde mich")
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (activeTab == PickupTabs.Search) {
                    if (loading) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LoadingIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                    if (searched && locations.isEmpty()) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("Nichts gefunden", style = MaterialTheme.typography.headlineLarge)
                            Text(
                                "Zu deiner Suche haben wir leider nichts gefunden. Bitte schau ob du dich evtl. vertippt hast",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else if (locations.isEmpty()) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Finde eine Adresse",
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Text(
                                "Du kannst entweder oben über die Lupe nach deiner Adresse suchen oder die Straßen in deiner Nähe anzeigen über den Button Finde mich",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(locations.size) {
                            val location = locations[it]
                            var open by remember { mutableStateOf(false) }
                            SegmentedListItem(
                                shapes = ListItemDefaults.segmentedShapes(it, locations.size),
                                supportingContent = {
                                    Text(nextPickupLine(location.pickups.filter { pickup -> pickup.date > today }))
                                },
                                onClick = {
                                    open = true
                                },
                            ) {
                                if (location.locality == location.district || location.locality.lowercase() == "hildesheim") {
                                    Text("${location.locality} – ${location.street}")
                                } else {
                                    Text("${location.locality} – ${location.district}, ${location.street}")
                                }
                            }
                            if (open) {
                                val databaseLocation by remember {
                                    derivedStateOf {
                                        database
                                            .abfuhrQueries
                                            .getLocationByStreetId(location.streetId)
                                            .executeAsOneOrNull()

                                    }
                                }
                                val databasePickups by remember {
                                    derivedStateOf {
                                        database
                                            .abfuhrQueries
                                            .getPickupsByStreetId(location.streetId)
                                            .executeAsList()
                                    }
                                }
                                PickupCalendarDialog(
                                    pickups = databasePickups,
                                    location = databaseLocation
                                        ?: dev.imanuel.abfuhr.database.AbfuhrLocation(
                                            streetId = location.streetId,
                                            street = location.street,
                                            locality = location.locality,
                                            localityId = location.localityId,
                                            district = location.district,
                                            districtId = location.districtId,
                                            streetLatitude = location.streetLatitude,
                                            streetLongitude = location.streetLongitude,
                                            hasReminder = 0
                                        ),
                                    canRemindMe = databaseLocation != null,
                                    onDismissRequest = {
                                        open = false
                                        loadReminderLocations = true
                                    }
                                )
                            }
                        }
                    }
                } else if (activeTab == PickupTabs.Active) {
                    if (locationsWithReminder.isEmpty()) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Keine Erinnerungen",
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Text(
                                "Du hast dir noch keine Erinnerungen eingerichtet, geh oben auf Adresssuche und wähle eine Adresse aus",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(locationsWithReminder.size) {
                            val location = locationsWithReminder[it]
                            var open by remember { mutableStateOf(false) }
                            val pickups by remember {
                                derivedStateOf {
                                    database
                                        .abfuhrQueries
                                        .getPickupsByStreetId(location.streetId)
                                        .executeAsList()
                                }
                            }
                            SegmentedListItem(
                                shapes = ListItemDefaults.segmentedShapes(
                                    it,
                                    locationsWithReminder.size
                                ),
                                supportingContent = {
                                    Text(
                                        nextPickupLine(
                                            pickups
                                                .map { pickup ->
                                                    dev.imanuel.abfuhr.models.AbfuhrPickup(
                                                        streetId = pickup.streetId,
                                                        date = Instant.fromEpochMilliseconds(pickup.date),
                                                        isPostponed = pickup.isPostponed == 1L,
                                                        type = pickup.type,
                                                    )
                                                }
                                                .filter { pickup -> pickup.date > today }
                                        ))
                                },
                                onClick = {
                                    open = true
                                },
                            ) {
                                if (location.locality == location.district || location.locality.lowercase() == "hildesheim") {
                                    Text("${location.locality} – ${location.street}")
                                } else {
                                    Text("${location.locality} – ${location.district}, ${location.street}")
                                }
                            }
                            if (open) {
                                PickupCalendarDialog(
                                    pickups = pickups,
                                    location = location,
                                    canRemindMe = true,
                                    onDismissRequest = {
                                        open = false
                                        loadReminderLocations = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        if (notInHildesheimMessageOpen) {
            AlertDialog(
                onDismissRequest = { notInHildesheimMessageOpen = false },
                title = { Text("Nicht in Hildesheim") },
                text = { Text("Du bist nicht im Landkreis Hildesheim. Bitte benutze die Textsuche um eine Straße zu finden.") },
                confirmButton = {
                    Button(onClick = { notInHildesheimMessageOpen = false }) {
                        Text("Schließen")
                    }
                }
            )
        }
    }
}
