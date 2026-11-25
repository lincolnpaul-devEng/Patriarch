package com.campus.network.feature.hub.market

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.campus.network.core.model.MarketItem
import com.campus.network.core.model.MicroGig
import com.campus.network.core.ui.components.HomescreenBackground
import com.campus.network.core.ui.components.HomescreenInk
import com.campus.network.feature.hub.HubUiState
import com.campus.network.feature.hub.HubViewMode
import com.campus.network.feature.hub.HubViewModel
import com.campus.network.core.ui.components.CampusSuccessSnackBanner
import com.campus.network.feature.hub.bestfootball.DText

/** Real campus marketplace categories — chips filter the live feed. */
private val marketCategories = listOf(
    "All", "Electronics", "Books", "Fashion", "Dorm", "Beauty", "Sports", "Food", "Other"
)

private enum class MarketSort(val label: String) {
    NEWEST("Newest"),
    PRICE_ASC("Price: Low to High"),
    PRICE_DESC("Price: High to Low")
}

private fun priceText(amount: Double, currency: String): String =
    "$currency ${"%,.0f".format(amount)}"

/**
 * Campus Market — the real marketplace screen (browse items + gigs, search, sell entry).
 * Design language: violet dotted canvas, white rounded cards, ink text, orange sell sheet.
 */
