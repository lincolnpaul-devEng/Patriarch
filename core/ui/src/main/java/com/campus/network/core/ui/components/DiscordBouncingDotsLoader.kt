package com.campus.network.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Discord-style 3-Dot Staggered Bounce & Fade Loading Animation.
 *
 * Replaces button text during asynchronous operations (e.g. "Preview", "Join Server").
 * Features:
 * - 3 staggered circular dots with coordinated keyframe-driven vertical bounce & alpha fading
 * - 1000ms duration per loop with 150ms phase offsets
 * - ZERO EMOJI POLICY: Strictly procedural canvas geometry and fluid animations.
 */
@Composable
fun DiscordBouncingDotsLoader(
    modifier: Modifier = Modifier,
    dotSize: Dp = 6.5.dp,
    dotColor: Color = Color.White,
    bounceHeight: Dp = 6.dp,
    spaceBetween: Dp = 5.dp,
    dotSpacing: Dp = spaceBetween
) {
    val infiniteTransition = rememberInfiniteTransition(label = "DiscordDotsTransition")

    val delays = listOf(0, 160, 320)

    val dotAnimations = delays.map { delayMillis ->
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1000
                    0.5f at 0 using FastOutSlowInEasing
                    1.0f at 300 using FastOutSlowInEasing
                    0.5f at 600 using FastOutSlowInEasing
                    0.5f at 1000 using FastOutSlowInEasing
                },
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(delayMillis)
            ),
            label = "dotScale_$delayMillis"
        )

        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1000
                    0.35f at 0 using FastOutSlowInEasing
                    1.0f at 300 using FastOutSlowInEasing
                    0.35f at 600 using FastOutSlowInEasing
                    0.35f at 1000 using FastOutSlowInEasing
                },
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(delayMillis)
            ),
            label = "dotAlpha_$delayMillis"
        )

        val offsetY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -bounceHeight.value,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1000
                    0f at 0 using FastOutSlowInEasing
                    -bounceHeight.value at 300 using FastOutSlowInEasing
                    0f at 600 using FastOutSlowInEasing
                    0f at 1000 using FastOutSlowInEasing
                },
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(delayMillis)
            ),
            label = "dotOffsetY_$delayMillis"
        )

        Triple(scale, alpha, offsetY)
    }

    val actualSpacing = if (dotSpacing != 5.dp) dotSpacing else spaceBetween

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(actualSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dotAnimations.forEach { (scale, alpha, offsetY) ->
            Box(
                modifier = Modifier
                    .offset(y = offsetY.dp)
                    .size(dotSize)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .background(dotColor, CircleShape)
            )
        }
    }
}
