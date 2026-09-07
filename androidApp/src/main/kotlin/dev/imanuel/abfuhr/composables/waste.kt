package dev.imanuel.abfuhr.composables

import android.app.DownloadManager
import android.content.Intent
import android.os.Environment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.os.ConfigurationCompat
import androidx.navigation.NavController
import dev.imanuel.abfuhr.api.client.AbfuhrClient
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.models.AbfallAbcDisposalRoute
import dev.imanuel.abfuhr.models.AbfallAbcWaste
import dev.imanuel.abfuhr.search.SearchClient
import dev.imanuel.abfuhr.sync.SyncClient
import dev.imanuel.abfuhr.ui.SimpleTopSearchBar
import dev.imanuel.abfuhr.utils.firstSyncHappened
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val validLanguages = listOf(
    "de",
    "en",
    "fr",
    "ku",
    "ar"
)

@Composable
fun DisposalRouteDialog(
    onDismissRequest: () -> Unit,
    language: String,
    disposalRoute: AbfallAbcDisposalRoute,
    abfuhrClient: AbfuhrClient = koinInject()
) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val downloadManager = remember {
        context.getSystemService<DownloadManager>()
    }

    val downloadFile = { file: String, description: String ->
        coroutineScope.launch(Dispatchers.IO) {
            val baseUrl = abfuhrClient.getContentFilesUrl(language).removeSuffix("/")
            val cleanFile = file.removePrefix("/")
            val fileName = cleanFile.substringAfterLast('/')

            val request = DownloadManager.Request("$baseUrl/$cleanFile".toUri()).apply {
                setTitle(description)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }

            downloadManager?.enqueue(request)
        }
    }

    val openLink = { link: String ->
        val intent = Intent(Intent.ACTION_VIEW, link.toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Schließen")
            }
        },
        title = { Text(disposalRoute.title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(disposalRoute.description)

                if (disposalRoute.openingHours.isNotBlank()) {
                    Text(
                        "Öffnungszeiten", style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(disposalRoute.openingHours)
                }

                if (disposalRoute.street.isNotBlank() || disposalRoute.zipcode.isNotBlank() || disposalRoute.city.isNotBlank()) {
                    Text(
                        "Adresse",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        listOf(
                            disposalRoute.street,
                            listOf(
                                disposalRoute.zipcode,
                                disposalRoute.city
                            ).filter { it.isNotBlank() }.joinToString(" ")
                        ).joinToString(", ")
                    )
                }

                if (disposalRoute.fees.isNotBlank()) {
                    Text(
                        "Gebühren", style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(disposalRoute.fees)
                }

                val files = listOfNotNull(
                    disposalRoute.file1,
                    disposalRoute.file2,
                    disposalRoute.file3
                ).filter { it.isNotBlank() }
                val fileDescriptions = listOfNotNull(
                    disposalRoute.descriptionFile1,
                    disposalRoute.descriptionFile2,
                    disposalRoute.descriptionFile3
                ).filter { it.isNotBlank() }
                if (files.isNotEmpty() && fileDescriptions.isNotEmpty() && files.size == fileDescriptions.size) {
                    Text(
                        "Downloads",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    files.forEachIndexed { index, file ->
                        SegmentedListItem(
                            shapes = ListItemDefaults.segmentedShapes(index, files.size),
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        downloadFile(file, fileDescriptions[index])
                                    },
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "Download")
                                }
                            }
                        ) {
                            Text(fileDescriptions[index])
                        }
                    }
                }

                val links = listOfNotNull(
                    disposalRoute.link1,
                    disposalRoute.link2,
                    disposalRoute.link3
                ).filter { it.isNotBlank() }
                val linkDescriptions = listOfNotNull(
                    disposalRoute.descriptionLink1,
                    disposalRoute.descriptionLink2,
                    disposalRoute.descriptionLink3
                ).filter { it.isNotBlank() }
                if (links.isNotEmpty() && linkDescriptions.isNotEmpty() && links.size == linkDescriptions.size) {
                    Text(
                        "Links",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    links.forEachIndexed { index, link ->
                        SegmentedListItem(
                            shapes = ListItemDefaults.segmentedShapes(index, links.size),
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        openLink(link)
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.OpenInBrowser,
                                        contentDescription = "Download"
                                    )
                                }
                            }
                        ) {
                            Text(linkDescriptions[index])
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun WasteAbc(
    navController: NavController,
    syncClient: SyncClient = koinInject(),
    abfuhrClient: AbfuhrClient = koinInject(),
    database: AbfallDatabase = koinInject()
) {
    val context = LocalContext.current

    var searched by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }

    var keyword by remember { mutableStateOf("") }

    var wastes by remember { mutableStateOf(emptyList<AbfallAbcWaste>()) }

    val searchClient = remember {
        SearchClient(abfuhrClient, database, syncClient, context.firstSyncHappened())
    }

    val coroutineScope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val currentLocale = remember {
        ConfigurationCompat.getLocales(configuration).get(0)
    }

    LaunchedEffect(searching) {
        if (searching) {
            val currentLanguage = currentLocale?.language ?: "de"
            val language = if (validLanguages.contains(currentLanguage)) currentLanguage else "de"
            wastes = searchClient.searchAbfallAbc(keyword, language)
            searched = true
            searchOpen = false
            searching = false
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
                                query = keyword,
                                onQueryChange = { keyword = it },
                                onSearchExecuted = { searching = true },
                                onClose = { searchOpen = false },
                                searchLabel = "Suchen",
                                closeLabel = "Suche schließen",
                                placeholder = "Finde eine Abfallart",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Text("Abfall ABC")
                        }
                    },
                    actions = {
                        if (!searchOpen) {
                            IconButton(
                                onClick = { searchOpen = true }
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Abfallart suchen")
                            }
                        }
                    }
                )
            }
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (searched && wastes.isEmpty()) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Nichts gefunden", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "Zu deiner Suche haben wir leider nichts gefunden. Bitte schau ob du dich evtl. vertippt hast oder versuch etwas ähnliches",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else if (wastes.isEmpty()) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Wie entsorge ich richtig?",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        "Du hast etwas zu entsorgen und weißt nicht wohin? Tipp einfach oben auf die Lupe und such danach.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            LazyVerticalStaggeredGrid(
                modifier = Modifier.fillMaxSize(),
                columns = StaggeredGridCells.Adaptive(320.dp),
                contentPadding = PaddingValues(16.dp),
                verticalItemSpacing = 16.dp,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(wastes) { waste ->
                    ElevatedCard {
                        Column(
                            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        ) {
                            Text(waste.title, style = MaterialTheme.typography.headlineMedium)
                            Text(waste.description, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Entsorgung",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                            waste.routes
                                .flatMap { route ->
                                    listOfNotNull(
                                        route.alternativeRoute,
                                        route.collection,
                                        route.dischargePoint
                                    )
                                }
                                .forEachIndexed { index, route ->
                                    var open by remember { mutableStateOf(false) }
                                    SegmentedListItem(
                                        shapes = ListItemDefaults.segmentedShapes(
                                            index,
                                            waste.routes.size
                                        ),
                                        onClick = {
                                            open = true
                                        },
                                    ) {
                                        Text(route.title)
                                    }
                                    if (open) {
                                        DisposalRouteDialog(
                                            onDismissRequest = { open = false },
                                            disposalRoute = route,
                                            language = waste.language
                                        )
                                    }
                                }
                            if (waste.tips.isNotEmpty()) {
                                var open by remember { mutableStateOf(false) }
                                TextButton(
                                    onClick = { open = true },
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .padding(top = 8.dp)
                                ) {
                                    Text("Tipps anzeigen")
                                }
                                if (open) {
                                    AlertDialog(
                                        onDismissRequest = { open = false },
                                        confirmButton = {
                                            TextButton(onClick = { open = false }) {
                                                Text("Schließen")
                                            }
                                        },
                                        title = { Text("Tipps") },
                                        text = {
                                            Column(
                                                modifier = Modifier.verticalScroll(
                                                    rememberScrollState()
                                                )
                                            ) {
                                                for (tip in waste.tips) {
                                                    Text(tip.trim())
                                                }
                                            }
                                        }
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