@Composable
fun MarketScreen(
    viewModel: HubViewModel,
    rendezvousRepository: com.campus.network.core.data.RendezvousRepository,
    onNavigateBack: () -> Unit,
    onNavigateToMySelling: () -> Unit,
    onOpenProduct: (MarketItem) -> Unit,
    onOpenGig: (MicroGig) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val feedback by viewModel.feedbackMessage.collectAsState()
    var isSellSheetOpen by remember { mutableStateOf(false) }
    var rendezvousSpots by remember { mutableStateOf<List<com.campus.network.core.data.RendezvousSpot>>(emptyList()) }
    var selectedItem by remember { mutableStateOf<com.campus.network.core.model.MarketItem?>(null) }
    var isBuyerSheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onEvent(com.campus.network.feature.hub.HubEvent.SwitchViewMode(HubViewMode.MARKETPLACE_GRID))
        rendezvousSpots = rendezvousRepository.getActiveSpots()
    }

    // Open listing detail when arriving from a market push notification deep link.
    LaunchedEffect(uiState.pendingOpenItemId, uiState.marketplaceItems) {
        val pendingId = uiState.pendingOpenItemId?.trim().orEmpty()
        if (pendingId.isBlank()) return@LaunchedEffect
        val match = uiState.marketplaceItems.firstOrNull { it.id.equals(pendingId, ignoreCase = true) }
        if (match != null) {
            selectedItem = match
            isBuyerSheetOpen = false
            viewModel.onEvent(com.campus.network.feature.hub.HubEvent.ClearPendingOpenItem)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        HomescreenBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                MarketTopBar(
                    onNavigateBack = onNavigateBack,
                    onSellClick = { isSellSheetOpen = true },
                    onMySellingClick = onNavigateToMySelling
                )
                MarketSearchField(
                    uiState = uiState,
                    onQueryChange = { viewModel.onEvent(com.campus.network.feature.hub.HubEvent.UpdateSearchQuery(it)) },
                    onSelectSuggestion = { viewModel.onEvent(com.campus.network.feature.hub.HubEvent.SelectAiSuggestion(it)) },
                    onClear = { viewModel.onEvent(com.campus.network.feature.hub.HubEvent.ClearSearch) }
                )
                MarketSegmentRow(uiState = uiState, onSwitch = { viewModel.onEvent(com.campus.network.feature.hub.HubEvent.SwitchViewMode(it)) })

                when (uiState.activeViewMode) {
                    HubViewMode.MARKETPLACE_GRID -> MarketplaceContent(
                        uiState = uiState,
                        onCategory = { viewModel.onEvent(com.campus.network.feature.hub.HubEvent.FilterCategory(it)) },
                        onOpenProduct = { item -> selectedItem = item }
                    )
                    HubViewMode.MICRO_GIG_BOARD -> GigBoardContent(
                        uiState = uiState,
                        onOpenGig = onOpenGig
                    )
                }
            }
        }

        selectedItem?.let { item ->
            if (isBuyerSheetOpen) {
                SafeTradeBuyerSheet(
                    item = item,
                    profile = uiState.studentProfile,
                    viewModel = viewModel,
                    onDismiss = {
                        isBuyerSheetOpen = false
                        selectedItem = null
                    }
                )
            } else {
                ProductDetailSheet(
                    item = item,
                    isPaymentProviderReady = uiState.isPaymentProviderReady,
                    paymentProviderName = uiState.paymentProviderName,
                    onDismiss = { selectedItem = null },
                    onStartBuy = { isBuyerSheetOpen = true }
                )
            }
        }

        if (isSellSheetOpen) {
            SellSheet(
                profile = uiState.studentProfile,
                rendezvousSpots = rendezvousSpots,
                onEvent = { viewModel.onEvent(it) },
                onDismiss = { isSellSheetOpen = false },
                onOpenMySelling = {
                    isSellSheetOpen = false
                    onNavigateToMySelling()
                }
            )
        }

        feedback?.let { message ->
            com.campus.network.core.ui.components.CampusSuccessSnackBanner(
                message = message,
                onDismiss = { viewModel.onEvent(com.campus.network.feature.hub.HubEvent.ClearFeedback) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun MarketTopBar(
    onNavigateBack: () -> Unit,
    onSellClick: () -> Unit,
    onMySellingClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onNavigateBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = HomescreenInk, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        DText(text = "MARKET", size = 30f, color = HomescreenInk, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onMySellingClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Storefront, "My Selling", tint = HomescreenInk, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(21.dp))
                .background(Color.White)
                .clickable(onClick = onSellClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Sell, null, tint = HomescreenInk, modifier = Modifier.size(17.dp))
            Text("Sell", color = HomescreenInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MarketSearchField(
    uiState: HubUiState,
    onQueryChange: (String) -> Unit,
    onSelectSuggestion: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        androidx.compose.material3.OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onQueryChange,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = HomescreenInk,
                focusedTextColor = HomescreenInk,
                unfocusedTextColor = HomescreenInk
            ),
            placeholder = {
                Text("Search market…", color = HomescreenInk.copy(alpha = 0.45f), fontSize = 14.sp)
            },
            leadingIcon = {
                Icon(Icons.Default.AutoAwesome, null, tint = HomescreenInk, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (uiState.isAiLoading) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = HomescreenInk, strokeWidth = 2.dp)
                } else if (uiState.searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close, "Clear",
                        tint = HomescreenInk.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(onClick = onClear)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        if (uiState.aiSuggestions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(vertical = 4.dp)
            ) {
                uiState.aiSuggestions.take(4).forEach { suggestion ->
                    Text(
                        text = suggestion,
                        color = HomescreenInk,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSuggestion(suggestion) }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketSegmentRow(uiState: HubUiState, onSwitch: (HubViewMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(4.dp)
    ) {
        SegmentTab("Items", Modifier.weight(1f), uiState.activeViewMode == HubViewMode.MARKETPLACE_GRID) {
            onSwitch(HubViewMode.MARKETPLACE_GRID)
        }
        SegmentTab("Gigs", Modifier.weight(1f), uiState.activeViewMode == HubViewMode.MICRO_GIG_BOARD) {
            onSwitch(HubViewMode.MICRO_GIG_BOARD)
        }
    }
}

@Composable
private fun SegmentTab(label: String, modifier: Modifier = Modifier, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) HomescreenInk else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (isSelected) Color.White else HomescreenInk,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MarketplaceContent(
    uiState: HubUiState,
    onCategory: (String?) -> Unit,
    onOpenProduct: (MarketItem) -> Unit
) {
    var sort by remember { mutableStateOf(MarketSort.NEWEST) }

    val browseItems = remember(uiState.marketplaceItems, sort) {
        uiState.marketplaceItems
            .filter { it.status != "SOLD" }
            .let { items ->
                when (sort) {
                    MarketSort.NEWEST -> items.sortedByDescending { it.timestampMs }
                    MarketSort.PRICE_ASC -> items.sortedBy { it.price }
                    MarketSort.PRICE_DESC -> items.sortedByDescending { it.price }
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            marketCategories.forEach { category ->
                val selected = (uiState.filterCategory ?: "All").equals(category, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) HomescreenInk else Color.White)
                        .clickable { onCategory(if (category == "All") null else category) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        category,
                        color = if (selected) Color.White else HomescreenInk,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MarketSort.entries.forEach { option ->
                val selected = sort == option
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, if (selected) HomescreenInk else HomescreenInk.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                        .clickable { sort = option }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        option.label,
                        color = if (selected) HomescreenInk else HomescreenInk.copy(alpha = 0.65f),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        if (browseItems.isEmpty()) {
            MarketEmptyState(
                title = if (uiState.searchQuery.isBlank()) "No listings yet" else "Nothing found",
                subtitle = if (uiState.searchQuery.isBlank()) {
                    "The campus market is open — be the first to sell."
                } else {
                    "No listings match \"${uiState.searchQuery}\". Try another search."
                }
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(browseItems, key = { it.id }) { item ->
                    MarketProductCard(item = item, onClick = { onOpenProduct(item) })
                }
            }
        }
    }
}

@Composable
private fun MarketProductCard(item: MarketItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.15f)
                .background(HomescreenInk.copy(alpha = 0.06f))
        ) {
            if (item.imageUrls.isNotEmpty()) {
                AsyncImage(
                    model = item.imageUrls.first(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.Sell, null,
                    tint = HomescreenInk.copy(alpha = 0.25f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(30.dp)
                )
            }
            if (item.status == "RESERVED") {
                Text(
                    "RESERVED",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HomescreenInk)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                item.title,
                color = HomescreenInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                priceText(item.price, item.currency),
                color = HomescreenInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, null, tint = HomescreenInk.copy(alpha = 0.55f), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(3.dp))
                Text(
                    item.campusHallLocation.ifBlank { "Campus" },
                    color = HomescreenInk.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val metaLabel = listOf(item.condition, item.category).filter { it.isNotBlank() }.joinToString(" · ")
            if (metaLabel.isNotBlank()) Text(
                metaLabel,
                color = HomescreenInk.copy(alpha = 0.55f),
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GigBoardContent(
    uiState: HubUiState,
    onOpenGig: (MicroGig) -> Unit
) {
    if (uiState.microGigs.isEmpty()) {
        MarketEmptyState(
            title = if (uiState.searchQuery.isBlank()) "No gigs posted yet" else "Nothing found",
            subtitle = if (uiState.searchQuery.isBlank()) {
                "Campus gigs board is open — post the first gig."
            } else {
                "No gigs match \"${uiState.searchQuery}\". Try another search."
            }
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(uiState.microGigs, key = { it.id }) { gig ->
                MarketGigCard(gig = gig, onClick = { onOpenGig(gig) })
            }
        }
    }
}

@Composable
private fun MarketGigCard(gig: MicroGig, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                gig.title,
                color = HomescreenInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                priceText(gig.payoutAmount, gig.currency),
                color = HomescreenInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            gig.description,
            color = HomescreenInk.copy(alpha = 0.65f),
            fontSize = 12.5.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (gig.tag.isNotBlank()) GigMetaChip(gig.tag)
            GigMetaChip(urgencyLabel(gig))
            if (gig.estimatedDuration.isNotBlank()) GigMetaChip(gig.estimatedDuration)
            GigMetaChip("${gig.spotsAvailable} spot${if (gig.spotsAvailable == 1) "" else "s"}")
        }
    }
}

private fun urgencyLabel(gig: MicroGig): String = when (gig.urgencyLevel) {
    com.campus.network.core.model.GigUrgency.NORMAL -> "Normal"
    com.campus.network.core.model.GigUrgency.HIGH -> "Urgent"
    com.campus.network.core.model.GigUrgency.FLASH_URGENT -> "Flash urgent"
}

@Composable
private fun GigMetaChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(HomescreenInk.copy(alpha = 0.07f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = HomescreenInk, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MarketEmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Sell, null, tint = HomescreenInk, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(title, color = HomescreenInk, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            color = HomescreenInk.copy(alpha = 0.7f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
    }
}
