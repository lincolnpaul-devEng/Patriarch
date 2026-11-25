package com.campus.network.feature.map

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.network.core.data.CampusEventsRepository
import com.campus.network.core.data.CampusInboxNotifier
import com.campus.network.core.data.FlashMeetRepository
import com.campus.network.core.data.LiveLocationRepository
import com.campus.network.core.data.PlacesRepository
import com.campus.network.core.data.StudentProfileRepository
import com.campus.network.core.database.dao.FriendDao
import com.campus.network.core.database.entity.FriendEntity
import com.campus.network.core.model.AudienceFilter
import com.campus.network.core.model.CampusEventRecord
import com.campus.network.core.model.CampusPlace
import com.campus.network.core.model.FlashMeet
import com.campus.network.core.model.FriendLiveLocation
import com.campus.network.core.model.FriendStatus
import com.campus.network.core.model.LocationPrivacyScope
import com.campus.network.core.model.MeetCategory
import com.campus.network.core.model.PlaceCategory
import com.campus.network.core.model.StudentProfile
import com.campus.network.core.model.initials
import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.model.stableUserId
import com.campus.network.feature.map.components.CampusFriend
import com.campus.network.feature.map.data.MapboxSearchRepository
import com.campus.network.feature.map.data.WalkingNavigationController
import com.campus.network.feature.map.domain.ActiveNavigationSession
import com.campus.network.feature.map.domain.ArViewMode
import com.campus.network.feature.map.domain.DeviceOrientationState
import com.campus.network.feature.map.domain.GeoCoordinate
import com.campus.network.feature.map.domain.GeoSpatialMath
import com.campus.network.feature.map.domain.GeospatialAnchorMode
import com.campus.network.feature.map.domain.SearchSuggestionItem
import com.campus.network.feature.map.domain.TiltSwitchPolicy
import com.campus.network.feature.map.events.CampusEvent
import com.campus.network.feature.map.events.EventBadgeType
import com.campus.network.feature.map.events.MapNavigationMode
import com.campus.network.feature.map.events.artworkKeyForRes
import com.campus.network.feature.map.events.artworkResForKey
import com.campus.network.feature.map.events.formatEventDateTime
import com.campus.network.feature.map.sensor.DeviceOrientationSensor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
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
    val availableRegions: List<String> = listOf("All Hubs"),
    val regionCentroids: Map<String, GeoCoordinate> = emptyMap(),
    val isOpenNowOnly: Boolean = false,
    val searchQuery: String = "",
    val searchSuggestions: List<SearchSuggestionItem> = emptyList(),
    val isSearchingDestinations: Boolean = false,
    val isSearchBarActive: Boolean = false,
    val userLocation: GeoCoordinate? = null,
    val liveMeets: List<FlashMeet> = emptyList(),
    val selectedMeetCategory: MeetCategory? = null,
    val selectedMeet: FlashMeet? = null,
    val activeHostedMeet: FlashMeet? = null,
    val joinedMeetIds: Set<String> = emptySet(),
    val isCreateDialogVisible: Boolean = false,
    val savedPlaceIds: Set<String> = emptySet(),
    val beenPlaceIds: Set<String> = emptySet(),
    val feedbackMessage: String? = null,
    val navigationMode: MapNavigationMode = MapNavigationMode.MAP,
    val events: List<CampusEvent> = emptyList(),
    val selectedEvent: CampusEvent? = null,
    val focusedEventLocation: GeoCoordinate? = null,
    val studentProfile: StudentProfile? = null,
    val stableCurrentUserId: String = "",
    val friends: List<CampusFriend> = emptyList(),
    val liveFriendLocations: List<FriendLiveLocation> = emptyList(),
    val selectedFriendLocation: FriendLiveLocation? = null,
    val isSharingLocation: Boolean = false,
    val locationPrivacyScope: LocationPrivacyScope = LocationPrivacyScope.VERIFIED_CAMPUS_PEERS,
    val activeShareToken: String? = null,
    // AR Navigation, Search & Tilt-to-Switch state
    val arViewMode: ArViewMode = ArViewMode.MAP_2D,
    val tiltSwitchPolicy: TiltSwitchPolicy = TiltSwitchPolicy.AUTO_TILT,
    val deviceOrientation: DeviceOrientationState = DeviceOrientationState(),
    val geospatialMode: GeospatialAnchorMode = GeospatialAnchorMode.INITIALIZING,
    val activeNavigation: ActiveNavigationSession? = null
) {
    val studentName: String
        get() = studentProfile?.fullName?.takeIf(String::isRealProfileDisplayName).orEmpty()

    val studentFirstName: String
        get() = studentProfile?.fullName
            ?.takeIf(String::isRealProfileDisplayName)
            ?.substringBefore(' ')
            ?.trim()
            .orEmpty()

    val studentInitials: String
        get() = studentProfile?.takeIf { it.fullName.isRealProfileDisplayName() }?.initials.orEmpty()

    val studentAvatarUrl: String?
        get() = studentProfile?.avatarUrl

    val activeShareUrl: String?
        get() = if (
            isSharingLocation &&
            locationPrivacyScope.allowsCoordinatesBroadcast &&
            !activeShareToken.isNullOrBlank()
        ) {
            "patriarch://map/share?tok=$activeShareToken"
        } else {
            null
        }
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
    data class SubmitDestinationSearch(val query: String) : MapEvent
    data class SetSearchBarActive(val active: Boolean) : MapEvent
    data object ClearDestinationSearch : MapEvent
    data class SelectSearchSuggestion(val item: SearchSuggestionItem) : MapEvent
    data class ClearFeedback(val message: String? = null) : MapEvent
    data class SubmitFeedback(val placeId: String, val comment: String) : MapEvent
    data class UpdateUserLocation(val latitude: Double, val longitude: Double) : MapEvent
    data class SelectMeet(val meet: FlashMeet?) : MapEvent
    data class ToggleJoinMeet(val meetId: String) : MapEvent
    data class EndMeet(val meetId: String) : MapEvent
    data class FilterMeetCategory(val category: MeetCategory?) : MapEvent
    data class CreateMeet(
        val title: String,
        val description: String,
        val category: MeetCategory,
        val locationName: String,
        val durationMinutes: Int,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val invitedFriendIds: List<String> = emptyList()
    ) : MapEvent
    data class CreateCampusEvent(
        val event: CampusEvent,
        val alsoPublishAsFlashMeet: Boolean = false,
        val flashMeetCategory: MeetCategory = MeetCategory.PROJECT_HACK,
        val durationMinutes: Int = 60
    ) : MapEvent
    data class DeleteCampusEvent(val eventId: String) : MapEvent
    data class ToggleCreateDialog(val show: Boolean) : MapEvent

    // Live friend location sharing events
    data class StartLocationSharing(val ttlMinutes: Int = 30) : MapEvent
    data object StopLocationSharing : MapEvent
    data class UpdateLocationScope(val scope: LocationPrivacyScope) : MapEvent
    data class PublishLiveCoordinates(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Float? = null,
        val heading: Float? = null
    ) : MapEvent
    data class RedeemLocationShareToken(val token: String) : MapEvent
    data class SelectFriendLocation(val location: FriendLiveLocation?) : MapEvent

    // AR & Mapbox Walking Navigation events
    data class UpdateOrientation(val orientation: DeviceOrientationState) : MapEvent
    data class SetTiltSwitchPolicy(val policy: TiltSwitchPolicy) : MapEvent
    data object ToggleArManualMode : MapEvent
    data object CycleTiltPolicy : MapEvent
    data class UpdateGeospatialMode(val mode: GeospatialAnchorMode) : MapEvent
    data class StartWalkingNavigation(
        val destination: GeoCoordinate,
        val title: String,
        val subtitle: String = "Walking Route",
        val switchToArImmediately: Boolean = false
    ) : MapEvent
    data object StopWalkingNavigation : MapEvent

    // Events screen UI flow events
    data object ShowEventsList : MapEvent
    data class SelectEvent(val event: CampusEvent) : MapEvent
    data object BackToEvents : MapEvent
    data object ShowMap : MapEvent
    data class ToggleRegisterEvent(val eventId: String) : MapEvent
    data class FocusEventLocationOnMap(val event: CampusEvent) : MapEvent
    data class FocusCoordinatesOnMap(val latitude: Double, val longitude: Double) : MapEvent
    data object RefreshMapData : MapEvent
}

