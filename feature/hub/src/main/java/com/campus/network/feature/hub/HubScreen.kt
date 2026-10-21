package com.campus.network.feature.hub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import com.campus.network.core.ui.components.CampusStudentAvatar
import kotlinx.coroutines.delay
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campus.network.core.model.GigUrgency
import com.campus.network.core.model.MarketItem
import com.campus.network.core.model.MicroGig
import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.ui.components.CampusInlineNetworkAlertCard
import com.campus.network.core.ui.components.CampusNoInternetEmptyState
import com.campus.network.core.ui.components.MarketProductHeroCardSkeleton
import com.campus.network.core.ui.components.MarketProductListItemSkeleton
import com.campus.network.core.ui.components.ShimmerAsyncImage
import com.campus.network.core.ui.components.StreamCogLoaderOverlay
import com.campus.network.core.ui.components.rememberCampusNetworkState
import com.campus.network.feature.hub.components.BasketEscrowCheckoutSheet
import com.campus.network.core.model.stableUserId
import com.campus.network.feature.hub.components.MarketProductDetailsFullScreen
import com.campus.network.feature.hub.components.PostProductDrawerSheet
import com.campus.network.feature.hub.components.ProductBasketConfig
import com.campus.network.feature.hub.components.ProductOptionsBasketDrawerSheet
import com.campus.network.feature.hub.components.SafeTradeEscrowBuyerSheet
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch

// Exact visual styling from the reference screen
private val BrandBluePrimary = Color(0xFF014784)
private val BrandBlueDeep = Color(0xFF0A335C)
private val HeroLocationDark = Color(0xB812151B)
private val AIBarBackground = Color(0xF2FFFFFF)
private val DarkCardTextPrimary = Color(0xFF0F172A)

