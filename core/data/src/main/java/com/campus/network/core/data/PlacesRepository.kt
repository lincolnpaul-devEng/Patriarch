package com.campus.network.core.data

import com.campus.network.core.model.AudienceFilter
import com.campus.network.core.model.CampusPlace
import com.campus.network.core.model.PlaceCategory
import com.campus.network.core.model.PlaceStats
import com.campus.network.core.model.SavedByUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class PlacesRepository {

    private val _places = MutableStateFlow<List<CampusPlace>>(initialCatalog())
    val places: Flow<List<CampusPlace>> = _places.asStateFlow()

    fun getPlacesByCategory(category: PlaceCategory?): Flow<List<CampusPlace>> {
        return _places.map { list ->
            if (category == null || category == PlaceCategory.TOP_PICKS) {
                list
            } else {
                list.filter { it.category == category }
            }
        }
    }

    /**
     * Returns places sorted by proximity to the user's actual GPS location.
     */
    fun getPlacesSortedByProximity(userLat: Double, userLng: Double): Flow<List<CampusPlace>> {
        return _places.map { list ->
            list.sortedBy { place ->
                haversineDistanceKm(userLat, userLng, place.latitude, place.longitude)
            }
        }
    }

    fun toggleSavedToTry(placeId: String) {
        val current = _places.value
        _places.value = current.map { place ->
            if (place.id == placeId) {
                val newStatus = !place.isSavedToTry
                place.copy(
                    isSavedToTry = newStatus,
                    savesCount = if (newStatus) place.savesCount + 1 else place.savesCount - 1
                )
            } else place
        }
    }

    fun toggleBeen(placeId: String) {
        val current = _places.value
        _places.value = current.map { place ->
            if (place.id == placeId) {
                place.copy(hasBeen = !place.hasBeen)
            } else place
        }
    }

    fun setPlaces(places: List<CampusPlace>) {
        _places.value = places
    }

    fun addPlace(place: CampusPlace) {
        _places.value = _places.value + place
    }

    private fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of the Earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun initialCatalog(): List<CampusPlace> = emptyList()
}
