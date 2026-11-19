package com.campus.network.feature.hub.bestfootball

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun BestFootballMatchScreen(
    userFullName: String? = null,
    userAvatarUrl: String? = null,
    campusStats: com.campus.network.core.data.CampusStatsRepository.CampusStats =
        com.campus.network.core.data.CampusStatsRepository.CampusStats.Empty,
    unreadFriendMessages: List<UnreadFriendMessage> = emptyList(),
    onNavigateToProfile: () -> Unit = {},
    onNavigateToGoLive: () -> Unit = {},
    onNavigateToChannels: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToMarket: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val rawName = userFullName?.trim().orEmpty()
    val nameTokens = rawName.split("\\s+".toRegex()).filter { it.isNotBlank() }
    val firstName = nameTokens.firstOrNull()?.uppercase().orEmpty()
    val lastName = if (nameTokens.size > 1) {
        nameTokens.drop(1).joinToString(" ").uppercase()
    } else {
        ""
    }

    // Dial State
    val posAnim = remember { Animatable(2f) } // start on item 2 (CHESS)
    val tiltAnim = remember { Animatable(0f) }
    val visited = remember { mutableStateListOf(false, false, true, false, false, false, false) }
    var selectedIndex by remember { mutableIntStateOf(2) }

    // Red Panel and Transition Springs
    var viewState by remember { mutableStateOf("home") } // "home" or "stats"
    var gen by remember { mutableIntStateOf(0) }

    val redY = remember { Animatable(712f) }
    val homeAlpha = remember { Animatable(1f) }
    val pcIn = remember { Animatable(1f) }
    val scIn = remember { Animatable(1f) }
    val stIn = remember { List(5) { Animatable(0f) } }
    val wbIn = remember { Animatable(0f) }

    // Touch Ring State
    var touchVisible by remember { mutableStateOf(false) }
    val touchAlpha = remember { Animatable(0f) }
    var touchPos by remember { mutableStateOf(Offset.Zero) }
    var touchScaleX by remember { mutableFloatStateOf(1f) }
    val touchFillScale = remember { Animatable(0.2f) }
    val touchFillAlpha = remember { Animatable(0f) }
    var lastTouchX by remember { mutableFloatStateOf(0f) }
    var lowPassVx by remember { mutableFloatStateOf(0f) }

    fun triggerTouchDown(pos: Offset) {
        touchPos = pos
        lastTouchX = pos.x
        lowPassVx = 0f
        touchScaleX = 1f
        touchVisible = true
        coroutineScope.launch {
            touchAlpha.snapTo(1f)
            touchFillAlpha.snapTo(1f)
            touchFillScale.snapTo(0.2f)
            launch {
                touchFillScale.animateTo(1f, tween(380))
            }
            launch {
                delay(170)
                touchFillAlpha.animateTo(0f, tween(210))
            }
        }
    }

    fun triggerTouchMove(pos: Offset) {
        val dx = pos.x - lastTouchX
        lowPassVx = 0.6f * lowPassVx + 0.4f * dx
        lastTouchX = pos.x
        touchPos = pos
        touchScaleX = 1f + min(abs(lowPassVx) * 0.06f, 0.5f)
    }

    fun triggerTouchUp() {
        coroutineScope.launch {
            touchAlpha.animateTo(0f, tween(200))
            touchVisible = false
        }
    }

    // Open / Close Stats
    fun openStats() {
        if (viewState == "stats") return
        viewState = "stats"
        val currentGen = ++gen

        coroutineScope.launch {
            launch { redY.animateTo(-6f, redSpec) }
            launch { homeAlpha.animateTo(1f, homeSpec) }

            for (i in 0 until 5) {
                launch {
                    delay(420L + i * 80L)
                    if (gen == currentGen) {
                        stIn[i].animateTo(1f, cardSpec)
                    }
                }
            }
            launch {
                delay(620L)
                if (gen == currentGen) {
                    wbIn.animateTo(1f, cardSpec)
                }
            }
            launch {
                delay(900L)
                if (gen == currentGen) {
                    pcIn.snapTo(0f)
                    scIn.snapTo(0f)
                    homeAlpha.snapTo(0f)
                }
            }
        }
    }

    fun closeStats() {
        if (viewState == "home") return
        viewState = "home"
        val currentGen = ++gen

        coroutineScope.launch {
            stIn.forEach { launch { it.animateTo(0f, cardSpec) } }
            launch { wbIn.animateTo(0f, cardSpec) }

            launch { redY.animateTo(712f, redSpec) }
            launch { homeAlpha.animateTo(1f, homeSpec) }

            launch {
                delay(330L)
                if (gen == currentGen) {
                    pcIn.animateTo(1f, cardSpec)
                }
            }
            launch {
                delay(430L)
                if (gen == currentGen) {
                    scIn.animateTo(1f, cardSpec)
                }
            }
        }
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragTarget by remember { mutableFloatStateOf(2f) }

    fun onDialDragStart() {
        isDragging = true
        dragTarget = posAnim.value
        coroutineScope.launch {
            posAnim.stop()
            tiltAnim.stop()
        }
    }

    fun onDialDragMove(dxDp: Float, dtMs: Long) {
        val dtSec = max(0.008f, dtMs / 1000f)
        var target = dragTarget - dxDp / 165f
        if (target < 0f) target *= 0.35f
        if (target > 6f) target = 6f + (target - 6f) * 0.35f
        dragTarget = target

        val velocity = -(dxDp / 165f) / dtSec
        val targetTilt = (-velocity * 7f).coerceIn(-30f, 30f)

        coroutineScope.launch {
            posAnim.snapTo(target)
            tiltAnim.snapTo(targetTilt)
            val currentIdx = target.roundToInt().coerceIn(0, 6)
            if (currentIdx != selectedIndex) {
                selectedIndex = currentIdx
                visited[currentIdx] = true
            }
        }
    }

    fun onDialDragEnd(touchXDp: Float, movedDp: Float, velocity: Float) {
        isDragging = false
        val targetIdx: Int = if (movedDp < 6f) {
            val x = touchXDp - 196.5f
            val d = (x / (240f * kotlin.math.sin(TH))).coerceIn(-2f, 2f)
            (posAnim.value + (if (abs(d) > 0.5f) kotlin.math.sign(d) else 0f)).roundToInt()
        } else {
            (posAnim.value + velocity * 0.22f).roundToInt()
        }.coerceIn(0, 6)

        selectedIndex = targetIdx
        visited[targetIdx] = true

        coroutineScope.launch {
            launch {
                posAnim.animateTo(
                    targetValue = targetIdx.toFloat(),
                    animationSpec = dialSpec,
                    initialVelocity = velocity.coerceIn(-6f, 6f)
                )
            }
            launch {
                tiltAnim.animateTo(0f, animationSpec = tiltSpec)
            }
        }
    }

    val prog = (1f - (redY.value / 712f)).coerceIn(0f, 1f)
    val redZIndex = if (viewState == "stats" && redY.value < 690f) 13f else 11f

    FittedScreen {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Purple)
        ) {
            // 0. Dot pattern (zIndex 1)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            ) {
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

            // 1. HOME Layer (zIndex 10)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f)
                    .graphicsLayer {
                        alpha = homeAlpha.value
                    }
            ) {
                // User's Real Profile Name (replaces mock name SLAVA KORNiLOV)
                Box(
                    modifier = Modifier
                        .offset(x = 15.dp, y = 116.dp)
                        .widthIn(max = 248.dp)
                ) {
                    Box {
                        DText(
                            text = firstName,
                            size = 86.5f,
                            color = Ink,
                            lineHeight = 71f,
                            letterSpacing = -0.9f
                        )
                    }
                    if (lastName.isNotBlank()) {
                        Box(modifier = Modifier.offset(y = 71.dp)) {
                            DText(
                                text = lastName,
                                size = 86.5f,
                                color = White,
                                lineHeight = 71f,
                                letterSpacing = -0.9f
                            )
                        }
                    }
                }

                // Product (Snack Ordering) Card: exact look of GO LIVE card, placed on the right of the user name on the same row (y = 116.dp), height = 142.dp (matching the two names)
                Box(
                    modifier = Modifier
                        .offset(x = 270.dp, y = 116.dp)
                        .size(width = 108.dp, height = 142.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(White)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onNavigateToMarket
                        )
                ) {
                    // Fold at top right: 38x26, triangle Lilac + line Purple@.55
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(width = 38.dp, height = 26.dp)
                    ) {
                        val fold = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width, size.height)
                            close()
                        }
                        drawPath(fold, color = Lilac)
                        drawLine(
                            color = Purple.copy(alpha = 0.55f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = 2.4f.dp.toPx()
                        )
                    }

                    // "BUY OR\nSELL" text at (12, 16)
                    Box(modifier = Modifier.offset(x = 12.dp, y = 16.dp)) {
                        DText(
                            text = "BUY OR\nSELL",
                            size = 23f,
                            color = Ink,
                            lineHeight = 21f
                        )
                    }

                    // "NOW" indicator below it at (12, 64)
                    Box(modifier = Modifier.offset(x = 12.dp, y = 64.dp)) {
                        DText(
                            text = "NOW",
                            size = 32f,
                            color = Red,
                            strokeEm = 0.02f
                        )
                    }
                }

                // Dial Layer
                DialLayer(
                    pos = posAnim.value,
                    tilt = tiltAnim.value,
                    visited = visited,
                    isStatsOpen = viewState == "stats",
                    onDragStart = { onDialDragStart() },
                    onDragMove = { dx, dt -> onDialDragMove(dx, dt) },
                    onDragEnd = { tx, m, v -> onDialDragEnd(tx, m, v) },
                    onPointerDown = { pt -> triggerTouchDown(pt) },
                    onPointerMove = { pt -> triggerTouchMove(pt) },
                    onPointerUp = { triggerTouchUp() },
                    modifier = Modifier.fillMaxSize()
                )

                // Date & Time Display
                DateTimeDisplay(
                    selectedMatchIndex = selectedIndex,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. Home Cards (zIndex 12)
            if (homeAlpha.value > 0.01f) {
                // Student Profile Card: left 4, top 574, 124x205
                val pcAlpha = (pcIn.value * 1.6f).coerceIn(0f, 1f)
                val pcTx = (1f - pcIn.value) * 170f
                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = 574.dp)
                        .size(width = 124.dp, height = 205.dp)
                        .zIndex(12f)
                        .graphicsLayer {
                            alpha = pcAlpha
                            translationX = pcTx
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(White)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onNavigateToGoLive
                        )
                ) {
                    // Fold at top right: 44x30, triangle Lilac + line Purple@.55
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(width = 44.dp, height = 30.dp)
                    ) {
                        val fold = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width, size.height)
                            close()
                        }
                        drawPath(fold, color = Lilac)
                        drawLine(
                            color = Purple.copy(alpha = 0.55f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = 2.4f.dp.toPx()
                        )
                    }

                    // Go Live Text at (12, 9)
                    Box(modifier = Modifier.offset(x = 12.dp, y = 9.dp)) {
                        DText(
                            text = "GO\nLiVE",
                            size = 25f,
                            color = Ink,
                            lineHeight = 23f
                        )
                    }

                    // "NOW" indicator at (12, 66)
                    Box(modifier = Modifier.offset(x = 12.dp, y = 66.dp)) {
                        DText(
                            text = "NOW",
                            size = 31f,
                            color = Red,
                            strokeEm = 0.02f
                        )
                    }

                    // Portrait star 150x150 at (-12, bottom -6)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-12).dp, y = 6.dp)
                            .size(150.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawFace("star", size.width)
                        }
                    }
                }

                // Campus Statistics Card: left 134, top 574, 254x205
                val scAlpha = (scIn.value * 1.6f).coerceIn(0f, 1f)
                val scTx = (1f - scIn.value) * 190f
                Box(
                    modifier = Modifier
                        .offset(x = 134.dp, y = 574.dp)
                        .size(width = 254.dp, height = 205.dp)
                        .zIndex(12f)
                        .graphicsLayer {
                            alpha = scAlpha
                            translationX = scTx
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(White)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { openStats() }
                        )
                ) {
                    // Fold at top right: 44x30
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(width = 44.dp, height = 30.dp)
                    ) {
                        val fold = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width, size.height)
                            close()
                        }
                        drawPath(fold, color = Lilac)
                        drawLine(
                            color = Purple.copy(alpha = 0.55f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = 2.4f.dp.toPx()
                        )
                    }

                    // Title: CAMPUS at (12, 9), STATISTICS below it
                    Box(modifier = Modifier.offset(x = 12.dp, y = 9.dp)) {
                        DText(
                            text = "CAMPUS",
                            size = 19f,
                            color = Ink,
                            lineHeight = 17f
                        )
                    }
                    Box(modifier = Modifier.offset(x = 12.dp, y = 26.dp)) {
                        DText(
                            text = "STATISTICS",
                            size = 19f,
                            color = Color(0xFFCDCDD2),
                            lineHeight = 17f
                        )
                    }

                    // Multiply Blend Mode Bars:
                    // Green: left 5, top 131, 116x68
                    // Mint: left 66, top 58, 143x141
                    // Peach: left 178, top 139, 70x60
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawIntoCanvas { canvas ->
                            val native = canvas.nativeCanvas
                            val count = native.saveLayer(0f, 0f, size.width, size.height, null)

                            drawRoundRect(
                                color = Green,
                                topLeft = Offset(5f.dp.toPx(), 131f.dp.toPx()),
                                size = Size(116f.dp.toPx(), 68f.dp.toPx()),
                                cornerRadius = CornerRadius(16f.dp.toPx(), 16f.dp.toPx()),
                                blendMode = BlendMode.Multiply
                            )
                            drawRoundRect(
                                color = Mint,
                                topLeft = Offset(66f.dp.toPx(), 58f.dp.toPx()),
                                size = Size(143f.dp.toPx(), 141f.dp.toPx()),
                                cornerRadius = CornerRadius(16f.dp.toPx(), 16f.dp.toPx()),
                                blendMode = BlendMode.Multiply
                            )
                            drawRoundRect(
                                color = Peach,
                                topLeft = Offset(178f.dp.toPx(), 139f.dp.toPx()),
                                size = Size(70f.dp.toPx(), 60f.dp.toPx()),
                                cornerRadius = CornerRadius(16f.dp.toPx(), 16f.dp.toPx()),
                                blendMode = BlendMode.Multiply
                            )

                            native.restoreToCount(count)
                        }
                    }

                    // Values at (14, 169), (76, 169), (226, 169)
                    Box(modifier = Modifier.offset(x = 14.dp, y = 169.dp)) {
                        DText(text = "${campusStats.eventsCount}", size = 17f, color = Color(0xFF8FA07E), lineHeight = 17f)
                    }
                    Box(modifier = Modifier.offset(x = 76.dp, y = 169.dp)) {
                        DText(text = "${campusStats.postsCount}", size = 17f, color = Ink, lineHeight = 17f)
                    }
                    Box(modifier = Modifier.offset(x = 226.dp, y = 169.dp)) {
                        DText(text = "${campusStats.guildsCount}", size = 17f, color = Ink, lineHeight = 17f)
                    }
                }
            }

            // 3. Red Panel (zIndex 11 or 13)
            RedPanel(
                translateY = redY.value,
                prog = prog,
                onOpenStats = { openStats() },
                modifier = Modifier.zIndex(redZIndex)
            )

            // 4. Stats Rows (zIndex 14)
            StatRows(
                prog = prog,
                eventsCount = campusStats.eventsCount,
                postsCount = campusStats.postsCount,
                guildsCount = campusStats.guildsCount,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(14f)
            )

            // 5. Stat Cards Row (zIndex 15) — only friends with unread messages
            if (prog > 0.01f && unreadFriendMessages.isNotEmpty()) {
                StatCardsRow(
                    stIn = stIn.map { it.value },
                    cards = unreadFriendMessages,
                    onCardClick = { _ ->
                        onNavigateToMessages()
                    },
                    modifier = Modifier.zIndex(15f)
                )
            }

            // 6. Win Rate Bar (zIndex 16)
            if (prog > 0.01f) {
                WinRateBar(
                    wbIn = wbIn.value,
                    modifier = Modifier.zIndex(16f)
                )
            }

            // 7. Header Controls (Avatar, Page Dots, Chevron) - zIndex 55
            // NO fake status bar, NO dynamic island, NO fake home bar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(55f)
            ) {
                // Header Avatar: circle 45x45 at (16, 50)
                Box(
                    modifier = Modifier
                        .offset(x = 16.dp, y = 50.dp)
                        .size(45.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onNavigateToProfile
                        )
                ) {
                    if (!userAvatarUrl.isNullOrBlank()) {
                        com.campus.network.core.ui.components.CampusStudentAvatar(
                            avatarUrl = userAvatarUrl,
                            fullName = rawName,
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape
                        )
                    } else {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawFace("me", size.width)
                        }
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Ink,
                            radius = size.width / 2f + 1f.dp.toPx(),
                            center = Offset(size.width / 2f, size.height / 2f),
                            style = Stroke(width = 2f.dp.toPx())
                        )
                    }
                }

                // Page dots: centres at 182, 198, 212.5, y center 74.5
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val yCenter = 74.5f.dp.toPx()
                    // Dot 1: 8dp Ink
                    drawCircle(color = Ink, radius = 4f.dp.toPx(), center = Offset(182f.dp.toPx(), yCenter))
                    // Dot 2: 5dp Ink@.14
                    drawCircle(color = Ink.copy(alpha = 0.14f), radius = 2.5f.dp.toPx(), center = Offset(198f.dp.toPx(), yCenter))
                    // Dot 3: 5dp Ink@.14
                    drawCircle(color = Ink.copy(alpha = 0.14f), radius = 2.5f.dp.toPx(), center = Offset(212.5f.dp.toPx(), yCenter))
                }

                // Tappable areas for page dots
                Box(
                    modifier = Modifier
                        .offset(x = 174.dp, y = 60.dp)
                        .size(16.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onNavigateToChannels
                        )
                )
                Box(
                    modifier = Modifier
                        .offset(x = 192.dp, y = 60.dp)
                        .size(16.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onNavigateToMap
                        )
                )
                Box(
                    modifier = Modifier
                        .offset(x = 206.dp, y = 60.dp)
                        .size(16.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onNavigateToMessages
                        )
                )

                // Chevron Button: 32x32 hit area at right 26, top 57
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-26).dp, y = 57.dp)
                        .size(32.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (viewState == "home") openStats() else closeStats()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(19.dp)
                            .graphicsLayer {
                                rotationZ = if (viewState == "stats") 180f else 0f
                            }
                    ) {
                        val sc = size.width / 18f
                        val chevronPath = Path().apply {
                            moveTo(3f * sc, 6.5f * sc)
                            lineTo(9f * sc, 12.5f * sc)
                            lineTo(15f * sc, 6.5f * sc)
                        }
                        drawPath(
                            path = chevronPath,
                            color = Ink,
                            style = Stroke(
                                width = 2.3f.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            // Touch ring (zIndex 70)
            if (touchVisible && touchAlpha.value > 0.01f) {
                Box(
                    modifier = Modifier
                        .offset(x = (touchPos.x - 17f).dp, y = (touchPos.y - 17f).dp)
                        .size(34.dp)
                        .zIndex(70f)
                        .graphicsLayer {
                            alpha = touchAlpha.value
                            scaleX = touchScaleX
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerPt = Offset(size.width / 2f, size.height / 2f)
                        val strokeW = 3f.dp.toPx()
                        drawCircle(
                            color = Ink,
                            radius = size.width / 2f - strokeW / 2f,
                            center = centerPt,
                            style = Stroke(width = strokeW)
                        )
                        if (touchFillAlpha.value > 0.01f) {
                            drawCircle(
                                color = Ink.copy(alpha = touchFillAlpha.value),
                                radius = (size.width / 2f) * touchFillScale.value,
                                center = centerPt
                            )
                        }
                    }
                }
            }
        }
    }
}
