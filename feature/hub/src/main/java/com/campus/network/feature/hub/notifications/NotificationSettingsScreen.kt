package com.campus.network.feature.hub.notifications

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class NotificationSettingItem(
    val id: String,
    val icon: ImageVector,
    val title: String,
    var subtitle: String,
    val deliveryPreference: String = "In-app, Push",
    val audiencePreference: String = "Everyone"
)

/**
 * Screen matching Reference Image 2 precisely:
 * - Top bar with Back arrow and centered "Notifications" title.
 * - Top row: "Enable push notifications" with phone vibration icon and switch.
 * - Categorized notification settings rows with vector icon, bold title, subtitle, and right chevron.
 * - Tailored for Campus Network features (Likes, Mentions, SafePay, Gigs, Messages, Flash Meets, etc.).
 * - OMITTED bottom navigation from Image 2 as strictly requested by user.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var pushNotificationsEnabled by remember {
        mutableStateOf(
            com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.isPushEnabled(context) &&
                com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.hasNotificationPermission(context)
        )
    }

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        pushNotificationsEnabled = granted
        com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.setPushEnabled(context, granted)
        scope.launch {
            if (granted) {
                snackbarHostState.showSnackbar("Push notifications enabled")
            } else {
                snackbarHostState.showSnackbar("Notification permission was denied in system settings")
            }
        }
    }

    val onTogglePushNotifications: (Boolean) -> Unit = { targetEnabled ->
        if (targetEnabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                pushNotificationsEnabled = true
                com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.setPushEnabled(context, true)
            }
        } else {
            pushNotificationsEnabled = targetEnabled
            com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.setPushEnabled(context, targetEnabled)
        }
    }

    // Selected item for preference customization sheet
    var activeEditingItem by remember { mutableStateOf<NotificationSettingItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Items tailored to our campus app matching the layout of Image 2
    val settingsList = remember {
        mutableStateListOf(
            NotificationSettingItem(
                id = "likes",
                icon = Icons.Outlined.FavoriteBorder,
                title = "Likes",
                subtitle = "In-app, Push, Everyone"
            ),
            NotificationSettingItem(
                id = "followers",
                icon = Icons.Outlined.PersonAdd,
                title = "New followers",
                subtitle = "In-app, Everyone"
            ),
            NotificationSettingItem(
                id = "replies",
                icon = Icons.Outlined.ChatBubbleOutline,
                title = "Replies",
                subtitle = "In-app, Push, Everyone"
            ),
            NotificationSettingItem(
                id = "mentions",
                icon = Icons.Outlined.AlternateEmail,
                title = "Mentions",
                subtitle = "In-app, Push, Everyone"
            ),
            NotificationSettingItem(
                id = "quotes",
                icon = Icons.Outlined.FormatQuote,
                title = "Quotes",
                subtitle = "In-app, Push, Everyone"
            ),
            NotificationSettingItem(
                id = "reposts",
                icon = Icons.Outlined.Repeat,
                title = "Reposts",
                subtitle = "In-app, Push, Everyone"
            ),
            NotificationSettingItem(
                id = "safepay",
                icon = Icons.Outlined.ShoppingBag,
                title = "SafePay & Orders",
                subtitle = "In-app, Push, Everyone"
            ),
            NotificationSettingItem(
                id = "gigs",
                icon = Icons.Outlined.WorkOutline,
                title = "Campus Gigs & Tasks",
                subtitle = "In-app, Push, Everyone"
            ),
            NotificationSettingItem(
                id = "activity",
                icon = Icons.Outlined.NotificationsNone,
                title = "Activity from others",
                subtitle = "In-app, Push"
            ),
            NotificationSettingItem(
                id = "repost_likes",
                icon = Icons.Outlined.FavoriteBorder,
                title = "Likes of your reposts",
                subtitle = "In-app, Push, Everyone"
            ),
            NotificationSettingItem(
                id = "repost_reposts",
                icon = Icons.Outlined.Repeat,
                title = "Reposts of your reposts",
                subtitle = "In-app, Push, Everyone"
            ),
            NotificationSettingItem(
                id = "servers",
                icon = Icons.Outlined.Forum,
                title = "Server & Channels",
                subtitle = "In-app, Push, Everyone"
            ),
            NotificationSettingItem(
                id = "flash_meets",
                icon = Icons.Outlined.Place,
                title = "Flash Meets & Quad alerts",
                subtitle = "In-app, Push"
            ),
            NotificationSettingItem(
                id = "everything_else",
                icon = Icons.Outlined.Tune,
                title = "Everything else",
                subtitle = "In-app, Push"
            )
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.White // Pure white matching Reference Image 2
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top App Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF14171A),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "Notifications",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF14171A),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Top Section: Enable push notifications
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onTogglePushNotifications(!pushNotificationsEnabled)
                                }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Phone Vibration Icon matching Reference Image 2
                            PhoneVibrationIcon(
                                tint = Color(0xFF14171A),
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(18.dp))

                            Text(
                                text = "Enable push notifications",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF14171A),
                                modifier = Modifier.weight(1f)
                            )

                            Switch(
                                checked = pushNotificationsEnabled,
                                onCheckedChange = { onTogglePushNotifications(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF007AFF),
                                    uncheckedThumbColor = Color(0xFFF4F5F7),
                                    uncheckedTrackColor = Color(0xFFD1D5DB)
                                )
                            )
                        }

                        HorizontalDivider(
                            thickness = 0.8.dp,
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.padding(start = 20.dp)
                        )
                    }

                    // Notification Setting Rows
                    items(settingsList, key = { it.id }) { setting ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activeEditingItem = setting
                                }
                                .padding(horizontal = 20.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left outline icon
                            Icon(
                                imageVector = setting.icon,
                                contentDescription = setting.title,
                                tint = Color(0xFF14171A),
                                modifier = Modifier.size(23.dp)
                            )

                            Spacer(modifier = Modifier.width(18.dp))

                            // Title & Subtitle column
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = setting.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF14171A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = setting.subtitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF64748B)
                                )
                            }

                            // Right Chevron
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            // Snackbar Host
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) { data ->
                com.campus.network.core.ui.components.CampusSuccessSnackBanner(
                    message = data.visuals.message,
                    onDismiss = { data.dismiss() }
                )
            }
        }

        // Customization Bottom Sheet for Individual Notification Preference
        activeEditingItem?.let { currentSetting ->
            ModalBottomSheet(
                onDismissRequest = { activeEditingItem = null },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 36.dp)
                ) {
                    Text(
                        text = "${currentSetting.title} Preferences",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF14171A)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Notification Channel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val channels = listOf("In-app, Push", "In-app only", "Off")
                    channels.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val idx = settingsList.indexOfFirst { it.id == currentSetting.id }
                                    if (idx != -1) {
                                        val audiencePart = currentSetting.subtitle.substringAfterLast(", ", "Everyone")
                                        val newSubtitle = if (option == "Off") "Off" else "$option, $audiencePart"
                                        settingsList[idx] = currentSetting.copy(subtitle = newSubtitle)
                                    }
                                    activeEditingItem = null
                                    scope.launch {
                                        snackbarHostState.showSnackbar("${currentSetting.title} updated to $option")
                                    }
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSetting.subtitle.startsWith(option),
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF007AFF))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = option,
                                fontSize = 15.sp,
                                color = Color(0xFF14171A)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Audience Filter",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val audiences = listOf("Everyone", "People you follow", "Nobody")
                    audiences.forEach { aud ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val idx = settingsList.indexOfFirst { it.id == currentSetting.id }
                                    if (idx != -1) {
                                        val channelPart = currentSetting.subtitle.substringBeforeLast(",", "In-app, Push")
                                        val newSubtitle = "$channelPart, $aud"
                                        settingsList[idx] = currentSetting.copy(subtitle = newSubtitle)
                                    }
                                    activeEditingItem = null
                                    scope.launch {
                                        snackbarHostState.showSnackbar("${currentSetting.title} audience: $aud")
                                    }
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSetting.subtitle.contains(aud),
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF007AFF))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = aud,
                                fontSize = 15.sp,
                                color = Color(0xFF14171A)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom vector icon matching the phone vibration icon in Reference Image 2:
 * Phone outline with vibration ripples on left and right side.
 */
