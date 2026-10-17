package com.campus.network.feature.profile

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
    val verificationHash: String
)

@Composable
fun ActivityLedgerScreen(
    onBackClick: () -> Unit,
    onShowFeedback: (String) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "SafeTrades", "Quests", "Badges", "Consistency")

    val ledgerEvents = remember {
        listOf(
            LedgerEvent(
                id = "evt-1",
                title = "Completed Quest: 3-Day Focus Library Streak",
                description = "Checked in to Strathmore Quad Library 3 consecutive study sessions (>45m each).",
                timestamp = "Today, 11:24 AM",
                category = "Quests",
                scoreDelta = "+150 XP • +1 Trust",
                icon = Icons.Default.Whatshot,
                verificationHash = "0x98f2...4a12"
            ),
            LedgerEvent(
                id = "evt-2",
                title = "SafeTrade Settled: TI-84 Plus CE Graphing Calculator",
                description = "Mutual confirmation with Kevin Cheruiyot at Student Guild Hub Table 4. SafePay released.",
                timestamp = "Yesterday, 3:15 PM",
                category = "SafeTrades",
                scoreDelta = "+300 XP • +2 Trust",
                icon = Icons.Default.Payments,
                verificationHash = "0x83e1...29c4"
            ),
            LedgerEvent(
                id = "evt-3",
                title = "Badge Endorsement: Jetpack Compose Architect",
                description = "Endorsed by Dr. Odhiambo (School of Computing) following peer design review.",
                timestamp = "14 Oct, 5:40 PM",
                category = "Badges",
                scoreDelta = "Gold Master Level",
                icon = Icons.Default.MilitaryTech,
                verificationHash = "0x54a0...ee19"
            ),
            LedgerEvent(
                id = "evt-4",
                title = "24-Day Campus Consistency Streak Milestone",
                description = "Maintained continuous daily engagement across course channels and study rooms.",
                timestamp = "12 Oct, 9:00 AM",
                category = "Consistency",
                scoreDelta = "+100 Merit Points",
                icon = Icons.Default.Stars,
                verificationHash = "0x22c4...77d8"
            ),
            LedgerEvent(
                id = "evt-5",
                title = "SafeTrade Protected: Calculus Early Transcendentals",
                description = "Buyer Sarah K. initiated SafePay protection vault. Delivery scheduled at Quad B.",
                timestamp = "10 Oct, 1:30 PM",
                category = "SafeTrades",
                scoreDelta = "Vault Locked (KES 2,400)",
                icon = Icons.Default.Security,
                verificationHash = "0x11e9...b341"
            )
        )
    }

    val filteredEvents = ledgerEvents.filter {
        selectedFilter == "All" || it.category == selectedFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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

        // Summary Statistics Card
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
                    Text("24 Days", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF0F172A))
                    Text("Daily Streak", fontSize = 11.5.sp, color = Color(0xFF64748B))
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color(0xFFE2E8F0))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("48 Trades", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF16A34A))
                    Text("100% Settled", fontSize = 11.5.sp, color = Color(0xFF64748B))
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color(0xFFE2E8F0))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("96 / 100", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFFEAB308))
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
                                    text = "Ledger Hash: ${event.verificationHash}",
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
