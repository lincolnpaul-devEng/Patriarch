package com.campus.network.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.unit.dp

val HomescreenPurple = Color(0xFFA969FE)
val HomescreenInk = Color(0xFF0B0612)
val HomescreenOrange = Color(0xFFFF4D32)

/**
 * Matches the exact outer frame geometry of `FittedScreen` on the Home screen:
 *  - Uniformly matches the scaled `52.dp` corner radius (`52.dp * s` where `s = minOf(maxWidth / 393.dp, maxHeight / 852.dp)`)
 *  - Matches the top/bottom black letterbox inset (`(maxHeight - 852.dp * s) / 2`) so the top and bottom
 *    rounded corners start at the exact same Y-coordinates as the Home screen
 *  - Also draws a solid black outer mask above all child layers (including AndroidView/SurfaceView maps)
 *    so the 4 rounded corners are never covered by hardware surfaces.
 */
@Composable
fun HomescreenRoundedFrame(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val s = minOf(maxWidth / 393.dp, maxHeight / 852.dp)
        val frameHeight = (852.dp * s).coerceAtMost(maxHeight)
        val cornerRadius = 52.dp * s
        val cornerShape = RoundedCornerShape(cornerRadius)

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(frameHeight)
                .clip(cornerShape)
                .drawWithContent {
                    drawContent()
                    val rPx = cornerRadius.toPx()
                    val maskPath = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(Offset.Zero, size))
                        addRoundRect(
                            RoundRect(
                                rect = Rect(Offset.Zero, Size(size.width, size.height)),
                                cornerRadius = CornerRadius(rPx, rPx)
                            )
                        )
                    }
                    drawPath(path = maskPath, color = Color.Black)
                }
        ) {
            content()
        }
    }
}

@Composable
fun HomescreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    HomescreenRoundedFrame(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HomescreenPurple)
                .homescreenDotGrid()
        ) {
            content()
        }
    }
}

/**
 * Orange drawer-sheet background matching the home screen's RedPanel look:
 * flat HomescreenOrange with a red dot grid fading to transparent at 60% height.
 */
@Composable
fun DrawerSheetBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomescreenOrange)
            .drawerDotGrid()
    ) {
        content()
    }
}

/** Violet dot grid drawn behind content — mirrors the home screen canvas. */
fun Modifier.homescreenDotGrid(): Modifier = drawBehind {
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

/** Red dot grid drawn behind content — mirrors the home screen's red panel canvas. */
fun Modifier.drawerDotGrid(): Modifier = drawBehind {
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