private val avatarColors = listOf(
    Color(0xFF6366F1),
    Color(0xFF0D9488),
    Color(0xFFEA580C),
    Color(0xFF8B5CF6),
    Color(0xFFEC4899),
    Color(0xFF10B981)
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MapViewModel @Inject constructor(
    private val flashMeetRepository: FlashMeetRepository,
    private val placesRepository: PlacesRepository,
    private val studentProfileRepository: StudentProfileRepository,
    private val campusEventsRepository: CampusEventsRepository,
    private val liveLocationRepository: LiveLocationRepository,
    private val campusInboxNotifier: CampusInboxNotifier,
    private val friendDao: FriendDao,
    val liveLocationClient: LiveLocationClient,
    private val orientationSensor: DeviceOrientationSensor,
    private val mapboxSearchRepository: MapboxSearchRepository,
    private val walkingNavigationController: WalkingNavigationController
) : ViewModel() {

    private val _selectedPlaceId = MutableStateFlow<String?>(null)
    private val _selectedCategory = MutableStateFlow(PlaceCategory.TOP_PICKS)
    private val _audienceFilter = MutableStateFlow(AudienceFilter.EVERYONE)
    private val _selectedRegion = MutableStateFlow("All Hubs")
    private val _isOpenNowOnly = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _searchSuggestions = MutableStateFlow<List<SearchSuggestionItem>>(emptyList())
    private val _isSearchingDestinations = MutableStateFlow(false)
    private val _isSearchBarActive = MutableStateFlow(false)
    private val _userLocation = MutableStateFlow<GeoCoordinate?>(null)
    private val _selectedMeetId = MutableStateFlow<String?>(null)
    private val _selectedMeetCategory = MutableStateFlow<MeetCategory?>(null)
    private val _isCreateDialogVisible = MutableStateFlow(false)
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    private val _navigationMode = MutableStateFlow(MapNavigationMode.MAP)
    private val _selectedEventId = MutableStateFlow<String?>(null)
    private val _focusedEventLocation = MutableStateFlow<GeoCoordinate?>(null)
    private val _selectedFriendUserId = MutableStateFlow<String?>(null)
    private val _currentProfile = MutableStateFlow<StudentProfile?>(null)
    private val _latestAllPlaces = MutableStateFlow<List<CampusPlace>>(emptyList())

    private val _tiltSwitchPolicy = MutableStateFlow(TiltSwitchPolicy.AUTO_TILT)
    private val _deviceOrientation = MutableStateFlow(DeviceOrientationState())
    private val _geospatialMode = MutableStateFlow(GeospatialAnchorMode.INITIALIZING)

    private var searchDebounceJob: Job? = null

    fun observeDeviceOrientation(): Flow<DeviceOrientationState> = orientationSensor.observeOrientation()

    private val acceptedFriendsFlow: Flow<List<FriendEntity>> = _currentProfile
        .map { p -> p?.stableUserId().orEmpty() to p?.universityEmail.orEmpty().trim() }
        .distinctUntilChanged()
        .flatMapLatest { (userId, email) ->
            if (userId.isBlank() && email.isBlank()) {
                flowOf(emptyList())
            } else {
                friendDao.getFriendsForUser(userId, email)
            }
        }

    private fun refreshAllRemoteData(userId: String?) {
        val friendIds = uiState.value.friends.map { it.id }.toSet()
        viewModelScope.launch { flashMeetRepository.syncRemoteFlashMeets() }
        viewModelScope.launch { flashMeetRepository.syncJoinedMeetIds(userId) }
        viewModelScope.launch { campusEventsRepository.syncRemoteEvents(userId) }
        viewModelScope.launch { liveLocationRepository.syncVisibleFriendLocations(userId) }
        viewModelScope.launch { placesRepository.syncPlaces(userId, friendIds) }
    }

    internal data class FilterState(
        val selectedCategory: PlaceCategory,
        val audienceFilter: AudienceFilter,
        val selectedRegion: String,
        val isOpenNowOnly: Boolean,
        val searchQuery: String,
        val selectedMeetCategory: MeetCategory?
    )

    private data class SelectionState(
        val selectedPlaceId: String?,
        val selectedMeetId: String?,
        val selectedFriendUserId: String?,
        val isCreateDialogVisible: Boolean,
        val feedbackMessage: String?,
        val userLocation: GeoCoordinate?
    )

    private data class PlacesAndMeetsState(
        val places: List<CampusPlace>,
        val savedIds: Set<String>,
        val beenIds: Set<String>,
        val friendSavedIds: Set<String>,
        val meets: List<FlashMeet>,
        val joinedMeetIds: Set<String>
    )

    private data class LocationShareState(
        val liveLocations: List<FriendLiveLocation>,
        val isSharing: Boolean,
        val shareToken: String?,
        val scope: LocationPrivacyScope
    )

    private data class EventsAndProfileState(
        val navMode: MapNavigationMode,
        val events: List<CampusEvent>,
        val selectedEventId: String?,
        val focusedLocation: GeoCoordinate?,
        val currentProfile: StudentProfile?,
        val knownProfiles: Map<String, StudentProfile>,
        val acceptedFriendEntities: List<FriendEntity>
    )

    private data class ArNavCompositeState(
        val tiltPolicy: TiltSwitchPolicy,
        val orientation: DeviceOrientationState,
        val geospatialMode: GeospatialAnchorMode,
        val activeNavigation: ActiveNavigationSession?,
        val searchSuggestions: List<SearchSuggestionItem>,
        val isSearching: Boolean,
        val isSearchBarActive: Boolean
    )

    private val filterStateFlow = combine(
        combine(_selectedCategory, _audienceFilter, _selectedRegion) { cat, aud, reg ->
            Triple(cat, aud, reg)
        },
        combine(_isOpenNowOnly, _searchQuery, _selectedMeetCategory) { openOnly, query, meetCat ->
            Triple(openOnly, query, meetCat)
        }
    ) { (cat, aud, reg), (openOnly, query, meetCat) ->
        FilterState(cat, aud, reg, openOnly, query, meetCat)
    }

    private val selectionStateFlow = combine(
        combine(_selectedPlaceId, _selectedMeetId, _selectedFriendUserId) { placeId, meetId, friendId ->
            Triple(placeId, meetId, friendId)
        },
        combine(_isCreateDialogVisible, _feedbackMessage, _userLocation) { createVis, feedback, userLoc ->
            Triple(createVis, feedback, userLoc)
        }
    ) { (placeId, meetId, friendId), (createVis, feedback, userLoc) ->
        SelectionState(placeId, meetId, friendId, createVis, feedback, userLoc)
    }

    private val placesAndMeetsFlow = combine(
        combine(
            placesRepository.places.map { list ->
                list.filterNot { it.id in LEGACY_HARDCODED_PLACE_IDS }
            },
            placesRepository.savedPlaceIds,
            placesRepository.beenPlaceIds
        ) { places, savedIds, beenIds -> Triple(places, savedIds, beenIds) },
        combine(
            placesRepository.friendSavedPlaceIds,
            flashMeetRepository.getActiveFlashMeets(),
            flashMeetRepository.joinedMeetIds
        ) { friendSavedIds, meets, joinedIds -> Triple(friendSavedIds, meets, joinedIds) }
    ) { (places, savedIds, beenIds), (friendSavedIds, meets, joinedIds) ->
        PlacesAndMeetsState(places, savedIds, beenIds, friendSavedIds, meets, joinedIds)
    }

    private val locationShareFlow = combine(
        liveLocationRepository.friendLocations,
        liveLocationRepository.isSharing,
        liveLocationRepository.activeShareToken,
        studentProfileRepository.locationPrivacyScope
    ) { liveLocs, isSharing, token, scope ->
        LocationShareState(liveLocs, isSharing, token, scope)
    }

    private val eventsAndProfileFlow = combine(
        combine(
            _navigationMode,
            campusEventsRepository.observeEvents(),
            _selectedEventId
        ) { navMode, eventRecords, selectedEventId ->
            Triple(navMode, eventRecords.map { it.toUiEvent() }, selectedEventId)
        },
        combine(
            _focusedEventLocation,
            _currentProfile,
            studentProfileRepository.knownProfiles,
            acceptedFriendsFlow
        ) { focusedLoc, currentProf, knownMap, friendEntities ->
            listOf(focusedLoc, currentProf, knownMap, friendEntities)
        }
    ) { (navMode, uiEvents, selectedEventId), extras ->
        @Suppress("UNCHECKED_CAST")
        EventsAndProfileState(
            navMode = navMode,
            events = uiEvents,
            selectedEventId = selectedEventId,
            focusedLocation = extras[0] as GeoCoordinate?,
            currentProfile = extras[1] as StudentProfile?,
            knownProfiles = (extras[2] as? Map<String, StudentProfile>).orEmpty(),
            acceptedFriendEntities = (extras[3] as? List<FriendEntity>).orEmpty()
        )
    }

    private val arNavCompositeFlow = combine(
        combine(
            _tiltSwitchPolicy,
            _deviceOrientation,
            _geospatialMode,
            walkingNavigationController.navigationSession
        ) { policy, orient, geoMode, activeNav ->
            listOf(policy, orient, geoMode, activeNav)
        },
        combine(
            _searchSuggestions,
            _isSearchingDestinations,
            _isSearchBarActive
        ) { suggestions, searching, barActive ->
            Triple(suggestions, searching, barActive)
        }
    ) { arList, searchTuple ->
        ArNavCompositeState(
            tiltPolicy = arList[0] as TiltSwitchPolicy,
            orientation = arList[1] as DeviceOrientationState,
            geospatialMode = arList[2] as GeospatialAnchorMode,
            activeNavigation = arList[3] as ActiveNavigationSession?,
            searchSuggestions = searchTuple.first,
            isSearching = searchTuple.second,
            isSearchBarActive = searchTuple.third
        )
    }

    private val coreCombinedFlow = combine(
        filterStateFlow,
        selectionStateFlow,
        placesAndMeetsFlow,
        locationShareFlow,
        eventsAndProfileFlow
    ) { filters, selection, data, locShare, evProf ->
        listOf(filters, selection, data, locShare, evProf)
    }

    val uiState: StateFlow<MapUiState> = combine(
        coreCombinedFlow,
        arNavCompositeFlow
    ) { coreList, arState ->
        val filters = coreList[0] as FilterState
        val selection = coreList[1] as SelectionState
        val data = coreList[2] as PlacesAndMeetsState
        val locShare = coreList[3] as LocationShareState
        val evProf = coreList[4] as EventsAndProfileState

        val currentProfile = evProf.currentProfile
        val knownMap = evProf.knownProfiles
        val stableCurrentId = currentProfile?.stableUserId().orEmpty()
        val canonicalProfile = resolveCanonicalProfile(currentProfile, knownMap)

        val campusFriends = buildAcceptedCampusFriends(
            acceptedEntities = evProf.acceptedFriendEntities,
            knownProfiles = knownMap,
            liveLocations = locShare.liveLocations
        )
        val friendIdSet = campusFriends.map { it.id.trim().lowercase() }.toSet()

        val filteredPlaces = filterPlaces(
            allPlaces = data.places,
            filters = filters,
            userLoc = selection.userLocation,
            savedIds = data.savedIds,
            beenIds = data.beenIds,
            friendSavedIds = data.friendSavedIds
        )

        val filteredMeets = filterMeets(
            allMeets = data.meets,
            filters = filters,
            stableCurrentId = stableCurrentId,
            friendIdSet = friendIdSet,
            joinedMeetIds = data.joinedMeetIds
        )

        val regions = listOf("All Hubs") + data.places
            .map { it.campusRegion.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        val regionCentroids = data.places
            .filter { it.campusRegion.isNotBlank() }
            .groupBy { it.campusRegion.trim() }
            .mapValues { (_, list) ->
                val avgLat = list.map { it.latitude }.average()
                val avgLng = list.map { it.longitude }.average()
                GeoCoordinate(avgLat, avgLng)
            }

        val activeHostedMeet = if (stableCurrentId.isNotBlank()) {
            data.meets.firstOrNull {
                it.creatorUserId.equals(stableCurrentId, ignoreCase = true) && !it.isExpired
            }
        } else {
            null
        }

        val effectiveArMode = when (arState.tiltPolicy) {
            TiltSwitchPolicy.LOCKED_AR -> ArViewMode.AR_CAMERA
            TiltSwitchPolicy.LOCKED_2D -> ArViewMode.MAP_2D
            TiltSwitchPolicy.AUTO_TILT -> {
                if (arState.orientation.isArPitchActive) ArViewMode.AR_CAMERA
                else ArViewMode.MAP_2D
            }
        }

        MapUiState(
            places = filteredPlaces,
            selectedPlace = data.places.firstOrNull { it.id == selection.selectedPlaceId },
            selectedCategory = filters.selectedCategory,
            audienceFilter = filters.audienceFilter,
            selectedRegion = filters.selectedRegion,
            availableRegions = regions,
            regionCentroids = regionCentroids,
            isOpenNowOnly = filters.isOpenNowOnly,
            searchQuery = filters.searchQuery,
            searchSuggestions = arState.searchSuggestions,
            isSearchingDestinations = arState.isSearching,
            isSearchBarActive = arState.isSearchBarActive,
            userLocation = selection.userLocation,
            liveMeets = filteredMeets,
            selectedMeetCategory = filters.selectedMeetCategory,
            selectedMeet = data.meets.firstOrNull { it.id == selection.selectedMeetId },
            activeHostedMeet = activeHostedMeet,
            joinedMeetIds = data.joinedMeetIds,
            isCreateDialogVisible = selection.isCreateDialogVisible,
            savedPlaceIds = data.savedIds,
            beenPlaceIds = data.beenIds,
            feedbackMessage = selection.feedbackMessage,
            navigationMode = evProf.navMode,
            events = evProf.events,
            selectedEvent = evProf.events.firstOrNull { it.id == evProf.selectedEventId },
            focusedEventLocation = evProf.focusedLocation,
            studentProfile = canonicalProfile,
            stableCurrentUserId = stableCurrentId,
            friends = campusFriends,
            liveFriendLocations = locShare.liveLocations,
            selectedFriendLocation = locShare.liveLocations.firstOrNull {
                it.userId.equals(selection.selectedFriendUserId, ignoreCase = true)
            },
            isSharingLocation = locShare.isSharing,
            locationPrivacyScope = locShare.scope,
            activeShareToken = locShare.shareToken,
            arViewMode = effectiveArMode,
            tiltSwitchPolicy = arState.tiltPolicy,
            deviceOrientation = arState.orientation,
            geospatialMode = arState.geospatialMode,
            activeNavigation = arState.activeNavigation
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MapUiState()
    )

    init {
        viewModelScope.launch {
            liveLocationClient.getLastOrCurrentLocation()?.let { loc ->
                _userLocation.value = GeoCoordinate(loc.latitude, loc.longitude)
            }
        }
        viewModelScope.launch {
            placesRepository.places.collect { list ->
                _latestAllPlaces.value = list.filterNot { it.id in LEGACY_HARDCODED_PLACE_IDS }
            }
        }
        viewModelScope.launch {
            studentProfileRepository.getProfile().collect { profile ->
                _currentProfile.value = profile
            }
        }
        viewModelScope.launch {
            _currentProfile
                .map { it?.stableUserId().orEmpty() }
                .distinctUntilChanged()
                .collect { userId ->
                    refreshAllRemoteData(userId.ifBlank { null })
                }
        }
        viewModelScope.launch {
            walkingNavigationController.routingStatus.collect { msg ->
                if (!msg.isNullOrBlank()) {
                    _feedbackMessage.value = msg
                }
            }
        }
    }

    fun onEvent(event: MapEvent) {
        when (event) {
            is MapEvent.SelectPlace -> _selectedPlaceId.value = event.place?.id
            is MapEvent.FilterCategory -> {
                _selectedCategory.value = event.category
                if (event.category == PlaceCategory.EVENTS) {
                    _navigationMode.value = MapNavigationMode.EVENTS_LIST
                }
            }
            is MapEvent.FilterAudience -> _audienceFilter.value = event.audience
            is MapEvent.FilterRegion -> _selectedRegion.value = event.region
            is MapEvent.ToggleOpenNow -> _isOpenNowOnly.value = !_isOpenNowOnly.value
            is MapEvent.UpdateSearchQuery -> {
                _searchQuery.value = event.query
                performUnifiedDestinationSearch(event.query)
            }
            is MapEvent.SubmitDestinationSearch -> {
                submitDirectDestinationSearch(event.query)
            }
            is MapEvent.SetSearchBarActive -> {
                _isSearchBarActive.value = event.active
                if (event.active && _searchQuery.value.isBlank()) {
                    populateDefaultSuggestions()
                }
            }
            is MapEvent.ClearDestinationSearch -> {
                _searchQuery.value = ""
                _searchSuggestions.value = emptyList()
                _isSearchingDestinations.value = false
                _isSearchBarActive.value = false
            }
            is MapEvent.SelectSearchSuggestion -> {
                handleSelectSuggestion(event.item)
            }
            is MapEvent.ClearFeedback -> _feedbackMessage.value = null
            is MapEvent.UpdateUserLocation -> {
                val coord = GeoCoordinate(event.latitude, event.longitude)
                _userLocation.value = coord
                walkingNavigationController.updateUserLocationFallback(coord)
            }
            is MapEvent.ToggleSavedToTry -> {
                viewModelScope.launch {
                    val userId = currentStableUserId()
                    placesRepository.toggleSavedToTry(event.placeId, userId)
                        .onSuccess { saved ->
                            _feedbackMessage.value = if (saved) {
                                "Saved to your bookmarks!"
                            } else {
                                "Removed from your bookmarks"
                            }
                        }
                        .onFailure {
                            _feedbackMessage.value = "Could not save bookmark. Check your connection."
                        }
                }
            }
            is MapEvent.ToggleBeen -> {
                viewModelScope.launch {
                    val userId = currentStableUserId()
                    placesRepository.toggleBeen(event.placeId, userId)
                        .onSuccess { checkedIn ->
                            _feedbackMessage.value = if (checkedIn) {
                                "Checked in! Location marked as visited."
                            } else {
                                "Removed check-in record"
                            }
                        }
                        .onFailure {
                            _feedbackMessage.value = "Could not update check-in. Check your connection."
                        }
                }
            }
            is MapEvent.SubmitFeedback -> {
                val comment = event.comment.trim()
                if (comment.isBlank()) return
                viewModelScope.launch {
                    val userId = currentStableUserId()
                    if (userId.isBlank()) {
                        _feedbackMessage.value = "Sign in to post a spot review."
                        return@launch
                    }
                    placesRepository.submitReview(
                        placeId = event.placeId,
                        userId = userId,
                        authorName = currentUiDisplayName(),
                        comment = comment
                    ).onSuccess {
                        _feedbackMessage.value = "Review posted for campus spot!"
                    }.onFailure { err ->
                        _feedbackMessage.value = err.localizedMessage ?: "Failed to post review."
                    }
                }
            }
            is MapEvent.SelectMeet -> _selectedMeetId.value = event.meet?.id
            is MapEvent.ToggleJoinMeet -> {
                viewModelScope.launch {
                    val userId = currentStableUserId()
                    if (userId.isBlank()) {
                        _feedbackMessage.value = "Sign in to join Flash Meets."
                        return@launch
                    }
                    val targetMeet = uiState.value.liveMeets.firstOrNull { it.id == event.meetId }
                    if (targetMeet != null && targetMeet.creatorUserId.equals(userId, ignoreCase = true)) {
                        _feedbackMessage.value = "You are hosting this Flash Meet."
                        return@launch
                    }
                    flashMeetRepository.toggleAttendance(event.meetId)
                        .onSuccess { (isJoined, _) ->
                            _feedbackMessage.value = if (isJoined) {
                                "Joined meetup! Host notified of your arrival."
                            } else {
                                "Left the flash meetup."
                            }
                            if (isJoined && targetMeet != null && targetMeet.creatorUserId.isNotBlank()) {
                                campusInboxNotifier.notifyFlashMeetJoined(
                                    hostUserId = targetMeet.creatorUserId,
                                    joinerUserId = userId,
                                    meetId = targetMeet.id,
                                    title = targetMeet.title,
                                    locationName = targetMeet.locationName,
                                    latitude = targetMeet.latitude,
                                    longitude = targetMeet.longitude
                                )
                            }
                        }
                        .onFailure { err ->
                            _feedbackMessage.value = err.localizedMessage ?: "Unable to update meetup attendance."
                        }
                }
            }
            is MapEvent.EndMeet -> {
                viewModelScope.launch {
                    flashMeetRepository.endFlashMeet(event.meetId)
                        .onSuccess {
                            if (_selectedMeetId.value == event.meetId) {
                                _selectedMeetId.value = null
                            }
                            _feedbackMessage.value = "Flash Meet ended."
                        }
                        .onFailure { err ->
                            _feedbackMessage.value = err.localizedMessage ?: "Could not end Flash Meet."
                        }
                }
            }
            is MapEvent.FilterMeetCategory -> {
                _selectedMeetCategory.value = event.category
            }
            is MapEvent.ToggleCreateDialog -> _isCreateDialogVisible.value = event.show
            is MapEvent.CreateMeet -> {
                viewModelScope.launch {
                    val creatorUserId = currentStableUserId()
                    if (creatorUserId.isBlank()) {
                        _feedbackMessage.value = "A Supabase profile is required to publish a flash meet."
                        return@launch
                    }
                    val liveLoc = _userLocation.value
                        ?: liveLocationClient.getLastOrCurrentLocation()?.let {
                            GeoCoordinate(it.latitude, it.longitude).also { coord -> _userLocation.value = coord }
                        }
                    val targetLat = event.latitude
                        ?: liveLoc?.latitude
                        ?: uiState.value.selectedPlace?.latitude
                    val targetLng = event.longitude
                        ?: liveLoc?.longitude
                        ?: uiState.value.selectedPlace?.longitude
                    if (targetLat == null || targetLng == null) {
                        _feedbackMessage.value = "Enable GPS or tap a location on the map to place your Flash Meet."
                        return@launch
                    }

                    val meet = flashMeetRepository.createFlashMeet(
                        title = event.title,
                        description = event.description,
                        category = event.category,
                        latitude = targetLat,
                        longitude = targetLng,
                        locationName = event.locationName,
                        creatorUserId = creatorUserId,
                        durationMinutes = event.durationMinutes,
                        creatorDisplayName = currentUiDisplayName()
                    )

                    if (meet.syncStatus == "SYNCED") {
                        var notifiedCount = 0
                        for (friendId in event.invitedFriendIds.distinct()) {
                            val res = campusInboxNotifier.notifyFlashMeetInvite(
                                recipientUserId = friendId,
                                hostUserId = creatorUserId,
                                meetId = meet.id,
                                title = meet.title,
                                locationName = meet.locationName,
                                latitude = targetLat,
                                longitude = targetLng
                            )
                            if (res.isSuccess) notifiedCount++
                        }

                        _isCreateDialogVisible.value = false
                        _feedbackMessage.value = if (notifiedCount > 0) {
                            "Flash Meet live! Invited $notifiedCount friend(s)."
                        } else {
                            "Campus Flash Meet published live!"
                        }
                    } else {
                        _isCreateDialogVisible.value = false
                        _feedbackMessage.value = "Could not publish Flash Meet to the remote database. Please check your connection and tap to retry. Tap to Retry."
                    }
                }
            }
            is MapEvent.CreateCampusEvent -> {
                viewModelScope.launch {
                    val creatorUserId = currentStableUserId()
                    if (creatorUserId.isBlank()) {
                        _feedbackMessage.value = "Sign in to publish a campus event."
                        return@launch
                    }
                    val profileName = currentUiDisplayName()
                    val resolvedHostName = event.event.hostName.trim().ifBlank { profileName }
                    val resolvedStartIso = event.event.startTimeIso.ifBlank {
                        java.time.Instant.now().toString()
                    }
                    val resolvedArtworkKey = event.event.artworkKey.ifBlank {
                        artworkKeyForRes(event.event.artworkRes)
                    }

                    val eventResult = campusEventsRepository.createEvent(
                        id = event.event.id,
                        creatorUserId = creatorUserId,
                        title = event.event.title,
                        hostName = resolvedHostName,
                        description = event.event.description,
                        locationName = event.event.locationName,
                        fullAddress = event.event.fullAddress,
                        latitude = event.event.latitude,
                        longitude = event.event.longitude,
                        startTimeIso = resolvedStartIso,
                        category = event.event.category,
                        priceLabel = event.event.priceLabel,
                        maxCapacity = event.event.maxCapacity,
                        artworkKey = resolvedArtworkKey,
                        bannerUrl = event.event.bannerUrl
                    )

                    if (eventResult.isFailure) {
                        _isCreateDialogVisible.value = false
                        _feedbackMessage.value = "Campus event was not published: ${
                            eventResult.exceptionOrNull()?.localizedMessage ?: "sync failed"
                        }"
                        return@launch
                    }

                    var confirmation = "Campus Event published to Events Hub!"
                    if (event.alsoPublishAsFlashMeet) {
                        val meet = flashMeetRepository.createFlashMeet(
                            title = event.event.title,
                            description = event.event.description,
                            category = event.flashMeetCategory,
                            latitude = event.event.latitude,
                            longitude = event.event.longitude,
                            locationName = event.event.locationName,
                            creatorUserId = creatorUserId,
                            durationMinutes = event.durationMinutes,
                            creatorDisplayName = resolvedHostName
                        )
                        if (meet.syncStatus != "SYNCED") {
                            confirmation = "Campus event published, but the Flash Meet could not be synced to the remote database. Tap to Retry."
                        } else {
                            confirmation = "Flash Meet & Campus Event published live!"
                        }
                    }

                    _isCreateDialogVisible.value = false
                    _feedbackMessage.value = confirmation
                }
            }
            is MapEvent.DeleteCampusEvent -> {
                viewModelScope.launch {
                    campusEventsRepository.deleteEvent(event.eventId)
                        .onSuccess {
                            _selectedEventId.value = null
                            _navigationMode.value = MapNavigationMode.EVENTS_LIST
                            _feedbackMessage.value = "Campus event cancelled."
                        }
                        .onFailure { err ->
                            _feedbackMessage.value = err.localizedMessage ?: "Failed to cancel event."
                        }
                }
            }
            is MapEvent.StartLocationSharing -> {
                viewModelScope.launch {
                    val loc = _userLocation.value
                    if (loc == null) {
                        _feedbackMessage.value = "Waiting for GPS lock before sharing location."
                        return@launch
                    }
                    val currentScope = uiState.value.locationPrivacyScope
                    val targetScope = if (currentScope.allowsCoordinatesBroadcast) {
                        currentScope
                    } else {
                        LocationPrivacyScope.VERIFIED_CAMPUS_PEERS
                    }
                    liveLocationRepository.startSharing(
                        scope = targetScope,
                        ttlMinutes = event.ttlMinutes,
                        latitude = loc.latitude,
                        longitude = loc.longitude
                    ).onSuccess {
                        _feedbackMessage.value = "Live location sharing active (${targetScope.badgeLabel})."
                    }.onFailure { err ->
                        _feedbackMessage.value = err.localizedMessage ?: "Could not start live location sharing."
                    }
                }
            }
            is MapEvent.StopLocationSharing -> {
                viewModelScope.launch {
                    liveLocationRepository.stopSharing()
                        .onSuccess {
                            _feedbackMessage.value = "Location sharing ended for all viewers."
                        }
                        .onFailure { err ->
                            _feedbackMessage.value = err.localizedMessage ?: "Failed to stop sharing."
                        }
                }
            }
            is MapEvent.UpdateLocationScope -> {
                viewModelScope.launch {
                    val loc = _userLocation.value
                    if (!event.scope.allowsCoordinatesBroadcast) {
                        liveLocationRepository.stopSharing()
                            .onSuccess {
                                _feedbackMessage.value = "Ghost Mode enabled — live location hidden."
                            }
                            .onFailure { err ->
                                _feedbackMessage.value = "Could not enable Ghost Mode on server: ${err.localizedMessage ?: "check your connection"}"
                            }
                    } else if (loc != null) {
                        liveLocationRepository.updateScope(
                            scope = event.scope,
                            latitude = loc.latitude,
                            longitude = loc.longitude
                        ).onSuccess {
                            _feedbackMessage.value = "Sharing updated: ${event.scope.badgeLabel}"
                        }.onFailure { err ->
                            _feedbackMessage.value = "Could not update sharing scope: ${err.localizedMessage ?: "check your connection"}"
                        }
                    } else {
                        studentProfileRepository.updateLocationPrivacyScope(event.scope)
                            .onSuccess {
                                _feedbackMessage.value = "Location privacy updated to ${event.scope.badgeLabel}."
                            }
                            .onFailure { err ->
                                _feedbackMessage.value = "Could not save location privacy to server: ${err.localizedMessage ?: "check your connection"}"
                            }
                    }
                }
            }
            is MapEvent.PublishLiveCoordinates -> {
                viewModelScope.launch {
                    val userId = currentStableUserId()
                    if (userId.isBlank()) return@launch
                    liveLocationRepository.publishLocationUpdate(
                        userId = userId,
                        displayName = currentUiDisplayName(),
                        avatarUrl = uiState.value.studentAvatarUrl,
                        latitude = event.latitude,
                        longitude = event.longitude,
                        accuracyMeters = event.accuracyMeters,
                        heading = event.heading,
                        scope = uiState.value.locationPrivacyScope
                    )
                }
            }
            is MapEvent.RedeemLocationShareToken -> {
                viewModelScope.launch {
                    val currentId = currentStableUserId().ifBlank { null }
                    liveLocationRepository.redeemShareToken(event.token, currentId)
                        .onSuccess { loc ->
                            _selectedFriendUserId.value = loc.userId
                            _focusedEventLocation.value = GeoCoordinate(loc.latitude, loc.longitude)
                            _navigationMode.value = MapNavigationMode.MAP
                            _feedbackMessage.value = "Viewing ${loc.displayName}'s live location."
                        }
                        .onFailure { err ->
                            _feedbackMessage.value = err.localizedMessage ?: "Share link has expired or been revoked."
                        }
                }
            }
            is MapEvent.SelectFriendLocation -> {
                _selectedFriendUserId.value = event.location?.userId
            }

            // AR & Mapbox Navigation handlers
            is MapEvent.UpdateOrientation -> {
                _deviceOrientation.value = event.orientation
            }
            is MapEvent.SetTiltSwitchPolicy -> {
                _tiltSwitchPolicy.value = event.policy
            }
            is MapEvent.ToggleArManualMode -> {
                val nextPolicy = when (uiState.value.arViewMode) {
                    ArViewMode.MAP_2D -> TiltSwitchPolicy.LOCKED_AR
                    ArViewMode.AR_CAMERA -> if (_deviceOrientation.value.pitchDegrees < 35f) {
                        TiltSwitchPolicy.AUTO_TILT
                    } else {
                        TiltSwitchPolicy.LOCKED_2D
                    }
                }
                _tiltSwitchPolicy.value = nextPolicy
            }
            is MapEvent.CycleTiltPolicy -> {
                val next = when (_tiltSwitchPolicy.value) {
                    TiltSwitchPolicy.AUTO_TILT -> TiltSwitchPolicy.LOCKED_AR
                    TiltSwitchPolicy.LOCKED_AR -> TiltSwitchPolicy.LOCKED_2D
                    TiltSwitchPolicy.LOCKED_2D -> TiltSwitchPolicy.AUTO_TILT
                }
                _tiltSwitchPolicy.value = next
            }
            is MapEvent.UpdateGeospatialMode -> {
                _geospatialMode.value = event.mode
            }
            is MapEvent.StartWalkingNavigation -> {
                viewModelScope.launch {
                    val origin = _userLocation.value
                        ?: liveLocationClient.getLastOrCurrentLocation()?.let {
                            GeoCoordinate(it.latitude, it.longitude).also { coord -> _userLocation.value = coord }
                        }

                    _selectedPlaceId.value = null
                    _selectedMeetId.value = null
                    _selectedFriendUserId.value = null
                    _navigationMode.value = MapNavigationMode.MAP
                    _isSearchBarActive.value = false
                    _focusedEventLocation.value = event.destination

                    if (origin != null) {
                        walkingNavigationController.startWalkingRoute(
                            origin = origin,
                            destination = event.destination,
                            destinationName = event.title,
                            destinationSubtitle = event.subtitle
                        )
                        if (event.switchToArImmediately) {
                            _tiltSwitchPolicy.value = TiltSwitchPolicy.LOCKED_AR
                        } else if (_tiltSwitchPolicy.value == TiltSwitchPolicy.LOCKED_2D) {
                            _tiltSwitchPolicy.value = TiltSwitchPolicy.AUTO_TILT
                        }
                        _feedbackMessage.value = "Walking route started to ${event.title}. Tilt up anytime for AR."
                    } else {
                        _feedbackMessage.value = "Mapped ${event.title}. Enable GPS to start walking guidance from your location."
                    }
                }
            }
            is MapEvent.StopWalkingNavigation -> {
                walkingNavigationController.stopNavigation()
                if (_tiltSwitchPolicy.value == TiltSwitchPolicy.LOCKED_AR) {
                    _tiltSwitchPolicy.value = TiltSwitchPolicy.AUTO_TILT
                }
            }

            // Events screen UI flow event handling
            is MapEvent.ShowEventsList -> {
                _navigationMode.value = MapNavigationMode.EVENTS_LIST
                viewModelScope.launch {
                    campusEventsRepository.syncRemoteEvents(currentStableUserId().ifBlank { null })
                }
            }
            is MapEvent.SelectEvent -> {
                _selectedEventId.value = event.event.id
                _navigationMode.value = MapNavigationMode.EVENT_DETAIL
            }
            is MapEvent.BackToEvents -> {
                _navigationMode.value = MapNavigationMode.EVENTS_LIST
            }
            is MapEvent.ShowMap -> {
                _navigationMode.value = MapNavigationMode.MAP
            }
            is MapEvent.ToggleRegisterEvent -> {
                viewModelScope.launch {
                    val userId = currentStableUserId()
                    if (userId.isBlank()) {
                        _feedbackMessage.value = "Sign in to RSVP for campus events."
                        return@launch
                    }
                    campusEventsRepository.toggleRsvp(event.eventId, currentUiDisplayName())
                        .onSuccess { (registered, _) ->
                            _feedbackMessage.value = if (registered) {
                                "RSVP confirmed! You're on the guest list."
                            } else {
                                "RSVP cancelled."
                            }
                        }
                        .onFailure { err ->
                            _feedbackMessage.value = err.localizedMessage ?: "Unable to update RSVP."
                        }
                }
            }
            is MapEvent.FocusEventLocationOnMap -> {
                _focusedEventLocation.value = GeoCoordinate(event.event.latitude, event.event.longitude)
                _navigationMode.value = MapNavigationMode.MAP
            }
            is MapEvent.FocusCoordinatesOnMap -> {
                _focusedEventLocation.value = GeoCoordinate(event.latitude, event.longitude)
                _navigationMode.value = MapNavigationMode.MAP
            }
            is MapEvent.RefreshMapData -> {
                viewModelScope.launch {
                    refreshAllRemoteData(currentStableUserId().ifBlank { null })
                }
            }
        }
    }

    private fun populateDefaultSuggestions() {
        val origin = _userLocation.value
        val nearbySuggestions = if (origin != null) {
            _latestAllPlaces.value
                .mapNotNull { place ->
                    val coord = GeoCoordinate(place.latitude, place.longitude)
                    val dist = GeoSpatialMath.haversineMeters(origin, coord)
                    if (dist <= 25_000.0) place to dist else null
                }
                .sortedBy { it.second }
                .take(5)
                .map { (place, dist) ->
                    SearchSuggestionItem(
                        id = "place_${place.id}",
                        name = place.name,
                        subtitle = "${place.campusRegion} · ${place.subtitle}",
                        distanceMeters = dist,
                        coordinate = GeoCoordinate(place.latitude, place.longitude),
                        isCampusPlace = true,
                        placeId = place.id
                    )
                }
        } else {
            emptyList()
        }
        _searchSuggestions.value = nearbySuggestions
    }

    private fun performUnifiedDestinationSearch(query: String) {
        searchDebounceJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            populateDefaultSuggestions()
            _isSearchingDestinations.value = false
            return
        }

        val origin = _userLocation.value
        val localMatches = _latestAllPlaces.value
            .filter { place ->
                place.name.contains(trimmed, ignoreCase = true) ||
                    place.subtitle.contains(trimmed, ignoreCase = true) ||
                    place.address.contains(trimmed, ignoreCase = true)
            }
            .take(4)
            .map { place ->
                val coord = GeoCoordinate(place.latitude, place.longitude)
                SearchSuggestionItem(
                    id = "place_${place.id}",
                    name = place.name,
                    subtitle = "${place.campusRegion} · ${place.subtitle}",
                    distanceMeters = origin?.let { GeoSpatialMath.haversineMeters(it, coord) },
                    coordinate = coord,
                    isCampusPlace = true,
                    placeId = place.id
                )
            }
        _searchSuggestions.value = localMatches

        if (trimmed.length < 2) {
            _isSearchingDestinations.value = false
            return
        }

        searchDebounceJob = viewModelScope.launch {
            delay(220L)
            _isSearchingDestinations.value = true
            val currentOrigin = _userLocation.value
                ?: liveLocationClient.getLastOrCurrentLocation()?.let {
                    GeoCoordinate(it.latitude, it.longitude).also { c -> _userLocation.value = c }
                }

            val googleResults = runCatching {
                liveLocationClient.searchLocations(trimmed, currentOrigin, maxResults = 8)
            }.getOrDefault(emptyList())

            val mapboxResults = mapboxSearchRepository.search(trimmed, currentOrigin)
                .getOrDefault(emptyList())

            val combined = (googleResults + mapboxResults + localMatches)
                .distinctBy { item -> "${item.name.trim().lowercase()}_${item.subtitle.trim().lowercase()}" }
                .sortedWith(compareBy(nullsLast()) { it.distanceMeters })
                .take(10)

            _searchSuggestions.value = combined
            _isSearchingDestinations.value = false
        }
    }

    private fun submitDirectDestinationSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val existingTop = _searchSuggestions.value.firstOrNull()
        if (existingTop != null) {
            handleSelectSuggestion(existingTop)
            return
        }
        viewModelScope.launch {
            _isSearchingDestinations.value = true
            val currentOrigin = _userLocation.value
                ?: liveLocationClient.getLastOrCurrentLocation()?.let {
                    GeoCoordinate(it.latitude, it.longitude).also { c -> _userLocation.value = c }
                }
            val googleResults = runCatching {
                liveLocationClient.searchLocations(trimmed, currentOrigin, maxResults = 5)
            }.getOrDefault(emptyList())
            val firstMatch = googleResults.firstOrNull()
                ?: mapboxSearchRepository.search(trimmed, currentOrigin).getOrNull()?.firstOrNull()
            _isSearchingDestinations.value = false

            if (firstMatch != null) {
                handleSelectSuggestion(firstMatch)
            } else {
                _feedbackMessage.value = "No location found for \"$trimmed\"."
            }
        }
    }

    private fun handleSelectSuggestion(item: SearchSuggestionItem) {
        viewModelScope.launch {
            _isSearchingDestinations.value = true
            val resolved = if (item.coordinate != null) {
                Result.success(item)
            } else {
                mapboxSearchRepository.selectSuggestion(item.id)
            }
            _isSearchingDestinations.value = false

            resolved.onSuccess { destItem ->
                val coord = destItem.coordinate ?: return@onSuccess
                _isSearchBarActive.value = false
                _searchSuggestions.value = emptyList()
                _searchQuery.value = destItem.name
                if (destItem.isCampusPlace && destItem.placeId != null) {
                    _selectedPlaceId.value = destItem.placeId
                }
                _focusedEventLocation.value = coord
                val origin = _userLocation.value
                    ?: liveLocationClient.getLastOrCurrentLocation()?.let {
                        GeoCoordinate(it.latitude, it.longitude).also { c -> _userLocation.value = c }
                    }

                if (origin != null) {
                    walkingNavigationController.startWalkingRoute(
                        origin = origin,
                        destination = coord,
                        destinationName = destItem.name,
                        destinationSubtitle = destItem.subtitle
                    )
                    _feedbackMessage.value = "Route ready to ${destItem.name}. Tilt upright for 3D AR."
                } else {
                    _feedbackMessage.value = "Mapped ${destItem.name}. Enable GPS to start walking route."
                }
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Could not resolve selected destination."
            }
        }
    }

    private fun currentStableUserId(): String = _currentProfile.value?.stableUserId().orEmpty()

    private fun currentUiDisplayName(): String {
        val current = _currentProfile.value ?: return ""
        return resolveCanonicalProfile(current, studentProfileRepository.knownProfiles.value)
            ?.fullName
            ?.takeIf(String::isRealProfileDisplayName)
            .orEmpty()
    }

    private fun CampusEventRecord.toUiEvent(): CampusEvent {
        return CampusEvent(
            id = id,
            creatorId = creatorId,
            title = title,
            hostName = hostName,
            hostAvatarRes = 0,
            artworkRes = artworkResForKey(artworkKey),
            artworkKey = artworkKey,
            bannerUrl = bannerUrl,
            dateTimeFormatted = formatEventDateTime(startTimeIso, startTimeEpochMs),
            startTimeIso = startTimeIso,
            startTimeEpochMs = startTimeEpochMs,
            locationName = locationName,
            fullAddress = fullAddress,
            statusBadge = if (isUserRegistered) EventBadgeType.GOING else null,
            priceLabel = priceLabel,
            maxCapacity = maxCapacity,
            isFeatured = false,
            featuredLabel = null,
            description = description,
            isUserRegistered = isUserRegistered,
            category = category,
            latitude = latitude,
            longitude = longitude,
            guestCount = attendeeCount,
            attendeeNames = attendeeNames,
            dateSection = if (isUserRegistered) "Your Events" else "Upcoming Events"
        )
    }

    companion object {
        private val LEGACY_HARDCODED_PLACE_IDS = setOf(
            "strat_quad_b",
            "main_library",
            "innovation_hub_auditorium",
            "student_guild_hub",
            "mombasa_marine_commons",
            "kisumu_lakeside_lab",
            "kisumu_lakeview_lab"
        )

        internal fun resolveCanonicalProfile(
            currentProfile: StudentProfile?,
            knownProfilesMap: Map<String, StudentProfile>
        ): StudentProfile? {
            currentProfile ?: return null
            val stableId = currentProfile.stableUserId()
            val canonicalKnownName = knownProfilesMap[stableId]?.fullName
                ?: knownProfilesMap[stableId.lowercase()]?.fullName
                ?: knownProfilesMap.values.firstOrNull {
                    stableId.isNotBlank() && it.stableUserId().equals(stableId, ignoreCase = true)
                }?.fullName
            val fallbackOwnName = currentProfile.fullName.takeIf(String::isRealProfileDisplayName).orEmpty()
            val bestName = canonicalKnownName?.takeIf(String::isRealProfileDisplayName) ?: fallbackOwnName
            return currentProfile.copy(fullName = bestName)
        }

        internal fun buildAcceptedCampusFriends(
            acceptedEntities: List<FriendEntity>,
            knownProfiles: Map<String, StudentProfile>,
            liveLocations: List<FriendLiveLocation>,
            nowMs: Long = System.currentTimeMillis()
        ): List<CampusFriend> {
            return acceptedEntities
                .asSequence()
                .filter { it.status.equals(FriendStatus.FRIENDS.name, ignoreCase = true) }
                .distinctBy { it.friendUserId.trim().lowercase() }
                .mapNotNull { entity ->
                    val friendId = entity.friendUserId.trim()
                    if (friendId.isBlank()) return@mapNotNull null
                    val known = knownProfiles[friendId]
                        ?: knownProfiles[friendId.lowercase()]
                        ?: knownProfiles.values.firstOrNull {
                            it.stableUserId().equals(friendId, ignoreCase = true)
                        }
                    val displayName = known?.fullName?.takeIf(String::isRealProfileDisplayName)
                        ?: entity.friendDisplayName.takeIf(String::isRealProfileDisplayName)
                        ?: return@mapNotNull null

                    val handle = entity.friendHandle.takeIf { it.isNotBlank() }
                        ?: known?.universityEmail?.takeIf { it.contains("@") }?.let { "@" + it.substringBefore("@") }
                        ?: ("@" + displayName.lowercase().replace(" ", ""))
                    val faculty = known?.degreeProgram?.ifBlank { known.universityName }
                        ?.ifBlank { "Campus Friend" }
                        ?: "Campus Friend"
                    val isLiveNow = liveLocations.any { loc ->
                        loc.userId.equals(friendId, ignoreCase = true) &&
                            (loc.updatedAtEpochMs == 0L || nowMs - loc.updatedAtEpochMs <= LiveLocationRepository.STALE_LOCATION_MS)
                    }
                    val initials = displayName.trim().split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.first().uppercase() }
                        .ifBlank { displayName.take(2).uppercase() }

                    CampusFriend(
                        id = friendId,
                        name = displayName,
                        handle = handle,
                        faculty = faculty,
                        avatarInitials = initials,
                        avatarBg = avatarColors[kotlin.math.abs(displayName.hashCode()) % avatarColors.size],
                        isOnline = isLiveNow
                    )
                }
                .toList()
        }

        internal fun filterPlaces(
            allPlaces: List<CampusPlace>,
            filters: FilterState,
            userLoc: GeoCoordinate?,
            savedIds: Set<String>,
            beenIds: Set<String>,
            friendSavedIds: Set<String>
        ): List<CampusPlace> {
            return allPlaces.filter { place ->
                val matchesCategory = filters.selectedCategory == PlaceCategory.TOP_PICKS ||
                    place.category == filters.selectedCategory
                val matchesRegion = filters.selectedRegion == "All Hubs" ||
                    place.campusRegion.equals(filters.selectedRegion, ignoreCase = true)
                val matchesOpenNow = !filters.isOpenNowOnly || place.isOpenNow
                val matchesAudience = when (filters.audienceFilter) {
                    AudienceFilter.EVERYONE -> true
                    AudienceFilter.FRIENDS -> place.id in friendSavedIds || place.id in savedIds
                    AudienceFilter.FOLLOWING -> place.id in friendSavedIds || place.id in savedIds || place.id in beenIds
                }
                val matchesQuery = filters.searchQuery.isBlank() ||
                    place.name.contains(filters.searchQuery, ignoreCase = true) ||
                    place.subtitle.contains(filters.searchQuery, ignoreCase = true) ||
                    place.address.contains(filters.searchQuery, ignoreCase = true) ||
                    place.campusRegion.contains(filters.searchQuery, ignoreCase = true)
                matchesCategory && matchesRegion && matchesOpenNow && matchesAudience && matchesQuery
            }.let { list ->
                if (userLoc != null) {
                    list.sortedBy { place ->
                        haversineDistance(userLoc.latitude, userLoc.longitude, place.latitude, place.longitude)
                    }
                } else {
                    list
                }
            }
        }

        internal fun filterMeets(
            allMeets: List<FlashMeet>,
            filters: FilterState,
            stableCurrentId: String,
            friendIdSet: Set<String>,
            joinedMeetIds: Set<String>
        ): List<FlashMeet> {
            return allMeets.filter { meet ->
                val matchesCategory = filters.selectedMeetCategory == null || meet.category == filters.selectedMeetCategory
                val creatorLower = meet.creatorUserId.trim().lowercase()
                val matchesAudience = when (filters.audienceFilter) {
                    AudienceFilter.EVERYONE -> true
                    AudienceFilter.FRIENDS,
                    AudienceFilter.FOLLOWING -> {
                        (stableCurrentId.isNotBlank() && meet.creatorUserId.equals(stableCurrentId, ignoreCase = true)) ||
                            creatorLower in friendIdSet ||
                            meet.id in joinedMeetIds
                    }
                }
                matchesCategory && matchesAudience
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
}
