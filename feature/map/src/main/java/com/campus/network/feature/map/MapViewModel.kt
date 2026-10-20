package com.campus.network.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.network.core.data.FlashMeetRepository
import com.campus.network.core.data.PlacesRepository
import com.campus.network.core.model.AudienceFilter
import com.campus.network.core.model.CampusPlace
import com.campus.network.core.model.FlashMeet
import com.campus.network.core.model.MeetCategory
import com.campus.network.core.model.PlaceCategory
import com.campus.network.core.data.StudentProfileRepository
import com.campus.network.core.model.StudentProfile
import com.campus.network.core.model.firstName
import com.campus.network.core.model.initials
import com.campus.network.feature.map.components.CampusFriend
import androidx.compose.ui.graphics.Color
import com.campus.network.feature.map.events.CampusEvent
import com.campus.network.feature.map.events.MapNavigationMode
import com.campus.network.feature.map.events.sampleCampusEvents
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class MapUiState(
    val places: List<CampusPlace> = emptyList(),
    val selectedPlace: CampusPlace? = null,
    val selectedCategory: PlaceCategory = PlaceCategory.TOP_PICKS,
    val audienceFilter: AudienceFilter = AudienceFilter.EVERYONE,
    val selectedRegion: String = "All Hubs",
    val isOpenNowOnly: Boolean = false,
    val searchQuery: String = "",
    val userLocation: LatLng? = null,
    val liveMeets: List<FlashMeet> = emptyList(),
    val selectedMeet: FlashMeet? = null,
    val joinedMeetIds: Set<String> = emptySet(),
    val isCreateDialogVisible: Boolean = false,
    val savedPlaceIds: Set<String> = emptySet(),
    val beenPlaceIds: Set<String> = emptySet(),
    val feedbackMessage: String? = null,
    val navigationMode: MapNavigationMode = MapNavigationMode.MAP,
    val events: List<CampusEvent> = sampleCampusEvents,
    val selectedEvent: CampusEvent? = null,
    val focusedEventLocation: LatLng? = null,
    val studentProfile: StudentProfile? = null,
    val friends: List<CampusFriend> = emptyList()
) {
    val studentName: String
        get() = studentProfile?.fullName?.ifBlank { "Student" } ?: "Student"

    val studentFirstName: String
        get() = studentProfile?.firstName?.ifBlank { "Student" } ?: "Student"

    val studentInitials: String
        get() = studentProfile?.initials ?: "S"

    val studentAvatarUrl: String?
        get() = studentProfile?.avatarUrl
}

sealed interface MapEvent {
    data class SelectPlace(val place: CampusPlace?) : MapEvent
    data class FilterCategory(val category: PlaceCategory) : MapEvent
    data class FilterAudience(val audience: AudienceFilter) : MapEvent
    data class FilterRegion(val region: String) : MapEvent
    data object ToggleOpenNow : MapEvent
    data class ToggleSavedToTry(val placeId: String) : MapEvent
    data class ToggleBeen(val placeId: String) : MapEvent
    data class UpdateSearchQuery(val query: String) : MapEvent
    data class ClearFeedback(val message: String? = null) : MapEvent
    data class SubmitFeedback(val placeId: String, val comment: String) : MapEvent
    data class UpdateUserLocation(val latitude: Double, val longitude: Double) : MapEvent
    data class SelectMeet(val meet: FlashMeet?) : MapEvent
    data class ToggleJoinMeet(val meetId: String) : MapEvent
    data class FilterMeetCategory(val category: MeetCategory?) : MapEvent
    data class CreateMeet(
        val title: String,
        val description: String,
        val category: MeetCategory,
        val locationName: String,
        val durationMinutes: Int,
        val latitude: Double? = null,
        val longitude: Double? = null
    ) : MapEvent
    data class CreateCampusEvent(
        val event: CampusEvent,
        val alsoPublishAsFlashMeet: Boolean = false,
        val flashMeetCategory: MeetCategory = MeetCategory.PROJECT_HACK,
        val durationMinutes: Int = 60
    ) : MapEvent
    data class ToggleCreateDialog(val show: Boolean) : MapEvent

    // Events screen UI flow events
    data object ShowEventsList : MapEvent
    data class SelectEvent(val event: CampusEvent) : MapEvent
    data object BackToEvents : MapEvent
    data object ShowMap : MapEvent
    data class ToggleRegisterEvent(val eventId: String) : MapEvent
    data class FocusEventLocationOnMap(val event: CampusEvent) : MapEvent
    data class FocusCoordinatesOnMap(val latitude: Double, val longitude: Double) : MapEvent
}