@Composable
fun HubScreen(
    viewModel: HubViewModel,
    onNavigateToChat: (peerId: String, peerName: String) -> Unit = { _, _ -> },
    onMessageSeller: ((sellerId: String, sellerName: String, initialOrderMessage: String?) -> Unit)? = null,
    onNavigateToLiveFeed: (String) -> Unit = {},
    onNavigateToGoLive: () -> Unit = {},
    onNavigateToMap: ((Double, Double, String) -> Unit)? = null,
    onNavigateToNotifications: () -> Unit = {},
    onToggleBottomBar: ((Boolean) -> Unit)? = null,
    unreadNotificationsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    androidx.compose.runtime.LaunchedEffect(feedbackMessage) {
        val message = feedbackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.onEvent(HubEvent.ClearFeedback)
    }

    var selectedCategory by remember { mutableStateOf("All") }
    var activeListingType by remember { mutableStateOf("All") }
    var isPromotionsFilterActive by remember { mutableStateOf(false) }
    var isQuickWalkActive by remember { mutableStateOf(false) }
    var isFreePickupActive by remember { mutableStateOf(false) }
    var isFullCardView by remember { mutableStateOf(true) } // true for Full View, false for List View (Image 2)
    var sortOption by remember { mutableStateOf("Top Rated") }
    var savedListingIds by remember { mutableStateOf(setOf<String>()) }
    var isPostDialogOpen by remember { mutableStateOf(false) }
    var isFilterDrawerOpen by remember { mutableStateOf(false) }
    var selectedItemForDetails by remember { mutableStateOf<MarketItem?>(null) }
    var isOptionsSheetOpen by remember { mutableStateOf(false) }
    var isCheckoutSheetOpen by remember { mutableStateOf(false) }
    var basketConfigsByItemId by remember { mutableStateOf<Map<String, ProductBasketConfig>>(emptyMap()) }
    var selectedItemForEscrowVerification by remember { mutableStateOf<MarketItem?>(null) }

    BackHandler(enabled = selectedItemForDetails != null) {
        selectedItemForDetails = null
    }

    androidx.compose.runtime.LaunchedEffect(
        isPostDialogOpen,
        selectedItemForDetails,
        isOptionsSheetOpen,
        isCheckoutSheetOpen,
        selectedItemForEscrowVerification,
        isFilterDrawerOpen
    ) {
        val anySheetOpen = isPostDialogOpen ||
            selectedItemForDetails != null ||
            isOptionsSheetOpen ||
            isCheckoutSheetOpen ||
            selectedItemForEscrowVerification != null ||
            isFilterDrawerOpen
        onToggleBottomBar?.invoke(!anySheetOpen)
    }

    // Advanced Drawer Filter States (Mirroring Image 1)
    var selectedWalkTimeFilter by remember { mutableStateOf("Any") }
    var selectedPriceFilter by remember { mutableStateOf("Any") }
    var selectedRatingFilter by remember { mutableStateOf("Any") }

    // Real-time marketplace items and gigs filtering
    val categoryFilteredItems = if (selectedCategory == "All" || selectedCategory == "Promotions") {
        uiState.marketplaceItems
    } else {
        uiState.marketplaceItems.filter { it.category.contains(selectedCategory, ignoreCase = true) }
    }

    val categoryFilteredGigs = if (selectedCategory == "All" || selectedCategory == "Promotions" || selectedCategory == "Gigs" || selectedCategory == "Tutoring") {
        uiState.microGigs
    } else {
        emptyList()
    }

    val showProducts = activeListingType == "All" || activeListingType == "Products"
    val showGigs = activeListingType == "All" || activeListingType == "Campus Gigs"

    var displayItems = if (showProducts) categoryFilteredItems else emptyList()
    var displayGigs = if (showGigs) categoryFilteredGigs else emptyList()

    if (isFreePickupActive) {
        displayItems = displayItems.filter { it.price < 1500 }
    }

    // Apply Price Filter from Drawer
    if (selectedPriceFilter == "Under KES 500") {
        displayItems = displayItems.filter { it.price < 500 }
        displayGigs = displayGigs.filter { it.payoutAmount < 500 }
    } else if (selectedPriceFilter == "KES 500-1,500") {
        displayItems = displayItems.filter { it.price in 500.0..1500.0 }
        displayGigs = displayGigs.filter { it.payoutAmount in 500.0..1500.0 }
    } else if (selectedPriceFilter == "KES 1,500+") {
        displayItems = displayItems.filter { it.price > 1500 }
        displayGigs = displayGigs.filter { it.payoutAmount > 1500 }
    }

    // Apply Rating Filter from Drawer
    if (selectedRatingFilter == "4.0 and up") {
        displayItems = displayItems.filter { it.sellerRating >= 4.0 }
        displayGigs = displayGigs.filter { (it.posterTrustScore / 20.0) >= 4.0 }
    } else if (selectedRatingFilter == "4.5 and up") {
        displayItems = displayItems.filter { it.sellerRating >= 4.5 }
        displayGigs = displayGigs.filter { (it.posterTrustScore / 20.0) >= 4.5 }
    } else if (selectedRatingFilter == "4.8 and up") {
        displayItems = displayItems.filter { it.sellerRating >= 4.8 }
        displayGigs = displayGigs.filter { (it.posterTrustScore / 20.0) >= 4.8 }
    }

    // Apply Walk Time filter from Drawer or Quick filter
    if (isQuickWalkActive || selectedWalkTimeFilter == "Under 15 min") {
        displayItems = displayItems.filter { !it.campusHallLocation.contains("Off-Campus", ignoreCase = true) }
    }

    val networkState = rememberCampusNetworkState()
    var isProductsLoading by remember { mutableStateOf(true) }

    LaunchedEffect(
        selectedCategory,
        selectedPriceFilter,
        selectedRatingFilter,
        selectedWalkTimeFilter,
        isPromotionsFilterActive,
        isQuickWalkActive,
        isFreePickupActive,
        networkState.isSlowNetwork
    ) {
        isProductsLoading = true
        val skeletonDuration = if (networkState.isSlowNetwork) 1350L else 700L
        delay(skeletonDuration)
        isProductsLoading = false
    }

    val rawCount = displayItems.size + displayGigs.size
    val totalListingCount = rawCount

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FB))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // -------------------------------------------------------------
            // 1. Scenic Hero Section with Transparent Logo & Action Buttons
            // -------------------------------------------------------------
            item {
                HeroScenicHeader(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.onEvent(HubEvent.UpdateSearchQuery(it)) },
                    aiSuggestions = uiState.aiSuggestions,
                    onSelectSuggestion = { viewModel.onEvent(HubEvent.SelectAiSuggestion(it)) },
                    isAiLoading = uiState.isAiLoading,
                    onLogoClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Campus Network: Premium Student Session Active")
                        }
                    },
                    onChatClick = {
                        onNavigateToChat("general", "Campus Chat")
                    },
                    onNotificationClick = {
                        onNavigateToNotifications()
                    },
                    unreadNotificationsCount = unreadNotificationsCount,
                    onLocationClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Current Campus Sector: North Quad Academic Commons")
                        }
                    }
                )
            }

            // Search Filter Active Pill + Inline Offline Search Alert (Pattern C)
            if (uiState.searchQuery.isNotEmpty()) {
                item {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = BrandBluePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Filtered by \"${uiState.searchQuery}\"",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandBluePrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEE2E2),
                                modifier = Modifier.clickable { viewModel.onEvent(HubEvent.ClearSearch) }
                            ) {
                                Text(
                                    text = "Clear",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                if (!networkState.isOnline) {
                    item {
                        CampusInlineNetworkAlertCard(
                            featureName = "Search",
                            description = "We are currently unable to reach live campus search servers while your device is offline. Please check your connection or message support.",
                            actionLabel = "Message us 24/7",
                            onActionClick = { networkState.triggerRetry() },
                            onSecondarySupportClick = { onNavigateToChat("campus_support", "Campus Support") }
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 2. Member Status / Loyalty Card (Deep Blue Gradient)
            // -------------------------------------------------------------
            item {
                val profile = uiState.studentProfile
                val displayMemberName = profile?.fullName?.takeIf(String::isRealProfileDisplayName)
                    .orEmpty()
                val displayMaskedId = profile?.studentId?.takeLast(4)?.takeIf { it.isNotBlank() }?.let { "•••• $it" } ?: "•••• 0000"
                val displayMeritPoints = "${profile?.campusMeritPoints ?: 0} pts"
                val displayTrustScore = "${profile?.behavioralTrustScore ?: 50} / 100"

                MemberLoyaltyCard(
                    memberName = displayMemberName,
                    memberIdMasked = displayMaskedId,
                    meritPoints = displayMeritPoints,
                    trustScore = displayTrustScore,
                    onCardClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Patriarch Honor Status: Campus Verified Scholar")
                        }
                    }
                )
            }

            // -------------------------------------------------------------
            // 3. Featured Stories Carousel (Horizontal Cards)
            // -------------------------------------------------------------
            item {
                Spacer(modifier = Modifier.height(18.dp))
                FeaturedStoriesCarousel(
                    onStoryClick = { storyId ->
                        if (storyId == "create_stream") {
                            onNavigateToGoLive()
                        } else {
                            onNavigateToLiveFeed(storyId)
                        }
                    }
                )
            }

            // -------------------------------------------------------------
            // 4. Campus Micro-Commerce & Gigs Deck (Senior Redesign)
            // -------------------------------------------------------------
            // 3D Squircle Category Dock Carousel
            item {
                Spacer(modifier = Modifier.height(24.dp))
                CampusCategoryDock(
                    selectedCategory = selectedCategory,
                    onSelectCategory = { cat ->
                        selectedCategory = if (selectedCategory.equals(cat, ignoreCase = true)) "All" else cat
                    }
                )
            }

            // Quick Filter & Sort Pills Strip
            item {
                Spacer(modifier = Modifier.height(14.dp))
                MarketQuickFilterBar(
                    isPromotionsActive = isPromotionsFilterActive,
                    onTogglePromotions = { isPromotionsFilterActive = !isPromotionsFilterActive },
                    isQuickWalkActive = isQuickWalkActive,
                    onToggleQuickWalk = { isQuickWalkActive = !isQuickWalkActive },
                    isFreePickupActive = isFreePickupActive,
                    onToggleFreePickup = { isFreePickupActive = !isFreePickupActive },
                    sortOption = sortOption,
                    onSelectSortOption = { sortOption = it },
                    onOpenFilterDrawer = { isFilterDrawerOpen = true }
                )
            }

            // Welcome Promotional Banner Card (Full Card Cover Image with Text Overlay)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                CampusWelcomePromoBanner(
                    onBannerClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Welcome Perk Active: 0% fee applied on your first listing or gig!")
                        }
                    }
                )
            }

            // Section Header: "Open near you" + Listing Count + Full/List View Switcher + Post Button (Image 2)
            item {
                Spacer(modifier = Modifier.height(20.dp))
                CampusMarketSectionHeader(
                    listingCount = totalListingCount,
                    isFullCardView = isFullCardView,
                    onToggleView = { isFullCardView = it },
                    onPostNewClick = { isPostDialogOpen = true }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (isProductsLoading || networkState.isRetrying) {
                // Shimmer Skeleton Loading State for Marketplace Products (Full View or List View)
                if (isFullCardView) {
                    items(3, key = { "skeleton-hero-$it" }) {
                        MarketProductHeroCardSkeleton(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    items(5, key = { "skeleton-list-$it" }) {
                        MarketProductListItemSkeleton()
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 96.dp, end = 16.dp),
                            thickness = 0.6.dp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            } else if (!networkState.isOnline) {
                // Pattern B: No Internet Connection Empty State with "Try again" button (Reference Image 1)
                item {
                    CampusNoInternetEmptyState(
                        onTryAgainClick = { networkState.triggerRetry() }
                    )
                }
            } else if (displayItems.isEmpty() && displayGigs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No active listings in this category. Be the first to post!",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else if (isFullCardView) {
                // FULL VIEW: Senior-level full-bleed photographic hero cards
                items(displayItems, key = { "item-${it.id}" }) { item ->
                    MarketProductHeroCard(
                        item = item,
                        isSaved = savedListingIds.contains("item-${item.id}"),
                        onToggleSave = {
                            val id = "item-${item.id}"
                            savedListingIds = if (savedListingIds.contains(id)) savedListingIds - id else savedListingIds + id
                            scope.launch {
                                val msg = if (savedListingIds.contains(id)) "Saved to your watch list: ${item.title}" else "Removed from watch list: ${item.title}"
                                snackbarHostState.showSnackbar(msg)
                            }
                        },
                        onClick = {
                            selectedItemForDetails = item
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(displayGigs, key = { "gig-${it.id}" }) { gig ->
                    MarketGigHeroCard(
                        gig = gig,
                        isSaved = savedListingIds.contains("gig-${gig.id}"),
                        onToggleSave = {
                            val id = "gig-${gig.id}"
                            savedListingIds = if (savedListingIds.contains(id)) savedListingIds - id else savedListingIds + id
                            scope.launch {
                                val msg = if (savedListingIds.contains(id)) "Saved gig to watch list: ${gig.title}" else "Removed gig from watch list: ${gig.title}"
                                snackbarHostState.showSnackbar(msg)
                            }
                        },
                        onAccept = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Applied for gig: ${gig.title}")
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                // LIST VIEW: Compact list view exactly as shown in Image 2
                items(displayItems, key = { "list-item-${it.id}" }) { item ->
                    MarketProductListItem(
                        item = item,
                        isSaved = savedListingIds.contains("item-${item.id}"),
                        onToggleSave = {
                            val id = "item-${item.id}"
                            savedListingIds = if (savedListingIds.contains(id)) savedListingIds - id else savedListingIds + id
                            scope.launch {
                                val msg = if (savedListingIds.contains(id)) "Saved to your watch list: ${item.title}" else "Removed from watch list: ${item.title}"
                                snackbarHostState.showSnackbar(msg)
                            }
                        },
                        onClick = {
                            selectedItemForDetails = item
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 96.dp, end = 16.dp),
                        thickness = 0.6.dp,
                        color = Color(0xFFE2E8F0)
                    )
                }

                items(displayGigs, key = { "list-gig-${it.id}" }) { gig ->
                    MarketGigListItem(
                        gig = gig,
                        isSaved = savedListingIds.contains("gig-${gig.id}"),
                        onToggleSave = {
                            val id = "gig-${gig.id}"
                            savedListingIds = if (savedListingIds.contains(id)) savedListingIds - id else savedListingIds + id
                            scope.launch {
                                val msg = if (savedListingIds.contains(id)) "Saved gig to watch list: ${gig.title}" else "Removed gig from watch list: ${gig.title}"
                                snackbarHostState.showSnackbar(msg)
                            }
                        },
                        onAccept = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Applied for gig: ${gig.title}")
                            }
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 96.dp, end = 16.dp),
                        thickness = 0.6.dp,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }

        // Full-Screen Progressive Product Details Screen (Reference Image 1)
        selectedItemForDetails?.let { detailItem ->
            val savedKey = "item-${detailItem.id}"
            val currentBasketConfig = basketConfigsByItemId[detailItem.id]
            MarketProductDetailsFullScreen(
                item = detailItem,
                isSaved = savedListingIds.contains(savedKey),
                basketConfig = currentBasketConfig,
                currentUserId = uiState.studentProfile?.stableUserId().orEmpty(),
                currentUserName = uiState.studentProfile?.fullName ?: "",
                onSecurityNotice = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                onToggleSave = {
                    savedListingIds = if (savedListingIds.contains(savedKey)) {
                        savedListingIds - savedKey
                    } else {
                        savedListingIds + savedKey
                    }
                    scope.launch {
                        val msg = if (savedListingIds.contains(savedKey)) {
                            "Saved to your watch list: ${detailItem.title}"
                        } else {
                            "Removed from watch list: ${detailItem.title}"
                        }
                        snackbarHostState.showSnackbar(msg)
                    }
                },
                onBackClick = { selectedItemForDetails = null },
                onShareClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Shared campus listing link: ${detailItem.title}")
                    }
                },
                onOpenOptionsSheet = {
                    isOptionsSheetOpen = true
                },
                onOpenCheckoutSheet = {
                    isCheckoutSheetOpen = true
                },
                onOpenQrScanner = {
                    selectedItemForEscrowVerification = detailItem
                },
                onMessageSeller = { sellerId, sellerName, initialOrderMessage ->
                    isCheckoutSheetOpen = false
                    isOptionsSheetOpen = false
                    if (onMessageSeller != null) {
                        onMessageSeller(sellerId, sellerName, initialOrderMessage)
                    } else {
                        onNavigateToChat(sellerId.ifBlank { sellerName }, sellerName)
                    }
                },
                onOpenMapLocation = { lat, lng, name ->
                    onNavigateToMap?.invoke(lat, lng, name)
                }
            )
        }

        // Stage 2: Product Options & Add-to-Basket Drawer Sheet (Reference Image 2)
        ProductOptionsBasketDrawerSheet(
            item = selectedItemForDetails,
            isOpen = isOptionsSheetOpen && selectedItemForDetails != null,
            initialConfig = selectedItemForDetails?.let { basketConfigsByItemId[it.id] },
            onDismiss = { isOptionsSheetOpen = false },
            onShareClick = {
                selectedItemForDetails?.let { item ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Shared campus listing link: ${item.title}")
                    }
                }
            },
            onAddToBasket = { newConfig ->
                basketConfigsByItemId = basketConfigsByItemId + (newConfig.item.id to newConfig)
                isOptionsSheetOpen = false
                isCheckoutSheetOpen = true
            }
        )

        // Stage 3: Basket Checkout & SafeTrade Escrow Lock Sheet -> Unlocks Seller Chat & QR Scanner
        val activeCheckoutConfig = selectedItemForDetails?.let { basketConfigsByItemId[it.id] }
        BasketEscrowCheckoutSheet(
            config = activeCheckoutConfig,
            isOpen = isCheckoutSheetOpen && activeCheckoutConfig != null,
            onDismiss = { isCheckoutSheetOpen = false },
            paymentProviderReady = uiState.isPaymentProviderReady,
            paymentProviderName = uiState.paymentProviderName,
            paymentMethods = uiState.paymentMethods,
            onConfirmEscrowPurchase = { basket, paymentMethodId, buyerPhone ->
                when (val result = viewModel.lockEscrowFunds(basket, paymentMethodId, buyerPhone)) {
                    is com.campus.network.core.model.PaymentResult.Failure -> result
                    is com.campus.network.core.model.PaymentResult.Success -> {
                        basketConfigsByItemId = basketConfigsByItemId + (result.value.item.id to result.value)
                        snackbarHostState.showSnackbar(
                            "Order ${result.value.orderReference} locked via ${uiState.paymentProviderName}. Message seller or scan QR at pickup."
                        )
                        result
                    }
                }
            },
            onMessageSeller = { sellerId, sellerName, initialOrderMessage ->
                isCheckoutSheetOpen = false
                if (onMessageSeller != null) {
                    onMessageSeller(sellerId, sellerName, initialOrderMessage)
                } else {
                    onNavigateToChat(sellerId.ifBlank { sellerName }, sellerName)
                }
            },
            onOpenQrScanner = {
                isCheckoutSheetOpen = false
                selectedItemForEscrowVerification = activeCheckoutConfig?.item
            }
        )

        // Floating Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
        ) { data ->
            com.campus.network.core.ui.components.CampusSuccessSnackBanner(
                message = data.visuals.message,
                onDismiss = { data.dismiss() }
            )
        }

        // Post New Product Drawer Sheet (Safe-Trade with Pickup, Payout, Multi-Photo Gallery & QR Escrow)
        PostProductDrawerSheet(
            isOpen = isPostDialogOpen,
            onDismiss = { isPostDialogOpen = false },
            onPublishProduct = { title, price, category, condition, location, description,
                                 pickupSpotName, pickupSecurityLevel, pickupLatitude, pickupLongitude,
                                 pickupTiming, payoutMethod, requiresQrReleaseCode, qrReleaseCode, imageUrl, imageUrls ->
                viewModel.onEvent(
                    HubEvent.PostItem(
                        title = title,
                        price = price,
                        category = category,
                        condition = condition,
                        location = location,
                        description = description,
                        pickupSpotName = pickupSpotName,
                        pickupSecurityLevel = pickupSecurityLevel,
                        pickupLatitude = pickupLatitude,
                        pickupLongitude = pickupLongitude,
                        pickupTiming = pickupTiming,
                        payoutMethod = payoutMethod,
                        requiresQrReleaseCode = requiresQrReleaseCode,
                        qrReleaseCode = qrReleaseCode,
                        imageUrl = imageUrl,
                        imageUrls = imageUrls
                    )
                )
                isPostDialogOpen = false
                scope.launch {
                    val photoNote = if (imageUrls.size > 1) " (${imageUrls.size} photos)" else ""
                    snackbarHostState.showSnackbar(
                        "Listed $title$photoNote • Pickup at $pickupSpotName"
                    )
                }
            },
            onOpenMapLocation = { lat, lng, name ->
                onNavigateToMap?.invoke(lat, lng, name)
            }
        )

        // Stage 4: SafeTrade Buyer Verification & Escrow QR Scanner Drawer Sheet (Only after purchase!)
        SafeTradeEscrowBuyerSheet(
            item = selectedItemForEscrowVerification,
            isOpen = selectedItemForEscrowVerification != null,
            onDismiss = { selectedItemForEscrowVerification = null },
            onEscrowReleased = { releasedItem ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "Trade completed! Escrow payout requested for ${releasedItem.currency} ${"%,.0f".format(releasedItem.price)} to ${releasedItem.sellerName}."
                    )
                }
            },
            onVerifyReleaseCode = { itemId, scannedCode ->
                viewModel.verifyReleaseCode(itemId, scannedCode)
            },
            onReleaseEscrowFunds = { itemId ->
                when (val result = viewModel.releaseEscrowFunds(itemId, releaseCodeVerified = true)) {
                    is com.campus.network.core.model.PaymentResult.Success -> true
                    is com.campus.network.core.model.PaymentResult.Failure -> {
                        snackbarHostState.showSnackbar(result.message)
                        false
                    }
                }
            }
        )

        // Filter Bottom Sheet Drawer Panel (Mirroring Image 1)
        CampusMarketFilterDrawerSheet(
            isOpen = isFilterDrawerOpen,
            onDismiss = { isFilterDrawerOpen = false },
            walkTimeFilter = selectedWalkTimeFilter,
            onSelectWalkTime = { selectedWalkTimeFilter = it },
            priceFilter = selectedPriceFilter,
            onSelectPrice = { selectedPriceFilter = it },
            ratingFilter = selectedRatingFilter,
            onSelectRating = { selectedRatingFilter = it },
            selectedCategory = selectedCategory,
            onSelectCategory = { selectedCategory = it },
            isFreePickupOnly = isFreePickupActive,
            onToggleFreePickup = { isFreePickupActive = !isFreePickupActive },
            onClearAll = {
                selectedCategory = "All"
                activeListingType = "All"
                isPromotionsFilterActive = false
                isQuickWalkActive = false
                isFreePickupActive = false
                selectedWalkTimeFilter = "Any"
                selectedPriceFilter = "Any"
                selectedRatingFilter = "Any"
            },
            totalCount = totalListingCount
        )
    }
}

/**
 * Scenic Hero Header Section:
 * Features full-bleed scenic photograph, distant flying birds Canvas layer,
 * the transparent logo replacing the "100" logo, circular glass action buttons,
 * the floating location pill ("Nairobi, Kenya"), and the interactive in-place AI Search Bar
 * with typewriter animated suggestions, soft keyboard auto-popup, and live AI suggestions dropdown.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroScenicHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    aiSuggestions: List<String>,
    onSelectSuggestion: (String) -> Unit,
    isAiLoading: Boolean,
    onLogoClick: () -> Unit,
    onChatClick: () -> Unit,
    onNotificationClick: () -> Unit,
    unreadNotificationsCount: Int = 0,
    onLocationClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    val typewriterSuggestions = remember {
        listOf(
            "Central Quad study pods open late",
            "Quiet quad study pods open late",
            "Engineering laptops and monitors",
            "High-payout campus coding gigs",
            "Used textbooks and class notes",
            "Calculus and physics study groups"
        )
    }

    var displayedSuggestion by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) return@LaunchedEffect
        var sIndex = 0
        while (isActive) {
            val target = typewriterSuggestions[sIndex % typewriterSuggestions.size]
            for (i in 1..target.length) {
                displayedSuggestion = target.substring(0, i)
                delay(45L)
            }
            delay(2400L)
            for (i in target.length downTo 0) {
                displayedSuggestion = target.substring(0, i)
                delay(20L)
            }
            delay(300L)
            sIndex++
        }
    }

    val showSuggestions = isFocused && aiSuggestions.isNotEmpty()
    val headerHeight by animateDpAsState(
        targetValue = if (showSuggestions) 470.dp else 390.dp,
        label = "heroHeaderHeight"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
    ) {
        // 1. Scenic Photography & World Cup 2026 Lottie Animation Hero Slider
        val heroPagerState = rememberPagerState(pageCount = { 2 })

        // Auto-scroll slider periodically between scenic campus image and World Cup Lottie animation
        LaunchedEffect(heroPagerState) {
            while (isActive) {
                delay(7000L)
                val nextPage = (heroPagerState.currentPage + 1) % 2
                heroPagerState.animateScrollToPage(nextPage)
            }
        }

        HorizontalPager(
            state = heroPagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> {
                    Image(
                        painter = painterResource(R.drawable.hero_campus_scenic),
                        contentDescription = "Scenic Campus Backdrop",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                1 -> {
                    val lottieComposition by rememberLottieComposition(
                        LottieCompositionSpec.RawRes(com.campus.network.core.ui.R.raw.worldcup_2026_football_animation)
                    )
                    val lottieProgress by animateLottieCompositionAsState(
                        composition = lottieComposition,
                        iterations = LottieConstants.IterateForever,
                        isPlaying = true
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF070D18))
                    ) {
                        LottieAnimation(
                            composition = lottieComposition,
                            progress = { lottieProgress },
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Hero Slider Indicators (Scenic Backdrop vs World Cup 2026 Lottie)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 22.dp, bottom = if (showSuggestions) 145.dp else 85.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.38f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(2) { index ->
                val isSelected = heroPagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .height(3.5.dp)
                        .width(if (isSelected) 14.dp else 4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.45f))
                )
            }
        }

        // 2. Distant Avian Silhouettes & Atmospheric Vignette (Custom Canvas)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top status vignette for high-contrast visibility
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.38f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.32f)
                    ),
                    startY = 0f,
                    endY = h
                )
            )

            // Soaring birds in upper center sky (matching reference)
            val birdColor = Color(0x66202428)
            val birdStroke = Stroke(width = 2.2f)

            // Bird 1
            val path1 = Path().apply {
                moveTo(w * 0.41f, h * 0.14f)
                quadraticBezierTo(w * 0.425f, h * 0.128f, w * 0.44f, h * 0.138f)
                quadraticBezierTo(w * 0.455f, h * 0.128f, w * 0.47f, h * 0.14f)
            }
            drawPath(path = path1, color = birdColor, style = birdStroke)

            // Bird 2 (smaller, slightly offset)
            val path2 = Path().apply {
                moveTo(w * 0.485f, h * 0.155f)
                quadraticBezierTo(w * 0.495f, h * 0.145f, w * 0.505f, h * 0.152f)
                quadraticBezierTo(w * 0.515f, h * 0.145f, w * 0.525f, h * 0.155f)
            }
            drawPath(path = path2, color = birdColor, style = birdStroke)
        }

        // 3. Top Header Bar: App Logo & Circular Frosted Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, start = 18.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Logo with transparent background (replacing 100 logo)
            Image(
                painter = painterResource(R.drawable.ic_logo_transparent),
                contentDescription = "App Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(46.dp)
                    .clickable { onLogoClick() }
            )

            // Right Action Buttons (Chat & Notifications)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Frosted Chat Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.94f))
                        .border(1.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
                        .shadow(3.dp, CircleShape)
                        .clickable { onChatClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Messages",
                        tint = Color(0xFF1E2229),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Circular Frosted Notifications Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.94f))
                        .border(1.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
                        .shadow(3.dp, CircleShape)
                        .clickable { onNotificationClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsNone,
                        contentDescription = "Notifications",
                        tint = Color(0xFF1E2229),
                        modifier = Modifier.size(23.dp)
                    )

                    // Small indicator dot (displayed dynamically when unread notifications exist)
                    if (unreadNotificationsCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-10).dp, y = 10.dp)
                                .background(Color(0xFFEA4335), CircleShape)
                        )
                    }
                }
            }
        }

        // 4. Floating Location Chip (Right-aligned over scenic view)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = if (showSuggestions) 140.dp else 80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(HeroLocationDark)
                .clickable { onLocationClick() }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Nairobi, Kenya",
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 5. Interactive In-Place AI Search Bar (Bottom of Hero Scene)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            shape = RoundedCornerShape(if (showSuggestions) 22.dp else 28.dp),
            color = AIBarBackground,
            shadowElevation = 8.dp,
            border = if (isFocused) BorderStroke(1.5.dp, BrandBluePrimary) else null
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Four-point AI Sparkle Icon
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Sparkle",
                        tint = BrandBluePrimary,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Real in-place text input field with typewriter placeholder
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = displayedSuggestion.ifEmpty { "Search campus with AI" },
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (displayedSuggestion.isNotEmpty() && !isFocused) {
                                    Text(
                                        text = "|",
                                        fontSize = 14.5.sp,
                                        color = BrandBluePrimary.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { isFocused = it.isFocused },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkCardTextPrimary
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            )
                        )
                    }

                    // Trailing Action: Spinner / Clear X / Search Lens
                    if (isAiLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = BrandBluePrimary,
                            strokeWidth = 2.dp
                        )
                    } else if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onSearchQueryChange("")
                                focusRequester.requestFocus()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Search",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = BrandBluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // AI Suggested Searches Dropdown
                if (showSuggestions) {
                    HorizontalDivider(
                        color = Color(0x1F000000),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = BrandBluePrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI Search Suggestions",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                        }

                        aiSuggestions.take(3).forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onSelectSuggestion(suggestion)
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    }
                                    .padding(vertical = 7.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = BrandBluePrimary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = suggestion,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E293B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Member Status / Loyalty Card:
 * Deep blue gradient card displaying member identity, masked student ID,
 * campus merit points, and trust score with zero rectangular layout artifacts.
 */
@Composable
private fun MemberLoyaltyCard(
    memberName: String,
    memberIdMasked: String,
    meritPoints: String,
    trustScore: String,
    onCardClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 6.dp,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(BrandBluePrimary, BrandBlueDeep)
                    )
                )
                .clickable { onCardClick() }
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column {
                // Top Member Identity Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Academic Honor Emblem (Canvas drawing)
                    AcademicHonorEmblem(modifier = Modifier.size(36.dp))

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = memberName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Patriarch Student ID #",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.85f)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = memberIdMasked.ifBlank { "•••• 4829" },
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View Membership",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Soft Ambient Card Separation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.12f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Merit & Trust Stats
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Merit Points Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Stars,
                            contentDescription = "Campus Merit",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Campus Merit Points: ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = meritPoints,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Trust Score Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Public,
                            contentDescription = "Trust Score",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Trust Score: ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = trustScore,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Academic Honor Emblem:
 * Precision canvas vector representing the Patriarch scholar crest in sky blue with inner radiance.
 */
@Composable
private fun AcademicHonorEmblem(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val crestColor = Color(0xFF6BB9F8)
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.15f)
            lineTo(w * 0.8f, h * 0.15f)
            lineTo(w * 0.8f, h * 0.55f)
            cubicTo(w * 0.8f, h * 0.8f, w * 0.5f, h * 0.95f, w * 0.5f, h * 0.95f)
            cubicTo(w * 0.5f, h * 0.95f, w * 0.2f, h * 0.8f, w * 0.2f, h * 0.55f)
            close()
        }
        drawPath(path = path, color = crestColor)

        // Inner crest accent highlight
        val innerPath = Path().apply {
            moveTo(w * 0.32f, h * 0.25f)
            lineTo(w * 0.68f, h * 0.25f)
            lineTo(w * 0.68f, h * 0.52f)
            cubicTo(w * 0.68f, h * 0.72f, w * 0.5f, h * 0.82f, w * 0.5f, h * 0.82f)
            cubicTo(w * 0.5f, h * 0.82f, w * 0.32f, h * 0.72f, w * 0.32f, h * 0.52f)
            close()
        }
        drawPath(path = innerPath, color = Color.White.copy(alpha = 0.3f))
    }
}

