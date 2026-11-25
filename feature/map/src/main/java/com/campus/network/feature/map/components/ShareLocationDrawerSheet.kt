package com.campus.network.feature.map.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.core.model.CampusSecurityPolicy
import com.campus.network.core.model.LocationPrivacyScope
import com.campus.network.core.ui.components.CampusSuccessSnackBanner
import com.campus.network.core.ui.components.HomescreenOrange
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareLocationDrawerSheet(
    userName: String = "",
    shareUrl: String? = null,
    isSharingActive: Boolean = false,
    scope: LocationPrivacyScope = LocationPrivacyScope.VERIFIED_CAMPUS_PEERS,
    onScopeChange: (LocationPrivacyScope) -> Unit = {},
    onStartSharing: () -> Unit = {},
    onDismiss: () -> Unit,
    onEndSharing: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }

    val canBroadcast = isSharingActive && scope.allowsCoordinatesBroadcast && !shareUrl.isNullOrBlank()
    val subjectName = userName.ifBlank { "Your" }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(3000)
            isCopied = false
        }
    }

    fun copyUrl() {
        if (!canBroadcast || shareUrl.isNullOrBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Location Link", shareUrl)
        clipboard.setPrimaryClip(clip)
        isCopied = true
    }

    fun shareUrlExternally() {
        if (!canBroadcast || shareUrl.isNullOrBlank()) return
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "View $subjectName live campus location on Patriarch: $shareUrl"
            )
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Location Link"))
        isCopied = true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HomescreenOrange,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFCBD5E1))
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (canBroadcast) Color(0xFFDCFCE7) else Color(0xFFF8FAFC)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (canBroadcast) Icons.Default.ShareLocation else Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = if (canBroadcast) Color(0xFF15803D) else Color(0xFF0F172A),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (canBroadcast) "Live location active" else "Share location",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (scope == LocationPrivacyScope.GHOST_MODE_HIDDEN || !isSharingActive) {
                    "Choose who can see your live location on campus. You can revoke access at any time."
                } else {
                    "Streaming live to authorized peers under ${scope.badgeLabel}."
                },
                fontSize = 14.sp,
                color = Color(0xFF475569),
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LocationPrivacyScope.values().forEach { option ->
                val selected = option == scope
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (selected) Color(0xFF014784) else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { onScopeChange(option) }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(option.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text(option.subtitle, fontSize = 12.sp, color = Color(0xFF64748B), lineHeight = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            CampusSecurityPolicy.getAudienceRulesForScope(scope).forEach { rule ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (rule.isAuthorized) Color(0xFF059669) else Color(0xFFDC2626))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(rule.groupName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        Text(rule.accessDescription, fontSize = 11.5.sp, color = Color(0xFF64748B))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val displayLinkText = when {
                !scope.allowsCoordinatesBroadcast -> "Sharing paused (Ghost Mode)"
                !isSharingActive || shareUrl.isNullOrBlank() -> "Start sharing to generate a private session link"
                else -> shareUrl
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (canBroadcast) Color(0xFFF8FAFC) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = canBroadcast) { copyUrl() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = displayLinkText,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (canBroadcast) Color(0xFF0F172A) else Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (canBroadcast) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Link",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isSharingActive && scope.allowsCoordinatesBroadcast) {
                Button(
                    onClick = onStartSharing,
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F172A),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShareLocation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Live Sharing",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Button(
                    onClick = { shareUrlExternally() },
                    enabled = canBroadcast,
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F172A),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF94A3B8),
                        disabledContentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (canBroadcast) "Share link" else "Sharing Paused (Ghost Mode)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedContent(
                targetState = isCopied,
                transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                label = "bannerOrEndSharing"
            ) { copied ->
                if (copied) {
                    CampusSuccessSnackBanner(
                        message = "Link copied",
                        contentPadding = false,
                        onDismiss = { isCopied = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                } else if (isSharingActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = {
                                onEndSharing()
                                onDismiss()
                            }
                        ) {
                            Text(
                                text = "End sharing for all",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
