package com.campus.network.feature.hub.bestfootball

import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

const val TH = 0.705f // radians between neighbouring items
const val HALF = 0.39f // half arc-length of an open pill (rad)
const val TAIL = 0.26f // rad, length of the tail of upcoming pills

data class MatchItem(
    val club: String,
    val label: String,
    val date: String,
    val time: String,
    val ph: String
)

// Campus activities / events data (replaces football matches while keeping same visual structure)
val MATCHES = listOf(
    MatchItem("red", "DEV", "02 SEP", "20:00", "d6"),
    MatchItem("mersey", "DESiGN", "05 SEP", "18:15", "d4"),
    MatchItem("kings", "CHESS", "08 SEP", "19:30", "d1"),
    MatchItem("hornets", "HACK", "11 SEP", "21:30", "d2"),
    MatchItem("azure", "SPORTS", "19 SEP", "14:00", "d3"),
    MatchItem("fox", "MEDiA", "23 SEP", "17:45", "d5"),
    MatchItem("atlas", "MEET", "26 SEP", "20:00", "d6")
)

@Composable
fun DialLayer(
    pos: Float,
    tilt: Float,
    visited: List<Boolean>,
    isStatsOpen: Boolean,
    onDragStart: () -> Unit,
    onDragMove: (dxDp: Float, dtMs: Long) -> Unit,
    onDragEnd: (touchXDp: Float, movedDp: Float, velocity: Float) -> Unit,
    onPointerDown: (Offset) -> Unit,
    onPointerMove: (Offset) -> Unit,
    onPointerUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val squadaTypeface = remember {
        try {
            ResourcesCompat.getFont(context, com.campus.network.feature.hub.R.font.squada_one)
                ?: Typeface.DEFAULT_BOLD
        } catch (e: Exception) {
            Typeface.DEFAULT_BOLD
        }
    }

    val density = LocalDensity.current

    Box(modifier = modifier) {
        // SVG / Canvas layer for ring text, shells, needle, badges, photos
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val cx = size.width / 2f
            val cy = 562f.dp.toPx()
            val rd = 240f.dp.toPx()
            val rp = 240f.dp.toPx()
            val thk = 62f.dp.toPx()
            val disc = 60f.dp.toPx()

            fun pAt(a: Float, r: Float) = Offset(cx + r * sin(a), cy - r * cos(a))

            // 1. Ring Text (Campus Network ring)
            val ringRadius = 205f.dp.toPx()
            val ringCenterY = 597f.dp.toPx()
            val ringAndroidPath = AndroidPath().apply {
                addCircle(cx, ringCenterY, ringRadius, AndroidPath.Direction.CW)
            }
            val ringRotation = -pos * TH * (180f / PI.toFloat()) * 0.6f
            rotate(ringRotation, pivot = Offset(cx, ringCenterY)) {
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        alpha = (255 * 0.11f).toInt()
                        textSize = 14f.dp.toPx()
                        typeface = squadaTypeface
                        isAntiAlias = true
                        letterSpacing = 0.08f
                    }
                    val text = "CAMPUS NETWORK · CAMPUS HUB · CAMPUS NETWORK · CAMPUS HUB · CAMPUS NETWORK · CAMPUS HUB · "
                    canvas.nativeCanvas.drawTextOnPath(text, ringAndroidPath, 0f, 5f.dp.toPx(), paint)
                }
            }

            // 2. Shells and Badges
            val n = MATCHES.size

            for (i in 0 until n) {
                val p = i - pos
                val ap = abs(p)
                if (ap > 2.3f) continue

                val m = MATCHES[i]
                val a = p * TH
                val e = max(0f, 1f - ap)
                val r = rd - (rd - rp) * e
                val h = HALF * e
                val upcoming = p >= 0f
                val thick = if (upcoming) thk else max(thk * e, disc)
                val shellOp = if (upcoming) {
                    ((2.2f - p) / 0.6f).coerceIn(0f, 1f)
                } else {
                    if (ap <= 1f) 1f else (1f - (ap - 1f) / 0.7f).coerceIn(0f, 1f)
                }

                val a1 = if (upcoming) a - HALF * 0.96f * e else a - h * 0.96f
                val a2 = (if (upcoming) a + HALF * 0.93f * e + TAIL * (1f - e) else a + h * 0.93f) + 0.0008f

                val isVis = visited.getOrElse(i) { false }
                val halo = !upcoming && !isVis && e < 0.5f
                val strokeColor = if (halo) Color(0xFF5A1FB0) else Color.White
                val strokeWidth = if (halo) 52f.dp.toPx() else thick
                val strokeAlpha = if (halo) shellOp * 0.17f else shellOp

                // Draw Shell Arc
                val startDeg = Math.toDegrees(a1.toDouble()).toFloat() - 90f
                val sweepDeg = Math.toDegrees((a2 - a1).toDouble()).toFloat()
                val rect = Rect(cx - r, cy - r, cx + r, cy + r)
                drawArc(
                    color = strokeColor,
                    startAngle = startDeg,
                    sweepAngle = sweepDeg,
                    useCenter = false,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    alpha = strokeAlpha
                )

                // Draw Badge (Crest) at p1 = P(a1, r)
                val p1 = pAt(a1, r)
                val base = if (upcoming) 42f else if (isVis) 40f else 30f
                val bsz = if (upcoming) 42f - 2f * e else base + (40f - base) * e
                val bsc = bsz / 48f
                val deg = Math.toDegrees(a.toDouble()).toFloat()
                var bop = 1f
                if (ap > 1.7f) {
                    bop *= ((2.3f - ap) / 0.6f).coerceIn(0f, 1f)
                }

                val badgeSizePx = 48f.dp.toPx()
                translate(left = p1.x - badgeSizePx / 2f, top = p1.y - badgeSizePx / 2f) {
                    rotate(deg, pivot = Offset(badgeSizePx / 2f, badgeSizePx / 2f)) {
                        scale(bsc, pivot = Offset(badgeSizePx / 2f, badgeSizePx / 2f)) {
                            drawIntoCanvas { canvas ->
                                val native = canvas.nativeCanvas
                                val alphaInt = (bop.coerceIn(0f, 1f) * 255).toInt()
                                val count = native.saveLayerAlpha(0f, 0f, badgeSizePx, badgeSizePx, alphaInt)
                                drawCrest(m.club, badgeSizePx)
                                native.restoreToCount(count)
                            }
                        }
                    }
                }

                // Draw Photo (Portrait) at pr = P(a2 - 0.0008, r)
                val pr = pAt(a2 - 0.0008f, r)
                val pop = if (upcoming) {
                    ((2.2f - p) / 0.5f).coerceIn(0f, 1f) * (0.45f + 0.55f * e)
                } else {
                    e.pow(3f)
                }
                val psc = 0.86f * (if (upcoming) 0.8f + 0.2f * e else 0.4f + 0.6f * e)
                val deg2 = Math.toDegrees(a2.toDouble()).toFloat()
                val photoSizePx = 48f.dp.toPx()

                translate(left = pr.x - photoSizePx / 2f, top = pr.y - photoSizePx / 2f) {
                    rotate(deg2, pivot = Offset(photoSizePx / 2f, photoSizePx / 2f)) {
                        scale(psc, pivot = Offset(photoSizePx / 2f, photoSizePx / 2f)) {
                            drawIntoCanvas { canvas ->
                                val native = canvas.nativeCanvas
                                val alphaInt = (pop.coerceIn(0f, 1f) * 255).toInt()
                                val count = native.saveLayerAlpha(0f, 0f, photoSizePx, photoSizePx, alphaInt)
                                val clipPath = Path().apply {
                                    addOval(Rect(0f, 0f, photoSizePx, photoSizePx))
                                }
                                drawContext.canvas.clipPath(clipPath)
                                drawFace(m.ph, photoSizePx)
                                native.restoreToCount(count)
                            }
                        }
                    }
                }
            }

            // 3. Needle at (cx, 375.dp.toPx())
            val needleScale = 1.dp.toPx()
            val needleY = 375f.dp.toPx()
            translate(left = cx, top = needleY) {
                rotate(tilt, pivot = Offset(0f, 0f)) {
                    scale(needleScale, pivot = Offset(0f, 0f)) {
                        val needlePath = Path().apply {
                            moveTo(0f, 0f)
                            cubicTo(2f, 11f, 8f, 20f, 8f, 30f)
                            arcTo(Rect(-8f, 22f, 8f, 38f), 0f, 180f, false)
                            cubicTo(-8f, 20f, -2f, 11f, 0f, 0f)
                            close()
                        }
                        drawPath(needlePath, color = Ink)
                    }
                }
            }
        }

        // 4. Dial Labels (placed along the arc using Dp offsets)
        for (i in MATCHES.indices) {
            val p = i - pos
            val ap = abs(p)
            if (ap > 2.3f) continue

            val m = MATCHES[i]
            val e = max(0f, 1f - ap)
            val upcoming = p >= 0f
            val a = p * TH
            val a1 = if (upcoming) a - HALF * 0.96f * e else a - (HALF * e) * 0.96f
            val a2 = (if (upcoming) a + HALF * 0.93f * e + TAIL * (1f - e) else a + (HALF * e) * 0.93f) + 0.0008f
            val am = (a1 + a2) / 2f
            val r = 240f
            val cx = 196.5f
            val cy = 562f
            val plX = cx + r * sin(am)
            val plY = cy - r * cos(am) + 2f
            val dm = Math.toDegrees(am.toDouble()).toFloat()
            val lop = if (upcoming) e.pow(1.4f) else e.pow(2.2f)

            Box(
                modifier = Modifier
                    .offset(x = (plX - 40f).dp, y = (plY - 10f).dp)
                    .size(width = 80.dp, height = 20.dp)
                    .graphicsLayer {
                        rotationZ = dm
                        alpha = lop
                    },
                contentAlignment = Alignment.Center
            ) {
                DText(
                    text = m.label,
                    size = 25f,
                    color = Ink,
                    lineHeight = 20f,
                    align = TextAlign.Center
                )
            }
        }

        // 5. Dial Gesture Hit Area (x: 0..393, y: 262..477)
        if (!isStatsOpen) {
            Box(
                modifier = Modifier
                    .offset(x = 0.dp, y = 262.dp)
                    .size(width = 393.dp, height = 215.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            onPointerDown(down.position + Offset(0f, 262f * density.density))
                            onDragStart()

                            var lastX = down.position.x
                            var lastT = System.currentTimeMillis()
                            var moved = 0f
                            var velocity = 0f

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (change.pressed) {
                                    val currentX = change.position.x
                                    val now = System.currentTimeMillis()
                                    val dt = max(8L, now - lastT)
                                    val dx = (currentX - lastX) / density.density
                                    moved = max(moved, abs(dx))

                                    val frameV = -(dx / 165f) / (dt / 1000f)
                                    velocity = 0.8f * velocity + 0.2f * frameV

                                    lastX = currentX
                                    lastT = now

                                    onPointerMove(change.position + Offset(0f, 262f * density.density))
                                    onDragMove(dx, dt)
                                    change.consume()
                                } else {
                                    // Pointer released
                                    onPointerUp()
                                    val touchX = (change.position.x) / density.density
                                    onDragEnd(touchX, moved, velocity)
                                    change.consume()
                                    break
                                }
                            }
                        }
                    }
            )
        }
    }
}

