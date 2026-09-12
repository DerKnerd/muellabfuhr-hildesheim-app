package dev.imanuel.abfuhr.composables

import android.graphics.Bitmap
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.imanuel.abfuhr.api.client.AbfuhrClient
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.geo.checkIfLocationInHildesheim
import dev.imanuel.abfuhr.models.AbfuhrLocation
import dev.imanuel.abfuhr.search.SearchClient
import dev.imanuel.abfuhr.sync.SyncClient
import dev.imanuel.abfuhr.utils.fetchFineLocation
import dev.imanuel.abfuhr.utils.firstSyncHappened
import dev.imanuel.abfuhr.utils.hasInternetConnection
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.ByteArrayOutputStream

private fun Bitmap.toJpegByteList(): ByteArray {
    val outputStream = ByteArrayOutputStream()

    compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

    return outputStream.toByteArray()
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ReportWasteScreen(
    syncClient: SyncClient = koinInject(),
    abfuhrClient: AbfuhrClient = koinInject(),
    database: AbfallDatabase = koinInject()
) {
    val context = LocalContext.current
    val searchClient = remember {
        SearchClient(abfuhrClient, database, syncClient, context.firstSyncHappened())
    }

    val geolocationPermission = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    val coroutineScope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    var locateMeNow by remember { mutableStateOf(false) }
    var notInHildesheimMessageOpen by remember { mutableStateOf(false) }

    var myLocation by remember { mutableStateOf<AbfuhrLocation?>(null) }
    var coordinates by remember { mutableStateOf<Location?>(null) }

    val inHildesheim by remember {
        derivedStateOf {
            if (coordinates != null) checkIfLocationInHildesheim(
                coordinates!!.latitude,
                coordinates!!.longitude
            ) else false
        }
    }

    var comment by remember { mutableStateOf("") }
    var picture by remember { mutableStateOf<Bitmap?>(null) }

    val locationInteractionsSource = remember { MutableInteractionSource() }
    val locationInteractions by locationInteractionsSource.collectIsPressedAsState()

    val locateMe = {
        locateMeNow = true
        if (!geolocationPermission.status.isGranted) {
            geolocationPermission.launchPermissionRequest()
        }
    }

    val reportWaste = {
        coroutineScope.launch {
            if (inHildesheim && coordinates != null && myLocation != null) {
                val address = if (myLocation!!.locality == "Hildesheim") {
                    "${myLocation!!.street} Hildesheim"
                } else {
                    "${myLocation!!.street} ${myLocation!!.district}"
                }
                val result = abfuhrClient.reportWaste(
                    coordinates!!.latitude,
                    coordinates!!.longitude,
                    address,
                    comment,
                    picture?.toJpegByteList()
                )
                if (result) {
                    snackbarHostState.showSnackbar("Der Müll wurde gemeldet")
                } else if (context.hasInternetConnection()) {
                    snackbarHostState.showSnackbar("Leider konnte der Müll nicht gemeldet werden")
                } else {
                    snackbarHostState.showSnackbar("Du hast kein Internet, ohne kann der Müll nicht gemeldet werden")
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        picture = bitmap
    }

    LaunchedEffect(locationInteractions) {
        if (locationInteractions) locateMe()
    }

    LaunchedEffect(locateMeNow, geolocationPermission.status) {
        if (locateMeNow && geolocationPermission.status.isGranted) {
            coordinates = context.fetchFineLocation()
            if (coordinates != null) {
                myLocation =
                    searchClient.searchAbfuhrByGeolocation(
                        coordinates!!.latitude,
                        coordinates!!.longitude
                    )
                        .firstOrNull()
            }
            notInHildesheimMessageOpen = !inHildesheim
            locateMeNow = false
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                TopAppBar(
                    title = {
                        Text("Müll melden")
                    },
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { reportWaste() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Send,
                        contentDescription = null
                    )
                    Text("Müll melden")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = myLocation?.let {
                        if (it.locality == "Hildesheim") {
                            "${it.street} Hildesheim"
                        } else {
                            "${it.street} ${it.district}"
                        }
                    } ?: "",
                    label = { Text("Meine Position") },
                    onValueChange = {},
                    trailingIcon = {
                        Icon(Icons.Default.LocationSearching, contentDescription = "Finde mich")
                    },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("Tippe hier um deine Position zu finden")
                    },
                    interactionSource = locationInteractionsSource
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Kommentar") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                )
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (picture != null) {
                        Image(
                            picture!!.asImageBitmap(),
                            contentDescription = "Dein Foto",
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth(0.5f)
                                .heightIn(240.dp),
                        )
                    }
                    Button(
                        onClick = {
                            cameraLauncher.launch(null)
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .fillMaxWidth(0.5f)
                    ) {
                        Text("Foto aufnehmen")
                    }
                }
            }
        }
        if (notInHildesheimMessageOpen) {
            AlertDialog(
                onDismissRequest = { notInHildesheimMessageOpen = false },
                title = { Text("Nicht in Hildesheim") },
                text = { Text("Du bist nicht im Landkreis Hildesheim. Daher kannst du leider keinen Müll melden.") },
                confirmButton = {
                    Button(onClick = { notInHildesheimMessageOpen = false }) {
                        Text("Schließen")
                    }
                }
            )
        }
    }
}
