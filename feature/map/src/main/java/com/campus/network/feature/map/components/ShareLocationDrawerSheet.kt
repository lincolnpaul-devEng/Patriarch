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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
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
import kotlinx.coroutines.delay

/**
 * Share Location Drawer Sheet matching Reference Image 0 & Image 1:
 * - Implemented with ModalBottomSheet to completely hide the bottom tab navigation
 * - Top drag handle pill
 * - Center group/person add icon
 * - "Share location" title
 * - "Anyone with this link can view and share [Name]'s location until you end sharing."
 * - Rounded input container with shareable URL & copy button
 * - Black "Share link" button
 * - Red "End sharing for all" button (swapped smoothly with green success banner upon copying)
 * - Green "Link copied" snack banner matching Image 1 exactly
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareLocationDrawerSheet(
    userName: String = "",
    shareUrl: String = "https://community.tryfi.com/share/loc_849204",
    scope: LocationPrivacyScope = LocationPrivacyScope.VERIFIED_CAMPUS_PEERS,
    onScopeChange: (LocationPrivacyScope) -> Unit = {},
    onDismiss: () -> Unit,
    onEndSharing: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }

    // Auto-dismiss copied banner after 3 seconds
    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(3000)
            isCopied = false
        }
    }

    fun copyUrl() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Location Link", shareUrl)
        clipboard.setPrimaryClip(clip)
        isCopied = true
    }

    fun shareUrlExternally() {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "View and share $userName's live campus location: $shareUrl")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Location Link")
        context.startActivity(shareIntent)
        isCopied = true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFCBD5E1))
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Center User Add Icon with Sub-badge
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF8FAFC)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title: "Share location"
            Text(
                text = "Share location",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Choose who can see ${userName}’s live location. You can change or revoke this at any time.",
                fontSize = 14.5.sp,
                color = Color(0xFF64748B),
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
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

            // Link container box with copy icon
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { copyUrl() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = shareUrl,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Link",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Black Pill "Share link" Button
            Button(
                onClick = { shareUrlExternally() },
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share link",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Either green success banner or red "End sharing for all" button (Image 0 vs Image 1)
            AnimatedContent(
                targetState = isCopied,
                transitionSpec = {
                    fadeIn().togetherWith(fadeOut())
                },
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
                } else {
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
                }
            }
        }
    }
}
