package com.roshas.arrivalalert

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.roundToInt

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wakey")

private val KEY_PLACES = stringPreferencesKey("places_json")

const val DEFAULT_RADIUS_METERS = 3000

data class SavedPlace(
    val id: String,
    val name: String,
    val subtitle: String,
    val lat: Double,
    val lon: Double,
    val radiusMeters: Int,
    val watching: Boolean
)

/** Emits the saved list on every change. */
fun placesFlow(context: Context): Flow<List<SavedPlace>> =
    context.dataStore.data.map { prefs -> decode(prefs[KEY_PLACES]) }

/** One-shot read, for the broadcast receivers that have no composition to collect in. */
suspend fun getPlaces(context: Context): List<SavedPlace> =
    decode(context.dataStore.data.first()[KEY_PLACES])

suspend fun getPlace(context: Context, id: String): SavedPlace? =
    getPlaces(context).firstOrNull { it.id == id }

/**
 * Adds a searched place. Returns the stored entry, which is the existing one if this
 * place was already saved, so the caller never ends up with two fences on one spot.
 */
suspend fun addPlace(
    context: Context,
    place: Place,
    radiusMeters: Int = DEFAULT_RADIUS_METERS
): SavedPlace {
    var stored: SavedPlace? = null
    context.dataStore.edit { prefs ->
        val places = decode(prefs[KEY_PLACES])
        val existing = places.firstOrNull { it.isSameSpotAs(place) }
        if (existing != null) {
            stored = existing
            return@edit
        }
        val added = SavedPlace(
            id = UUID.randomUUID().toString(),
            name = place.name,
            subtitle = place.subtitle,
            lat = place.lat,
            lon = place.lon,
            radiusMeters = radiusMeters,
            watching = false
        )
        stored = added
        prefs[KEY_PLACES] = encode(places + added)
    }
    return stored!!
}

suspend fun deletePlace(context: Context, id: String) {
    context.dataStore.edit { prefs ->
        prefs[KEY_PLACES] = encode(decode(prefs[KEY_PLACES]).filterNot { it.id == id })
    }
}

suspend fun updateRadius(context: Context, id: String, radiusMeters: Int) {
    updatePlace(context, id) { it.copy(radiusMeters = radiusMeters) }
}

suspend fun setWatching(context: Context, id: String, watching: Boolean) {
    updatePlace(context, id) { it.copy(watching = watching) }
}

private suspend fun updatePlace(
    context: Context,
    id: String,
    change: (SavedPlace) -> SavedPlace
) {
    context.dataStore.edit { prefs ->
        val places = decode(prefs[KEY_PLACES])
        if (places.none { it.id == id }) return@edit
        prefs[KEY_PLACES] = encode(places.map { if (it.id == id) change(it) else it })
    }
}

/**
 * Photon hands back coordinates at full precision, so two searches for the same
 * landmark can differ in the last decimal. Five decimals is about a metre, which is
 * far tighter than any alert radius and still catches the repeat-add case.
 */
private fun SavedPlace.isSameSpotAs(place: Place): Boolean =
    round5(lat) == round5(place.lat) && round5(lon) == round5(place.lon)

private fun round5(value: Double): Long = (value * 100_000).roundToInt().toLong()

private fun encode(places: List<SavedPlace>): String {
    val array = JSONArray()
    places.forEach { place ->
        array.put(
            JSONObject()
                .put("id", place.id)
                .put("name", place.name)
                .put("subtitle", place.subtitle)
                .put("lat", place.lat)
                .put("lon", place.lon)
                .put("radius", place.radiusMeters)
                .put("watching", place.watching)
        )
    }
    return array.toString()
}

private fun decode(json: String?): List<SavedPlace> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val id = obj.optString("id")
            val name = obj.optString("name")
            if (id.isBlank() || name.isBlank()) return@mapNotNull null
            SavedPlace(
                id = id,
                name = name,
                subtitle = obj.optString("subtitle"),
                lat = obj.optDouble("lat"),
                lon = obj.optDouble("lon"),
                radiusMeters = obj.optInt("radius", DEFAULT_RADIUS_METERS),
                watching = obj.optBoolean("watching")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}
