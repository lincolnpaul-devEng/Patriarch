package com.campus.network.feature.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.network.core.data.CampusHubRepository
import com.campus.network.core.model.GigUrgency
import com.campus.network.core.model.MarketItem
import com.campus.network.core.model.MicroGig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HubViewMode {
    MARKETPLACE_GRID,
    MICRO_GIG_BOARD
}

data class HubUiState(
    val activeViewMode: HubViewMode = HubViewMode.MARKETPLACE_GRID,
    val marketplaceItems: List<MarketItem> = emptyList(),
    val microGigs: List<MicroGig> = emptyList(),
    val isPostDialogOpen: Boolean = false,
    val filterCategory: String? = null
)

sealed interface HubEvent {
    data class SwitchViewMode(val mode: HubViewMode) : HubEvent
    data class FilterCategory(val category: String?) : HubEvent
    data class TogglePostDialog(val open: Boolean) : HubEvent
    data class PostItem(
        val title: String,
        val price: Double,
        val category: String,
        val condition: String,
        val location: String
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
    private val repository: CampusHubRepository
) : ViewModel() {

    private val _viewMode = MutableStateFlow(HubViewMode.MARKETPLACE_GRID)
    private val _filterCategory = MutableStateFlow<String?>(null)
    private val _isPostDialogOpen = MutableStateFlow(false)

    val uiState: StateFlow<HubUiState> = combine(
        _viewMode,
        repository.getMarketplaceItems(),
        repository.getMicroGigs(),
        _filterCategory,
        _isPostDialogOpen
    ) { mode, items, gigs, category, isPostOpen ->
        val filteredItems = if (category == null) items else items.filter { it.category == category }
        HubUiState(
            activeViewMode = mode,
            marketplaceItems = filteredItems,
            microGigs = gigs,
            isPostDialogOpen = isPostOpen,
            filterCategory = category
        )
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
            is HubEvent.PostItem -> {
                viewModelScope.launch {
                    repository.postMarketItem(
                        title = event.title,
                        price = event.price,
                        category = event.category,
                        condition = event.condition,
                        sellerName = "Lincoln (Verified Student)",
                        location = event.location
                    )
                    _isPostDialogOpen.value = false
                }
            }
            is HubEvent.PostGig -> {
                viewModelScope.launch {
                    repository.postMicroGig(
                        title = event.title,
                        description = event.description,
                        payoutAmount = event.payout,
                        estimatedDuration = event.duration,
                        posterName = "Lincoln (Verified Student)",
                        tag = event.tag,
                        urgency = event.urgency
                    )
                    _isPostDialogOpen.value = false
                }
            }
        }
    }
}
