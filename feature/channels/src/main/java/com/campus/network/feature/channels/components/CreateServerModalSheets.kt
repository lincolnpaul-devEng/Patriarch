package com.campus.network.feature.channels.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.campus.network.core.network.CloudinaryUploader
import kotlinx.coroutines.launch

/**
 * Screen modes for Server Creation and Joining bottom sheets.
 */
enum class ServerSheetMode {
    CREATE_SERVER,
    CUSTOMIZE_SERVER,
    JOIN_SERVER
}

/**
 * Pixel-Perfect Modal Sheet hosting "Create Your Server", "Customize Your Server",
 * and "Join an existing server".
 *
 * ZERO EMOJI POLICY: Strictly retro pixel-art canvas illustrations and typography.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateServerModalSheet(
    mode: ServerSheetMode,
    onDismiss: () -> Unit,
    onNavigateToJoin: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onCreateServer: (serverName: String, category: String) -> Unit,
    onJoinInvite: (inviteCode: String) -> Unit,
    onCreateServerFull: ((serverName: String, category: String, description: String, avatarResName: String, bannerResName: String?, onResult: (Result<Unit>) -> Unit) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentMode by remember(mode) { mutableStateOf(mode) }
    var selectedTemplateTitle by remember { mutableStateOf("My") }
    var selectedCategory by remember { mutableStateOf("Community") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null,
        modifier = Modifier.fillMaxSize()
    ) {
        when (currentMode) {
            ServerSheetMode.CREATE_SERVER -> {
                CreateYourServerContent(
                    onDismiss = onDismiss,
                    onSelectTemplate = { templateTitle, category ->
                        selectedTemplateTitle = templateTitle
                        selectedCategory = category
                        currentMode = ServerSheetMode.CUSTOMIZE_SERVER
                    },
                    onJoinServerClick = onNavigateToJoin
                )
            }
            ServerSheetMode.CUSTOMIZE_SERVER -> {
                CustomizeServerContent(
                    initialName = if (selectedTemplateTitle.contains("My")) "My Server" else "$selectedTemplateTitle Server",
                    initialCategory = selectedCategory,
                    onBackClick = { currentMode = ServerSheetMode.CREATE_SERVER },
                    onDismiss = onDismiss,
                    onCreateServer = { name, category, description, avatarResName, bannerResName, onResult ->
                        if (onCreateServerFull != null) {
                            onCreateServerFull(name, category, description, avatarResName, bannerResName, onResult)
                        } else {
                            onCreateServer(name, category)
                            onResult(Result.success(Unit))
                            onDismiss()
                        }
                    }
                )
            }
            ServerSheetMode.JOIN_SERVER -> {
                JoinExistingServerContent(
                    onBackClick = onNavigateToCreate,
                    onJoinWithInvite = onJoinInvite,
                    onJoinStudentHub = {
                        onJoinInvite("campus-student-hub")
                    }
                )
            }
        }
    }
}

/**
 * SCREEN 1: "Create Your Server" matching uploaded_media_0_1792149447421.png
 */
