package com.campus.network.feature.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.network.core.data.CampusHubRepository
import com.campus.network.core.model.GigUrgency
import com.campus.network.core.model.MarketItem
import com.campus.network.core.model.MicroGig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.campus.network.core.data.StudentProfileRepository
import com.campus.network.core.model.StudentProfile

enum class HubViewMode {
    MARKETPLACE_GRID,
    MICRO_GIG_BOARD
}

data class HubUiState(
    val activeViewMode: HubViewMode = HubViewMode.MARKETPLACE_GRID,
    val marketplaceItems: List<MarketItem> = emptyList(),
    val microGigs: List<MicroGig> = emptyList(),
    val isPostDialogOpen: Boolean = false,
    val filterCategory: String? = null,
    val searchQuery: String = "",
    val aiSuggestions: List<String> = emptyList(),
    val isAiLoading: Boolean = false,
    val studentProfile: StudentProfile? = null
)

sealed interface HubEvent {
    data class SwitchViewMode(val mode: HubViewMode) : HubEvent
    data class FilterCategory(val category: String?) : HubEvent
    data class TogglePostDialog(val open: Boolean) : HubEvent
    data class UpdateSearchQuery(val query: String) : HubEvent
    data class SelectAiSuggestion(val suggestion: String) : HubEvent
    data object ClearSearch : HubEvent
    data class PostItem(
        val title: String,
        val price: Double,
        val category: String,
        val condition: String,
        val location: String,
        val description: String = "",
        val pickupSpotName: String = "Quad Library Ground Floor Lockers",
        val pickupSecurityLevel: String = "CCTV Monitored Safe Zone",
        val pickupLatitude: Double = -1.3090,
        val pickupLongitude: Double = 36.8120,
        val pickupTiming: String = "Immediate (within 1 hr)",
        val payoutMethod: String = "Campus SafePay Escrow",
        val requiresQrReleaseCode: Boolean = true,
        val qrReleaseCode: String = "CP-849201",
        val imageUrl: String? = null,
        val imageUrls: List<String> = emptyList()
    ) : HubEvent
    data class PostGig(
        val title: String,
        val description: String,
        val payout: Double,
        val duration: String,
        val tag: String,
        val urgency: GigUrgency
    ) : HubEvent
}

