package com.campus.network.feature.map

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.campus.network.feature.map.domain.GeoCoordinate
import com.campus.network.feature.map.domain.GeoSpatialMath
import com.campus.network.feature.map.domain.SearchSuggestionItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Lifecycle-safe wrapper around FusedLocationProviderClient and API-level-aware Geocoder.
 * Supports balanced updates for map browsing and high-accuracy 1s updates during active AR navigation.
 */
@Singleton
class LiveLocationClient @Inject constructor(
    @ApplicationContext context: Context
) {
    private val appContext = context.applicationContext
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(appContext)

    @SuppressLint("MissingPermission")
    fun observeLocationUpdates(
        intervalMs: Long = 5_000L,
        minDistanceMeters: Float = 10f,
        highAccuracy: Boolean = false
    ): Flow<Location> = callbackFlow {
        val priority = if (highAccuracy) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }
        val effectiveInterval = if (highAccuracy) intervalMs.coerceAtMost(1_500L) else intervalMs
        val effectiveDistance = if (highAccuracy) minDistanceMeters.coerceAtMost(1.5f) else minDistanceMeters

        val request = LocationRequest.Builder(priority, effectiveInterval)
            .setMinUpdateIntervalMillis((effectiveInterval / 2L).coerceAtLeast(500L))
            .setMinUpdateDistanceMeters(effectiveDistance)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }

        try {
            fusedClient.lastLocation.addOnSuccessListener { last ->
                if (last != null) trySend(last)
            }
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (se: SecurityException) {
            close(se)
        }

        awaitClose {
            runCatching { fusedClient.removeLocationUpdates(callback) }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastOrCurrentLocation(): Location? {
        return requestCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY)
            ?: requestCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            ?: requestLastLocation()
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrentLocation(priority: Int): Location? =
        suspendCancellableCoroutine { cont ->
            try {
                val tokenSource = CancellationTokenSource()
                cont.invokeOnCancellation { tokenSource.cancel() }
                fusedClient.getCurrentLocation(priority, tokenSource.token)
                    .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                    .addOnCanceledListener { if (cont.isActive) cont.resume(null) }
            } catch (_: SecurityException) {
                if (cont.isActive) cont.resume(null)
            } catch (_: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }

    @SuppressLint("MissingPermission")
    private suspend fun requestLastLocation(): Location? =
        suspendCancellableCoroutine { cont ->
            try {
                fusedClient.lastLocation
                    .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                    .addOnCanceledListener { if (cont.isActive) cont.resume(null) }
            } catch (_: SecurityException) {
                if (cont.isActive) cont.resume(null)
            } catch (_: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }

    suspend fun reverseGeocode(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(appContext, Locale.getDefault())
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                runCatching {
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (cont.isActive) cont.resume(formatAddress(addresses.firstOrNull()))
                            }

                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(null)
                            }
                        }
                    )
                }.onFailure {
                    if (cont.isActive) cont.resume(null)
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    @Suppress("DEPRECATION")
                    val list = geocoder.getFromLocation(latitude, longitude, 1)
                    formatAddress(list?.firstOrNull())
                }.getOrNull()
            }
        }
    }

    private fun formatAddress(addr: Address?): String? {
        if (addr == null) return null
        val parts = listOfNotNull(
            addr.featureName,
            addr.thoroughfare,
            addr.subLocality,
            addr.locality
        )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            addr.getAddressLine(0)?.takeIf { it.isNotBlank() }
        }
    }

    /**
     * Forward geocoding search powered by Android's Google Play Services Geocoder.
     * Resolves real-world places, streets, landmarks, businesses, and cities worldwide.
     */
    suspend fun searchLocations(
        query: String,
        proximity: GeoCoordinate? = null,
        maxResults: Int = 8
    ): List<SearchSuggestionItem> {
        val trimmed = query.trim()
        if (trimmed.length < 2 || !Geocoder.isPresent()) return emptyList()
        val geocoder = Geocoder(appContext, Locale.getDefault())

        // Try proximity-biased bounding box (~0.45 deg ~ 50 km) first if user GPS is available, then global fallback.
        val localAddresses = if (proximity != null) {
            queryGeocoderAddresses(
                geocoder = geocoder,
                query = trimmed,
                maxResults = maxResults,
                lowerLeftLat = (proximity.latitude - 0.45).coerceIn(-89.9, 89.9),
                lowerLeftLng = (proximity.longitude - 0.45).coerceIn(-179.9, 179.9),
                upperRightLat = (proximity.latitude + 0.45).coerceIn(-89.9, 89.9),
                upperRightLng = (proximity.longitude + 0.45).coerceIn(-179.9, 179.9)
            )
        } else {
            emptyList()
        }

        val globalAddresses = if (localAddresses.size < 3) {
            queryGeocoderAddresses(geocoder = geocoder, query = trimmed, maxResults = maxResults)
        } else {
            emptyList()
        }

        return (localAddresses + globalAddresses)
            .filter { it.hasLatitude() && it.hasLongitude() }
            .distinctBy { "${"%.5f".format(Locale.US, it.latitude)}:${"%.5f".format(Locale.US, it.longitude)}" }
            .take(maxResults)
            .mapIndexed { idx, addr ->
                val coord = GeoCoordinate(addr.latitude, addr.longitude)
                val fullLine = addr.getAddressLine(0)?.trim().orEmpty()
                val primaryName = addr.featureName?.trim()
                    ?.takeIf { it.isNotBlank() && !it.all { ch -> ch.isDigit() } }
                    ?: addr.thoroughfare?.trim()?.takeIf { it.isNotBlank() }
                    ?: addr.subLocality?.trim()?.takeIf { it.isNotBlank() }
                    ?: addr.locality?.trim()?.takeIf { it.isNotBlank() }
                    ?: fullLine.substringBefore(",").trim().ifBlank { trimmed }
                val subtitleParts = listOfNotNull(
                    addr.thoroughfare,
                    addr.subLocality,
                    addr.locality,
                    addr.adminArea,
                    addr.countryName
                )
                    .map { it.trim() }
                    .filter { it.isNotBlank() && !it.equals(primaryName, ignoreCase = true) }
                    .distinct()
                val subtitle = if (subtitleParts.isNotEmpty()) {
                    subtitleParts.joinToString(", ")
                } else {
                    fullLine.ifBlank { formatAddress(addr) ?: "Google Location" }
                }

                SearchSuggestionItem(
                    id = "geo_${idx}_${"%.5f".format(Locale.US, addr.latitude)}_${"%.5f".format(Locale.US, addr.longitude)}",
                    name = primaryName,
                    subtitle = subtitle,
                    distanceMeters = proximity?.let { GeoSpatialMath.haversineMeters(it, coord) },
                    coordinate = coord,
                    isCampusPlace = false
                )
            }
    }

    private suspend fun queryGeocoderAddresses(
        geocoder: Geocoder,
        query: String,
        maxResults: Int,
        lowerLeftLat: Double? = null,
        lowerLeftLng: Double? = null,
        upperRightLat: Double? = null,
        upperRightLng: Double? = null
    ): List<Address> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                runCatching {
                    val listener = object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (cont.isActive) cont.resume(addresses.toList())
                        }

                        override fun onError(errorMessage: String?) {
                            if (cont.isActive) cont.resume(emptyList())
                        }
                    }
                    if (lowerLeftLat != null && lowerLeftLng != null && upperRightLat != null && upperRightLng != null) {
                        geocoder.getFromLocationName(
                            query,
                            maxResults,
                            lowerLeftLat,
                            lowerLeftLng,
                            upperRightLat,
                            upperRightLng,
                            listener
                        )
                    } else {
                        geocoder.getFromLocationName(query, maxResults, listener)
                    }
                }.onFailure {
                    if (cont.isActive) cont.resume(emptyList())
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    @Suppress("DEPRECATION")
                    if (lowerLeftLat != null && lowerLeftLng != null && upperRightLat != null && upperRightLng != null) {
                        geocoder.getFromLocationName(
                            query,
                            maxResults,
                            lowerLeftLat,
                            lowerLeftLng,
                            upperRightLat,
                            upperRightLng
                        ).orEmpty()
                    } else {
                        geocoder.getFromLocationName(query, maxResults).orEmpty()
                    }
                }.getOrDefault(emptyList())
            }
        }
    }
}