@Composable
private fun PhoneVibrationIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Central Phone Rectangle
        val phoneW = w * 0.46f
        val phoneH = h * 0.88f
        val phoneX = (w - phoneW) / 2f
        val phoneY = (h - phoneH) / 2f

        drawRoundRect(
            color = tint,
            topLeft = Offset(phoneX, phoneY),
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(4.dp.toPx()),
            style = Stroke(width = 1.8.dp.toPx())
        )

        // Left vibration wave
        val leftArcPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(phoneX - 3.5.dp.toPx(), phoneY + phoneH * 0.28f)
            cubicTo(
                phoneX - 6.5.dp.toPx(), phoneY + phoneH * 0.40f,
                phoneX - 6.5.dp.toPx(), phoneY + phoneH * 0.60f,
                phoneX - 3.5.dp.toPx(), phoneY + phoneH * 0.72f
            )
        }
        drawPath(leftArcPath, color = tint, style = Stroke(width = 1.8.dp.toPx()))

        // Right vibration wave
        val rightArcPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(phoneX + phoneW + 3.5.dp.toPx(), phoneY + phoneH * 0.28f)
            cubicTo(
                phoneX + phoneW + 6.5.dp.toPx(), phoneY + phoneH * 0.40f,
                phoneX + phoneW + 6.5.dp.toPx(), phoneY + phoneH * 0.60f,
                phoneX + phoneW + 3.5.dp.toPx(), phoneY + phoneH * 0.72f
            )
        }
        drawPath(rightArcPath, color = tint, style = Stroke(width = 1.8.dp.toPx()))
    }
}
