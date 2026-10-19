package com.campus.network.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage

/**
 * Universal Student Avatar Composable that stays reactive to the user's profile identity.
 * Supports bundled drawable resources, content URIs, web URLs, and clean initials fallbacks.
 *
 * ZERO EMOJI POLICY: Clean vector and typography visuals only.
 */
@Composable
fun CampusStudentAvatar(
    avatarUrl: String?,
    fullName: String,
    modifier: Modifier = Modifier,
    initialsColor: Color = Color(0xFFF1F5F9),
    backgroundColor: Color = Color(0xFF1E293B),
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    val initials = fullName.trim()
        .split("\\s+".toRegex())
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "ME" }

    val isRemoteOrLocalUri = !avatarUrl.isNullOrBlank() && (
        avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://") ||
        avatarUrl.startsWith("content://") || avatarUrl.startsWith("file://")
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (isRemoteOrLocalUri) {
            SubcomposeAsyncImage(
                model = avatarUrl,
                contentDescription = fullName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = initialsColor,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        } else {
            Text(
                text = initials,
                color = initialsColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
