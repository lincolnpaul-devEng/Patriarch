package com.campus.network.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.core.model.StudentProfile

@Composable
fun TrustScoreScreen(
    profile: StudentProfile? = null,
    score: Int = profile?.behavioralTrustScore ?: 50,
    onBackClick: () -> Unit
) {
    val uniName = profile?.universityName?.takeIf { it.isNotBlank() } ?: "Campus"
    val tierTitle = when {
        score >= 85 -> "Tier 1: Campus Exemplar"
        score >= 65 -> "Tier 2: Verified Scholar"
        else -> "Tier 3: Standard Member"
    }
    val tierDesc = when {
        score >= 85 -> "Exemplary integrity standing at $uniName. Full privileges for instant escrow release, campus commerce, and verified channels."
        score >= 65 -> "Verified student in good standing at $uniName. Standard privileges for campus marketplace and academic channels."
        else -> "Welcome to $uniName. Complete verified study room check-ins, skill tests, and dispute-free peer trades to increase your trust score."
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
                    text = "Behavioral Trust Score",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Algorithmic Campus Reputation & Integrity",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Score Gauge Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$score",
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFEAB308)
                                )
                                Text(
                                    text = "OUT OF 100",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFEAB308).copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color(0xFFEAB308),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tierTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFDE047)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = tierDesc,
                            fontSize = 12.5.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Score Factors
            item {
                Text(
                    text = "Score Factors & Breakdown",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ScoreFactorRow(
                            icon = Icons.Default.Security,
                            title = "SafeTrade Dispute-Free Rate",
                            valueText = if (score > 50) "${score}%" else "No trades yet",
                            progress = if (score > 50) (score / 100f) else 0f,
                            description = if (score > 50) "Transactions completed with zero escrow disputes." else "Complete SafeTrades to establish a peer dispute record."
                        )

                        ScoreFactorRow(
                            icon = Icons.Default.Speed,
                            title = "Rendezvous Punctuality",
                            valueText = if (score > 50) "${score}%" else "Pending meetups",
                            progress = if (score > 50) (score / 100f) else 0f,
                            description = if (score > 50) "Punctuality record for safe physical trade handoffs." else "Arrive on time to rendezvous points to earn punctuality points."
                        )

                        ScoreFactorRow(
                            icon = Icons.Default.CheckCircle,
                            title = "Faculty & Academic Verification",
                            valueText = "${profile?.verifiedSkillBadges?.size ?: 0} Verified",
                            progress = ((profile?.verifiedSkillBadges?.size ?: 0) * 0.25f).coerceAtMost(1.0f),
                            description = "${profile?.verifiedSkillBadges?.size ?: 0} academic faculty badge endorsements registered."
                        )

                        ScoreFactorRow(
                            icon = Icons.Default.ThumbUp,
                            title = "Community Peer Reviews",
                            valueText = if (score > 50) "${String.format("%.1f", (score * 5.0 / 100.0))} / 5.0" else "No reviews yet",
                            progress = if (score > 50) (score / 100f) else 0f,
                            description = if (score > 50) "Peer rating average from verified campus course groups." else "Peer ratings will appear after completing group projects or trades."
                        )
                    }
                }
            }

            // Unlocked Perks & Privileges
            item {
                Text(
                    text = "Unlocked Campus Privileges",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PrivilegeRow(
                            title = "SafeTrade Peer Escrow",
                            status = if (score >= 50) "Unlocked" else "Locked",
                            isUnlocked = score >= 50
                        )
                        PrivilegeRow(
                            title = "Instant Vault Payouts",
                            status = if (score >= 70) "Unlocked" else "Locked (Requires 70+)",
                            isUnlocked = score >= 70
                        )
                        PrivilegeRow(
                            title = "Study Group Moderator Rights",
                            status = if (score >= 80) "Unlocked" else "Locked (Requires 80+)",
                            isUnlocked = score >= 80
                        )
                        PrivilegeRow(
                            title = "Campus Guild Leadership Eligibility",
                            status = if (score >= 85) "Unlocked" else "Locked (Requires 85+)",
                            isUnlocked = score >= 85
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreFactorRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    valueText: String,
    progress: Float,
    description: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF0F172A),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueText,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF16A34A)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = Color(0xFF16A34A),
            trackColor = Color(0xFFE2E8F0),
            strokeCap = StrokeCap.Round
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            fontSize = 12.sp,
            color = Color(0xFF64748B),
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun PrivilegeRow(
    title: String,
    status: String,
    isUnlocked: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.LockOpen,
                contentDescription = null,
                tint = if (isUnlocked) Color(0xFF16A34A) else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 13.5.sp,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = status,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isUnlocked) Color(0xFF16A34A) else Color(0xFF94A3B8)
        )
    }
}
