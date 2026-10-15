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

data class MapUiState(
    val places: List<CampusPlace> = emptyList(),
    val selectedPlace: CampusPlace? = null,
    val selectedCategory: PlaceCategory = PlaceCategory.TOP_PICKS,
    val audienceFilter: AudienceFilter = AudienceFilter.EVERYONE,
    val isOpenNowOnly: Boolean = false,
    val searchQuery: String = "",
    val liveMeets: List<FlashMeet> = emptyList(),
    val selectedMeet: FlashMeet? = null,
    val isCreateDialogVisible: Boolean = false,
    val savedPlaceIds: Set<String> = setOf("la_cabra"),
    val beenPlaceIds: Set<String> = emptySet(),
    val feedbackMessage: String? = null,
    val navigationMode: MapNavigationMode = MapNavigationMode.MAP,
    val events: List<CampusEvent> = sampleCampusEvents,
    val selectedEvent: CampusEvent? = null,
    val focusedEventLocation: LatLng? = null
)

sealed interface MapEvent {
    data class SelectPlace(val place: CampusPlace?) : MapEvent
    data class FilterCategory(val category: PlaceCategory) : MapEvent
    data class FilterAudience(val audience: AudienceFilter) : MapEvent
    data object ToggleOpenNow : MapEvent
    data class ToggleSavedToTry(val placeId: String) : MapEvent
    data class ToggleBeen(val placeId: String) : MapEvent
    data class UpdateSearchQuery(val query: String) : MapEvent
    data class ClearFeedback(val message: String? = null) : MapEvent
    data class SubmitFeedback(val placeId: String, val comment: String) : MapEvent
    data class SelectMeet(val meet: FlashMeet?) : MapEvent
    data class FilterMeetCategory(val category: MeetCategory?) : MapEvent
    data class CreateMeet(
        val title: String,
        val description: String,
        val category: MeetCategory,
        val locationName: String,
        val durationMinutes: Int
    ) : MapEvent
    data class ToggleCreateDialog(val show: Boolean) : MapEvent

    // Events screen UI flow events
    data object ShowEventsList : MapEvent
    data class SelectEvent(val event: CampusEvent) : MapEvent
    data object BackToEvents : MapEvent
    data object ShowMap : MapEvent
    data class ToggleRegisterEvent(val eventId: String) : MapEvent
    data class FocusEventLocationOnMap(val event: CampusEvent) : MapEvent
}

