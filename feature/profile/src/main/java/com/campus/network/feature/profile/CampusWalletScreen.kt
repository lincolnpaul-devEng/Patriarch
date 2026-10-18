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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SafePayHold(
    val id: String,
    val itemTitle: String,
    val amount: String,
    val role: String, // "Buyer" or "Seller"
    val counterparty: String,
    val status: String,
    val canRelease: Boolean
)

data class WalletTransaction(
    val id: String,
    val title: String,
    val timestamp: String,
    val amount: String,
    val isCredit: Boolean,
    val status: String,
    val note: String
)

@Composable
fun CampusWalletScreen(
    onBackClick: () -> Unit,
    onShowFeedback: (String) -> Unit = {}
) {
    var availableBalance by remember { mutableStateOf(9200.0) }
    var vaultProtectedBalance by remember { mutableStateOf(5650.0) }
    var showDepositDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showSafePayInfoDialog by remember { mutableStateOf(false) }

    var depositAmountText by remember { mutableStateOf("") }
    var withdrawPhoneText by remember { mutableStateOf("+254 712 345678") }
    var withdrawAmountText by remember { mutableStateOf("") }

    var activeHolds by remember {
        mutableStateOf(
            listOf(
                SafePayHold(
                    id = "hold-1",
                    itemTitle = "Calculus Early Transcendentals (Stewart)",
                    amount = "KES 2,400.00",
                    role = "Buyer",
                    counterparty = "Sarah K. (Math Dept)",
                    status = "Protected in Vault — Awaiting Meetup Confirmation",
                    canRelease = true
                ),
                SafePayHold(
                    id = "hold-2",
                    itemTitle = "Organic Chemistry Lab Coat & Splash Goggles",
                    amount = "KES 1,250.00",
                    role = "Seller",
                    counterparty = "Brian M. (Biochem Dept)",
                    status = "Protected in Vault — Buyer Confirming Delivery",
                    canRelease = false
                ),
                SafePayHold(
                    id = "hold-3",
                    itemTitle = "Python Lab Assignment Peer Tutoring Session",
                    amount = "KES 2,000.00",
                    role = "Buyer",
                    counterparty = "Faith Chebet (TA)",
                    status = "Protected in Vault — In Progress",
                    canRelease = true
                )
            )
        )
    }

    val transactions = remember {
        listOf(
            WalletTransaction(
                id = "tx-1",
                title = "SafeTrade Settled: TI-84 Plus CE Calculator",
                timestamp = "Today, 2:15 PM",
                amount = "+KES 8,500.00",
                isCredit = true,
                status = "Completed",
                note = "Buyer confirmed inspection & release"
            ),
            WalletTransaction(
                id = "tx-2",
                title = "M-Pesa Instant Top-Up",
                timestamp = "Yesterday, 9:40 AM",
                amount = "+KES 5,000.00",
                isCredit = true,
                status = "Completed",
                note = "Ref: QK782390MP"
            ),
            WalletTransaction(
                id = "tx-3",
                title = "SafeTrade SafePay Protection Lock",
                timestamp = "14 Oct, 4:20 PM",
                amount = "-KES 2,400.00",
                isCredit = false,
                status = "Protected in Vault",
                note = "Held safely until Calculus book handover"
            ),
            WalletTransaction(
                id = "tx-4",
                title = "Withdrawal to M-Pesa",
                timestamp = "10 Oct, 6:10 PM",
                amount = "-KES 4,500.00",
                isCredit = false,
                status = "Completed",
                note = "Disbursed to +254 712 345678"
            )
        )
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
                    text = "Campus Wallet",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "SafePay Protected Student Treasury",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            IconButton(
                onClick = { showSafePayInfoDialog = true },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "SafePay Info",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Luxury Wallet Balance Card (Deep Obsidian & Gradient Cobalt)
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1F36)),
                    border = BorderStroke(1.dp, Color(0xFF163252)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E3A8A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Total Student Balance",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF22C55E).copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "SafePay Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4ADE80)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Large Numeric Balance
                        val totalBalance = availableBalance + vaultProtectedBalance
                        Text(
                            text = "KES ${String.format("%,.2f", totalBalance)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Balance Sub-Breakdown
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Available to spend",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "KES ${String.format("%,.2f", availableBalance)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(32.dp)
                                    .background(Color.White.copy(alpha = 0.1f))
                            )

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "In SafePay Vault",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "KES ${String.format("%,.2f", vaultProtectedBalance)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showDepositDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF007AFF),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Top Up", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color.White)
                            }

                            Button(
                                onClick = { showWithdrawDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Withdraw", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }
                        }
                    }
                }
            }

            // 2. SafePay Vault Protection Banner
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDBEAFE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Zero-Risk Student Protection",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A)
                            )
                            Text(
                                text = "Money is held safely until the buyer and seller meet up and confirm the item in person.",
                                fontSize = 11.5.sp,
                                color = Color(0xFF3B82F6),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // 3. Active SafePay Vault Holds (Active Deals)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active SafePay Holds (${activeHolds.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Auto-settles on meetup",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            items(activeHolds, key = { it.id }) { hold ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFEF3C7))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = hold.role,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = hold.amount,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = hold.itemTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Counterparty: ${hold.counterparty}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = hold.status,
                                fontSize = 11.5.sp,
                                color = Color(0xFF475569)
                            )
                        }

                        if (hold.canRelease) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    activeHolds = activeHolds.filterNot { it.id == hold.id }
                                    vaultProtectedBalance -= 2400.0
                                    onShowFeedback("Confirmed handover! Payment successfully disbursed to peer.")
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF16A34A),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Confirm Receipt & Release Payment", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // 4. Recent Transactions
            item {
                Text(
                    text = "Recent Transactions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            items(transactions, key = { it.id }) { tx ->
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (tx.isCredit) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (tx.isCredit) Icons.AutoMirrored.Filled.TrendingUp else Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = if (tx.isCredit) Color(0xFF16A34A) else Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.title,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${tx.timestamp} • ${tx.note}",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = tx.amount,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (tx.isCredit) Color(0xFF16A34A) else Color(0xFF0F172A),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }

    // Top Up Dialog
    if (showDepositDialog) {
        AlertDialog(
            onDismissRequest = { showDepositDialog = false },
            title = { Text("Top Up via M-Pesa", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter amount to deposit into your student wallet. You will receive an instant STK push prompt on your registered phone.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    OutlinedTextField(
                        value = depositAmountText,
                        onValueChange = { depositAmountText = it },
                        placeholder = { Text("e.g. 1500") },
                        prefix = { Text("KES ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = depositAmountText.toDoubleOrNull() ?: 1000.0
                        availableBalance += amount
                        showDepositDialog = false
                        depositAmountText = ""
                        onShowFeedback("M-Pesa STK push completed! +KES $amount added to wallet.")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF),
                        contentColor = Color.White
                    )
                ) {
                    Text("Pay with M-Pesa", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDepositDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Withdraw Dialog
    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            title = { Text("Withdraw to M-Pesa", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Disburse available wallet funds directly to your mobile money number with zero transaction fees for students.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    OutlinedTextField(
                        value = withdrawPhoneText,
                        onValueChange = { withdrawPhoneText = it },
                        label = { Text("M-Pesa Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = withdrawAmountText,
                        onValueChange = { withdrawAmountText = it },
                        label = { Text("Amount (KES)") },
                        placeholder = { Text("Max KES ${String.format("%,.2f", availableBalance)}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = withdrawAmountText.toDoubleOrNull() ?: 500.0
                        if (amount <= availableBalance) {
                            availableBalance -= amount
                            showWithdrawDialog = false
                            withdrawAmountText = ""
                            onShowFeedback("Withdrawal initiated! KES $amount sent to $withdrawPhoneText.")
                        } else {
                            onShowFeedback("Insufficient available balance.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F172A),
                        contentColor = Color.White
                    )
                ) {
                    Text("Confirm Payout", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false }) { Text("Cancel") }
            }
        )
    }

    // SafePay Info Modal
    if (showSafePayInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSafePayInfoDialog = false },
            icon = {
                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(32.dp))
            },
            title = { Text("How SafePay Protection Works", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "1. When you buy a textbook, gadget, or service, your payment is held safely — NOT sent to the seller yet.",
                        fontSize = 13.sp,
                        color = Color(0xFF334155)
                    )
                    Text(
                        "2. You meet up safely at an agreed campus spot (Library, Quad, Student Center).",
                        fontSize = 13.sp,
                        color = Color(0xFF334155)
                    )
                    Text(
                        "3. Inspect the item in person. Once completely satisfied, tap 'Confirm Receipt & Release Payment'.",
                        fontSize = 13.sp,
                        color = Color(0xFF334155)
                    )
                    Text(
                        "4. If there is an issue, student support helps you and refunds 100% of your money.",
                        fontSize = 13.sp,
                        color = Color(0xFF334155)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSafePayInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF),
                        contentColor = Color.White
                    )
                ) {
                    Text("Got It", color = Color.White)
                }
            }
        )
    }
}
