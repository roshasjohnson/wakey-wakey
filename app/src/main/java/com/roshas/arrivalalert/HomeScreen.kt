package com.roshas.arrivalalert

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.roshas.arrivalalert.ui.theme.Accent
import com.roshas.arrivalalert.ui.theme.Divider
import com.roshas.arrivalalert.ui.theme.Muted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ScreenMargin = 24.dp

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val places by placesFlow(context).collectAsState(initial = emptyList())

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var openPlaceId by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    // Distances are only known after the user asks for one in the sheet. Kept here
    // so the row keeps showing it after the sheet closes.
    val distances = remember { mutableStateMapOf<String, Float>() }

    // The whole point of the app is a notification, so ask once on first open.
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !canPostNotifications(context)
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length < 3) {
            results = emptyList()
            searching = false
            return@LaunchedEffect
        }
        delay(350)
        searching = true
        results = searchPlaces(trimmed, null, null)
        searching = false
    }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            "Wakey Wakey",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(
                start = ScreenMargin,
                end = ScreenMargin,
                top = 16.dp,
                bottom = 24.dp
            )
        )

        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = {
                Text(
                    "Add a place",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Muted
                )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Accent,
                unfocusedIndicatorColor = Divider,
                cursorColor = Accent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenMargin)
        )

        // Reserve the row whether or not it is loading, so the list below does not
        // jump every time a search starts.
        Box(Modifier.fillMaxWidth().height(2.dp)) {
            if (searching) {
                LinearProgressIndicator(
                    color = Accent,
                    trackColor = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        notice?.let { text ->
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = Muted,
                modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 12.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        if (query.isNotBlank()) {
            SearchResults(
                results = results,
                searching = searching,
                belowMinimum = query.trim().length < 3,
                onPick = { place ->
                    scope.launch {
                        addPlace(context, place)
                        query = ""
                        results = emptyList()
                        notice = null
                    }
                }
            )
        } else {
            SavedPlaces(
                places = places,
                distances = distances,
                onOpen = { openPlaceId = it.id },
                onToggle = { place, on ->
                    notice = null
                    if (!on) {
                        removeGeofence(context, place.id)
                        scope.launch { setWatching(context, place.id, false) }
                    } else if (!hasBackgroundLocationPermission(context)) {
                        // Background location can only be granted in system settings,
                        // never from a dialog.
                        notice = "Set location to \"Allow all the time\", then try again"
                        openAppSettings(context)
                    } else {
                        registerGeofence(context, place) { ok, error ->
                            if (ok) {
                                scope.launch { setWatching(context, place.id, true) }
                            } else {
                                notice = error ?: "Could not start watching"
                            }
                        }
                    }
                }
            )
        }
    }

    openPlaceId?.let { id ->
        places.firstOrNull { it.id == id }?.let { place ->
            PlaceSheet(
                place = place,
                onDismiss = { openPlaceId = null },
                onDistance = { metres -> distances[place.id] = metres }
            )
        }
    }
}

@Composable
private fun SearchResults(
    results: List<Place>,
    searching: Boolean,
    belowMinimum: Boolean,
    onPick: (Place) -> Unit
) {
    if (results.isEmpty()) {
        // Nothing to say yet while the query is still too short to have been sent.
        if (!searching && !belowMinimum) {
            Message("No places found.")
        }
        return
    }
    LazyColumn {
        items(results) { place ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(place) }
                    .padding(horizontal = ScreenMargin, vertical = 16.dp)
            ) {
                Text(place.name, style = MaterialTheme.typography.bodyLarge)
                if (place.subtitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        place.subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            HorizontalDivider(color = Divider)
        }
    }
}

@Composable
private fun SavedPlaces(
    places: List<SavedPlace>,
    distances: Map<String, Float>,
    onOpen: (SavedPlace) -> Unit,
    onToggle: (SavedPlace, Boolean) -> Unit
) {
    if (places.isEmpty()) {
        Message("You haven't saved any places yet.")
        return
    }
    LazyColumn {
        items(places, key = { it.id }) { place ->
            PlaceRow(
                place = place,
                distance = distances[place.id],
                onOpen = { onOpen(place) },
                onToggle = { on -> onToggle(place, on) }
            )
            HorizontalDivider(color = Divider)
        }
    }
}

@Composable
private fun PlaceRow(
    place: SavedPlace,
    distance: Float?,
    onOpen: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val radius = "Alert within %.1f km".format(place.radiusMeters / 1000f)
    val secondary = if (distance == null) radius else "$radius • ${formatDistance(distance)}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = ScreenMargin, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                place.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(secondary, style = MaterialTheme.typography.labelMedium, color = Muted)
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = place.watching,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Accent,
                checkedBorderColor = Accent,
                uncheckedThumbColor = Muted,
                uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = Muted
            )
        )
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = Muted,
        modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 8.dp)
    )
}
