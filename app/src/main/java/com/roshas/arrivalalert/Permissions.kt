package com.roshas.arrivalalert

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.math.roundToInt

fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

fun hasBackgroundLocationPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

/**
 * A single on-demand fix. Balanced power lets Android answer from wifi and cell
 * towers rather than spinning up the GPS radio; nothing in this app ever asks for
 * continuous updates.
 */
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

/**
 * Background location cannot be asked for in a dialog, only granted in system
 * settings, so the UI sends the user here instead of launching a request.
 */
fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

fun formatDistance(metres: Float): String =
    if (metres < 1000) "${metres.roundToInt()} m away"
    else "%.1f km away".format(metres / 1000)
