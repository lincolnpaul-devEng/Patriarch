package com.campus.network.feature.channels.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.campus.network.core.network.CloudinaryUploader
import com.campus.network.core.ui.R
import com.campus.network.feature.channels.ServerItem
import kotlinx.coroutines.launch

/**
 * Pixel-Perfect Server Overview & Settings Sheet for Server Owners.
 * Allows owner to edit:
 * - Server Name
 * - Server Description
 * - Server Category
 * - Server Avatar (Presets or Custom Photo Upload)
 * - Server Banner (Presets or Custom Banner Upload)
 *
 * ZERO EMOJI POLICY: Strictly vector icons and typography.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditServerSettingsSheet(
    isOpen: Boolean,
    server: ServerItem,
    onDismiss: () -> Unit,
    onSaveSettings: (name: String, description: String, category: String, avatarResName: String, bannerResName: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var serverName by remember(server.name) { mutableStateOf(server.name) }
    var serverDescription by remember { mutableStateOf("") }
    var selectedCategory by remember(server.categoryText) { mutableStateOf(server.categoryText.ifBlank { "Community" }) }
    var selectedAvatar by remember(server.iconResName) { mutableStateOf(server.iconResName.ifBlank { "server_codesistency" }) }
    var isUploadingAvatar by remember { mutableStateOf(false) }

    var selectedBanner by remember(server.bannerResName) { mutableStateOf(server.bannerResName ?: "banner_gradient_cyber") }
    var isUploadingBanner by remember { mutableStateOf(false) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isUploadingAvatar = true
            coroutineScope.launch {
                val result = CloudinaryUploader().uploadFromUri(context, uri)
                isUploadingAvatar = false
                if (result.isSuccess) {
                    selectedAvatar = result.getOrThrow()
                }
            }
        }
    }

    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isUploadingBanner = true
            coroutineScope.launch {
                val result = CloudinaryUploader().uploadFromUri(context, uri)
                isUploadingBanner = false
                if (result.isSuccess) {
                    selectedBanner = result.getOrThrow()
                }
            }
        }
    }

    val presetAvatars = remember {
        listOf(
            "server_codesistency",
            "server_gradient",
            "server_owl",
            "server_dragon",
            "server_curly",
            "server_ai_brain",
            "server_eureka",
            "server_nvidia",
            "server_logo2",
            "server_avatar2",
            "server_w_red"
        )
    }

    val presetBanners = remember {
        listOf(
            "banner_gradient_cyber" to "Cyber",
            "banner_campus_quad" to "Quad",
            "banner_tech_neon" to "Neon",
            "banner_welcome_perk" to "Welcome",
            "server_banner_clash" to "Clash",
            "server_banner_basi" to "Basi"
        )
    }

    val categories = remember {
        listOf("Community", "Gaming", "Education", "Study Jam", "Tech & AI", "Social")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Header: Close 'X' and Save button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF1E1F22),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "Server Settings",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1F22)
                )

                Button(
                    onClick = {
                        onSaveSettings(serverName, serverDescription, selectedCategory, selectedAvatar, selectedBanner)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                    shape = RoundedCornerShape(8.dp),
                    enabled = serverName.isNotBlank()
                ) {
                    Text(
                        text = "Save",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. SERVER AVATAR ICON
            Text(
                text = "SERVER ICON",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5C6470),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Current Preview
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFF2F3F5))
                        .border(2.dp, Color(0xFF5865F2), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedAvatar.startsWith("http") || selectedAvatar.startsWith("content:")) {
                        AsyncImage(
                            model = selectedAvatar,
                            contentDescription = "Server Icon",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val iconRes = resolveAvatarRes(selectedAvatar)
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = "Server Icon",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Upload button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF2F3F5),
                    modifier = Modifier
                        .clickable { avatarPickerLauncher.launch("image/*") }
                        .height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isUploadingAvatar) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF5865F2)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = Color(0xFF5865F2),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = if (isUploadingAvatar) "Uploading..." else "Upload Photo",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E1F22)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Presets Horizontal Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presetAvatars.forEach { preset ->
                    val isSelected = selectedAvatar == preset
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF5865F2) else Color(0xFFE3E5E8),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedAvatar = preset }
                    ) {
                        Image(
                            painter = painterResource(id = resolveAvatarRes(preset)),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 2. SERVER NAME
            Text(
                text = "SERVER NAME",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5C6470),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF2F3F5),
                modifier = Modifier.fillMaxWidth()
            ) {
                BasicTextField(
                    value = serverName,
                    onValueChange = { serverName = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E1F22)
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. SERVER CATEGORY
            Text(
                text = "SERVER CATEGORY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5C6470),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory.equals(category, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFF5865F2) else Color(0xFFF2F3F5),
                        modifier = Modifier.clickable { selectedCategory = category }
                    ) {
                        Text(
                            text = category,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF5C6470),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. SERVER BANNER
            Text(
                text = "SERVER BANNER",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5C6470),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presetBanners.forEach { (resName, title) ->
                    val isSelected = selectedBanner == resName
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF5865F2) else Color(0xFFF2F3F5),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF5865F2) else Color(0xFFE3E5E8)
                        ),
                        modifier = Modifier
                            .clickable { selectedBanner = resName }
                            .width(110.dp)
                            .height(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF1E1F22)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Upload Custom Banner button
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF2F3F5),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { bannerPickerLauncher.launch("image/*") }
                    .height(44.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isUploadingBanner) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF5865F2)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = Color(0xFF5865F2),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isUploadingBanner) "Uploading Custom Banner..." else "Upload Custom Banner Artwork",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E1F22)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

private fun resolveAvatarRes(name: String): Int {
    return when (name) {
        "server_codesistency" -> R.drawable.server_codesistency
        "server_avatar2" -> R.drawable.server_avatar2
        "server_ai_brain" -> R.drawable.server_ai_brain
        "server_logo2" -> R.drawable.server_logo2
        "server_dragon" -> R.drawable.server_dragon
        "server_curly" -> R.drawable.server_curly
        "server_eureka" -> R.drawable.server_eureka
        "server_nvidia" -> R.drawable.server_nvidia
        "server_owl" -> R.drawable.server_owl
        "server_gradient" -> R.drawable.server_gradient
        "server_w_red" -> R.drawable.server_w_red
        else -> R.drawable.server_codesistency
    }
}