/**
 * Data Model for Campus Live Radar & Featured Event Stories.
 */
private data class CampusLiveUpdate(
    val id: String,
    val title: String,
    val subtitle: String,
    val tag: String,
    val isLive: Boolean = false,
    val liveViewerCount: String? = null,
    val broadcasterName: String,
    val broadcasterHandle: String,
    val broadcasterAvatarRes: Int,
    val imageRes: Int? = null,
    val isCreateStreamCard: Boolean = false
)

private val createStreamLiveUpdate = CampusLiveUpdate(
    id = "create_stream",
    title = "create a stream",
    subtitle = "Broadcast live to campus peers in real-time",
    tag = "LIVE STUDIO",
    isLive = false,
    broadcasterName = "",
    broadcasterHandle = "",
    broadcasterAvatarRes = 0,
    imageRes = null,
    isCreateStreamCard = true
)

private val sampleLiveUpdates = listOf(createStreamLiveUpdate)

/**
 * Featured Stories Carousel:
 * Clean, editorial commercial carousel with horizontal auto-scroll,
 * interactive play/pause, dynamic progress pills, and direct redirection to fullscreen Live Feed.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FeaturedStoriesCarousel(onStoryClick: (String) -> Unit) {
    if (sampleLiveUpdates.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { sampleLiveUpdates.size })
    var isAutoScrollPaused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Auto-Scroll Loop: Smoothly advances every 4 seconds unless manually dragged or paused (only when multiple items exist)
    LaunchedEffect(pagerState, isAutoScrollPaused) {
        if (!isAutoScrollPaused && sampleLiveUpdates.size > 1) {
            while (true) {
                delay(4000L)
                if (!pagerState.isScrollInProgress) {
                    val nextPage = (pagerState.currentPage + 1) % sampleLiveUpdates.size
                    pagerState.animateScrollToPage(
                        page = nextPage,
                        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Section Header Row: Professional Commercial Editorial Styling
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "Campus Stories",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    letterSpacing = (-0.4).sp
                )
                Text(
                    text = "Live broadcasts & student spotlights",
                    fontSize = 12.5.sp,
                    color = Color(0xFF64748B)
                )
            }

            if (sampleLiveUpdates.size > 1) {
                // Auto-scroll pause/play and page counter
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.clickable { isAutoScrollPaused = !isAutoScrollPaused }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isAutoScrollPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isAutoScrollPaused) "Resume Auto-scroll" else "Pause Auto-scroll",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${pagerState.currentPage + 1}/${sampleLiveUpdates.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Auto-Scrolling Pager
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 14.dp,
            modifier = Modifier.fillMaxWidth()
        ) { pageIndex ->
            val update = sampleLiveUpdates[pageIndex]
            if (update.isCreateStreamCard) {
                CreateStreamGradientCard(
                    onClick = { onStoryClick(update.id) }
                )
            } else {
                LiveUpdateStoryCard(
                    update = update,
                    onClick = { onStoryClick(update.id) }
                )
            }
        }

        if (sampleLiveUpdates.size > 1) {
            Spacer(modifier = Modifier.height(14.dp))

            // Sleek Animated Progress Indicators Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                sampleLiveUpdates.indices.forEach { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 26.dp else 7.dp,
                        label = "indicatorWidth"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(width)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isSelected) BrandBluePrimary else Color(0xFFCBD5E1)
                            )
                            .clickable {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                    )
                }
            }
        }
    }
}

/**
 * Story Card with Photographic Backdrop & Editorial Vignette Overlay.
 * Designed with clean glassmorphic tags and broadcaster metadata.
 */