@Composable
fun DateTimeDisplay(
    selectedMatchIndex: Int,
    modifier: Modifier = Modifier
) {
    val currentMatch = MATCHES.getOrElse(selectedMatchIndex) { MATCHES[2] }

    var displayedDate by remember { mutableStateOf(currentMatch.date) }
    var displayedTime by remember { mutableStateOf(currentMatch.time) }
    var ghostDate by remember { mutableStateOf<String?>(null) }
    var ghostTime by remember { mutableStateOf<String?>(null) }

    val curDateAlpha = remember { Animatable(1f) }
    val curDateScale = remember { Animatable(1f) }
    val ghDateAlpha = remember { Animatable(0f) }
    val ghDateScale = remember { Animatable(1f) }

    val curTimeAlpha = remember { Animatable(1f) }
    val curTimeScale = remember { Animatable(1f) }
    val ghTimeAlpha = remember { Animatable(0f) }
    val ghTimeScale = remember { Animatable(1f) }

    LaunchedEffect(currentMatch) {
        if (displayedDate != currentMatch.date) {
            ghostDate = displayedDate
            ghostTime = displayedTime
            displayedDate = currentMatch.date
            displayedTime = currentMatch.time

            // Animate ghosts out
            launch {
                ghDateAlpha.snapTo(0.55f)
                ghDateScale.snapTo(1f)
                ghDateAlpha.animateTo(0f, animationSpec = tween(500))
            }
            launch {
                ghDateScale.animateTo(1.04f, animationSpec = tween(500))
            }

            launch {
                ghTimeAlpha.snapTo(0.55f)
                ghTimeScale.snapTo(1f)
                ghTimeAlpha.animateTo(0f, animationSpec = tween(500))
            }
            launch {
                ghTimeScale.animateTo(1.04f, animationSpec = tween(500))
            }

            // Animate currents in
            launch {
                curDateAlpha.snapTo(0.2f)
                curDateScale.snapTo(0.97f)
                curDateAlpha.animateTo(1f, animationSpec = tween(500))
            }
            launch {
                curDateScale.animateTo(1f, animationSpec = tween(500))
            }

            launch {
                curTimeAlpha.snapTo(0.2f)
                curTimeScale.snapTo(0.97f)
                curTimeAlpha.animateTo(1f, animationSpec = tween(500))
            }
            launch {
                curTimeScale.animateTo(1f, animationSpec = tween(500))
            }
        }
    }

    Box(modifier = modifier) {
        // Date: top 446, height 22, 21sp, DateTxt alpha .95, stroke .02em
        Box(
            modifier = Modifier
                .offset(x = 0.dp, y = 446.dp)
                .size(width = 393.dp, height = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            ghostDate?.let { gh ->
                if (ghDateAlpha.value > 0.01f) {
                    DText(
                        text = gh,
                        size = 21f,
                        color = DateTxt.copy(alpha = 0.95f),
                        strokeEm = 0.02f,
                        lineHeight = 22f,
                        align = TextAlign.Center,
                        modifier = Modifier.graphicsLayer {
                            alpha = ghDateAlpha.value
                            scaleX = ghDateScale.value
                            scaleY = ghDateScale.value
                        }
                    )
                }
            }
            DText(
                text = displayedDate,
                size = 21f,
                color = DateTxt.copy(alpha = 0.95f),
                strokeEm = 0.02f,
                lineHeight = 22f,
                align = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = curDateAlpha.value
                    scaleX = curDateScale.value
                    scaleY = curDateScale.value
                }
            )
        }

        // Time: top 467, height 100, 107sp, White, letterSpacing -1sp, line-height 100sp
        Box(
            modifier = Modifier
                .offset(x = 0.dp, y = 467.dp)
                .size(width = 393.dp, height = 100.dp),
            contentAlignment = Alignment.Center
        ) {
            ghostTime?.let { gh ->
                if (ghTimeAlpha.value > 0.01f) {
                    DText(
                        text = gh,
                        size = 107f,
                        color = White,
                        strokeEm = 0.036f,
                        lineHeight = 100f,
                        letterSpacing = -1f,
                        align = TextAlign.Center,
                        modifier = Modifier.graphicsLayer {
                            alpha = ghTimeAlpha.value
                            scaleX = ghTimeScale.value
                            scaleY = ghTimeScale.value
                        }
                    )
                }
            }
            DText(
                text = displayedTime,
                size = 107f,
                color = White,
                strokeEm = 0.036f,
                lineHeight = 100f,
                letterSpacing = -1f,
                align = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = curTimeAlpha.value
                    scaleX = curTimeScale.value
                    scaleY = curTimeScale.value
                }
            )
        }
    }
}