@Composable
fun CreateYourServerContent(
    onDismiss: () -> Unit,
    onSelectTemplate: (title: String, category: String) -> Unit,
    onJoinServerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top-left Close Button 'X'
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF1E1F22),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title: Create Your Server
        Text(
            text = "Create Your Server",
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1E1F22),
            textAlign = TextAlign.Center,
            letterSpacing = (-0.4).sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = "Your server is where you and your friends hang out. Make yours and start talking.",
            fontSize = 14.5.sp,
            color = Color(0xFF4E5058),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Card: "Create My Own"
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onSelectTemplate("My", "Custom") },
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF2F3F5)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PixelArtHatIcon(modifier = Modifier.size(34.dp))

                Text(
                    text = "Create My Own",
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1F22)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section Label: "Start from a template"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Start from a template",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4E5058)
            )
        }

        // Grouped Card Container for the 6 Templates
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF2F3F5)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Gaming
                TemplateListItem(
                    icon = { PixelArtGamepadIcon(modifier = Modifier.size(30.dp)) },
                    title = "Gaming",
                    onClick = { onSelectTemplate("Gaming Squad", "Gaming") }
                )
                TemplateDivider()

                // 2. School Club
                TemplateListItem(
                    icon = { PixelArtBookIcon(modifier = Modifier.size(30.dp)) },
                    title = "School Club",
                    onClick = { onSelectTemplate("Campus Club", "Education") }
                )
                TemplateDivider()

                // 3. Study Group
                TemplateListItem(
                    icon = { PixelArtFruitIcon(modifier = Modifier.size(30.dp)) },
                    title = "Study Group",
                    onClick = { onSelectTemplate("Study Group", "Education") }
                )
                TemplateDivider()

                // 4. Friends
                TemplateListItem(
                    icon = { PixelArtHeartIcon(modifier = Modifier.size(30.dp)) },
                    title = "Friends",
                    onClick = { onSelectTemplate("Friends Hangout", "Social") }
                )
                TemplateDivider()

                // 5. Artists & Creators
                TemplateListItem(
                    icon = { PixelArtPaletteIcon(modifier = Modifier.size(30.dp)) },
                    title = "Artists & Creators",
                    onClick = { onSelectTemplate("Artists Studio", "Art & Design") }
                )
                TemplateDivider()

                // 6. Local Community
                TemplateListItem(
                    icon = { PixelArtLeafIcon(modifier = Modifier.size(30.dp)) },
                    title = "Local Community",
                    onClick = { onSelectTemplate("Campus Local Community", "Community") }
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Bottom Section: "Have an invite already?"
        Text(
            text = "Have an invite already?",
            fontSize = 18.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E1F22),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        // "Join a Server" Button (Solid blurple)
        Button(
            onClick = onJoinServerClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Join a Server",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

/**
 * SCREEN 2: "Customize your server"
 * Lets the user configure the server name, avatar preset, topic description, and category.
 */
@Composable
fun CustomizeServerContent(
    initialName: String,
    initialCategory: String,
    onBackClick: () -> Unit,
    onDismiss: () -> Unit,
    onCreateServer: (name: String, category: String, description: String, avatarResName: String, bannerResName: String?, onResult: (Result<Unit>) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var serverName by remember { mutableStateOf(initialName) }
    var serverDescription by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var selectedAvatar by remember { mutableStateOf("server_gradient") }
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBanner by remember { mutableStateOf<String?>("banner_gradient_cyber") }
    var selectedBannerUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedAvatarUri = uri
            submitError = null
        }
    }

    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedBannerUri = uri
            submitError = null
        }
    }

    val presetAvatars = remember {
        listOf(
            "server_gradient",
            "server_codesistency",
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
            "banner_welcome_perk" to "Welcome"
        )
    }

    val categories = remember {
        listOf("Community", "Gaming", "Education", "Study Jam", "Tech & AI", "Social")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row: Back button & Close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1E1F22),
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF1E1F22),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Customize your server",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1E1F22),
            textAlign = TextAlign.Center,
            letterSpacing = (-0.4).sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Give your new server a personality with a name, an icon, and a banner. You can always change them later.",
            fontSize = 14.5.sp,
            color = Color(0xFF4E5058),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Avatar selector squircle with camera badge & upload support
        Box(
            modifier = Modifier
                .size(80.dp)
                .clickable { avatarPickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFE3E5E8))
                    .border(2.dp, Color(0xFF5865F2).copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (selectedAvatarUri != null) {
                    AsyncImage(
                        model = selectedAvatarUri,
                        contentDescription = "Selected Server Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (selectedAvatar.startsWith("http")) {
                    AsyncImage(
                        model = selectedAvatar,
                        contentDescription = "Selected Server Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val avatarResId = resolveServerAvatarDrawable(selectedAvatar)
                    Image(
                        painter = painterResource(id = avatarResId),
                        contentDescription = "Selected Server Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (isSubmitting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // Camera / Upload Badge in bottom right corner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF5865F2))
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Upload Icon",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Upload custom image button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { avatarPickerLauncher.launch("image/*") }
                .background(Color(0xFFF2F3F5))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = Color(0xFF5865F2),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (selectedAvatarUri != null || selectedAvatar.startsWith("http")) "Custom Icon Selected" else "Upload Custom Icon",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF5865F2)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Choose an Icon Preset",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF5C6470)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Preset Avatar Choices
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            presetAvatars.forEach { avatarName ->
                val isSelected = selectedAvatar == avatarName
                val resId = resolveServerAvatarDrawable(avatarName)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) Color(0xFF5865F2) else Color(0xFFF2F3F5))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF5865F2) else Color(0xFFE3E5E8),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            selectedAvatar = avatarName
                            selectedAvatarUri = null
                        }
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = avatarName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Banner Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SERVER BANNER",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4E5058),
                letterSpacing = 0.5.sp
            )
            if (selectedBanner != null) {
                Text(
                    text = "Remove",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFDA373C),
                    modifier = Modifier.clickable { selectedBanner = null }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Banner Preview Box
        val bannerDrawableId = resolveServerBannerDrawable(selectedBanner)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF2B2D31))
                .clickable { bannerPickerLauncher.launch("image/*") }
        ) {
            if (selectedBannerUri != null) {
                AsyncImage(
                    model = selectedBannerUri,
                    contentDescription = "Server Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (selectedBanner?.startsWith("http") == true) {
                AsyncImage(
                    model = selectedBanner,
                    contentDescription = "Server Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (bannerDrawableId != null) {
                Image(
                    painter = painterResource(id = bannerDrawableId),
                    contentDescription = "Server Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF2B2D31), Color(0xFF1E1F22))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No Banner Selected",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Upload Overlay / Button Badge
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.65f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload Banner",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Upload Banner",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (isSubmitting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Banner Presets Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presetBanners.forEach { (presetKey, label) ->
                val isSelected = selectedBanner == presetKey
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFF5865F2) else Color(0xFFF2F3F5),
                    modifier = Modifier.clickable {
                        selectedBanner = presetKey
                        selectedBannerUri = null
                    }
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF1E1F22),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Server Name Input
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "SERVER NAME",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4E5058),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF2F3F5)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = serverName,
                        onValueChange = { serverName = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 15.5.sp,
                            color = Color(0xFF1E1F22),
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    if (serverName.isNotEmpty()) {
                        IconButton(
                            onClick = { serverName = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFF5C6470),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Category Selector Chips
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "CATEGORY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4E5058),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isCatSelected = selectedCategory.equals(cat, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = if (isCatSelected) Color(0xFF5865F2) else Color(0xFFF2F3F5),
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            fontSize = 13.sp,
                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCatSelected) Color.White else Color(0xFF1E1F22),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Server Description (Optional)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "SERVER TOPIC & GUIDELINES (OPTIONAL)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4E5058),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF2F3F5)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (serverDescription.isEmpty()) {
                        Text(
                            text = "What is this server about? Share your community vision...",
                            fontSize = 14.sp,
                            color = Color(0xFF949BA4)
                        )
                    }
                    BasicTextField(
                        value = serverDescription,
                        onValueChange = { serverDescription = it },
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = Color(0xFF1E1F22),
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "By creating a server, you agree to Campus Network's Community Guidelines and Server Conduct Policies.",
            fontSize = 12.sp,
            color = Color(0xFF5C6470),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        if (submitError != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = submitError!!,
                fontSize = 12.5.sp,
                color = Color(0xFFDA373C),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val finalName = serverName.trim().ifEmpty { "New Campus Server" }
                isSubmitting = true
                submitError = null
                coroutineScope.launch {
                    var finalAvatar = selectedAvatar
                    var finalBanner = selectedBanner

                    if (selectedAvatarUri != null) {
                        val upRes = CloudinaryUploader().uploadFromUri(context, selectedAvatarUri!!)
                        if (upRes.isSuccess) {
                            finalAvatar = upRes.getOrThrow()
                        } else {
                            isSubmitting = false
                            submitError = "Failed to upload avatar: ${upRes.exceptionOrNull()?.localizedMessage ?: "Network error"}"
                            return@launch
                        }
                    }

                    if (selectedBannerUri != null) {
                        val upRes = CloudinaryUploader().uploadFromUri(context, selectedBannerUri!!)
                        if (upRes.isSuccess) {
                            finalBanner = upRes.getOrThrow()
                        } else {
                            isSubmitting = false
                            submitError = "Failed to upload banner: ${upRes.exceptionOrNull()?.localizedMessage ?: "Network error"}"
                            return@launch
                        }
                    }

                    onCreateServer(finalName, selectedCategory, serverDescription.trim(), finalAvatar, finalBanner) { result ->
                        isSubmitting = false
                        if (result.isSuccess) {
                            onDismiss()
                        } else {
                            submitError = "Failed to create server on cloud: ${result.exceptionOrNull()?.localizedMessage ?: "Unknown error"}"
                        }
                    }
                }
            },
            enabled = !isSubmitting,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Creating Server...",
                    color = Color.White,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "Create Server",
                    color = Color.White,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

fun resolveServerBannerDrawable(name: String?): Int? {
    return when (name) {
        "banner_gradient_cyber" -> com.campus.network.core.ui.R.drawable.banner_gradient_cyber
        "banner_campus_quad" -> com.campus.network.core.ui.R.drawable.banner_campus_quad
        "banner_tech_neon" -> com.campus.network.core.ui.R.drawable.banner_tech_neon
        "banner_welcome_perk" -> com.campus.network.core.ui.R.drawable.banner_welcome_perk
        else -> null
    }
}

fun resolveServerAvatarDrawable(name: String): Int {
    return when (name) {
        "server_codesistency" -> com.campus.network.core.ui.R.drawable.server_codesistency
        "server_owl" -> com.campus.network.core.ui.R.drawable.server_owl
        "server_dragon" -> com.campus.network.core.ui.R.drawable.server_dragon
        "server_curly" -> com.campus.network.core.ui.R.drawable.server_curly
        "server_gradient" -> com.campus.network.core.ui.R.drawable.server_gradient
        "server_ai_brain" -> com.campus.network.core.ui.R.drawable.server_ai_brain
        "server_eureka" -> com.campus.network.core.ui.R.drawable.server_eureka
        "server_nvidia" -> com.campus.network.core.ui.R.drawable.server_nvidia
        "server_logo2" -> com.campus.network.core.ui.R.drawable.server_logo2
        "server_avatar2" -> com.campus.network.core.ui.R.drawable.server_avatar2
        "server_w_red" -> com.campus.network.core.ui.R.drawable.server_w_red
        else -> com.campus.network.core.ui.R.drawable.server_gradient
    }
}

/**
 * SCREEN 3: "Join an existing server" matching uploaded_media_1_1792149447421.png
 */
@Composable
fun JoinExistingServerContent(
    onBackClick: () -> Unit,
    onJoinWithInvite: (inviteCode: String) -> Unit,
    onJoinStudentHub: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inviteLink by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Top App Bar with Back Arrow and Centered Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1E1F22),
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Join an existing server",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1F22),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 36.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Centered Big Heading
        Text(
            text = "Join an existing server",
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1E1F22),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            letterSpacing = (-0.4).sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = "Enter an invite below to join an existing server.",
            fontSize = 14.5.sp,
            color = Color(0xFF4E5058),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Label: "Invite link"
        Text(
            text = "Invite link",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4E5058)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Input Field Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF2F3F5))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (inviteLink.isEmpty()) {
                Text(
                    text = "https://patriarch.gg/hTKzmak",
                    color = Color(0xFF949BA4),
                    fontSize = 15.sp
                )
            }

            BasicTextField(
                value = inviteLink,
                onValueChange = { inviteLink = it },
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    color = Color(0xFF1E1F22),
                    fontWeight = FontWeight.Normal
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Helper explanation text
        Text(
            text = "Invites should look like https://patriarch.gg/hTKzmak, hTKzmak, or https://patriarch.gg/wumpus-friends",
            fontSize = 13.5.sp,
            color = Color(0xFF4E5058),
            lineHeight = 18.5.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        // "Join with Invite Link" Button (Solid blurple)
        Button(
            onClick = {
                val code = if (inviteLink.isNotBlank()) inviteLink.trim() else "campus-invite"
                onJoinWithInvite(code)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Join with Invite Link",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // "OR" Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Color(0xFFE3E5E8),
                thickness = 1.dp
            )
            Text(
                text = "OR",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5C5E66),
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Color(0xFFE3E5E8),
                thickness = 1.dp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // "Join a Student Hub" Button (Light gray)
        Button(
            onClick = onJoinStudentHub,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F3F5)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Join a Student Hub",
                color = Color(0xFF1E1F22),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Reusable list item for template choices.
 */
@Composable
private fun TemplateListItem(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        icon()

        Text(
            text = title,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E1F22)
        )
    }
}

@Composable
private fun TemplateDivider() {
    HorizontalDivider(
        color = Color(0xFFE3E5E8),
        thickness = 1.dp,
        modifier = Modifier.padding(start = 62.dp)
    )
}

// =======================================================================
// RETRO 8-BIT PIXEL ART ICONS (CANVAS)
// Pixel-perfect reproductions of patriarch's retro icon set
// Strictly ZERO EMOJIS!
// =======================================================================

/**
 * Helper to render an ASCII pixel map onto a Compose Canvas.
 */
private fun renderPixelMap(
    canvas: androidx.compose.ui.graphics.drawscope.DrawScope,
    map: List<String>,
    palette: Map<Char, Color>
) {
    val rows = map.size
    val cols = map.firstOrNull()?.length ?: return
    val pixelW = canvas.size.width / cols
    val pixelH = canvas.size.height / rows

    for (r in 0 until rows) {
        val rowStr = map[r]
        for (c in 0 until cols) {
            val ch = rowStr[c]
            val color = palette[ch]
            if (color != null && color != Color.Transparent) {
                canvas.drawRect(
                    color = color,
                    topLeft = Offset(c * pixelW, r * pixelH),
                    size = Size(pixelW + 0.5f, pixelH + 0.5f)
                )
            }
        }
    }
}

/**
 * Pixel Party Hat icon for "Create My Own"
 */
@Composable
fun PixelArtHatIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val map = listOf(
            "...XXXX...",
            "..XGGGGX..",
            ".XGGGGGGX.",
            ".XGGYYGGX.",
            "XGYYYYYYGX",
            "XYYPPPPYYX",
            "XPPPPPPPPX",
            "XPPPPPPPPX",
            ".XXXXXXXX."
        )
        val palette = mapOf(
            'X' to Color(0xFF1E1F22),
            'G' to Color(0xFF23A55A),
            'Y' to Color(0xFFFEE75C),
            'P' to Color(0xFFEB459E)
        )
        renderPixelMap(this, map, palette)
    }
}

/**
 * Pixel Gamepad icon for "Gaming"
 */
@Composable
fun PixelArtGamepadIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val map = listOf(
            "..XXXXXXXX..",
            ".XBBBBBBBBX.",
            "XBBWBBBBWBBX",
            "XBWWWBBRRBBR",
            "XBBWBBBBRRBX",
            "XBBBBBBBBBBX",
            ".XBBXXXXBBX.",
            ".XX......XX."
        )
        val palette = mapOf(
            'X' to Color(0xFF1E1F22),
            'B' to Color(0xFF5865F2),
            'W' to Color.White,
            'R' to Color(0xFFED4245)
        )
        renderPixelMap(this, map, palette)
    }
}