@Composable
private fun LiveUpdateStoryCard(
    update: CampusLiveUpdate,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp),
        shape = RoundedCornerShape(26.dp),
        shadowElevation = 8.dp,
        color = Color(0xFF0F172A)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() }
        ) {
            // 1. Photographic Image Backdrop (if available)
            if (update.imageRes != null) {
                Image(
                    painter = painterResource(update.imageRes),
                    contentDescription = update.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. Multi-stop Vignette Gradient Overlay for Legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.72f),
                                0.35f to Color.Black.copy(alpha = 0.35f),
                                0.65f to Color.Black.copy(alpha = 0.65f),
                                1.0f to Color.Black.copy(alpha = 0.94f)
                            )
                        )
                    )
            )

            // 2b. Paramount+ Style Radial Cog Animated Loader for Live Story Stream Sync
            var isStoryStreamReady by remember(update.id) { mutableStateOf(false) }
            LaunchedEffect(update.id) {
                isStoryStreamReady = false
                kotlinx.coroutines.delay(580L)
                isStoryStreamReady = true
            }
            if (!isStoryStreamReady && update.isLive) {
                StreamCogLoaderOverlay(
                    message = "Optimizing video playback...",
                    dimBackground = false,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 3. Card Content Stack
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Tag & Live Indicator Row (Clean Senior Level Glassmorphism)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (update.isLive) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Vibrant Gradient LIVE Pill (Matching reference image)
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFFF512F), Color(0xFFDD2476))
                                        )
                                    )
                                    .padding(horizontal = 11.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "LIVE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // Viewer Count Pill
                            if (update.liveViewerCount != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.55f),
                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = update.liveViewerCount,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Clean Translucent Editorial Tag
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.50f),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = update.tag,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                letterSpacing = 0.3.sp,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Bottom Content: Broadcaster Info + Title + Subtitle + Clean Direct Action Hint
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (update.broadcasterName.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            CampusStudentAvatar(
                                avatarUrl = null,
                                fullName = update.broadcasterName,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = update.broadcasterName,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "@${update.broadcasterHandle}",
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Text(
                        text = update.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 25.sp,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = update.subtitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.90f),
                        lineHeight = 18.sp,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Clean Senior-Level Action Prompt (No bulky blue pills)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Watch live feed",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Text(
                            text = "Swipe up in feed",
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.65f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Senior-Level Empty Card with Radiant Gradient Color Layout.
 * Features an uncluttered, modern glassmorphic layout with the text 'create a stream'.
 */
@Composable
private fun CreateStreamGradientCard(
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp),
        shape = RoundedCornerShape(26.dp),
        shadowElevation = 10.dp,
        color = Color(0xFF1E1B4B)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() }
        ) {
            // 1. Vibrant Gradient Color Layout
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF4338CA), // Deep Indigo
                                Color(0xFF6366F1), // Electric Indigo
                                Color(0xFF9333EA), // Vivid Purple
                                Color(0xFFDB2777), // Deep Rose / Pink
                                Color(0xFFEA580C)  // Warm Sunset Accent
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(1000f, 1000f)
                        )
                    )
            )

            // 2. Soft Ambient Radial Glow Overlay for visual depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = Offset(300f, 250f),
                            radius = 500f
                        )
                    )
            )

            // 3. Delicate Translucent Border Outline
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.55f),
                                Color.White.copy(alpha = 0.15f)
                            )
                        ),
                        shape = RoundedCornerShape(26.dp)
                    )
            )

            // 4. Card Content: Clean, uncluttered layout with text 'create a stream'
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Studio Pill
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.25f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "STUDIO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Live Ready Indicator
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.20f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.40f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "GO LIVE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }
                }

                // Center: Aperture Icon + 'create a stream'
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Circular Aperture Icon
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.60f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "create a stream",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "create a stream",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.4).sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Broadcast live to campus peers in real-time",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.88f),
                        textAlign = TextAlign.Center
                    )
                }

                // Bottom Call To Action Button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable { onClick() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Start broadcasting now",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1B4B)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF1E1B4B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Data model for the 3D Circular Category Dock.
 */
