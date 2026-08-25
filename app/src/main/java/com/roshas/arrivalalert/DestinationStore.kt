package com.roshas.arrivalalert

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wakey")

private val KEY_NAME = stringPreferencesKey("dest_name")
private val KEY_SUBTITLE = stringPreferencesKey("dest_subtitle")
private val KEY_LAT = doublePreferencesKey("dest_lat")
private val KEY_LON = doublePreferencesKey("dest_lon")
private val KEY_RADIUS = intPreferencesKey("radius_m")

data class Destination(
    val name: String,
    val subtitle: String,
    val lat: Double,
    val lon: Double,
    val radiusMeters: Int
)

fun destinationFlow(context: Context): Flow<Destination?> =
    context.dataStore.data.map { prefs ->
        val name = prefs[KEY_NAME]
        val lat = prefs[KEY_LAT]
        val lon = prefs[KEY_LON]
        if (name == null || lat == null || lon == null) null
        else Destination(
            name = name,
            subtitle = prefs[KEY_SUBTITLE] ?: "",
            lat = lat,
            lon = lon,
            radiusMeters = prefs[KEY_RADIUS] ?: 3000
        )
    }

suspend fun saveDestination(context: Context, place: Place, radiusMeters: Int) {
    context.dataStore.edit { prefs ->
        prefs[KEY_NAME] = place.name
        prefs[KEY_SUBTITLE] = place.subtitle
        prefs[KEY_LAT] = place.lat
        prefs[KEY_LON] = place.lon
        prefs[KEY_RADIUS] = radiusMeters
    }
}

suspend fun saveRadius(context: Context, radiusMeters: Int) {
    context.dataStore.edit { prefs -> prefs[KEY_RADIUS] = radiusMeters }
}