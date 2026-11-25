package com.campus.network.feature.map.data

import android.content.Context
import com.campus.network.feature.map.domain.GeoCoordinate
import com.campus.network.feature.map.domain.GeoSpatialMath
import com.campus.network.feature.map.domain.SearchSuggestionItem
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.search.ResponseInfo
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings
import com.mapbox.search.SearchOptions
import com.mapbox.search.SearchSelectionCallback
import com.mapbox.search.SearchSuggestionsCallback
import com.mapbox.search.common.AsyncOperationTask
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Multi-provider destination autocomplete and POI lookup combining:
 *  1. OpenStreetMap Photon & Nominatim POI/Street/Estate search (deep street, building, university,
 *     and business coverage across Kenya and worldwide, returning immediate coordinates)
 *  2. Mapbox Geocoding v5 HTTP API (`poi,address,neighborhood,locality,place,district,region`)
 *  3. Mapbox Search SDK v2 (`SearchEngine`)
 */
@Singleton
class MapboxSearchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("mapboxAccessToken") private val accessToken: String
) {
    private val suggestionCache = ConcurrentHashMap<String, SearchSuggestion>()

    private val searchEngine: SearchEngine? by lazy {
        if (accessToken.isBlank()) return@lazy null
        runCatching {
            if (MapboxOptions.accessToken.isBlank()) {
                MapboxOptions.accessToken = accessToken
            }
            SearchEngine.createSearchEngineWithBuiltInDataProviders(
                SearchEngineSettings()
            )
        }.getOrNull()
    }

    suspend fun search(
        query: String,
        proximity: GeoCoordinate?
    ): Result<List<SearchSuggestionItem>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext Result.success(emptyList())

        // Default proximity bias inside Kenya if device GPS has not locked yet
        val biasCoord = proximity ?: GeoCoordinate(-1.286389, 36.817223)

        coroutineScope {
            val photonDeferred = async {
                runCatching { queryPhotonPlaces(trimmed, proximity, biasCoord) }.getOrDefault(emptyList())
            }
            val mapboxHttpDeferred = async {
                runCatching { queryMapboxGeocodingV5(trimmed, proximity, biasCoord) }.getOrDefault(emptyList())
            }
            val sdkDeferred = async {
                runCatching { queryMapboxSearchSdk(trimmed, biasCoord).getOrDefault(emptyList()) }.getOrDefault(emptyList())
            }

            val merged = (photonDeferred.await() + mapboxHttpDeferred.await() + sdkDeferred.await())
                .distinctBy { item ->
                    val coordKey = item.coordinate?.let {
                        "${"%.4f".format(Locale.US, it.latitude)},${"%.4f".format(Locale.US, it.longitude)}"
                    } ?: item.id
                    "${item.name.trim().lowercase()}|$coordKey"
                }
                .sortedWith(
                    compareBy<SearchSuggestionItem> { item ->
                        // Prioritize results with resolved coordinates and closer distance to user
                        if (item.coordinate != null) 0 else 1
                    }.thenBy(nullsLast()) { it.distanceMeters }
                )
                .take(12)

            if (merged.isNotEmpty()) {
                Result.success(merged)
            } else {
                Result.failure(IllegalStateException("No matching locations found for \"$trimmed\"."))
            }
        }
    }

    private fun queryPhotonPlaces(
        query: String,
        userOrigin: GeoCoordinate?,
        biasCoord: GeoCoordinate
    ): List<SearchSuggestionItem> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val urlStr = "https://photon.komoot.io/api/?q=$encoded&lat=${biasCoord.latitude}&lon=${biasCoord.longitude}&limit=10"
        val body = httpGetJson(urlStr) ?: return emptyList()
        val features = JSONObject(body).optJSONArray("features") ?: return emptyList()
        val results = ArrayList<SearchSuggestionItem>(features.length())

        for (i in 0 until features.length()) {
            val feat = features.optJSONObject(i) ?: continue
            val geom = feat.optJSONObject("geometry") ?: continue
            val coords = geom.optJSONArray("coordinates") ?: continue
            if (coords.length() < 2) continue
            val lng = coords.optDouble(0, Double.NaN)
            val lat = coords.optDouble(1, Double.NaN)
            if (lat.isNaN() || lng.isNaN()) continue

            val props = feat.optJSONObject("properties") ?: continue
            val rawName = props.optString("name").trim()
            val street = props.optString("street").trim()
            val district = props.optString("district").trim()
            val locality = props.optString("locality").trim()
            val city = props.optString("city").trim()
            val county = props.optString("county").trim()
            val state = props.optString("state").trim()
            val country = props.optString("country").trim()

            val primaryName = rawName.ifBlank {
                street.ifBlank { district.ifBlank { locality.ifBlank { city.ifBlank { county } } } }
            }
            if (primaryName.isBlank()) continue

            val subtitleParts = listOf(street, locality, district, city, county, state, country)
                .filter { it.isNotBlank() && !it.equals(primaryName, ignoreCase = true) }
                .distinct()
            val subtitle = subtitleParts.joinToString(", ").ifBlank { "Location" }
            val coord = GeoCoordinate(latitude = lat, longitude = lng)

            results.add(
                SearchSuggestionItem(
                    id = "photon_${props.optLong("osm_id", i.toLong())}_$i",
                    name = primaryName,
                    subtitle = subtitle,
                    distanceMeters = userOrigin?.let { GeoSpatialMath.haversineMeters(it, coord) },
                    coordinate = coord,
                    isCampusPlace = false
                )
            )
        }
        return results
    }

    private fun queryMapboxGeocodingV5(
        query: String,
        userOrigin: GeoCoordinate?,
        biasCoord: GeoCoordinate
    ): List<SearchSuggestionItem> {
        if (accessToken.isBlank()) return emptyList()
        val encoded = URLEncoder.encode(query, "UTF-8")
        val prox = "${biasCoord.longitude},${biasCoord.latitude}"
        val types = "poi,address,neighborhood,locality,place,district,region"
        val urlStr = "https://api.mapbox.com/geocoding/v5/mapbox.places/$encoded.json" +
            "?autocomplete=true&limit=8&proximity=$prox&types=$types&access_token=$accessToken"
        val body = httpGetJson(urlStr) ?: return emptyList()
        val features = JSONObject(body).optJSONArray("features") ?: return emptyList()
        val results = ArrayList<SearchSuggestionItem>(features.length())

        for (i in 0 until features.length()) {
            val feat = features.optJSONObject(i) ?: continue
            val center = feat.optJSONArray("center") ?: continue
            if (center.length() < 2) continue
            val lng = center.optDouble(0, Double.NaN)
            val lat = center.optDouble(1, Double.NaN)
            if (lat.isNaN() || lng.isNaN()) continue

            val name = feat.optString("text").trim()
            val placeName = feat.optString("place_name").trim()
            if (name.isBlank()) continue
            val subtitle = placeName.removePrefix(name).removePrefix(",").trim().ifBlank { placeName }
            val coord = GeoCoordinate(latitude = lat, longitude = lng)

            results.add(
                SearchSuggestionItem(
                    id = feat.optString("id").ifBlank { "mbx_v5_$i" },
                    name = name,
                    subtitle = subtitle,
                    distanceMeters = userOrigin?.let { GeoSpatialMath.haversineMeters(it, coord) },
                    coordinate = coord,
                    isCampusPlace = false
                )
            )
        }
        return results
    }

    private suspend fun queryMapboxSearchSdk(
        trimmed: String,
        proximity: GeoCoordinate?
    ): Result<List<SearchSuggestionItem>> {
        val engine = searchEngine
            ?: return Result.failure(IllegalStateException("Mapbox access token not configured."))

        return suspendCancellableCoroutine { cont ->
            val optionsBuilder = SearchOptions.Builder().limit(6)
            if (proximity != null) {
                optionsBuilder.proximity(Point.fromLngLat(proximity.longitude, proximity.latitude))
            }
            val task: AsyncOperationTask = engine.search(
                trimmed,
                optionsBuilder.build(),
                object : SearchSuggestionsCallback {
                    override fun onSuggestions(
                        suggestions: List<SearchSuggestion>,
                        responseInfo: ResponseInfo
                    ) {
                        val items = suggestions.map { s ->
                            suggestionCache[s.id] = s
                            SearchSuggestionItem(
                                id = s.id,
                                name = s.name,
                                subtitle = s.address?.formattedAddress(com.mapbox.search.result.SearchAddress.FormatStyle.Short)
                                    ?: s.descriptionText
                                    ?: "Destination",
                                distanceMeters = s.distanceMeters,
                                coordinate = null,
                                isCampusPlace = false
                            )
                        }
                        if (cont.isActive) cont.resume(Result.success(items))
                    }

                    override fun onError(e: Exception) {
                        if (cont.isActive) cont.resume(Result.failure(e))
                    }
                }
            )
            cont.invokeOnCancellation { task.cancel() }
        }
    }

    private fun httpGetJson(urlStr: String): String? {
        val conn = (URL(urlStr).openConnection() as? HttpURLConnection) ?: return null
        return try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 6_000
            conn.readTimeout = 6_000
            conn.setRequestProperty("User-Agent", "CampusNetworkAndroid/1.0")
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    suspend fun selectSuggestion(suggestionId: String): Result<SearchSuggestionItem> {
        val suggestion = suggestionCache[suggestionId]
            ?: return Result.failure(IllegalArgumentException("Suggestion expired; search again."))
        val engine = searchEngine
            ?: return Result.failure(IllegalStateException("Mapbox access token not configured."))

        return suspendCancellableCoroutine { cont ->
            var refinementCount = 0
            var activeTask: AsyncOperationTask? = null
            val callback = object : SearchSelectionCallback {
                override fun onSuggestions(
                    suggestions: List<SearchSuggestion>,
                    responseInfo: ResponseInfo
                ) {
                    val first = suggestions.firstOrNull()
                    if (first != null && suggestions.size == 1 && refinementCount == 0) {
                        refinementCount += 1
                        activeTask = engine.select(first, this)
                    } else if (cont.isActive) {
                        val hint = first?.name
                        cont.resume(
                            Result.failure(
                                IllegalStateException(
                                    if (hint != null) "Refine destination search (e.g. $hint)."
                                    else "Refine destination query to select a specific place."
                                )
                            )
                        )
                    }
                }

                override fun onResult(
                    suggestion: SearchSuggestion,
                    result: SearchResult,
                    responseInfo: ResponseInfo
                ) {
                    val coord = result.coordinate.let {
                        GeoCoordinate(latitude = it.latitude(), longitude = it.longitude())
                    }
                    val item = SearchSuggestionItem(
                        id = result.id,
                        name = result.name,
                        subtitle = result.address?.formattedAddress(com.mapbox.search.result.SearchAddress.FormatStyle.Medium)
                            ?: result.descriptionText
                            ?: "Selected Destination",
                        distanceMeters = suggestion.distanceMeters,
                        coordinate = coord,
                        isCampusPlace = false
                    )
                    if (cont.isActive) cont.resume(Result.success(item))
                }

                override fun onResults(
                    suggestion: SearchSuggestion,
                    results: List<SearchResult>,
                    responseInfo: ResponseInfo
                ) {
                    val first = results.firstOrNull()
                    if (first != null) {
                        onResult(suggestion, first, responseInfo)
                    } else if (cont.isActive) {
                        cont.resume(Result.failure(IllegalStateException("No coordinates found for ${suggestion.name}")))
                    }
                }

                override fun onError(e: Exception) {
                    if (cont.isActive) cont.resume(Result.failure(e))
                }
            }
            activeTask = engine.select(suggestion, callback)
            cont.invokeOnCancellation { activeTask?.cancel() }
        }
    }
}
