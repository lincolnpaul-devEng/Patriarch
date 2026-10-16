package com.campus.network.feature.channels.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.campus.network.core.ui.R

/**
 * Data model for server creation templates.
 */
data class ServerTemplate(
    val id: String,
    val title: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val icon: ImageVector,
    val templateCategory: String = "Community"
)

/**
 * Data model for featured communities.
 */
data class FeaturedCommunity(
    val id: String,
    val name: String,
    val isVerified: Boolean = true,
    val verifiedBadgeColor: Color = Color(0xFF23A55A), // Green or Blue badge
    val description: String,
    val onlineCountText: String,
    val membersCountText: String,
    val bannerGradient: Brush,
    val avatarBgColor: Color,
    val avatarDrawableRes: Int? = null,
    val avatarLetter: String = "C"
)

/**
 * Pixel-Perfect Create or Join Server / Discover Communities Panel matching:
 * - uploaded_media_0_1792147196391.png
 * - uploaded_media_1_1792147196391.png
 *
 * ZERO EMOJI POLICY: Strictly vector icons and typography across all UI.
 */
@Composable
fun DiscoverServersPanel(
    onCreateServerSubmit: (name: String, category: String) -> Unit,
    onJoinInviteSubmit: (inviteCode: String) -> Unit,
    onPreviewCommunitySubmit: (serverName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSheetMode by remember { mutableStateOf<ServerSheetMode?>(null) }

    val templates = remember {
        listOf(
            ServerTemplate(
                id = "tmpl_hangout",
                title = "Just to hangout",
                description = "A private hangout for you and your people — no theme required.",
                primaryColor = Color(0xFF5865F2),
                secondaryColor = Color(0xFFA855F7),
                icon = Icons.Default.Handshake,
                templateCategory = "Hangout"
            ),
            ServerTemplate(
                id = "tmpl_gaming",
                title = "For gaming",
                description = "A home base for you to squad up, stream, and talk strategy.",
                primaryColor = Color(0xFF7C3AED),
                secondaryColor = Color(0xFF4F46E5),
                icon = Icons.Default.SportsEsports,
                templateCategory = "Gaming"
            ),
            ServerTemplate(
                id = "tmpl_club",
                title = "School Club",
                description = "For your study group, classroom, or extracurricular campus squad.",
                primaryColor = Color(0xFF0284C7),
                secondaryColor = Color(0xFF0D9488),
                icon = Icons.Default.School,
                templateCategory = "Education"
            ),
            ServerTemplate(
                id = "tmpl_friends",
                title = "Friends & Study",
                description = "Stay in touch with daily chats, calls, and shared moments.",
                primaryColor = Color(0xFFE11D48),
                secondaryColor = Color(0xFFDB2777),
                icon = Icons.Default.Groups,
                templateCategory = "Social"
            )
        )
    }

    val featuredCommunities = remember {
        listOf(
            FeaturedCommunity(
                id = "comm_heartopia",
                name = "Heartopia",
                isVerified = true,
                verifiedBadgeColor = Color(0xFF23A55A),
                description = "Heartopia Official Discord where you can contact official group and communicate with other players here",
                onlineCountText = "90,935 Online",
                membersCountText = "811,139 Members",
                bannerGradient = Brush.linearGradient(
                    colors = listOf(Color(0xFF2D1B4E), Color(0xFF1E293B), Color(0xFF475569))
                ),
                avatarBgColor = Color(0xFFFFD1DC),
                avatarDrawableRes = R.drawable.server_curly
            ),
            FeaturedCommunity(
                id = "comm_coc",
                name = "Clash of Clans",
                isVerified = true,
                verifiedBadgeColor = Color(0xFF5865F2),
                description = "Official Discord server for Clash of Clans. Join the biggest server dedicated to Clash of Clans!",
                onlineCountText = "30,449 Online",
                membersCountText = "458,299 Members",
                bannerGradient = Brush.linearGradient(
                    colors = listOf(Color(0xFF7F1D1D), Color(0xFF431407), Color(0xFF1C1917))
                ),
                avatarBgColor = Color(0xFF1C1917),
                avatarDrawableRes = R.drawable.server_dragon
            ),
            FeaturedCommunity(
                id = "comm_ai_guild",
                name = "Campus Tech & AI Guild",
                isVerified = true,
                verifiedBadgeColor = Color(0xFF23A55A),
                description = "Connect with student software engineers, machine learning researchers, and open-source contributors.",
                onlineCountText = "14,820 Online",
                membersCountText = "92,400 Members",
                bannerGradient = Brush.linearGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF0369A1), Color(0xFF0284C7))
                ),
                avatarBgColor = Color(0xFF0F172A),
                avatarDrawableRes = R.drawable.server_ai_brain
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Screen Main Title
        Text(
            text = "Servers",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E1F22),
            letterSpacing = (-0.4).sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Section 1 Header: Create a place for you and your friends
        Text(
            text = "Create a place for you and your friends",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E1F22),
            letterSpacing = (-0.2).sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Servers are where you and your friends hang out. Make yours and start talking.",
            fontSize = 14.sp,
            color = Color(0xFF4E5058),
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Carousel of Templates (Matching Image 1)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(templates, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    onClick = {
                        activeSheetMode = ServerSheetMode.CREATE_SERVER
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // "Create a server" Button (Solid blurple)
        Button(
            onClick = {
                activeSheetMode = ServerSheetMode.CREATE_SERVER
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Text(
                text = "Create a server",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // "I have an invite link" Button (Light gray container)
        Button(
            onClick = { activeSheetMode = ServerSheetMode.JOIN_SERVER },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEBEDEF)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Text(
                text = "I have an invite link",
                color = Color(0xFF1E1F22),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Section 2 Header: Join communities (Matching Image 1 & 2)
        Text(
            text = "Join communities",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E1F22),
            letterSpacing = (-0.2).sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Check out these servers we think you may like. Find game tips, teammates, and more.",
            fontSize = 14.sp,
            color = Color(0xFF4E5058),
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // List of Featured Community Cards
        featuredCommunities.forEach { community ->
            CommunityFeaturedCard(
                community = community,
                onPreviewClick = { onPreviewCommunitySubmit(community.name) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bottom space so content scrolls past floating user profile bar comfortably
        Spacer(modifier = Modifier.height(84.dp))
    }

    // Modal Bottom Sheet: Create Server / Join Server
    activeSheetMode?.let { mode ->
        CreateServerModalSheet(
            mode = mode,
            onDismiss = { activeSheetMode = null },
            onNavigateToJoin = { activeSheetMode = ServerSheetMode.JOIN_SERVER },
            onNavigateToCreate = { activeSheetMode = ServerSheetMode.CREATE_SERVER },
            onCreateServer = { serverName, category ->
                activeSheetMode = null
                onCreateServerSubmit(serverName, category)
            },
            onJoinInvite = { inviteCode ->
                activeSheetMode = null
                onJoinInviteSubmit(inviteCode)
            }
        )
    }
}

/**
 * Server Template Card matching Image 1:
 * - Rounded gray card
 * - Illustrated art area
 * - Bold title
 * - Subtle description
 */
@Composable
private fun TemplateCard(
    template: ServerTemplate,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(208.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF2F3F5)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Illustrated Graphic Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                when (template.id) {
                    "tmpl_hangout" -> {
                        // Stylized friendship hands / high five graphic
                        HandshakeIllustration()
                    }
                    "tmpl_gaming" -> {
                        // 3D Isometric purple gaming badge
                        GamingBadgeIllustration()
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(template.primaryColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = template.icon,
                                contentDescription = null,
                                tint = template.primaryColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Column {
                Text(
                    text = template.title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1F22)
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = template.description,
                    fontSize = 12.5.sp,
                    color = Color(0xFF4E5058),
                    lineHeight = 16.5.sp,
                    maxLines = 3
                )
            }
        }
    }
}

/**
 * Stylized friendship handshake / pinky swear vector illustration matching Image 1.
 */
@Composable
private fun HandshakeIllustration() {
    Row(
        horizontalArrangement = Arrangement.spacedBy((-8).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left hand (Blue sleeve with smartwatch)
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 6.dp, bottomEnd = 6.dp))
                .background(Color(0xFF5865F2)),
            contentAlignment = Alignment.Center
        ) {
            // Smartwatch band
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFE3E5E8))
            )
        }

        // Right hand (Purple sleeve with gemstone)
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp, topStart = 6.dp, bottomStart = 6.dp))
                .background(Color(0xFFA855F7)),
            contentAlignment = Alignment.Center
        ) {
            // Gemstone pendant
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3E8FF))
            )
        }
    }
}