class MapViewModel(
    private val flashMeetRepository: FlashMeetRepository,
    private val placesRepository: PlacesRepository = PlacesRepository()
) : ViewModel() {

    private val _selectedPlace = MutableStateFlow<CampusPlace?>(null)
    private val _selectedCategory = MutableStateFlow(PlaceCategory.TOP_PICKS)
    private val _audienceFilter = MutableStateFlow(AudienceFilter.EVERYONE)
    private val _isOpenNowOnly = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedMeet = MutableStateFlow<FlashMeet?>(null)
    private val _isCreateDialogVisible = MutableStateFlow(false)
    private val _savedPlaceIds = MutableStateFlow(setOf("la_cabra"))
    private val _beenPlaceIds = MutableStateFlow(emptySet<String>())
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    private val _navigationMode = MutableStateFlow(MapNavigationMode.MAP)
    private val _events = MutableStateFlow(sampleCampusEvents)
    private val _selectedEvent = MutableStateFlow<CampusEvent?>(null)
    private val _focusedEventLocation = MutableStateFlow<LatLng?>(null)

    val uiState: StateFlow<MapUiState> = combine(
        placesRepository.places,
        _selectedPlace,
        _selectedCategory,
        _audienceFilter,
        _isOpenNowOnly,
        _searchQuery,
        flashMeetRepository.getActiveFlashMeets(),
        _selectedMeet,
        _isCreateDialogVisible,
        _savedPlaceIds,
        _beenPlaceIds,
        _feedbackMessage,
        _navigationMode,
        _events,
        _selectedEvent,
        _focusedEventLocation
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val allPlaces = args[0] as List<CampusPlace>
        val selectedPlace = args[1] as CampusPlace?
        val category = args[2] as PlaceCategory
        val audience = args[3] as AudienceFilter
        val openNowOnly = args[4] as Boolean
        val query = args[5] as String
        @Suppress("UNCHECKED_CAST")
        val meets = args[6] as List<FlashMeet>
        val selectedMeet = args[7] as FlashMeet?
        val isCreateVisible = args[8] as Boolean
        @Suppress("UNCHECKED_CAST")
        val savedIds = args[9] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val beenIds = args[10] as Set<String>
        val feedback = args[11] as String?
        val navMode = args[12] as MapNavigationMode
        @Suppress("UNCHECKED_CAST")
        val eventList = args[13] as List<CampusEvent>
        val currentEvent = args[14] as CampusEvent?
        val focusedLocation = args[15] as LatLng?

        val filtered = allPlaces.filter { place ->
            val matchesCategory = category == PlaceCategory.TOP_PICKS || place.category == category
            val matchesOpenNow = !openNowOnly || place.isOpenNow
            val matchesQuery = query.isBlank() ||
                    place.name.contains(query, ignoreCase = true) ||
                    place.subtitle.contains(query, ignoreCase = true) ||
                    place.neighborhood.contains(query, ignoreCase = true)
            matchesCategory && matchesOpenNow && matchesQuery
        }

        MapUiState(
            places = filtered,
            selectedPlace = selectedPlace,
            selectedCategory = category,
            audienceFilter = audience,
            isOpenNowOnly = openNowOnly,
            searchQuery = query,
            liveMeets = meets,
            selectedMeet = selectedMeet,
            isCreateDialogVisible = isCreateVisible,
            savedPlaceIds = savedIds,
            beenPlaceIds = beenIds,
            feedbackMessage = feedback,
            navigationMode = navMode,
            events = eventList,
            selectedEvent = currentEvent,
            focusedEventLocation = focusedLocation
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
            is MapEvent.ToggleOpenNow -> _isOpenNowOnly.value = !_isOpenNowOnly.value
            is MapEvent.UpdateSearchQuery -> _searchQuery.value = event.query
            is MapEvent.ClearFeedback -> _feedbackMessage.value = null
            is MapEvent.ToggleSavedToTry -> {
                val current = _savedPlaceIds.value.toMutableSet()
                if (current.contains(event.placeId)) {
                    current.remove(event.placeId)
                    _feedbackMessage.value = "Removed from your to-try list"
                } else {
                    current.add(event.placeId)
                    _feedbackMessage.value = "Saved to your to-try list!"
                }
                _savedPlaceIds.value = current
                placesRepository.toggleSavedToTry(event.placeId)
            }
            is MapEvent.ToggleBeen -> {
                val current = _beenPlaceIds.value.toMutableSet()
                if (current.contains(event.placeId)) {
                    current.remove(event.placeId)
                    _feedbackMessage.value = "Removed from places you've been"
                } else {
                    current.add(event.placeId)
                    _feedbackMessage.value = "Marked as been!"
                }
                _beenPlaceIds.value = current
                placesRepository.toggleBeen(event.placeId)
            }
            is MapEvent.SubmitFeedback -> {
                _feedbackMessage.value = "Thanks for your thoughts on this place!"
            }
            is MapEvent.SelectMeet -> _selectedMeet.value = event.meet
            is MapEvent.FilterMeetCategory -> Unit
            is MapEvent.ToggleCreateDialog -> _isCreateDialogVisible.value = event.show
            is MapEvent.CreateMeet -> {
                viewModelScope.launch {
                    flashMeetRepository.createFlashMeet(
                        title = event.title,
                        description = event.description,
                        category = event.category,
                        latitude = 40.7410 + (Math.random() - 0.5) * 0.015,
                        longitude = -73.9890 + (Math.random() - 0.5) * 0.015,
                        locationName = event.locationName,
                        creatorName = "You (Verified Student)",
                        durationMinutes = event.durationMinutes
                    )
                    _isCreateDialogVisible.value = false
                    _feedbackMessage.value = "Flash Meet created!"
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
                _feedbackMessage.value = "Registration updated!"
            }
            is MapEvent.FocusEventLocationOnMap -> {
                _focusedEventLocation.value = LatLng(event.event.latitude, event.event.longitude)
                _navigationMode.value = MapNavigationMode.MAP
            }
        }
    }
}
