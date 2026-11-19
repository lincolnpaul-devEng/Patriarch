package com.campus.network.feature.hub.market

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.campus.network.core.data.CampusHubRepository
import com.campus.network.core.model.MarketItem
import com.campus.network.core.model.stableUserId
import com.campus.network.core.ui.components.HomescreenInk
import com.campus.network.core.ui.components.HomescreenOrange
import com.campus.network.core.ui.components.HomescreenPurple
import com.campus.network.core.ui.components.homescreenDotGrid
import com.campus.network.feature.hub.bestfootball.DText
import com.campus.network.feature.hub.bestfootball.SquadaOne
import com.campus.network.feature.hub.HubEvent
import com.campus.network.feature.hub.HubViewModel
import com.campus.network.feature.hub.components.RealQrCodeUtils

private data class MySellingState(
    val listings: List<MarketItem> = emptyList(),
    val orders: List<com.campus.network.core.model.EscrowOrder> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * Seller cockpit: manage live listings and watch Safe Trade escrow orders.
 * Every row is real data from `campus_market_items` / `campus_escrow_orders`.
 */
@Composable
fun MySellingScreen(
    viewModel: HubViewModel,
    repository: CampusHubRepository,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var state by remember { mutableStateOf(MySellingState()) }
    var refreshTick by remember { mutableStateOf(0) }
    var qrItem by remember { mutableStateOf<MarketItem?>(null) }
    val profile = viewModel.uiState.collectAsState().value.studentProfile
    val sellerId = profile?.stableUserId().orEmpty()
    val feedback by viewModel.feedbackMessage.collectAsState()

    LaunchedEffect(sellerId, refreshTick) {
        if (sellerId.isBlank()) {
            state = MySellingState(isLoading = false, error = "A Supabase profile is required to see your selling data.")
            return@LaunchedEffect
        }
        state = state.copy(isLoading = true, error = null)
        val listings = repository.getSellerListings(sellerId)
        val orders = repository.fetchEscrowOrdersAsSeller(sellerId)
        state = MySellingState(listings = listings, orders = orders, isLoading = false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomescreenPurple)
            .homescreenDotGrid()
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = HomescreenInk,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                DText(text = "MY SELLING", size = 28f, color = HomescreenInk, modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(HomescreenInk)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("MY LISTINGS", "ORDERS").forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selectedTab == index) Color.White else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontFamily = SquadaOne,
                            color = if (selectedTab == index) HomescreenInk else Color.White,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading your selling data…", color = Color.White, fontSize = 13.sp)
                }
                state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "", color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(24.dp))
                }
                selectedTab == 0 -> ListingsTab(
                    listings = state.listings,
                    onMarkSold = { viewModel.onEvent(HubEvent.MarkListingSold(it)); refreshTick++ },
                    onDelete = { viewModel.onEvent(HubEvent.DeleteListing(it)); refreshTick++ },
                    onShowQr = { qrItem = it }
                )
                else -> OrdersTab(orders = state.orders)
            }
        }
        feedback?.let { message ->
            Text(
                message,
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 22.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(HomescreenInk.copy(alpha = 0.85f))
                    .clickable { viewModel.onEvent(HubEvent.ClearFeedback) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
        qrItem?.let { item ->
            ReleaseCodeDialog(item = item, onDismiss = { qrItem = null })
        }
    }
}

/** Seller-side QR: what the buyer scans at the rendezvous point to release the payout. */
@Composable
private fun ReleaseCodeDialog(item: MarketItem, onDismiss: () -> Unit) {
    val code = item.qrReleaseCode
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SELLER RELEASE CODE", fontFamily = SquadaOne, color = HomescreenInk, fontSize = 15.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(10.dp))
            if (item.requiresQrReleaseCode && code.isNotBlank()) {
                val bitmap = remember(code) {
                    RealQrCodeUtils.generateQrBitmap("SAFE_TRADE_RELEASE:$code", 512)
                }
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Release QR",
                    modifier = Modifier.size(200.dp)
                )
                Spacer(Modifier.height(8.dp))
            } else {
                Text("This listing does not require a release code.", color = HomescreenInk.copy(alpha = 0.6f), fontSize = 12.sp)
            }
            if (code.isNotBlank()) {
                Text(code, fontFamily = SquadaOne, color = HomescreenOrange, fontSize = 20.sp, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Show this to the buyer at the meetup. Scanning it releases your M-Pesa payout.",
                color = HomescreenInk.copy(alpha = 0.65f),
                fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "CLOSE",
                fontFamily = SquadaOne,
                color = HomescreenInk,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(HomescreenPurple.copy(alpha = 0.2f))
                    .clickable { onDismiss() }
                    .padding(horizontal = 22.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ListingsTab(
    listings: List<MarketItem>,
    onMarkSold: (String) -> Unit,
    onDelete: (String) -> Unit,
    onShowQr: (MarketItem) -> Unit
) {
    if (listings.isEmpty()) {
        EmptyNote("No listings yet. Tap SELL on the market to post your first item.")
        return
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(listings) { item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (item.imageUrl != null) {
                        AsyncImage(
                            item.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(HomescreenPurple.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("NO\nPIC", fontFamily = SquadaOne, color = HomescreenInk, fontSize = 11.sp, lineHeight = 11.sp)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, color = HomescreenInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        Text(
                            "KES ${item.price.toLong()}  •  ${item.pickupSpotName.ifBlank { item.pickupTiming }}",
                            color = HomescreenInk.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        StatusChip(item.status)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (item.status == "ACTIVE") {
                            Button(
                                onClick = { onMarkSold(item.id) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HomescreenInk, contentColor = Color.White),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("SOLD", fontFamily = SquadaOne, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "SHOW QR",
                                fontFamily = SquadaOne,
                                color = HomescreenOrange,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { onShowQr(item) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        TextButton(onClick = { onDelete(item.id) }) {
                            Text("Delete", color = Color(0xFFB3261E), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersTab(orders: List<com.campus.network.core.model.EscrowOrder>) {
    if (orders.isEmpty()) {
        EmptyNote("No Safe Trade orders yet. Buyers who pay M-Pesa into escrow appear here.")
        return
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(orders) { order ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(order.itemTitle, color = HomescreenInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 2)
                    StatusChip(order.status.name)
                }
                Text(
                    "Order ${order.orderNumber}  •  KES ${order.amount.toLong()}",
                    color = HomescreenInk.copy(alpha = 0.65f),
                    fontSize = 11.sp
                )
                Text(
                    "Meet at ${order.rendezvousSpot.ifBlank { "agreed pickup point" }}",
                    color = HomescreenInk.copy(alpha = 0.65f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (bg, fg) = when (status) {
        "ACTIVE" -> Color(0xFF00A651) to Color.White
        "RESERVED" -> HomescreenOrange to Color.White
        "SOLD" -> HomescreenInk to Color.White
        "FUNDS_HELD" -> HomescreenOrange to Color.White
        "RELEASED_TO_SELLER" -> Color(0xFF00A651) to Color.White
        "PENDING_PAYMENT" -> Color(0xFF52456B) to Color.White
        else -> HomescreenInk.copy(alpha = 0.2f) to HomescreenInk
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            status.replace('_', ' '),
            fontFamily = SquadaOne,
            color = fg,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun EmptyNote(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.padding(24.dp)
        )
    }
}