/**
 * 3D Isometric purple gaming badge matching Image 1.
 */
@Composable
private fun GamingBadgeIllustration() {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF7C3AED), Color(0xFF4F46E5))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SportsEsports,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color(0xFFA5B4FC),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Featured Community Card matching Image 1 & 2:
 * - Top scenic banner with three dots menu
 * - Overlapping squircle avatar with white border
 * - Verified badge + Server title
 * - Server description
 * - Green/Gray member count stats
 * - Solid blurple "Preview" button
 */
@Composable
private fun CommunityFeaturedCard(
    community: FeaturedCommunity,
    onPreviewClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE3E5E8), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Banner Container with Overlapping Avatar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
            ) {
                // Background Banner Artwork
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp)
                        .background(community.bannerGradient)
                ) {
                    // Subtle background geometric art elements
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = size.width * 0.4f,
                            center = Offset(size.width * 0.85f, size.height * 0.2f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f),
                            radius = size.width * 0.25f,
                            center = Offset(size.width * 0.2f, size.height * 0.8f)
                        )
                    }

                    // Top-right three-dots menu button
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clickable { /* options menu */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Community options",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Overlapping Avatar Squircle (with 3.dp white border)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 14.dp)
                        .size(58.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(3.dp, Color.White, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (community.avatarDrawableRes != null) {
                        Image(
                            painter = painterResource(community.avatarDrawableRes),
                            contentDescription = community.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(13.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(13.dp))
                                .background(community.avatarBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = community.avatarLetter,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Text Info & Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Name Row with Verified Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (community.verifiedBadgeColor == Color(0xFF23A55A)) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.Verified
                        },
                        contentDescription = "Verified community",
                        tint = community.verifiedBadgeColor,
                        modifier = Modifier.size(18.dp)
                    )

                    Text(
                        text = community.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1F22),
                        letterSpacing = (-0.2).sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Description
                Text(
                    text = community.description,
                    fontSize = 13.5.sp,
                    color = Color(0xFF4E5058),
                    lineHeight = 18.5.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Member Stats Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Online Indicator Dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF23A55A))
                    )
                    Text(
                        text = community.onlineCountText,
                        fontSize = 12.sp,
                        color = Color(0xFF5C5E66),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Total Members Indicator Dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF5C5E66))
                    )
                    Text(
                        text = community.membersCountText,
                        fontSize = 12.sp,
                        color = Color(0xFF5C5E66),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // "Preview" Button (Solid blurple)
                Button(
                    onClick = onPreviewClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        text = "Preview",
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

