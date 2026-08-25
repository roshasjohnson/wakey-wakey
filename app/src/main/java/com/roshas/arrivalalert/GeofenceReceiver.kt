package com.roshas.arrivalalert

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        val firedIds = event.triggeringGeofences?.map { it.requestId }.orEmpty()
        if (firedIds.isEmpty()) return

        // Reading storage to name the place is a suspend call, so hold the broadcast
        // open until it finishes. This is a short one-shot, not a running job.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val saved = getPlaces(context).associateBy { it.id }
                firedIds.forEach { id ->
                    notifyArrival(context, saved[id]?.name, id)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

private fun notifyArrival(context: Context, placeName: String?, placeId: String) {
    val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    if (!allowed) return

    val text = if (placeName.isNullOrBlank()) {
        "You are nearly there"
    } else {
        "You are approaching $placeName"
    }

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Wakey wakey")
        .setContentText(text)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    try {
        // Keyed off the place so two fences firing together do not overwrite
        // each other's notification.
        NotificationManagerCompat.from(context).notify(placeId.hashCode(), notification)
    } catch (e: SecurityException) {
    }
}
