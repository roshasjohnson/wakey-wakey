package com.roshas.arrivalalert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
                    // Fire once. Without this the user keeps getting notified for as
                    // long as they sit inside the circle. They turn it back on by hand
                    // next time they travel.
                    setWatching(context, id, false)
                    removeGeofence(context, id)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