/**
 * Pixel Book icon for "School Club"
 */
@Composable
fun PixelArtBookIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val map = listOf(
            ".XXXXXXXX.",
            "XPPPPPPPPX",
            "XPWWWWWWPX",
            "XPWWWWWWPX",
            "XPPPPPPPPX",
            "XPWWWWWWPX",
            "XPRRRPPWPX",
            ".XXXXXXXX."
        )
        val palette = mapOf(
            'X' to Color(0xFF1E1F22),
            'P' to Color(0xFF5865F2),
            'W' to Color.White,
            'R' to Color(0xFFEB459E)
        )
        renderPixelMap(this, map, palette)
    }
}

/**
 * Pixel Peach / Apple Fruit icon for "Study Group"
 */
@Composable
fun PixelArtFruitIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val map = listOf(
            "....XX....",
            "...XGGX...",
            "..XRRRRX..",
            ".XRRWWRRX.",
            "XRRRRRRRRX",
            "XRRRYYYRRX",
            ".XRYYYYYX.",
            "..XXXXXX.."
        )
        val palette = mapOf(
            'X' to Color(0xFF1E1F22),
            'G' to Color(0xFF57F287),
            'R' to Color(0xFFED4245),
            'W' to Color.White,
            'Y' to Color(0xFFFEE75C)
        )
        renderPixelMap(this, map, palette)
    }
}

