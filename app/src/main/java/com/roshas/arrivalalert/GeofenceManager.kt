package com.roshas.arrivalalert

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

/**
 * One PendingIntent is shared by every geofence; that is how the API works. The
 * receiver tells the fences apart by reading triggeringGeofences off the event,
 * whose request ids are the SavedPlace ids.
 */
private fun geofencePendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, GeofenceReceiver::class.java)
    return PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    )
}

private fun SavedPlace.toGeofence(): Geofence =
    Geofence.Builder()
        .setRequestId(id)
        .setCircularRegion(lat, lon, radiusMeters.toFloat())
        .setExpirationDuration(Geofence.NEVER_EXPIRE)
        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
        .setNotificationResponsiveness(0)
        .build()

/**
 * Registers one place. Adding a geofence whose request id already exists replaces
 * it in place, so this doubles as the way to apply a changed radius.
 */
fun registerGeofence(
    context: Context,
    place: SavedPlace,
    onResult: (Boolean, String?) -> Unit
) = registerGeofences(context, listOf(place), onResult)

/** Bulk register, used on boot to restore everything that was being watched. */
@SuppressLint("MissingPermission")
fun registerGeofences(
    context: Context,
    places: List<SavedPlace>,
    onResult: (Boolean, String?) -> Unit
) {
    if (places.isEmpty()) {
        onResult(true, null)
        return
    }

    val request = GeofencingRequest.Builder()
        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
        .addGeofences(places.map { it.toGeofence() })
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

/** Removes a single place's fence, leaving every other one registered. */
fun removeGeofence(context: Context, id: String, onResult: (Boolean) -> Unit = {}) {
    LocationServices.getGeofencingClient(context)
        .removeGeofences(listOf(id))
        .addOnSuccessListener { onResult(true) }
        .addOnFailureListener { onResult(false) }
}
