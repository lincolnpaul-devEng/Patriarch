package com.campus.network.feature.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.network.core.data.CampusHubRepository
import com.campus.network.core.data.SafeTradeEscrowService
import com.campus.network.core.data.StudentProfileRepository
import com.campus.network.core.model.EscrowOrder
import com.campus.network.core.model.EscrowOrderStatus
import com.campus.network.core.model.GigUrgency
import com.campus.network.core.model.MarketItem
import com.campus.network.core.model.MicroGig
import com.campus.network.core.model.PaymentMethodOption
import com.campus.network.core.model.PaymentResult
import com.campus.network.core.model.StudentProfile
import com.campus.network.core.model.stableUserId
import com.campus.network.feature.hub.components.ProductBasketConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    val filterCategory: String? = null,
    val searchQuery: String = "",
    val aiSuggestions: List<String> = emptyList(),
    val isAiLoading: Boolean = false,
    val studentProfile: StudentProfile? = null,
    val isPaymentProviderReady: Boolean = false,
    val paymentProviderName: String = "Payment provider not connected",
    val paymentMethods: List<PaymentMethodOption> = emptyList()
)

sealed interface HubEvent {
    data class SwitchViewMode(val mode: HubViewMode) : HubEvent
    data class FilterCategory(val category: String?) : HubEvent
    data class TogglePostDialog(val open: Boolean) : HubEvent
    data class UpdateSearchQuery(val query: String) : HubEvent
    data class SelectAiSuggestion(val suggestion: String) : HubEvent
    data object ClearSearch : HubEvent
    data object ClearFeedback : HubEvent
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
        val qrReleaseCode: String = "",
        val imageUrl: String? = null,
        val imageUrls: List<String> = emptyList(),
        val itemId: String = "item_${java.util.UUID.randomUUID()}"
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
    private val studentProfileRepository: StudentProfileRepository = StudentProfileRepository(),
    private val escrowService: SafeTradeEscrowService = SafeTradeEscrowService()
) : ViewModel() {

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage

    private val _viewMode = MutableStateFlow(HubViewMode.MARKETPLACE_GRID)
    private val _filterCategory = MutableStateFlow<String?>(null)
    private val _isPostDialogOpen = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _aiSuggestions = MutableStateFlow<List<String>>(emptyList())
    private val _isAiLoading = MutableStateFlow(false)

    private var aiSearchJob: Job? = null

    /** Active escrow orders keyed by market item id (in-session). */
    private val escrowOrdersByItemId = mutableMapOf<String, EscrowOrder>()

    init {
        viewModelScope.launch {
            val viewerId = studentProfileRepository.getProfile().first().stableUserId()
            repository.syncRemoteMarketItems(currentUserId = viewerId.ifBlank { null })
            repository.syncRemoteMicroGigs()
        }
    }

    fun isPaymentProviderReady(): Boolean = escrowService.isPaymentReady()

    fun paymentProviderName(): String = escrowService.providerDisplayName()

    fun paymentMethods(): List<PaymentMethodOption> = escrowService.availablePaymentMethods()

    val uiState: StateFlow<HubUiState> = combine(
        _viewMode,
        repository.getMarketplaceItems(),
        repository.getMicroGigs(),
        _filterCategory,
        _isPostDialogOpen
    ) { mode, items, gigs, category, isPostOpen ->
        CombinedBaseData(mode, items, gigs, category, isPostOpen)
    }.let { baseFlow ->
        val profileAndKnownProfiles = combine(
            studentProfileRepository.getProfile(),
            studentProfileRepository.knownProfiles
        ) { profile, knownProfiles -> profile to knownProfiles }
        combine(
            baseFlow,
            _searchQuery,
            _aiSuggestions,
            _isAiLoading,
            profileAndKnownProfiles
        ) { base, query, suggestions, aiLoading, profileAndKnown ->
            val (profile, knownProfiles) = profileAndKnown
            val stableId = profile.stableUserId()
            val canonicalName = knownProfiles[stableId]?.fullName
                ?: knownProfiles.values.firstOrNull {
                    it.stableUserId().equals(stableId, ignoreCase = true)
                }?.fullName
            val canonicalProfile = profile.copy(fullName = canonicalName.orEmpty())
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
                isAiLoading = aiLoading,
                studentProfile = canonicalProfile,
                isPaymentProviderReady = escrowService.isPaymentReady(),
                paymentProviderName = escrowService.providerDisplayName(),
                paymentMethods = escrowService.availablePaymentMethods()
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HubUiState(
            isPaymentProviderReady = escrowService.isPaymentReady(),
            paymentProviderName = escrowService.providerDisplayName(),
            paymentMethods = escrowService.availablePaymentMethods()
        )
    )

    suspend fun verifyReleaseCode(itemId: String, scannedCode: String): Boolean {
        return repository.verifyReleaseCode(itemId, scannedCode)
    }

    /**
     * Locks buyer funds via the pluggable [SafeTradeEscrowService] payment socket.
     * Returns failure when no provider is configured — never fakes a successful hold.
     */
    suspend fun lockEscrowFunds(
        basket: ProductBasketConfig,
        paymentMethodId: String,
        buyerPhoneE164: String? = null
    ): PaymentResult<ProductBasketConfig> {
        val buyerId = uiState.value.studentProfile?.stableUserId().orEmpty()
        val result = escrowService.lockFundsInEscrow(
            SafeTradeEscrowService.LockEscrowParams(
                item = basket.item,
                buyerUserId = buyerId,
                amount = basket.totalAmount,
                currencyCode = basket.item.currency,
                orderReference = basket.orderReference,
                paymentMethodId = paymentMethodId,
                buyerPhoneE164 = buyerPhoneE164,
                handoverTitle = basket.handoverOptionTitle,
                packageTitle = basket.packageOptionTitle,
                buyerNote = basket.buyerNote,
                quantity = basket.quantity
            )
        )
        return when (result) {
            is PaymentResult.Failure -> result
            is PaymentResult.Success -> {
                val order = result.value.order
                escrowOrdersByItemId[basket.item.id] = order
                val funded = order.status == EscrowOrderStatus.FUNDS_HELD ||
                    order.status == EscrowOrderStatus.PENDING_PAYMENT
                PaymentResult.Success(
                    basket.copy(
                        isOrderConfirmedInEscrow = funded && order.providerHoldId != null,
                        escrowOrderId = order.id,
                        providerHoldId = order.providerHoldId,
                        paymentMethodId = paymentMethodId,
                        escrowStatus = order.status.name
                    )
                )
            }
        }
    }

    /**
     * Releases held funds to the seller after QR verification.
     */
    suspend fun releaseEscrowFunds(itemId: String, releaseCodeVerified: Boolean): PaymentResult<EscrowOrder> {
        val order = escrowOrdersByItemId[itemId]
            ?: return PaymentResult.Failure(
                com.campus.network.core.model.PaymentErrorCode.INVALID_REQUEST,
                "No active escrow order for this listing on this device"
            )
        return when (val released = escrowService.releaseFundsToSeller(order, releaseCodeVerified)) {
            is PaymentResult.Failure -> released
            is PaymentResult.Success -> {
                escrowOrdersByItemId[itemId] = released.value
                released
            }
        }
    }

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
            HubEvent.ClearFeedback -> _feedbackMessage.value = null
            is HubEvent.PostItem -> {
                viewModelScope.launch {
                    val profile = uiState.value.studentProfile
                    val sellerUserId = profile?.stableUserId().orEmpty()
                    if (sellerUserId.isBlank()) {
                        _feedbackMessage.value = "A Supabase profile is required to publish a listing."
                        return@launch
                    }
                    try {
                        repository.postMarketItem(
                            title = event.title,
                            price = event.price,
                            category = event.category,
                            condition = event.condition,
                            sellerUserId = sellerUserId,
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
                            imageUrls = event.imageUrls,
                            itemId = event.itemId
                        )
                        _isPostDialogOpen.value = false
                        _feedbackMessage.value = "Listing published."
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        _feedbackMessage.value = error.message ?: "Unable to publish listing."
                    }
                }
            }
            is HubEvent.PostGig -> {
                viewModelScope.launch {
                    val profile = uiState.value.studentProfile
                    val posterUserId = profile?.stableUserId().orEmpty()
                    if (posterUserId.isBlank()) {
                        _feedbackMessage.value = "A Supabase profile is required to publish a gig."
                        return@launch
                    }
                    try {
                        repository.postMicroGig(
                            title = event.title,
                            description = event.description,
                            payoutAmount = event.payout,
                            estimatedDuration = event.duration,
                            posterUserId = posterUserId,
                            tag = event.tag,
                            urgency = event.urgency
                        )
                        _isPostDialogOpen.value = false
                        _feedbackMessage.value = "Gig published."
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        _feedbackMessage.value = error.message ?: "Unable to publish gig."
                    }
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