/**
 * Pixel Heart icon for "Friends"
 */
@Composable
fun PixelArtHeartIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val map = listOf(
            ".XXX...XXX.",
            "XWWWX.XPPPX",
            "XWWPPXPPPPX",
            "XPPPPPPPPPX",
            ".XPPPPPPPX.",
            "..XPPPPPX..",
            "...XPPPX...",
            "....XPX....",
            ".....X....."
        )
        val palette = mapOf(
            'X' to Color(0xFF1E1F22),
            'P' to Color(0xFFEB459E),
            'W' to Color.White
        )
        renderPixelMap(this, map, palette)
    }
}

/**
 * Pixel Artist Palette icon for "Artists & Creators"
 */
@Composable
fun PixelArtPaletteIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val map = listOf(
            "..XXXXXX..",
            ".XOOOOOOX.",
            "XOROOYOOOX",
            "XOOOOOOGOX",
            "XOBBOOOOOX",
            ".XOOOOOOX.",
            "..XXXXXX.."
        )
        val palette = mapOf(
            'X' to Color(0xFF1E1F22),
            'O' to Color(0xFFE5A93C),
            'R' to Color(0xFFED4245),
            'Y' to Color(0xFFFEE75C),
            'G' to Color(0xFF57F287),
            'B' to Color(0xFF5865F2)
        )
        renderPixelMap(this, map, palette)
    }
}

/**
 * Pixel Leaf icon for "Local Community"
 */
@Composable
fun PixelArtLeafIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val map = listOf(
            "....XXXXX.",
            "...XGGGGGX",
            "..XGGWWGGX",
            ".XGGWGGGGX",
            "XGGGGGGGGX",
            ".XGGGGGGX.",
            "..XGGGGX..",
            "...XGGX...",
            "....XX...."
        )
        val palette = mapOf(
            'X' to Color(0xFF1E1F22),
            'G' to Color(0xFF57F287),
            'W' to Color.White
        )
        renderPixelMap(this, map, palette)
    }
}
