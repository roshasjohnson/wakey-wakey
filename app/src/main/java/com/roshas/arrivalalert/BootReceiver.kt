package com.roshas.arrivalalert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Geofences do not survive a reboot, so anything the user left switched on has to
 * be registered again once the phone comes back up.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Registering needs background location. If the user has since revoked it
        // there is nothing to re-register into, and the switch in the UI will send
        // them to settings the next time they try.
        if (!hasBackgroundLocationPermission(context)) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val watched = getPlaces(context).filter { it.watching }
                if (watched.isNotEmpty()) {
                    registerGeofences(context, watched) { _, _ -> }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
