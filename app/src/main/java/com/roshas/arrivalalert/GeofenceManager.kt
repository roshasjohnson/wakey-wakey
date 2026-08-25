package com.roshas.arrivalalert

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

const val GEOFENCE_ID = "wakey_destination"

private fun geofencePendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, GeofenceReceiver::class.java)
    return PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    )
}

@SuppressLint("MissingPermission")
fun registerGeofence(
    context: Context,
    lat: Double,
    lon: Double,
    radiusMeters: Float,
    onResult: (Boolean, String?) -> Unit
) {
    val geofence = Geofence.Builder()
        .setRequestId(GEOFENCE_ID)
        .setCircularRegion(lat, lon, radiusMeters)
        .setExpirationDuration(Geofence.NEVER_EXPIRE)
        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
        .setNotificationResponsiveness(0)
        .build()

    val request = GeofencingRequest.Builder()
        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
        .addGeofence(geofence)
        .build()

    try {
        LocationServices.getGeofencingClient(context)
            .addGeofences(request, geofencePendingIntent(context))
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    } catch (e: SecurityException) {
        onResult(false, "Permission missing")
    }
}

fun removeGeofence(context: Context, onResult: (Boolean) -> Unit) {
    LocationServices.getGeofencingClient(context)
        .removeGeofences(listOf(GEOFENCE_ID))
        .addOnSuccessListener { onResult(true) }
        .addOnFailureListener { onResult(false) }
}