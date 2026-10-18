package com.campus.network.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * GPU-optimized diagonal Shimmer modifier using `drawWithCache` so animation frames
 * run strictly in the draw phase without recomposing the layout tree.
 */
fun Modifier.campusShimmer(
    shape: Shape = RoundedCornerShape(12.dp),
    baseColor: Color = Color(0xFFE2E8F0),
    highlightColor: Color = Color(0xFFF8FAFC)
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "CampusShimmerTransition")
    val translateProgress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CampusShimmerSweep"
    )

    this
        .clip(shape)
        .drawWithCache {
            val width = size.width.coerceAtLeast(1f)
            val height = size.height.coerceAtLeast(1f)
            val startX = translateProgress * width
            val brush = Brush.linearGradient(
                colors = listOf(baseColor, highlightColor, baseColor),
                start = Offset(startX, 0f),
                end = Offset(startX + width * 0.75f, height)
            )
            onDrawBehind {
                drawRect(brush = brush)
            }
        }
}

/**
 * Progressive Image Loader that displays a live diagonal shimmer skeleton while the image
 * downloads over slow networks, smoothly revealing the bitmap once decoded.
 */
@Composable
fun ShimmerAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(0.dp)
) {
    var isLoading by remember(model) { mutableStateOf(true) }

    Box(modifier = modifier.clip(shape)) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .campusShimmer(shape = shape)
            )
        }
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = contentScale,
            onState = { state ->
                isLoading = state is AsyncImagePainter.State.Loading
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Cog / Radial Spoke Animated Loader for Live Stories & Video Streams
 * (Direct realization of the Paramount+ reference screen: 10 rounded radial spokes
 * with stepped luminance rotation + "Optimizing video playback..." caption).
 */
@Composable
fun StreamCogLoaderOverlay(
    message: String = "Optimizing video playback...",
    spokeSize: Dp = 46.dp,
    dimBackground: Boolean = true,
    modifier: Modifier = Modifier
) {
    val spokeCount = 10
    val infiniteTransition = rememberInfiniteTransition(label = "StreamCogTransition")
    val activeStepFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = spokeCount.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "StreamCogActiveStep"
    )
    val activeIndex = activeStepFloat.toInt() % spokeCount

    Box(
        modifier = modifier
            .then(
                if (dimBackground) {
                    Modifier.background(Color.Black.copy(alpha = 0.48f))
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(modifier = Modifier.size(spokeSize)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = size.minDimension * 0.46f
                val innerRadius = size.minDimension * 0.22f
                val strokeWidth = size.minDimension * 0.115f

                for (i in 0 until spokeCount) {
                    val angleDeg = (i * (360f / spokeCount)) - 90f
                    val angleRad = angleDeg * (PI / 180.0)

                    // Calculate distance behind active leading spoke for trail fade
                    val distanceBehind = (activeIndex - i + spokeCount) % spokeCount
                    val spokeAlpha = when (distanceBehind) {
                        0 -> 1.0f
                        1 -> 0.82f
                        2 -> 0.64f
                        3 -> 0.48f
                        4 -> 0.36f
                        else -> 0.24f
                    }

                    val start = Offset(
                        x = center.x + (innerRadius * cos(angleRad)).toFloat(),
                        y = center.y + (innerRadius * sin(angleRad)).toFloat()
                    )
                    val end = Offset(
                        x = center.x + (outerRadius * cos(angleRad)).toFloat(),
                        y = center.y + (outerRadius * sin(angleRad)).toFloat()
                    )

                    drawLine(
                        color = Color.White.copy(alpha = spokeAlpha),
                        start = start,
                        end = end,
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }

            if (message.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = message,
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.1.sp
                )
            }
        }
    }
}

/**
 * Full-bleed Marketplace Product Card Shimmer Skeleton (`MarketProductHeroCardSkeleton`).
 * Matches the exact geometry of `MarketProductHeroCard` in `HubScreen.kt`.
 */
@Composable
fun MarketProductHeroCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Hero Image Area Skeleton (204.dp height) with overlay badge placeholders
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(204.dp)
                    .campusShimmer(shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            ) {
                // Top-left category/condition badge placeholder
                Box(
                    modifier = Modifier
                        .padding(14.dp)
                        .align(Alignment.TopStart)
                        .width(96.dp)
                        .height(26.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.55f))
                )
                // Top-right favorite circle placeholder
                Box(
                    modifier = Modifier
                        .padding(14.dp)
                        .align(Alignment.TopEnd)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.55f))
                )
            }

            // 2. Product Metadata Section Skeleton
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Product Title Bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
                            .campusShimmer(shape = RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    // Rating Pill Bar
                    Box(
                        modifier = Modifier
                            .width(54.dp)
                            .height(20.dp)
                            .campusShimmer(shape = CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Location & Walk Time Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .height(14.dp)
                        .campusShimmer(shape = RoundedCornerShape(6.dp))
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Seller Row + Price Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .campusShimmer(shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .height(14.dp)
                                .campusShimmer(shape = RoundedCornerShape(6.dp))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(84.dp)
                            .height(28.dp)
                            .campusShimmer(shape = CircleShape)
                    )
                }
            }
        }
    }
}

/**
 * Compact List-View Marketplace Product Shimmer Skeleton (`MarketProductListItemSkeleton`).
 * Matches `MarketProductListItem` when `isFullCardView == false`.
 */
@Composable
fun MarketProductListItemSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left 72.dp Rounded Thumbnail Skeleton
        Box(
            modifier = Modifier
                .size(72.dp)
                .campusShimmer(shape = RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Middle 3 Staggered Metadata Lines
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(17.dp)
                    .campusShimmer(shape = RoundedCornerShape(6.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.52f)
                    .height(13.dp)
                    .campusShimmer(shape = RoundedCornerShape(5.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(78.dp)
                    .height(16.dp)
                    .campusShimmer(shape = RoundedCornerShape(6.dp))
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right Action / Heart Placeholder
        Box(
            modifier = Modifier
                .size(32.dp)
                .campusShimmer(shape = CircleShape)
        )
    }
}

/**
 * Shimmer Skeleton Row for Channels & Direct Message Threads (`MessageThreadRowSkeleton`).
 */
@Composable
fun MessageThreadRowSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .campusShimmer(shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(128.dp)
                        .height(15.dp)
                        .campusShimmer(shape = RoundedCornerShape(5.dp))
                )
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(12.dp)
                        .campusShimmer(shape = RoundedCornerShape(4.dp))
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(13.dp)
                    .campusShimmer(shape = RoundedCornerShape(5.dp))
            )
        }
    }
}
