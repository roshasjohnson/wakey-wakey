package com.roshas.arrivalalert

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

const val CHANNEL_ID = "arrival_alerts"

fun createNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        CHANNEL_ID,
        "Arrival Alerts",
        NotificationManager.IMPORTANCE_HIGH
    )
    channel.description = "Alerts when you are close to a saved place"
    context.getSystemService(NotificationManager::class.java)
        .createNotificationChannel(channel)
}

fun canPostNotifications(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

/**
 * Notification id is derived from the place id so two fences firing close together
 * do not overwrite each other.
 */
fun notifyArrival(context: Context, placeName: String?, placeId: String) {
    if (!canPostNotifications(context)) return

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
        NotificationManagerCompat.from(context).notify(placeId.hashCode(), notification)
    } catch (e: SecurityException) {
    }
}
