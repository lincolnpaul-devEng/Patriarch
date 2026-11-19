package com.campus.network.feature.profile

import com.campus.network.core.model.EscrowOrder
import com.campus.network.core.model.StudentProfile
import com.campus.network.core.model.stableUserId
import com.campus.network.core.ui.components.HomescreenPurple
import com.campus.network.core.ui.components.homescreenDotGrid
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LedgerEvent(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: String,
    val category: String,
    val scoreDelta: String,
    val icon: ImageVector,
    val verificationHash: String,
    val timestampEpochMs: Long = 0L
)

private fun formatLedgerTime(epochMs: Long): String {
    if (epochMs <= 0L) return ""
    val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMM, h:mm a")
        .withZone(java.time.ZoneId.systemDefault())
    return formatter.format(java.time.Instant.ofEpochMilli(epochMs))
}

@Composable
fun ActivityLedgerScreen(
    onBackClick: () -> Unit,
    onShowFeedback: (String) -> Unit = {},
    profile: StudentProfile? = null,
    /** Real Safe Trade history — buyer + seller orders from campus_escrow_orders. */
    escrowOrdersLoader: suspend () -> List<EscrowOrder> = { emptyList() }
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "SafeTrades", "Quests", "Badges", "Consistency")

    var escrowOrders by remember { mutableStateOf<List<EscrowOrder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        escrowOrders = runCatching { escrowOrdersLoader() }.getOrDefault(emptyList())
        isLoading = false
    }

    val ledgerEvents: List<LedgerEvent> = remember(escrowOrders, profile) {
        buildList<LedgerEvent> {
            escrowOrders.forEach { order ->
                val isSeller = profile?.stableUserId() == order.sellerUserId
                val statusLabel = order.status.name.replace('_', ' ')
                LedgerEvent(
                    id = "trade-${order.id}",
                    title = "Safe Trade: ${order.itemTitle.ifBlank { "Listing" }}",
                    description = buildString {
                        append(if (isSeller) "You sold this listing." else "You bought this listing.")
                        if (order.rendezvousSpot.isNotBlank()) append(" Meet point: ${order.rendezvousSpot}.")
                        append(" Order ${order.orderNumber}.")
                    },
                    timestamp = formatLedgerTime(order.createdAtEpochMs),
                    category = "SafeTrades",
                    scoreDelta = "$statusLabel (KES ${order.amount.toLong()})",
                    icon = when (order.status.name) {
                        "RELEASED_TO_SELLER" -> Icons.Default.CheckCircle
                        "FUNDS_HELD" -> Icons.Default.Lock
                        else -> Icons.Default.Security
                    },
                    verificationHash = order.orderNumber,
                    timestampEpochMs = order.createdAtEpochMs
                )
            }
            profile?.privateQuestDeck?.forEach { quest ->
                LedgerEvent(
                    id = "quest-${quest.id}",
                    title = "Quest: ${quest.title}",
                    description = quest.description,
                    timestamp = "",
                    category = "Quests",
                    scoreDelta = if (quest.isCompleted) {
                        "Completed • +${quest.xpReward} XP"
                    } else {
                        "${quest.currentProgress}/${quest.targetProgress} • +${quest.xpReward} XP"
                    },
                    icon = Icons.Default.Whatshot,
                    verificationHash = quest.id
                )
            }
            profile?.verifiedSkillBadges?.forEach { badge ->
                LedgerEvent(
                    id = "badge-${badge.id}",
                    title = "Badge: ${badge.name}",
                    description = "Verified by ${badge.facultyVerifier.ifBlank { "faculty" }} • ${badge.endorsementCount} endorsements",
                    timestamp = badge.verifiedDate,
                    category = "Badges",
                    scoreDelta = badge.level.name,
                    icon = Icons.Default.MilitaryTech,
                    verificationHash = badge.id
                )
            }
            val streak = profile?.consistencyStreakDays ?: 0
            if (streak > 0) {
                LedgerEvent(
                    id = "streak-current",
                    title = "Campus Consistency Streak",
                    description = "Maintained continuous daily engagement across course channels and study rooms.",
                    timestamp = "",
                    category = "Consistency",
                    scoreDelta = "$streak Day${if (streak == 1) "" else "s"}",
                    icon = Icons.Default.Stars,
                    verificationHash = "streak-$streak"
                )
            }
        }.sortedByDescending { it.timestampEpochMs }
    }

    val filteredEvents = ledgerEvents.filter {
        selectedFilter == "All" || it.category == selectedFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(HomescreenPurple).homescreenDotGrid()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Activity Ledger",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Cryptographic Campus Audit Log",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            IconButton(
                onClick = { onShowFeedback("Exporting verified activity ledger statement...") },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Export",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Summary Statistics Card — real numbers from the profile + live escrow history
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${profile?.consistencyStreakDays ?: 0}",
                        fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF0F172A)
                    )
                    Text("Daily Streak", fontSize = 11.5.sp, color = Color(0xFF64748B))
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color(0xFFE2E8F0))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        escrowOrders.size.toString(),
                        fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF16A34A)
                    )
                    Text("Safe Trades", fontSize = 11.5.sp, color = Color(0xFF64748B))
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color(0xFFE2E8F0))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${profile?.behavioralTrustScore ?: 0} / 100",
                        fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFFEAB308)
                    )
                    Text("Trust Score", fontSize = 11.5.sp, color = Color(0xFF64748B))
                }
            }
        }

        // Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) Color(0xFF0F172A) else Color.White)
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = filter,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF475569)
                    )
                }
            }
        }

        // Timeline List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isLoading && filteredEvents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No verified activity yet. Safe Trades, quests and badges appear here as they happen.",
                            fontSize = 12.5.sp,
                            color = Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            items(filteredEvents, key = { it.id }) { event ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = event.icon,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = event.timestamp,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.weight(1f, fill = false),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = event.scoreDelta,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = event.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = event.description,
                                fontSize = 12.5.sp,
                                color = Color(0xFF475569),
                                lineHeight = 17.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FactCheck,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Ledger Ref: ${event.verificationHash}",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