private data class CampusMarketCategory(
    val id: String,
    val title: String,
    val iconRes: Int,
    val categoryFilter: String
)

private val campusMarketCategories = listOf(
    CampusMarketCategory("promo", "Promotions", R.drawable.cat_promotions, "Promotions"),
    CampusMarketCategory("textbooks", "Textbooks", R.drawable.cat_textbooks, "Textbooks"),
    CampusMarketCategory("tech", "Tech & Gear", R.drawable.cat_tech_gear, "Electronics"),
    CampusMarketCategory("gigs", "Campus Gigs", R.drawable.cat_campus_gigs, "Gigs"),
    CampusMarketCategory("dorm", "Dorm Living", R.drawable.cat_dorm_living, "Hardware"),
    CampusMarketCategory("tutoring", "Tutoring", R.drawable.cat_tutoring, "Tutoring"),
    CampusMarketCategory("lab", "Lab & Gear", R.drawable.cat_lab_gear, "Lab & Gear"),
    CampusMarketCategory("mobility", "Mobility", R.drawable.cat_mobility, "Mobility")
)

/**
 * Senior-level Section Header for Campus Marketplace & Gigs (Matching Reference Header - Image 2).
 * Displays "Open near you [count] places", full view vs list view switcher, and Post button.
 */
@Composable
private fun CampusMarketSectionHeader(
    listingCount: Int,
    isFullCardView: Boolean,
    onToggleView: (Boolean) -> Unit,
    onPostNewClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Open near you",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )
            if (listingCount > 0) {
                Text(
                    text = "$listingCount places",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF64748B)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Layout Switcher: Full View vs List View (Matching Image 2 Illustration)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.height(38.dp)
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Full View Tab (Squircle Rounded Rectangle Outline)
                    Box(
                        modifier = Modifier
                            .size(width = 38.dp, height = 32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isFullCardView) Color.White else Color.Transparent)
                            .border(
                                width = if (isFullCardView) 1.dp else 0.dp,
                                color = if (isFullCardView) Color(0xFFE2E8F0) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onToggleView(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(15.dp)) {
                            drawRoundRect(
                                color = if (isFullCardView) Color(0xFF0F172A) else Color(0xFF64748B),
                                topLeft = Offset(1.5f, 1.5f),
                                size = Size(size.width - 3f, size.height - 3f),
                                cornerRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx()),
                                style = Stroke(width = 1.8.dp.toPx())
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // List View Tab (Three Horizontal Parallel Bars - Active in Image 2)
                    Box(
                        modifier = Modifier
                            .size(width = 38.dp, height = 32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isFullCardView) Color.White else Color.Transparent)
                            .border(
                                width = if (!isFullCardView) 1.dp else 0.dp,
                                color = if (!isFullCardView) Color(0xFFE2E8F0) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onToggleView(false) },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(15.dp)) {
                            val barColor = if (!isFullCardView) Color(0xFF0F172A) else Color(0xFF64748B)
                            val barHeight = 2.dp.toPx()
                            val spacing = (size.height - 3 * barHeight) / 2
                            for (i in 0..2) {
                                val y = i * (barHeight + spacing)
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(0f, y),
                                    size = Size(size.width, barHeight),
                                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }

            // "+ Post" Button
            Button(
                onClick = onPostNewClick,
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBluePrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Post", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

/**
 * 3D Squircle Category Dock Carousel (Mirroring Reference Row 1).
 */
@Composable
private fun CampusCategoryDock(
    selectedCategory: String,
    onSelectCategory: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(campusMarketCategories, key = { it.id }) { cat ->
            val isSelected = selectedCategory.equals(cat.categoryFilter, ignoreCase = true)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(68.dp)
                    .clickable { onSelectCategory(cat.categoryFilter) }
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 68.dp, height = 70.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (isSelected) BrandBluePrimary.copy(alpha = 0.12f)
                            else Color(0xFFF1F5F9)
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) BrandBluePrimary else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(22.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(cat.iconRes),
                        contentDescription = cat.title,
                        modifier = Modifier.size(48.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(7.dp))

                Text(
                    text = cat.title,
                    fontSize = 11.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) BrandBluePrimary else Color(0xFF1E293B),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Filter & Sort Quick-Pill Bar (Mirroring Reference Row 2).
 */
@Composable
private fun MarketQuickFilterBar(
    isPromotionsActive: Boolean,
    onTogglePromotions: () -> Unit,
    isQuickWalkActive: Boolean,
    onToggleQuickWalk: () -> Unit,
    isFreePickupActive: Boolean,
    onToggleFreePickup: () -> Unit,
    sortOption: String,
    onSelectSortOption: (String) -> Unit,
    onOpenFilterDrawer: () -> Unit
) {
    var isSortMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular Tune / Filter Button (Opens Drawer Panel)
        Surface(
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 1.dp,
            modifier = Modifier
                .size(38.dp)
                .clickable { onOpenFilterDrawer() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Filters",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        // Circular Sort Button
        Box {
            Surface(
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .size(38.dp)
                    .clickable { isSortMenuOpen = true }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.SwapVert,
                        contentDescription = "Sort",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = isSortMenuOpen,
                onDismissRequest = { isSortMenuOpen = false }
            ) {
                listOf("Top Rated", "Price: Low to High", "Newest", "Distance").forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onSelectSortOption(option)
                            isSortMenuOpen = false
                        }
                    )
                }
            }
        }

        // Pill: Nearby Quad
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isQuickWalkActive) BrandBluePrimary.copy(alpha = 0.12f) else Color.White,
            border = BorderStroke(
                1.dp,
                if (isQuickWalkActive) BrandBluePrimary else Color(0xFFE2E8F0)
            ),
            shadowElevation = 1.dp,
            modifier = Modifier.clickable { onToggleQuickWalk() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = if (isQuickWalkActive) BrandBluePrimary else Color(0xFF64748B),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Nearby Quad",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isQuickWalkActive) BrandBluePrimary else Color(0xFF1E293B)
                )
            }
        }

        // Pill: Free Pickup
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isFreePickupActive) BrandBluePrimary.copy(alpha = 0.12f) else Color.White,
            border = BorderStroke(
                1.dp,
                if (isFreePickupActive) BrandBluePrimary else Color(0xFFE2E8F0)
            ),
            shadowElevation = 1.dp,
            modifier = Modifier.clickable { onToggleFreePickup() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingBag,
                    contentDescription = null,
                    tint = if (isFreePickupActive) BrandBluePrimary else Color(0xFF64748B),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Free Pickup",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isFreePickupActive) BrandBluePrimary else Color(0xFF1E293B)
                )
            }
        }

        // Pill: Promotions
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isPromotionsActive) Color(0xFFE11D48) else Color.White,
            border = BorderStroke(
                1.dp,
                if (isPromotionsActive) Color(0xFFE11D48) else Color(0xFFE2E8F0)
            ),
            shadowElevation = 1.dp,
            modifier = Modifier.clickable { onTogglePromotions() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalOffer,
                    contentDescription = null,
                    tint = if (isPromotionsActive) Color.White else Color(0xFFE11D48),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Promotions",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPromotionsActive) Color.White else Color(0xFF1E293B)
                )
            }
        }
    }
}

