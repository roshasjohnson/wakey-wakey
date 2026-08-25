package com.roshas.arrivalalert

import android.Manifest
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.roshas.arrivalalert.ui.theme.Accent
import com.roshas.arrivalalert.ui.theme.Destructive
import com.roshas.arrivalalert.ui.theme.Divider
import com.roshas.arrivalalert.ui.theme.Muted
import com.roshas.arrivalalert.ui.theme.SheetBackground
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ScreenMargin = 24.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceSheet(
    place: SavedPlace,
    onDismiss: () -> Unit,
    onDistance: (Float) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    var radiusKm by remember(place.id) {
        mutableFloatStateOf(place.radiusMeters / 1000f)
    }
    var distanceText by remember(place.id) { mutableStateOf<String?>(null) }
    var checking by remember(place.id) { mutableStateOf(false) }

    fun measure() {
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
                    place.lat, place.lon,
                    out
                )
                distanceText = formatDistance(out[0])
                onDistance(out[0])
            }
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) measure()
        else distanceText = "Location permission denied"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBackground,
        shape = RectangleShape
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = ScreenMargin)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Text(place.name, style = MaterialTheme.typography.headlineMedium)
            if (place.subtitle.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    place.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted
                )
            }

            Spacer(Modifier.height(32.dp))
            HorizontalDivider(color = Divider)
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text("Alert radius", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "%.1f km".format(radiusKm),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Accent
                )
            }

            Slider(
                value = radiusKm,
                onValueChange = { radiusKm = it },
                // Only written once the drag ends, never on every frame.
                onValueChangeFinished = {
                    val metres = (radiusKm * 1000).roundToInt()
                    scope.launch {
                        updateRadius(context, place.id, metres)
                        // A live fence keeps its old radius until it is re-registered,
                        // and re-adding the same request id replaces it in place.
                        if (place.watching) {
                            registerGeofence(
                                context,
                                place.copy(radiusMeters = metres)
                            ) { _, _ -> }
                        }
                    }
                },
                valueRange = 1f..10f,
                steps = 17,
                colors = SliderDefaults.colors(
                    thumbColor = Accent,
                    activeTrackColor = Accent,
                    inactiveTrackColor = Divider
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1 km", style = MaterialTheme.typography.labelMedium, color = Muted)
                Text("10 km", style = MaterialTheme.typography.labelMedium, color = Muted)
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Divider)
            Spacer(Modifier.height(32.dp))

            OutlinedButton(
                onClick = {
                    if (!hasLocationPermission(context)) {
                        locationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    } else {
                        measure()
                    }
                },
                enabled = !checking,
                shape = RectangleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (checking) "CHECKING..." else "AM I CLOSE?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            distanceText?.let { text ->
                Spacer(Modifier.height(32.dp))
                Text(
                    text,
                    style = MaterialTheme.typography.displayLarge,
                    color = Accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(32.dp))

            TextButton(
                onClick = {
                    removeGeofence(context, place.id)
                    scope.launch {
                        deletePlace(context, place.id)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "DELETE",
                    style = MaterialTheme.typography.labelLarge,
                    color = Destructive
                )
            }
        }
    }
}
