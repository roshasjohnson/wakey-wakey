package com.roshas.arrivalalert

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

data class Place(
    val name: String,
    val subtitle: String,
    val lat: Double,
    val lon: Double
)

private val httpClient = OkHttpClient()

private val SKIP_TYPES = setOf("country", "state")

suspend fun searchPlaces(
    query: String,
    nearLat: Double?,
    nearLon: Double?
): List<Place> = withContext(Dispatchers.IO) {
    if (query.isBlank()) return@withContext emptyList()

    val encoded = URLEncoder.encode(query, "UTF-8")
    val bias = if (nearLat != null && nearLon != null) "&lat=$nearLat&lon=$nearLon" else ""
    val url = "https://photon.komoot.io/api?q=$encoded&limit=8$bias"

    val request = Request.Builder()
        .url(url)
        .header("User-Agent", "WakeyWakey/1.0")
        .build()

    try {
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string()
            if (!response.isSuccessful || body == null) emptyList()
            else parsePhoton(body)
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun parsePhoton(json: String): List<Place> {
    val features = JSONObject(json).optJSONArray("features") ?: return emptyList()
    val places = mutableListOf<Place>()

    for (i in 0 until features.length()) {
        val feature = features.optJSONObject(i) ?: continue
        val props = feature.optJSONObject("properties") ?: continue
        val coords = feature.optJSONObject("geometry")?.optJSONArray("coordinates") ?: continue
        if (coords.length() < 2) continue
        if (props.optString("osm_value") in SKIP_TYPES) continue

        val name = props.optString("name")
        if (name.isBlank()) continue

        val subtitle = listOf(
            props.optString("street"),
            props.optString("district"),
            props.optString("city"),
            props.optString("state"),
            props.optString("country")
        ).filter { it.isNotBlank() }.distinct().joinToString(", ")

        places.add(
            Place(
                name = name,
                subtitle = subtitle,
                lon = coords.getDouble(0),
                lat = coords.getDouble(1)
            )
        )
    }
    return places
}