/**
 * Welcome Promotional Banner Card (Full Card Cover Image with Text Overlay).
 * Retains welcome banner section, covers entire card with image and overlays text.
 */
@Composable
private fun CampusWelcomePromoBanner(
    onBannerClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(145.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable { onBannerClick() }
    ) {
        // Full bleed background cover image
        Image(
            painter = painterResource(R.drawable.banner_welcome_perk),
            contentDescription = "Campus Welcome Perk",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient scrim for text contrast overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.88f),
                        0.55f to Color.Black.copy(alpha = 0.60f),
                        1f to Color.Black.copy(alpha = 0.18f)
                    )
                )
        )

        // Overlaid text content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF59E0B),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = "CAMPUS WELCOME GIFT",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Text(
                text = "0% fee on your first listing or gig",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                lineHeight = 23.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Free verified quad pickup & peer trade safety",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

/**
 * Senior Level Hero Product Listing Card ("Salt & Lime" Full-Bleed Overlay Style).
 */
@Composable
private fun MarketProductHeroCard(
    item: MarketItem,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageRes = when (item.id) {
        "item-1" -> R.drawable.place_photo_1
        "item-2" -> R.drawable.card_campus_landmark
        "item-3" -> R.drawable.course_thumb_1
        "item-4" -> R.drawable.course_thumb_4
        "item-5" -> R.drawable.place_photo_3
        "item-6" -> R.drawable.place_photo_2
        else -> R.drawable.card_student_founder
    }

    val badgeText = if (item.requiresQrReleaseCode && item.id.startsWith("item-posted-") || item.requiresQrReleaseCode) {
        "SafePay QR Verified"
    } else when (item.id) {
        "item-1" -> "Quad Deal"
        "item-2" -> "2 for 1 notes"
        "item-3" -> "Verified Student"
        "item-4" -> "Campus Special"
        "item-5" -> "Top Rated"
        "item-6" -> "Lab Ready"
        else -> "Special Offer"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(215.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        // Full-bleed photo covering the entire card (Real photo with progressive shimmer or preset fallback)
        if (!item.imageUrl.isNullOrBlank()) {
            ShimmerAsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(imageRes),
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Top-Left Badge Pill
        Box(
            modifier = Modifier
                .padding(12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE11D48))
                .padding(horizontal = 9.dp, vertical = 4.dp)
                .align(Alignment.TopStart)
        ) {
            Text(
                text = badgeText,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Top-Right Floating Bookmark/Favorite Button
        Box(
            modifier = Modifier
                .padding(12.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { onToggleSave() }
                .align(Alignment.TopEnd),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Save Item",
                tint = if (isSaved) Color(0xFFE11D48) else Color(0xFF0F172A),
                modifier = Modifier.size(18.dp)
            )
        }

        // Bottom gradient scrim & overlaid listing information
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.42f to Color.Black.copy(alpha = 0.52f),
                        1f to Color.Black.copy(alpha = 0.94f)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = item.title,
                            fontSize = 18.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", item.sellerRating),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = " (120)",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = " • ",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = item.category,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = " • ",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = item.pickupSpotName.ifBlank { item.campusHallLocation },
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "10 min",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${item.currency} ${item.price.toInt()}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF34D399)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Senior Level Hero MicroGig Listing Card ("Salt & Lime" Full-Bleed Overlay Style).
 */
@Composable
private fun MarketGigHeroCard(
    gig: MicroGig,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageRes = when (gig.id) {
        "gig-1" -> R.drawable.card_student_founder
        "gig-2" -> R.drawable.event_hero_trees
        "gig-3" -> R.drawable.course_thumb_2
        else -> R.drawable.card_campus_landmark
    }

    val badgeText = when (gig.urgencyLevel) {
        GigUrgency.FLASH_URGENT -> "Flash Urgent"
        GigUrgency.HIGH -> "Paid Same Day"
        GigUrgency.NORMAL -> "Verified Task"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(215.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable { onAccept() }
    ) {
        // Full-bleed photo covering the entire card (Real banner or preset fallback)
        if (!gig.bannerUrl.isNullOrBlank()) {
            AsyncImage(
                model = gig.bannerUrl,
                contentDescription = gig.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(imageRes),
                contentDescription = gig.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Top-Left Badge Pill
        Box(
            modifier = Modifier
                .padding(12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE11D48))
                .padding(horizontal = 9.dp, vertical = 4.dp)
                .align(Alignment.TopStart)
        ) {
            Text(
                text = badgeText,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Top-Right Floating Bookmark/Favorite Button
        Box(
            modifier = Modifier
                .padding(12.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { onToggleSave() }
                .align(Alignment.TopEnd),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Save Gig",
                tint = if (isSaved) Color(0xFFE11D48) else Color(0xFF0F172A),
                modifier = Modifier.size(18.dp)
            )
        }

        // Bottom gradient scrim & overlaid gig information
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.42f to Color.Black.copy(alpha = 0.52f),
                        1f to Color.Black.copy(alpha = 0.94f)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = gig.title,
                            fontSize = 18.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", gig.posterTrustScore / 20.0),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = " (${gig.posterName})",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = " • ",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = gig.tag,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = gig.estimatedDuration,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${gig.currency} ${gig.payoutAmount.toInt()}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF34D399)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact Product List Item (Mirroring Image 2 List View).
 */
@Composable
private fun MarketProductListItem(
    item: MarketItem,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageRes = when (item.id) {
        "item-1" -> R.drawable.place_photo_1
        "item-2" -> R.drawable.card_campus_landmark
        "item-3" -> R.drawable.course_thumb_1
        "item-4" -> R.drawable.course_thumb_4
        "item-5" -> R.drawable.place_photo_3
        "item-6" -> R.drawable.place_photo_2
        else -> R.drawable.card_student_founder
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rounded Square Thumbnail with floating subtle save button (Mirroring Image 2)
        Box(
            modifier = Modifier.size(64.dp)
        ) {
            if (!item.imageUrl.isNullOrBlank()) {
                ShimmerAsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.88f))
                    .clickable { onToggleSave() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Save Item",
                    tint = if (isSaved) Color(0xFFE11D48) else Color(0xFF64748B),
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Right Info Area: Strictly 64dp tall with 2 rows (SpaceBetween)
        Column(
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Line 1: Title (Left) + Walk Time (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "20 min",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Line 2: Rating + Category + Distance (Left) + Price / Free (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    val ratingStr = String.format(java.util.Locale.US, "%.1f", item.sellerRating)
                    val subtitleAnnotated = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))) {
                            append(ratingStr)
                        }
                        withStyle(SpanStyle(color = Color(0xFF64748B))) {
                            val loc = if (item.pickupSpotName.isNotBlank()) item.pickupSpotName else "0.4 km"
                            append(" (120) · ${item.category} · $loc")
                        }
                    }
                    Text(
                        text = subtitleAnnotated,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                val isFree = item.price == 0.0
                Text(
                    text = if (isFree) "Free" else "${item.currency} ${item.price.toInt()}",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isFree) Color(0xFF16A34A) else Color(0xFF475569),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Compact MicroGig List Item (Mirroring Image 2 List View).
 */
@Composable
private fun MarketGigListItem(
    gig: MicroGig,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageRes = when (gig.id) {
        "gig-1" -> R.drawable.card_student_founder
        "gig-2" -> R.drawable.event_hero_trees
        "gig-3" -> R.drawable.course_thumb_2
        else -> R.drawable.card_campus_landmark
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .clickable { onAccept() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rounded Square Thumbnail with floating subtle save button (Mirroring Image 2)
        Box(
            modifier = Modifier.size(64.dp)
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = gig.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.88f))
                    .clickable { onToggleSave() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Save Gig",
                    tint = if (isSaved) Color(0xFFE11D48) else Color(0xFF64748B),
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Right Info Area: Strictly 64dp tall with 2 rows (SpaceBetween)
        Column(
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Line 1: Title (Left) + Duration (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = gig.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = gig.estimatedDuration,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Line 2: Rating + Tag + Distance (Left) + Payout (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    val gigRatingStr = String.format(java.util.Locale.US, "%.1f", gig.posterTrustScore / 20.0)
                    val reviewCount = (gig.posterTrustScore * 3 / 4)
                    val subtitleAnnotated = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))) {
                            append(gigRatingStr)
                        }
                        withStyle(SpanStyle(color = Color(0xFF64748B))) {
                            append(" ($reviewCount) · ${gig.tag} · 0.2 km")
                        }
                    }
                    Text(
                        text = subtitleAnnotated,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${gig.currency} ${gig.payoutAmount.toInt()}",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF16A34A),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Filter Bottom Sheet Drawer Panel (Mirroring Image 1 with Campus Category Style).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CampusMarketFilterDrawerSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    walkTimeFilter: String,
    onSelectWalkTime: (String) -> Unit,
    priceFilter: String,
    onSelectPrice: (String) -> Unit,
    ratingFilter: String,
    onSelectRating: (String) -> Unit,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    isFreePickupOnly: Boolean,
    onToggleFreePickup: () -> Unit,
    onClearAll: () -> Unit,
    totalCount: Int
) {
    if (isOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFCBD5E1))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Header Row: Filters + Clear all
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filters",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )

                    Text(
                        text = "Clear all",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .clickable { onClearAll() }
                            .padding(4.dp)
                    )
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    // SECTION 1: DELIVERY / WALK TIME (DELIVERY TIME in reference)
                    FilterSection(title = "DELIVERY TIME") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(
                                "Any",
                                "Under 15 min",
                                "Under 30 min",
                                "Under 45 min"
                            ).forEach { label ->
                                val isSelected = walkTimeFilter == label
                                FilterPill(
                                    label = label,
                                    count = if (label == "Any" && totalCount > 0) totalCount.toString() else null,
                                    isSelected = isSelected,
                                    onClick = { onSelectWalkTime(label) }
                                )
                            }
                        }
                    }

                    // SECTION 2: PRICE
                    FilterSection(title = "PRICE") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(
                                "Under KES 500",
                                "KES 500-1,500",
                                "KES 1,500+"
                            ).forEach { label ->
                                val isSelected = priceFilter == label
                                FilterPill(
                                    label = label,
                                    count = null,
                                    isSelected = isSelected,
                                    onClick = { onSelectPrice(if (isSelected) "Any" else label) }
                                )
                            }
                        }
                    }

                    // SECTION 3: RATING
                    FilterSection(title = "RATING") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(
                                "4.0 and up",
                                "4.5 and up",
                                "4.8 and up"
                            ).forEach { label ->
                                val isSelected = ratingFilter == label
                                FilterPill(
                                    label = label,
                                    count = null,
                                    isSelected = isSelected,
                                    onClick = { onSelectRating(if (isSelected) "Any" else label) }
                                )
                            }
                        }
                    }

                    // SECTION 4: CAMPUS CATEGORY (DIETARY counterpart in reference, tailored to student campus)
                    FilterSection(title = "CAMPUS CATEGORY") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(
                                "Textbooks",
                                "Tech & Gear",
                                "Campus Gigs",
                                "Dorm Living",
                                "Tutoring",
                                "Lab Ready"
                            ).forEach { label ->
                                val isSelected = selectedCategory.contains(label, ignoreCase = true)
                                FilterPill(
                                    label = label,
                                    count = null,
                                    isSelected = isSelected,
                                    onClick = {
                                        onSelectCategory(if (isSelected) "All" else label)
                                    }
                                )
                            }
                        }
                    }

                    // SECTION 5: DELIVERY FEE
                    FilterSection(title = "DELIVERY FEE") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf("Free delivery").forEach { label ->
                                FilterPill(
                                    label = label,
                                    count = null,
                                    isSelected = isFreePickupOnly,
                                    onClick = { onToggleFreePickup() }
                                )
                            }
                        }
                    }
                }

                // Bottom Sticky CTA Button: "Show X places" (App Theme Color)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandBluePrimary
                        )
                    ) {
                        Text(
                            text = if (totalCount > 0) "Show $totalCount places" else "Apply Filters",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Filter Section with uppercase header.
 */
@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

/**
 * Filter Pill chip with label and count (Mirroring Image 1).
 */
@Composable
private fun FilterPill(
    label: String,
    count: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (isSelected) BrandBluePrimary else Color.White,
        border = BorderStroke(
            1.dp,
            if (isSelected) BrandBluePrimary else Color(0xFFE2E8F0)
        ),
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF1E293B)
            )
            if (!count.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = count,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color(0xFF64748B)
                )
            }
        }
    }
}
