package com.roshas.arrivalalert
import androidx.compose.material3.Button
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.roshas.arrivalalert.ui.theme.ArrivalAlertTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Slider



const val CHANNEL_ID = "arrival_alerts"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()
        setContent {
            ArrivalAlertTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Arrival Alerts",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "Alerts when you are close to your destination"
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}


@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val destination by destinationFlow(context).collectAsState(initial = null)

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var distanceText by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var watching by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var radiusKm by remember { mutableStateOf(3f) }

    LaunchedEffect(destination) {
        destination?.let { radiusKm = it.radiusMeters / 1000f }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            checking = true
            fetchLocation(context) { location ->
                checking = false
                distanceText = if (location == null) "Could not get your location" else null
            }
        } else {
            distanceText = "Location permission denied"
        }
    }

    LaunchedEffect(query) {
        if (query.trim().length < 3) {
            results = emptyList()
            searching = false
            return@LaunchedEffect
        }
        delay(350)
        searching = true
        results = searchPlaces(query.trim(), null, null)
        searching = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("Wakey Wakey", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search a place") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (searching) {
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator()
        }

        if (results.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(results) { place ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    saveDestination(context, place, (radiusKm * 1000).roundToInt())
                                    query = ""
                                    results = emptyList()
                                    distanceText = null
                                    statusText = null
                                }
                            }
                            .padding(vertical = 10.dp)
                    ) {
                        Text(place.name, style = MaterialTheme.typography.bodyLarge)
                        if (place.subtitle.isNotBlank()) {
                            Text(
                                place.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        destination?.let { dest ->
            Text("Alerting at", style = MaterialTheme.typography.labelMedium)
            Text(dest.name, style = MaterialTheme.typography.titleMedium)
            if (dest.subtitle.isNotBlank()) {
                Text(dest.subtitle, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (!hasLocationPermission(context)) {
                        locationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    } else {
                        checking = true
                        distanceText = null
                        fetchLocation(context) { location ->
                            checking = false
                            if (location == null) {
                                distanceText = "Could not get your location"
                            } else {
                                val out = FloatArray(1)
                                Location.distanceBetween(
                                    location.latitude, location.longitude,
                                    dest.lat, dest.lon,
                                    out
                                )
                                distanceText = formatDistance(out[0])
                            }
                        }
                    }
                },
                enabled = !checking
            ) {
                Text(if (checking) "Checking..." else "Am I close?")
            }

            distanceText?.let { text ->
                Spacer(Modifier.height(12.dp))
                Text(text, style = MaterialTheme.typography.headlineSmall)
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Alert distance", style = MaterialTheme.typography.bodyMedium)
                Text("%.1f km".format(radiusKm), style = MaterialTheme.typography.bodyMedium)
            }

            Slider(
                value = radiusKm,
                onValueChange = { radiusKm = it },
                onValueChangeFinished = {
                    scope.launch { saveRadius(context, (radiusKm * 1000).roundToInt()) }
                },
                valueRange = 1f..10f,
                steps = 17
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (watching) {
                        removeGeofence(context) { ok ->
                            if (ok) {
                                watching = false
                                statusText = "Not watching"
                            } else {
                                statusText = "Could not stop"
                            }
                        }
                    } else if (!hasBackgroundLocationPermission(context)) {
                        statusText = "Set location to 'Allow all the time', then try again"
                        openAppSettings(context)
                    } else {
                        statusText = "Setting up..."
                        registerGeofence(
                            context,
                            dest.lat,
                            dest.lon,
                            (radiusKm * 1000)
                        ) { ok, error ->
                            if (ok) {
                                watching = true
                                statusText = "Watching for arrival"
                            } else {
                                statusText = "Failed: $error"
                            }
                        }
                    }
                }
            ) {
                Text(if (watching) "Stop watching" else "Start watching")
            }

            statusText?.let { text ->
                Spacer(Modifier.height(8.dp))
                Text(text, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}


fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

fun fetchLocation(context: Context, onResult: (Location?) -> Unit) {
    if (!hasLocationPermission(context)) {
        onResult(null)
        return
    }
    try {
        LocationServices.getFusedLocationProviderClient(context)
            .getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            )
            .addOnSuccessListener { onResult(it) }
            .addOnFailureListener { onResult(null) }
    } catch (e: SecurityException) {
        onResult(null)
    }
}

fun formatDistance(metres: Float): String =
    if (metres < 1000) "${metres.roundToInt()} m away"
    else "%.1f km away".format(metres / 1000)


fun hasBackgroundLocationPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}