class HubViewModel(
    private val repository: CampusHubRepository,
    private val studentProfileRepository: StudentProfileRepository = StudentProfileRepository()
) : ViewModel() {

    private val _viewMode = MutableStateFlow(HubViewMode.MARKETPLACE_GRID)
    private val _filterCategory = MutableStateFlow<String?>(null)
    private val _isPostDialogOpen = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _aiSuggestions = MutableStateFlow<List<String>>(emptyList())
    private val _isAiLoading = MutableStateFlow(false)

    private var aiSearchJob: Job? = null

    val uiState: StateFlow<HubUiState> = combine(
        _viewMode,
        repository.getMarketplaceItems(),
        repository.getMicroGigs(),
        _filterCategory,
        _isPostDialogOpen
    ) { mode, items, gigs, category, isPostOpen ->
        CombinedBaseData(mode, items, gigs, category, isPostOpen)
    }.let { baseFlow ->
        combine(
            baseFlow,
            _searchQuery,
            _aiSuggestions,
            _isAiLoading,
            studentProfileRepository.getProfile()
        ) { base, query, suggestions, aiLoading, profile ->
            val categoryFilteredItems = if (base.category == null || base.category == "All") {
                base.items
            } else {
                base.items.filter { it.category.equals(base.category, ignoreCase = true) }
            }

            val searchFilteredItems = if (query.isBlank()) {
                categoryFilteredItems
            } else {
                categoryFilteredItems.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true) ||
                    it.campusHallLocation.contains(query, ignoreCase = true)
                }
            }

            val searchFilteredGigs = if (query.isBlank()) {
                base.gigs
            } else {
                base.gigs.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.tag.contains(query, ignoreCase = true)
                }
            }

            val prefs = profile.onboardingPreferences
            val favCats = prefs.favoriteCategories.map { it.lowercase() }
            val priorities = prefs.priorities
            val preferredCurrency = prefs.currencyCode.ifBlank { "KES" }

            val personalizedItems = searchFilteredItems
                .map { item ->
                    if (preferredCurrency != "KES" && item.currency == "KES") {
                        item.copy(currency = preferredCurrency)
                    } else {
                        item
                    }
                }
                .sortedWith(
                    compareByDescending<MarketItem> { item ->
                        val catLower = item.category.lowercase()
                        val titleLower = item.title.lowercase()
                        favCats.any { fav -> catLower.contains(fav) || fav.contains(catLower) || titleLower.contains(fav) }
                    }.thenByDescending { item ->
                        if ("verified_student_seller" in priorities) item.isCampusVerifiedSeller else false
                    }.thenBy { item ->
                        if ("reasonable_price" in priorities) item.price else 0.0
                    }
                )

            val personalizedGigs = searchFilteredGigs
                .map { gig ->
                    if (preferredCurrency != "KES" && gig.currency == "KES") {
                        gig.copy(currency = preferredCurrency)
                    } else {
                        gig
                    }
                }
                .sortedByDescending { gig ->
                    val tagLower = gig.tag.lowercase()
                    favCats.any { fav -> tagLower.contains(fav) || fav.contains(tagLower) }
                }

            val autoViewMode = if (
                base.mode == HubViewMode.MARKETPLACE_GRID &&
                "micro_gigs" in prefs.intents &&
                "buy_sell_marketplace" !in prefs.intents
            ) {
                HubViewMode.MICRO_GIG_BOARD
            } else {
                base.mode
            }

            HubUiState(
                activeViewMode = autoViewMode,
                marketplaceItems = personalizedItems,
                microGigs = personalizedGigs,
                isPostDialogOpen = base.isPostOpen,
                filterCategory = base.category,
                searchQuery = query,
                aiSuggestions = suggestions,
                aiLoading,
                studentProfile = profile
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HubUiState()
    )

    fun onEvent(event: HubEvent) {
        when (event) {
            is HubEvent.SwitchViewMode -> _viewMode.value = event.mode
            is HubEvent.FilterCategory -> _filterCategory.value = event.category
            is HubEvent.TogglePostDialog -> _isPostDialogOpen.value = event.open
            is HubEvent.UpdateSearchQuery -> {
                _searchQuery.value = event.query
                triggerAiSuggestions(event.query)
            }
            is HubEvent.SelectAiSuggestion -> {
                _searchQuery.value = event.suggestion
                _aiSuggestions.value = emptyList()
            }
            is HubEvent.ClearSearch -> {
                _searchQuery.value = ""
                _aiSuggestions.value = emptyList()
            }
            is HubEvent.PostItem -> {
                viewModelScope.launch {
                    val profile = uiState.value.studentProfile
                    val authorName = profile?.fullName?.ifBlank { "Lincoln Kiprotich" } ?: "Lincoln Kiprotich"
                    repository.postMarketItem(
                        title = event.title,
                        price = event.price,
                        category = event.category,
                        condition = event.condition,
                        sellerName = "$authorName (Verified Student)",
                        location = event.location,
                        description = event.description,
                        pickupSpotName = event.pickupSpotName,
                        pickupSecurityLevel = event.pickupSecurityLevel,
                        pickupLatitude = event.pickupLatitude,
                        pickupLongitude = event.pickupLongitude,
                        pickupTiming = event.pickupTiming,
                        payoutMethod = event.payoutMethod,
                        requiresQrReleaseCode = event.requiresQrReleaseCode,
                        qrReleaseCode = event.qrReleaseCode,
                        imageUrl = event.imageUrl,
                        imageUrls = event.imageUrls
                    )
                    _isPostDialogOpen.value = false
                }
            }
            is HubEvent.PostGig -> {
                viewModelScope.launch {
                    val profile = uiState.value.studentProfile
                    val authorName = profile?.fullName?.ifBlank { "Lincoln Kiprotich" } ?: "Lincoln Kiprotich"
                    repository.postMicroGig(
                        title = event.title,
                        description = event.description,
                        payoutAmount = event.payout,
                        estimatedDuration = event.duration,
                        posterName = "$authorName (Verified Student)",
                        tag = event.tag,
                        urgency = event.urgency
                    )
                    _isPostDialogOpen.value = false
                }
            }
        }
    }

    private fun triggerAiSuggestions(query: String) {
        aiSearchJob?.cancel()
        if (query.isBlank()) {
            _aiSuggestions.value = emptyList()
            _isAiLoading.value = false
            return
        }

        aiSearchJob = viewModelScope.launch {
            _isAiLoading.value = true
            delay(350L)
            try {
                val suggestions = repository.getAiSearchSuggestions(query)
                _aiSuggestions.value = suggestions
            } catch (e: Exception) {
                _aiSuggestions.value = emptyList()
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    private data class CombinedBaseData(
        val mode: HubViewMode,
        val items: List<MarketItem>,
        val gigs: List<MicroGig>,
        val category: String?,
        val isPostOpen: Boolean
    )
}
