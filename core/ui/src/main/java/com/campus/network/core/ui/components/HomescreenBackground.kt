package com.campus.network.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val HomescreenPurple = Color(0xFFA969FE)
val HomescreenInk = Color(0xFF0B0612)

@Composable
fun HomescreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomescreenPurple)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotColor = Color(70, 20, 140, (255 * 0.16f).toInt())
            val step = 14f.dp.toPx()
            val rows = (size.height / step).toInt()
            val cols = (size.width / step).toInt()
            val fadeStart = 324f.dp.toPx()
            val fadeEnd = 596f.dp.toPx()

            for (r in 0..rows) {
                val cy = 4f.dp.toPx() + r * step
                val alpha = when {
                    cy <= fadeStart -> 1f
                    cy >= fadeEnd -> 0f
                    else -> 1f - (cy - fadeStart) / (fadeEnd - fadeStart)
                }
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
        content()
    }
}

/**
 * Top horizontal screen navigation indicator dots (...)
 * Reflects active screen index (0: Home, 1: Channels, 2: Map, 3: Messages, 4: Profile)
 */
@Composable
fun ScreenNavIndicatorDots(
    selectedPage: Int,
    onSelectPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
    pageCount: Int = 5
) {
    Row(
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 10.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until pageCount) {
            val isSelected = i == selectedPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isSelected) 8.dp else 5.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) HomescreenInk else HomescreenInk.copy(alpha = 0.22f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelectPage(i) }
                    )
            )
        }
    }
}
