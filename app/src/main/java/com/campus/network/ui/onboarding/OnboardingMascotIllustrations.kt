package com.campus.network.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.R

/**
 * 1. Welcome Screen Character (Reference Image 1):
 * Hand-drawn editorial line-art Campus Guide character with bob haircut, round glasses,
 * lanyard, holding notepad and pen.
 */
@Composable
fun CampusGuideNotepadIllustration(
    modifier: Modifier = Modifier.size(width = 200.dp, height = 220.dp)
) {
    Image(
        painter = painterResource(id = R.drawable.mascot_welcome_notepad),
        contentDescription = "Campus Guide Welcome Mascot",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

/**
 * 2. Question Header Left Avatar (Reference Images 2, 3, 4, 5, 6):
 * Hand-drawn Campus Guide character on the left of the question prompt, gesturing with one hand toward the text.
 */
@Composable
fun CampusGuidePointingAvatar(
    modifier: Modifier = Modifier.size(width = 76.dp, height = 84.dp)
) {
    Image(
        painter = painterResource(id = R.drawable.mascot_pointing_header),
        contentDescription = "Campus Guide Pointing Mascot",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

/**
 * 3. Bottom Recommendation Scene (Reference Images 2 & 3):
 * Hand-drawn Campus Advisor holding tablet surrounded by 3 speech bubbles (item, NEWS, peer student).
 */
@Composable
fun CampusGuideRecommendationScene(
    modifier: Modifier = Modifier.size(width = 155.dp, height = 165.dp)
) {
    Image(
        painter = painterResource(id = R.drawable.mascot_recommendation_bubbles),
        contentDescription = "Campus Guide Recommendations Mascot",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

/**
 * 4. Push Notifications Illustration (Reference Image 6):
 * Hand-drawn smartphone held in hand with Campus Network ("CN") brand blue icon on screen,
 * surrounded by NEW hoodie, NEWS, and SALE t-shirt speech bubbles.
 */
@Composable
fun CampusPushNotificationScene(
    modifier: Modifier = Modifier.size(width = 270.dp, height = 220.dp)
) {
    Image(
        painter = painterResource(id = R.drawable.mascot_push_notifications),
        contentDescription = "Push Notifications Mascot",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

/**
 * 5. Personalized News & Recommendations Consent Scene (Reference Image 7):
 * Hand-drawn student in terracotta sweater holding phone with glowing lightbulb above head,
 * ON SALE bubble on left, and item card bubble on right.
 */
@Composable
fun CampusPersonalizedNewsScene(
    modifier: Modifier = Modifier.size(width = 265.dp, height = 225.dp)
) {
    Image(
        painter = painterResource(id = R.drawable.mascot_personalized_news),
        contentDescription = "Personalized Recommendations Mascot",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

/**
 * 6. Vector Country Flag Badge (Zero-Emoji Policy):
 * Renders crisp geometric vector flag stripes for priority countries and a clean ISO-2 monogram badge fallback.
 */
@Composable
fun CountryFlagVectorBadge(
    countryCode: String,
    modifier: Modifier = Modifier,
    width: Dp = 32.dp,
    height: Dp = 22.dp
) {
    val code = countryCode.uppercase()
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(3.dp))
            .border(0.75.dp, Color(0xFFD0D5DD), RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            when (code) {
                "KE" -> {
                    drawRect(Color(0xFF111111), Offset(0f, 0f), Size(w, h * 0.30f))
                    drawRect(Color.White, Offset(0f, h * 0.30f), Size(w, h * 0.05f))
                    drawRect(Color(0xFFBB0000), Offset(0f, h * 0.35f), Size(w, h * 0.30f))
                    drawRect(Color.White, Offset(0f, h * 0.65f), Size(w, h * 0.05f))
                    drawRect(Color(0xFF006600), Offset(0f, h * 0.70f), Size(w, h * 0.30f))
                    drawOval(
                        color = Color(0xFFBB0000),
                        topLeft = Offset(w * 0.40f, h * 0.22f),
                        size = Size(w * 0.20f, h * 0.56f)
                    )
                }
                "US" -> {
                    val stripes = 7
                    val sh = h / stripes
                    for (i in 0 until stripes) {
                        drawRect(
                            color = if (i % 2 == 0) Color(0xFFB22234) else Color.White,
                            topLeft = Offset(0f, i * sh),
                            size = Size(w, sh)
                        )
                    }
                    drawRect(Color(0xFF3C3B6E), Offset(0f, 0f), Size(w * 0.45f, h * 0.55f))
                }
                "GB" -> {
                    drawRect(Color(0xFF012169), Offset.Zero, size)
                    drawRect(Color.White, Offset(w * 0.40f, 0f), Size(w * 0.20f, h))
                    drawRect(Color.White, Offset(0f, h * 0.36f), Size(w, h * 0.28f))
                    drawRect(Color(0xFFC8102E), Offset(w * 0.44f, 0f), Size(w * 0.12f, h))
                    drawRect(Color(0xFFC8102E), Offset(0f, h * 0.42f), Size(w, h * 0.16f))
                }
                "NG" -> {
                    drawRect(Color(0xFF008751), Offset(0f, 0f), Size(w / 3f, h))
                    drawRect(Color.White, Offset(w / 3f, 0f), Size(w / 3f, h))
                    drawRect(Color(0xFF008751), Offset(w * 2f / 3f, 0f), Size(w / 3f, h))
                }
                "ZA" -> {
                    drawRect(Color(0xFFE03C31), Offset(0f, 0f), Size(w, h * 0.5f))
                    drawRect(Color(0xFF001489), Offset(0f, h * 0.5f), Size(w, h * 0.5f))
                    drawRect(Color(0xFF007749), Offset(0f, h * 0.36f), Size(w, h * 0.28f))
                }
                "UG" -> {
                    val bh = h / 6f
                    val colors = listOf(
                        Color(0xFF111111), Color(0xFFFCDC04), Color(0xFFD90000),
                        Color(0xFF111111), Color(0xFFFCDC04), Color(0xFFD90000)
                    )
                    colors.forEachIndexed { idx, c ->
                        drawRect(c, Offset(0f, idx * bh), Size(w, bh))
                    }
                }
                "TZ" -> {
                    drawRect(Color(0xFF1EB53A), Offset(0f, 0f), Size(w, h * 0.5f))
                    drawRect(Color(0xFF00A3DD), Offset(0f, h * 0.5f), Size(w, h * 0.5f))
                    drawLine(Color(0xFFFCD116), Offset(0f, h), Offset(w, 0f), strokeWidth = h * 0.38f)
                    drawLine(Color(0xFF111111), Offset(0f, h), Offset(w, 0f), strokeWidth = h * 0.22f)
                }
                "RW" -> {
                    drawRect(Color(0xFF00A1DE), Offset(0f, 0f), Size(w, h * 0.50f))
                    drawRect(Color(0xFFFAD201), Offset(0f, h * 0.50f), Size(w, h * 0.25f))
                    drawRect(Color(0xFF20603D), Offset(0f, h * 0.75f), Size(w, h * 0.25f))
                }
                "GH" -> {
                    drawRect(Color(0xFFCE1126), Offset(0f, 0f), Size(w, h / 3f))
                    drawRect(Color(0xFFFCD116), Offset(0f, h / 3f), Size(w, h / 3f))
                    drawRect(Color(0xFF006B3F), Offset(0f, h * 2f / 3f), Size(w, h / 3f))
                    drawCircle(Color(0xFF111111), radius = h * 0.11f, center = Offset(w * 0.5f, h * 0.5f))
                }
                "ET" -> {
                    drawRect(Color(0xFF078930), Offset(0f, 0f), Size(w, h / 3f))
                    drawRect(Color(0xFFFCDD09), Offset(0f, h / 3f), Size(w, h / 3f))
                    drawRect(Color(0xFFDA121A), Offset(0f, h * 2f / 3f), Size(w, h / 3f))
                    drawCircle(Color(0xFF0F47AF), radius = h * 0.18f, center = Offset(w * 0.5f, h * 0.5f))
                }
                "CA" -> {
                    drawRect(Color(0xFFFF0000), Offset(0f, 0f), Size(w * 0.26f, h))
                    drawRect(Color.White, Offset(w * 0.26f, 0f), Size(w * 0.48f, h))
                    drawRect(Color(0xFFFF0000), Offset(w * 0.74f, 0f), Size(w * 0.26f, h))
                    drawCircle(Color(0xFFFF0000), radius = h * 0.18f, center = Offset(w * 0.5f, h * 0.5f))
                }
                "IN" -> {
                    drawRect(Color(0xFFFF9933), Offset(0f, 0f), Size(w, h / 3f))
                    drawRect(Color.White, Offset(0f, h / 3f), Size(w, h / 3f))
                    drawRect(Color(0xFF138808), Offset(0f, h * 2f / 3f), Size(w, h / 3f))
                    drawCircle(Color(0xFF000080), radius = h * 0.12f, center = Offset(w * 0.5f, h * 0.5f))
                }
                "DE" -> {
                    drawRect(Color(0xFF111111), Offset(0f, 0f), Size(w, h / 3f))
                    drawRect(Color(0xFFDD0000), Offset(0f, h / 3f), Size(w, h / 3f))
                    drawRect(Color(0xFFFFCE00), Offset(0f, h * 2f / 3f), Size(w, h / 3f))
                }
                "SG" -> {
                    drawRect(Color(0xFFEF3340), Offset(0f, 0f), Size(w, h * 0.5f))
                    drawRect(Color.White, Offset(0f, h * 0.5f), Size(w, h * 0.5f))
                }
                else -> {
                    drawRect(Color(0xFFF2F4F7), Offset.Zero, size)
                }
            }
        }
        if (code !in setOf("KE", "US", "GB", "NG", "ZA", "UG", "TZ", "RW", "GH", "ET", "CA", "IN", "DE", "SG")) {
            Text(
                text = code.take(2),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
        }
    }
}