private val avatarColors = listOf(
    Color(0xFF6366F1), // Indigo
    Color(0xFF0D9488), // Teal
    Color(0xFFEA580C), // Orange
    Color(0xFF8B5CF6), // Purple
    Color(0xFFEC4899), // Pink
    Color(0xFF10B981)  // Emerald
)

class MapViewModel(
    private val flashMeetRepository: FlashMeetRepository,
    private val placesRepository: PlacesRepository = PlacesRepository(),
    private val studentProfileRepository: StudentProfileRepository = StudentProfileRepository()
) : ViewModel() {

    private val _selectedPlace = MutableStateFlow<CampusPlace?>(null)
    private val _selectedCategory = MutableStateFlow(PlaceCategory.TOP_PICKS)
    private val _audienceFilter = MutableStateFlow(AudienceFilter.EVERYONE)
    private val _selectedRegion = MutableStateFlow("All Hubs")
    private val _isOpenNowOnly = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _userLocation = MutableStateFlow<LatLng?>(null)
    private val _selectedMeet = MutableStateFlow<FlashMeet?>(null)
    private val _joinedMeetIds = MutableStateFlow(emptySet<String>())
    private val _isCreateDialogVisible = MutableStateFlow(false)
    private val _savedPlaceIds = MutableStateFlow(setOf("strat_quad_b", "main_library"))
    private val _beenPlaceIds = MutableStateFlow(emptySet<String>())
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    private val _navigationMode = MutableStateFlow(MapNavigationMode.MAP)
    private val _events = MutableStateFlow(sampleCampusEvents)
    private val _selectedEvent = MutableStateFlow<CampusEvent?>(null)
    private val _focusedEventLocation = MutableStateFlow<LatLng?>(null)
    private val _currentProfile = MutableStateFlow<StudentProfile?>(null)

    init {
        viewModelScope.launch {
            studentProfileRepository.getProfile().collect { profile ->
                _currentProfile.value = profile
            }
        }
    }

    val uiState: StateFlow<MapUiState> = combine(
        placesRepository.places,
        _selectedPlace,
        _selectedCategory,
        _audienceFilter,
        _selectedRegion,
        _isOpenNowOnly,
        _searchQuery,
        _userLocation,
        flashMeetRepository.getActiveFlashMeets(),
        _selectedMeet,
        _joinedMeetIds,
        _isCreateDialogVisible,
        _savedPlaceIds,
        _beenPlaceIds,
        _feedbackMessage,
        _navigationMode,
        _events,
        _selectedEvent,
        _focusedEventLocation,
        _currentProfile,
        studentProfileRepository.knownProfiles
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val allPlaces = args[0] as List<CampusPlace>
        val selectedPlace = args[1] as CampusPlace?
        val category = args[2] as PlaceCategory
        val audience = args[3] as AudienceFilter
        val region = args[4] as String
        val openNowOnly = args[5] as Boolean
        val query = args[6] as String
        val userLoc = args[7] as LatLng?
        @Suppress("UNCHECKED_CAST")
        val meets = args[8] as List<FlashMeet>
        val selectedMeet = args[9] as FlashMeet?
        @Suppress("UNCHECKED_CAST")
        val joinedIds = args[10] as Set<String>
        val isCreateVisible = args[11] as Boolean
        @Suppress("UNCHECKED_CAST")
        val savedIds = args[12] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val beenIds = args[13] as Set<String>
        val feedback = args[14] as String?
        val navMode = args[15] as MapNavigationMode
        @Suppress("UNCHECKED_CAST")
        val eventList = args[16] as List<CampusEvent>
        val currentEvent = args[17] as CampusEvent?
        val focusedLocation = args[18] as LatLng?
        val currentProfile = args[19] as StudentProfile?
        @Suppress("UNCHECKED_CAST")
        val knownProfilesMap = (args[20] as? Map<String, StudentProfile>) ?: emptyMap()

        val currentEmail = currentProfile?.universityEmail.orEmpty()
        val campusFriends = knownProfilesMap.values
            .filter { it.universityEmail.isNotBlank() && it.universityEmail != currentEmail && it.fullName.isNotBlank() }
            .distinctBy { it.universityEmail }
            .map { profile ->
                val initials = profile.initials.ifBlank {
                    profile.fullName.take(2).uppercase()
                }
                val handle = if (profile.universityEmail.contains("@")) {
                    "@" + profile.universityEmail.substringBefore("@")
                } else {
                    "@" + profile.fullName.lowercase().replace(" ", "")
                }
                val faculty = profile.degreeProgram.ifBlank { profile.universityName.ifBlank { "Campus Student" } }
                val color = avatarColors[kotlin.math.abs(profile.fullName.hashCode()) % avatarColors.size]
                CampusFriend(
                    id = profile.studentId.ifBlank { profile.universityEmail },
                    name = profile.fullName,
                    handle = handle,
                    faculty = faculty,
                    avatarInitials = initials,
                    avatarBg = color,
                    isOnline = true
                )
            }

        val filtered = allPlaces.filter { place ->
            val matchesCategory = category == PlaceCategory.TOP_PICKS || place.category == category
            val matchesRegion = region == "All Hubs" || place.campusRegion.equals(region, ignoreCase = true)
            val matchesOpenNow = !openNowOnly || place.isOpenNow
            val matchesQuery = query.isBlank() ||
                    place.name.contains(query, ignoreCase = true) ||
                    place.subtitle.contains(query, ignoreCase = true) ||
                    place.address.contains(query, ignoreCase = true) ||
                    place.campusRegion.contains(query, ignoreCase = true)
            matchesCategory && matchesRegion && matchesOpenNow && matchesQuery
        }.let { list ->
            if (userLoc != null) {
                list.sortedBy { place ->
                    haversineDistance(userLoc.latitude, userLoc.longitude, place.latitude, place.longitude)
                }
            } else {
                list
            }
        }

        MapUiState(
            places = filtered,
            selectedPlace = selectedPlace,
            selectedCategory = category,
            audienceFilter = audience,
            selectedRegion = region,
            isOpenNowOnly = openNowOnly,
            searchQuery = query,
            userLocation = userLoc,
            liveMeets = meets,
            selectedMeet = selectedMeet,
            joinedMeetIds = joinedIds,
            isCreateDialogVisible = isCreateVisible,
            savedPlaceIds = savedIds,
            beenPlaceIds = beenIds,
            feedbackMessage = feedback,
            navigationMode = navMode,
            events = eventList,
            selectedEvent = currentEvent,
            focusedEventLocation = focusedLocation,
            studentProfile = currentProfile,
            friends = campusFriends
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MapUiState()
    )

    fun onEvent(event: MapEvent) {
        when (event) {
            is MapEvent.SelectPlace -> _selectedPlace.value = event.place
            is MapEvent.FilterCategory -> {
                _selectedCategory.value = event.category
                if (event.category == PlaceCategory.EVENTS) {
                    _navigationMode.value = MapNavigationMode.EVENTS_LIST
                }
            }
            is MapEvent.FilterAudience -> _audienceFilter.value = event.audience
            is MapEvent.FilterRegion -> _selectedRegion.value = event.region
            is MapEvent.ToggleOpenNow -> _isOpenNowOnly.value = !_isOpenNowOnly.value
            is MapEvent.UpdateSearchQuery -> _searchQuery.value = event.query
            is MapEvent.ClearFeedback -> _feedbackMessage.value = null
            is MapEvent.UpdateUserLocation -> {
                _userLocation.value = LatLng(event.latitude, event.longitude)
            }
            is MapEvent.ToggleSavedToTry -> {
                val current = _savedPlaceIds.value.toMutableSet()
                if (current.contains(event.placeId)) {
                    current.remove(event.placeId)
                    _feedbackMessage.value = "Removed from your bookmarks"
                } else {
                    current.add(event.placeId)
                    _feedbackMessage.value = "Saved to your bookmarks!"
                }
                _savedPlaceIds.value = current
                placesRepository.toggleSavedToTry(event.placeId)
            }
            is MapEvent.ToggleBeen -> {
                val current = _beenPlaceIds.value.toMutableSet()
                if (current.contains(event.placeId)) {
                    current.remove(event.placeId)
                    _feedbackMessage.value = "Removed check-in record"
                } else {
                    current.add(event.placeId)
                    _feedbackMessage.value = "Checked in! Location marked as visited."
                }
                _beenPlaceIds.value = current
                placesRepository.toggleBeen(event.placeId)
            }
            is MapEvent.SubmitFeedback -> {
                _feedbackMessage.value = "Feedback submitted for verified campus spot!"
            }
            is MapEvent.SelectMeet -> _selectedMeet.value = event.meet
            is MapEvent.ToggleJoinMeet -> {
                val current = _joinedMeetIds.value.toMutableSet()
                if (current.contains(event.meetId)) {
                    current.remove(event.meetId)
                    _feedbackMessage.value = "Left the flash meetup."
                } else {
                    current.add(event.meetId)
                    _feedbackMessage.value = "Joined meetup! Host notified of your arrival."
                }
                _joinedMeetIds.value = current
            }
            is MapEvent.FilterMeetCategory -> Unit
            is MapEvent.ToggleCreateDialog -> _isCreateDialogVisible.value = event.show
            is MapEvent.CreateMeet -> {
                viewModelScope.launch {
                    val defaultLat = _userLocation.value?.latitude ?: _selectedPlace.value?.latitude ?: -1.3094
                    val defaultLng = _userLocation.value?.longitude ?: _selectedPlace.value?.longitude ?: 36.8122
                    val targetLat = event.latitude ?: defaultLat
                    val targetLng = event.longitude ?: defaultLng

                    flashMeetRepository.createFlashMeet(
                        title = event.title,
                        description = event.description,
                        category = event.category,
                        latitude = targetLat,
                        longitude = targetLng,
                        locationName = event.locationName,
                        creatorName = _currentProfile.value?.fullName ?: "You (Verified Student)",
                        durationMinutes = event.durationMinutes
                    )
                    _isCreateDialogVisible.value = false
                    _feedbackMessage.value = "Campus Flash Meet published live!"
                }
            }
            is MapEvent.CreateCampusEvent -> {
                viewModelScope.launch {
                    val updatedEvents = listOf(event.event) + _events.value
                    _events.value = updatedEvents

                    if (event.alsoPublishAsFlashMeet) {
                        flashMeetRepository.createFlashMeet(
                            title = event.event.title,
                            description = event.event.description,
                            category = event.flashMeetCategory,
                            latitude = event.event.latitude,
                            longitude = event.event.longitude,
                            locationName = event.event.locationName,
                            creatorName = event.event.hostName,
                            durationMinutes = event.durationMinutes
                        )
                    }

                    _isCreateDialogVisible.value = false
                    _feedbackMessage.value = if (event.alsoPublishAsFlashMeet) {
                        "Flash Meet & Campus Event published live!"
                    } else {
                        "Campus Event published to Events Hub!"
                    }
                }
            }

            // Events screen UI flow event handling
            is MapEvent.ShowEventsList -> {
                _navigationMode.value = MapNavigationMode.EVENTS_LIST
            }
            is MapEvent.SelectEvent -> {
                _selectedEvent.value = event.event
                _navigationMode.value = MapNavigationMode.EVENT_DETAIL
            }
            is MapEvent.BackToEvents -> {
                _navigationMode.value = MapNavigationMode.EVENTS_LIST
            }
            is MapEvent.ShowMap -> {
                _navigationMode.value = MapNavigationMode.MAP
            }
            is MapEvent.ToggleRegisterEvent -> {
                val updated = _events.value.map { ev ->
                    if (ev.id == event.eventId) {
                        val newRegistered = !ev.isUserRegistered
                        ev.copy(
                            isUserRegistered = newRegistered,
                            guestCount = if (newRegistered) ev.guestCount + 1 else maxOf(0, ev.guestCount - 1)
                        )
                    } else ev
                }
                _events.value = updated
                val current = _selectedEvent.value
                if (current != null && current.id == event.eventId) {
                    val newRegistered = !current.isUserRegistered
                    _selectedEvent.value = current.copy(
                        isUserRegistered = newRegistered,
                        guestCount = if (newRegistered) current.guestCount + 1 else maxOf(0, current.guestCount - 1)
                    )
                }
                _feedbackMessage.value = "Event RSVP updated!"
            }
            is MapEvent.FocusEventLocationOnMap -> {
                _focusedEventLocation.value = LatLng(event.event.latitude, event.event.longitude)
                _navigationMode.value = MapNavigationMode.MAP
            }
            is MapEvent.FocusCoordinatesOnMap -> {
                _focusedEventLocation.value = LatLng(event.latitude, event.longitude)
                _navigationMode.value = MapNavigationMode.MAP
            }
        }
    }

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
