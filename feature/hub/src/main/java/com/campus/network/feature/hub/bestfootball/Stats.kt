package com.campus.network.feature.hub.bestfootball

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/** Real friend conversation with unread messages — drives the home message cards. */
data class UnreadFriendMessage(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val unreadCount: Int = 0
)

private val FRIEND_CARD_COLORS = listOf(
    Green,
    Mint,
    Peach,
    Color(0xFFE8DCFF),
    Color(0xFFFFD9D0)
)

@Composable
fun RedPanel(
    translateY: Float,
    prog: Float,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .offset(x = 0.dp, y = translateY.dp)
            .size(width = 393.dp, height = 920.dp)
            .background(Red)
    ) {
        // Red dot pattern: radial dots rgba(130,20,0,.20), size 14x14, fading to 0 at 60% height
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotColor = Color(130, 20, 0, (255 * 0.20f).toInt())
            val step = 14f.dp.toPx()
            val rows = (size.height / step).toInt()
            val cols = (size.width / step).toInt()
            val fadeEnd = size.height * 0.60f

            for (r in 0..rows) {
                val cy = 4f.dp.toPx() + r * step
                val alpha = (1f - (cy / fadeEnd)).coerceIn(0f, 1f)
                if (alpha <= 0.01f) continue
                for (c in 0..cols) {
                    val cx = 7f.dp.toPx() + c * step
                    drawCircle(
                        color = dotColor.copy(alpha = dotColor.alpha * alpha),
                        radius = 1.7f.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }
        }

        // "ALL ACTiViTiES" button (replaces football "ALL MATCHES")
        val allMatchesAlpha = (1f - prog * 6f).coerceIn(0f, 1f)
        if (allMatchesAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .offset(x = 0.dp, y = 80.dp)
                    .size(width = 393.dp, height = 40.dp)
                    .graphicsLayer { alpha = allMatchesAlpha }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenStats
                    ),
                contentAlignment = Alignment.Center
            ) {
                DText(
                    text = "ALL ACTiViTiES",
                    size = 23f,
                    color = Ink,
                    lineHeight = 40f,
                    align = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun StatRows(
    prog: Float,
    eventsCount: Int = 0,
    postsCount: Int = 0,
    guildsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Row 0: top 100
        val o0 = ((prog - 0.18f - 0f * 0.09f) / 0.4f).coerceIn(0f, 1f)
        if (o0 > 0.01f) {
            Box(
                modifier = Modifier
                    .offset(x = 0.dp, y = (100f + (1f - o0) * 70f).dp)
                    .size(width = 393.dp, height = 150.dp)
                    .graphicsLayer { alpha = o0 }
            ) {
                // Number 28
                Box(modifier = Modifier.offset(x = 17.dp, y = (-2).dp)) {
                    DText(
                        text = eventsCount.toString(),
                        size = 190f,
                        color = White,
                        strokeEm = 0.036f,
                        lineHeight = 190f,
                        letterSpacing = -2f
                    )
                }
                // Label EVENTS at (198, 29)
                Box(modifier = Modifier.offset(x = 198.dp, y = 29.dp)) {
                    DText(
                        text = "EVENTS",
                        size = 22f,
                        color = White.copy(alpha = 0.55f),
                        strokeEm = 0.02f,
                        lineHeight = 20f
                    )
                }
                // Step trapezoid at (171, 128) size 60x36: M0 36L17 1H52V36Z
                Canvas(
                    modifier = Modifier
                        .offset(x = 171.dp, y = 128.dp)
                        .size(width = 60.dp, height = 36.dp)
                ) {
                    val scX = size.width / 60f
                    val scY = size.height / 36f
                    val path = Path().apply {
                        moveTo(0f, 36f * scY)
                        lineTo(17f * scX, 1f * scY)
                        lineTo(52f * scX, 1f * scY)
                        lineTo(52f * scX, 36f * scY)
                        close()
                    }
                    drawPath(path, color = White.copy(alpha = 0.42f))
                }
            }
        }

        // Row 1: top 250
        val o1 = ((prog - 0.18f - 1f * 0.09f) / 0.4f).coerceIn(0f, 1f)
        if (o1 > 0.01f) {
            Box(
                modifier = Modifier
                    .offset(x = 0.dp, y = (250f + (1f - o1) * 70f).dp)
                    .size(width = 393.dp, height = 150.dp)
                    .graphicsLayer { alpha = o1 }
            ) {
                // Number 120
                Box(modifier = Modifier.offset(x = 17.dp, y = (-2).dp)) {
                    DText(
                        text = postsCount.toString(),
                        size = 190f,
                        color = White,
                        strokeEm = 0.036f,
                        lineHeight = 190f,
                        letterSpacing = -2f
                    )
                }
                // Label POSTS at (255, 30)
                Box(modifier = Modifier.offset(x = 255.dp, y = 30.dp)) {
                    DText(
                        text = "POSTS",
                        size = 22f,
                        color = White.copy(alpha = 0.55f),
                        strokeEm = 0.02f,
                        lineHeight = 20f
                    )
                }
                // Step trapezoid at (236, 129) size 112x33: M0 33L9 0H112V33Z
                Canvas(
                    modifier = Modifier
                        .offset(x = 236.dp, y = 129.dp)
                        .size(width = 112.dp, height = 33.dp)
                ) {
                    val scX = size.width / 112f
                    val scY = size.height / 33f
                    val path = Path().apply {
                        moveTo(0f, 33f * scY)
                        lineTo(9f * scX, 0f)
                        lineTo(112f * scX, 0f)
                        lineTo(112f * scX, 33f * scY)
                        close()
                    }
                    drawPath(path, color = White.copy(alpha = 0.42f))
                }
            }
        }

        // Row 2: top 400
        val o2 = ((prog - 0.18f - 2f * 0.09f) / 0.4f).coerceIn(0f, 1f)
        if (o2 > 0.01f) {
            Box(
                modifier = Modifier
                    .offset(x = 0.dp, y = (400f + (1f - o2) * 70f).dp)
                    .size(width = 393.dp, height = 150.dp)
                    .graphicsLayer { alpha = o2 }
            ) {
                // Number 19
                Box(modifier = Modifier.offset(x = 17.dp, y = (-2).dp)) {
                    DText(
                        text = guildsCount.toString(),
                        size = 190f,
                        color = White,
                        strokeEm = 0.036f,
                        lineHeight = 190f,
                        letterSpacing = -2f
                    )
                }
                // Label GUiLDS at (163, 30)
                Box(modifier = Modifier.offset(x = 163.dp, y = 30.dp)) {
                    DText(
                        text = "GUiLDS",
                        size = 22f,
                        color = White.copy(alpha = 0.55f),
                        strokeEm = 0.02f,
                        lineHeight = 20f
                    )
                }
                // Step trapezoid at (147, 122) size 60x36: M0 36L17 1H52V36Z
                Canvas(
                    modifier = Modifier
                        .offset(x = 147.dp, y = 122.dp)
                        .size(width = 60.dp, height = 36.dp)
                ) {
                    val scX = size.width / 60f
                    val scY = size.height / 36f
                    val path = Path().apply {
                        moveTo(0f, 36f * scY)
                        lineTo(17f * scX, 1f * scY)
                        lineTo(52f * scX, 1f * scY)
                        lineTo(52f * scX, 36f * scY)
                        close()
                    }
                    drawPath(path, color = White.copy(alpha = 0.42f))
                }
            }
        }
    }
}

@Composable
fun StatCardsRow(
    stIn: List<Float>,
    cards: List<UnreadFriendMessage>,
    onCardClick: ((UnreadFriendMessage) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (cards.isEmpty()) {
        Box(
            modifier = modifier
                .offset(x = 0.dp, y = 574.dp)
                .size(width = 393.dp, height = 206.dp)
                .padding(horizontal = 12.dp, vertical = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(White),
                contentAlignment = Alignment.Center
            ) {
                DText(
                    text = "NO NEW MESSAGES YET",
                    size = 16f,
                    color = Ink.copy(alpha = 0.45f),
                    strokeEm = 0.02f,
                    lineHeight = 16f,
                    align = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
        return
    }

    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .offset(x = 0.dp, y = 574.dp)
            .size(width = 393.dp, height = 206.dp)
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp)
    ) {
        cards.forEachIndexed { i, card ->
            val v = stIn.getOrElse(i) { stIn.lastOrNull() ?: 0f }
            val alpha = (v * 1.6f).coerceIn(0f, 1f)
            val tx = (1f - v) * 180f
            val blockColor = FRIEND_CARD_COLORS[i % FRIEND_CARD_COLORS.size]
            val nameParts = card.name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            val line1 = nameParts.firstOrNull()?.uppercase().orEmpty()
            val line2 = nameParts.drop(1).joinToString(" ").uppercase()
            val initials = nameParts.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")

            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(width = 126.dp, height = 206.dp)
                    .graphicsLayer {
                        this.alpha = alpha
                        translationX = tx
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onCardClick?.invoke(card)
                    }
            ) {
                // Red fold at top-right (44x30)
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(width = 44.dp, height = 30.dp)
                ) {
                    val foldPath = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width, size.height)
                        close()
                    }
                    drawPath(foldPath, color = Red.copy(alpha = 0.2f))
                    drawLine(
                        color = Red.copy(alpha = 0.9f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2.4f.dp.toPx()
                    )
                }

                // Real friend avatar at (8, 9) 48x48
                Box(
                    modifier = Modifier
                        .offset(x = 8.dp, y = 9.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(blockColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (!card.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = card.avatarUrl,
                            contentDescription = card.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = initials.ifBlank { "?" },
                            color = Ink,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Color block at bottom (left/right/bottom 5, radius 16)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
                        .size(width = 116.dp, height = 92.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(blockColor)
                )

                // Number of new messages at left 14, bottom 60
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 14.dp, y = (-60).dp)
                ) {
                    DText(
                        text = "${card.unreadCount} NEW",
                        size = 15f,
                        color = Ink.copy(alpha = 0.55f),
                        strokeEm = 0.02f,
                        lineHeight = 15f
                    )
                }

                // Real friend name at left 14, bottom 14 (two lines)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 14.dp, y = (-14).dp)
                ) {
                    DText(
                        text = if (line2.isBlank()) line1 else "$line1\n$line2",
                        size = 17f,
                        color = Ink,
                        lineHeight = 16f
                    )
                }
            }
        }
    }
}

@Composable
fun WinRateBar(
    wbIn: Float,
    modifier: Modifier = Modifier
) {
    val ty = (1f - wbIn) * 90f
    Box(
        modifier = modifier
            .offset(x = 0.dp, y = (852f - 67f + ty).dp)
            .size(width = 393.dp, height = 67.dp)
            .background(White)
    ) {
        // Yellow 72% fill
        Box(
            modifier = Modifier
                .size(width = (393f * 0.72f).dp, height = 67.dp)
                .background(Yellow)
        )

        // 72% text at (16, 14)
        Box(modifier = Modifier.offset(x = 16.dp, y = 14.dp)) {
            DText(
                text = "72%",
                size = 25f,
                color = Ink,
                lineHeight = 25f
            )
        }

        // 28% text at right 18, top 15
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-18).dp, y = 15.dp)
        ) {
            DText(
                text = "28%",
                size = 25f,
                color = Grey,
                lineHeight = 25f
            )
        }

        // Red notch at left 177, top 0, size 44x7dp: polygon (0,0)(44,0)(34.3,7)(9.7,7)
        Canvas(
            modifier = Modifier
                .offset(x = 177.dp, y = 0.dp)
                .size(width = 44.dp, height = 7.dp)
        ) {
            val scX = size.width / 44f
            val scY = size.height / 7f
            val notchPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(44f * scX, 0f)
                lineTo(34.32f * scX, 7f * scY)
                lineTo(9.68f * scX, 7f * scY)
                close()
            }
            drawPath(notchPath, color = Red)
        }
    }
}
