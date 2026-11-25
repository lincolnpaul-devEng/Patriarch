package com.campus.network.core.ui.components

import android.content.Context
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.core.ui.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// =====================================================================
// Mood check-in prompt: shown after a real session on Map / Live screens
// =====================================================================

enum class MoodSource { MAP, LIVE_ROOM, GO_LIVE, LIVE_FEED, PROFILE }

/**
 * Queues the "What's your vibe today?" check-in when a user finishes a meaningful session.
 * Throttled by a cooldown so it feels like a moment, not nagging.
 */
object MoodCheckIn {
    private const val PREFS = "campus_mood_check_in"
    private const val KEY_LAST_SHOWN = "last_shown_ms"
    private const val KEY_HISTORY = "history"
    private const val MAX_HISTORY = 60

    var cooldownMs: Long = 4L * 60 * 60 * 1000

    private val _pending = MutableStateFlow<MoodSource?>(null)
    val pending: StateFlow<MoodSource?> = _pending

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun request(context: Context, source: MoodSource, force: Boolean = false) {
        if (_pending.value != null) return
        if (!force) {
            val last = prefs(context).getLong(KEY_LAST_SHOWN, 0L)
            if (System.currentTimeMillis() - last < cooldownMs) return
        }
        _pending.value = source
    }

    fun openNow(source: MoodSource = MoodSource.PROFILE) {
        _pending.value = source
    }

    fun getLatestMood(context: Context): Float? {
        val p = prefs(context)
        val lines = p.getString(KEY_HISTORY, "").orEmpty().lines().filter { it.isNotBlank() }
        val lastLine = lines.lastOrNull() ?: return null
        return lastLine.split("|").getOrNull(2)?.toFloatOrNull()
    }

    fun getLatestMoodLabel(context: Context): String {
        val mood = getLatestMood(context) ?: return "Tap to check in"
        return when {
            mood < 0.25f -> "Not great"
            mood < 0.50f -> "Hanging in"
            mood < 0.75f -> "Good vibes"
            else -> "Amazing"
        }
    }

    fun getMascotDrawableForMood(mood: Float?): Int {
        val m = (mood ?: 0.5f).coerceIn(0f, 1f)
        val idx = (m * (MascotFrames.size - 1)).roundToInt().coerceIn(0, MascotFrames.size - 1)
        return MascotFrames[idx]
    }

    internal fun markShown(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_SHOWN, System.currentTimeMillis()).apply()
    }

    internal fun dismiss() {
        _pending.value = null
    }

    /** Stored as "epochMs|SOURCE|value" lines, newest last. */
    internal fun record(context: Context, source: MoodSource, value: Float) {
        val p = prefs(context)
        val lines = p.getString(KEY_HISTORY, "").orEmpty().lines().filter { it.isNotBlank() }
        val updated = (lines + "${System.currentTimeMillis()}|${source.name}|${"%.2f".format(value)}")
            .takeLast(MAX_HISTORY)
        p.edit().putString(KEY_HISTORY, updated.joinToString("\n")).apply()
    }
}

/**
 * Requests a mood check-in when the user leaves a screen they actually used.
 * [isActive] lets pager pages report entry/exit without leaving composition.
 */
@Composable
fun MoodSessionTracker(
    source: MoodSource,
    isActive: Boolean = true,
    minDwellMs: Long = 20_000L
) {
    val context = LocalContext.current
    DisposableEffect(isActive) {
        val start = SystemClock.elapsedRealtime()
        onDispose {
            if (isActive && SystemClock.elapsedRealtime() - start >= minDwellMs) {
                MoodCheckIn.request(context, source)
            }
        }
    }
}

/** App-level host. [canPresent] is false while the user is inside an immersive screen. */
@Composable
fun MoodCheckInOverlay(canPresent: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pending by MoodCheckIn.pending.collectAsState()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(pending, canPresent) {
        if (pending != null && canPresent && !visible) {
            delay(450L) // let the exit transition settle first
            MoodCheckIn.markShown(context)
            visible = true
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + slideInVertically(tween(420, easing = FastOutSlowInEasing)) { it / 3 },
        exit = fadeOut(tween(200)) + slideOutVertically(tween(320)) { it / 3 },
        modifier = modifier.fillMaxSize()
    ) {
        MoodVibeCheckScreen(
            onNext = { value ->
                pending?.let { MoodCheckIn.record(context, it, value) }
                visible = false
                MoodCheckIn.dismiss()
            },
            onDismiss = {
                visible = false
                MoodCheckIn.dismiss()
            }
        )
    }
}

// =====================================================================
// Screen: 1:1 with the "Mood Tracker App UI Animation" reference
// =====================================================================

internal val MascotOpenFrames = listOf(
    R.drawable.mood_mascot_00, R.drawable.mood_mascot_01, R.drawable.mood_mascot_02,
    R.drawable.mood_mascot_03, R.drawable.mood_mascot_04, R.drawable.mood_mascot_05,
    R.drawable.mood_mascot_06, R.drawable.mood_mascot_07, R.drawable.mood_mascot_08,
    R.drawable.mood_mascot_09, R.drawable.mood_mascot_10, R.drawable.mood_mascot_11,
    R.drawable.mood_mascot_12
)

internal val MascotBlinkFrames = listOf(
    R.drawable.mood_mascot_blink_00, R.drawable.mood_mascot_blink_01, R.drawable.mood_mascot_blink_02,
    R.drawable.mood_mascot_blink_03, R.drawable.mood_mascot_blink_04, R.drawable.mood_mascot_blink_05,
    R.drawable.mood_mascot_blink_06, R.drawable.mood_mascot_blink_07, R.drawable.mood_mascot_blink_08,
    R.drawable.mood_mascot_blink_09, R.drawable.mood_mascot_blink_10, R.drawable.mood_mascot_blink_11,
    R.drawable.mood_mascot_blink_12
)

internal val MascotFrames = MascotOpenFrames

// Slider fill colours sampled from the video track (red -> orange -> sand -> lime)
private val TrackGradient = listOf(
    Color(0xFFEC4B3B), Color(0xFFEF7F4E), Color(0xFFE9C46F), Color(0xFFC9CC6A), Color(0xFFA9C463)
)

// Screen backgrounds sampled from the video: warm blush (Not great) -> sand -> sage (Amazing)
private val BgStops = listOf(
    0f to Color(0xFFD6BDB1),
    0.5f to Color(0xFFD4CBB2),
    1f to Color(0xFFC3CCAE)
)

private fun sampleStops(stops: List<Pair<Float, Color>>, t: Float): Color {
    val v = t.coerceIn(0f, 1f)
    for (i in 0 until stops.size - 1) {
        val (t0, c0) = stops[i]
        val (t1, c1) = stops[i + 1]
        if (v <= t1) return lerp(c0, c1, (v - t0) / (t1 - t0))
    }
    return stops.last().second
}

private fun trackColorAt(t: Float): Color =
    sampleStops(TrackGradient.mapIndexed { i, c -> i / (TrackGradient.size - 1f) to c }, t)

private val Ink = Color(0xFF1B1B1B)
private val Muted = Color(0xFF7D766F)
private val AppSans = FontFamily.SansSerif

@Composable
fun MoodVibeCheckScreen(
    onNext: (Float) -> Unit,
    onDismiss: () -> Unit,
    initialValue: Float = 0f,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)
    var value by remember { mutableFloatStateOf(initialValue) }
    val bg = sampleStops(BgStops, value)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(lerp(bg, Color.White, 0.18f), bg, lerp(bg, Color.White, 0.08f))))
            // swallow taps so nothing underneath reacts
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        // Subtle top dismiss button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 14.dp, end = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Ink.copy(alpha = 0.08f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Ink.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.75f))
            Text(
                text = "What\u2019s your vibe today?",
                color = Ink,
                fontFamily = AppSans,
                fontWeight = FontWeight.Medium,
                fontSize = 27.sp,
                letterSpacing = (-0.3).sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Drag to express how you feel",
                color = Muted,
                fontFamily = AppSans,
                fontSize = 15.5.sp
            )
            Spacer(Modifier.weight(0.55f))

            InteractiveMoodMascot(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.size(300.dp)
            )

            Spacer(Modifier.weight(1f))

            MoodSlider(value = value, onValueChange = { value = it })
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
                Text("Not great", color = Muted, fontFamily = AppSans, fontSize = 14.5.sp)
                Spacer(Modifier.weight(1f))
                Text("Amazing", color = Muted, fontFamily = AppSans, fontSize = 14.5.sp)
            }

            Spacer(Modifier.height(44.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.Black)
                    .clickable { onNext(value) },
                contentAlignment = Alignment.Center
            ) {
                Text("Next", color = Color.White, fontFamily = AppSans, fontWeight = FontWeight.Medium, fontSize = 17.sp)
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

/**
 * Fur mascot rendered from the rigged Blender asset (13 baked mood poses, cross-faded).
 * Interactive: breathes, tilts toward your drag, squishes when tapped, and can be dragged
 * sideways to change mood directly.
 */
@Composable
private fun InteractiveMoodMascot(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val breath by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathPhase"
    )
    val squashY = remember { Animatable(1f) }
    val tilt = remember { Animatable(0f) }
    val lookX = remember { Animatable(0f) }

    val pos = value.coerceIn(0f, 1f) * (MascotFrames.size - 1)
    val i0 = pos.toInt().coerceIn(0, MascotFrames.size - 1)
    val i1 = (i0 + 1).coerceAtMost(MascotFrames.size - 1)
    val blend = pos - i0
    val nearest = pos.roundToInt()

    // Haptic tick + tiny pop whenever the mascot moves to a new pose
    var lastFrame by remember { mutableStateOf(nearest) }
    LaunchedEffect(nearest) {
        if (nearest != lastFrame) {
            lastFrame = nearest
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            squashY.snapTo(0.97f)
            squashY.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium))
        }
    }

    // Lifelike autonomous blinking engine:
    // Blinks every 2.5 to 4.5s with natural eyelid snap (~80ms close, ~105ms open)
    // and occasional biological double-blinks.
    val blinkAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            val waitMs = kotlin.random.Random.nextLong(2400L, 4600L)
            delay(waitMs)
            blinkAnim.animateTo(1f, tween(75, easing = FastOutSlowInEasing))
            delay(30L)
            blinkAnim.animateTo(0f, tween(100, easing = FastOutSlowInEasing))

            // 30% chance of double blink
            if (kotlin.random.Random.nextFloat() < 0.30f) {
                delay(110L)
                blinkAnim.animateTo(1f, tween(65, easing = FastOutSlowInEasing))
                delay(25L)
                blinkAnim.animateTo(0f, tween(90, easing = FastOutSlowInEasing))
            }
        }
    }

    val openPainters = MascotOpenFrames.map { painterResource(it) }
    val blinkPainters = MascotBlinkFrames.map { painterResource(it) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        squashY.animateTo(0.84f, tween(90))
                        squashY.animateTo(1f, spring(dampingRatio = 0.32f, stiffness = Spring.StiffnessMediumLow))
                    }
                    scope.launch {
                        blinkAnim.animateTo(1f, tween(60, easing = FastOutSlowInEasing))
                        delay(35L)
                        blinkAnim.animateTo(0f, tween(100, easing = FastOutSlowInEasing))
                    }
                })
            }
            .pointerInput(Unit) {
                val widthPx = size.width.toFloat()
                var current = 0f
                detectDragGestures(
                    onDragStart = { current = pos / (MascotFrames.size - 1) },
                    onDragEnd = {
                        scope.launch { tilt.animateTo(0f, spring(dampingRatio = 0.4f)) }
                        scope.launch { lookX.animateTo(0f, spring(dampingRatio = 0.5f)) }
                    },
                    onDragCancel = {
                        scope.launch { tilt.animateTo(0f, spring(dampingRatio = 0.4f)) }
                        scope.launch { lookX.animateTo(0f, spring(dampingRatio = 0.5f)) }
                    }
                ) { change, drag ->
                    change.consume()
                    current = (current + drag.x / (widthPx * 1.4f)).coerceIn(0f, 1f)
                    onValueChange(current)
                    scope.launch { tilt.snapTo((tilt.value + drag.x * 0.06f).coerceIn(-9f, 9f)) }
                    scope.launch { lookX.snapTo((lookX.value + drag.x * 0.25f).coerceIn(-18f, 18f)) }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Soft contact shadow on the "floor"
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width * 0.62f * (1f + 0.02f * breath)
            val h = size.height * 0.07f
            val center = Offset(size.width / 2f + lookX.value * density.density * 0.4f, size.height * 0.80f)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.26f), Color.Transparent),
                    center = center,
                    radius = w / 2f
                ),
                topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
                size = Size(w, h)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = with(density) { (lookX.value * 0.6f).toDp() })
                .graphicsLayer {
                    val b = 1f + 0.025f * breath
                    scaleX = b * (2f - squashY.value).coerceAtMost(1.12f)
                    scaleY = b * squashY.value
                    rotationZ = tilt.value
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.82f)
                }
        ) {
            val blinkVal = blinkAnim.value
            val openAlpha = (1f - blinkVal).coerceIn(0f, 1f)

            // Open eyes layer (interpolates smoothly with mood slider)
            if (openAlpha > 0.01f) {
                Image(
                    painter = openPainters[i0],
                    contentDescription = "Mood mascot",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = openAlpha }
                )
                if (i1 != i0 && blend > 0.01f) {
                    Image(
                        painter = openPainters[i1],
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().graphicsLayer { alpha = blend * openAlpha }
                    )
                }
            }

            // Blinking eyelids layer (matches current mood color and closes/opens autonomously)
            if (blinkVal > 0.01f) {
                Image(
                    painter = blinkPainters[i0],
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = blinkVal }
                )
                if (i1 != i0 && blend > 0.01f) {
                    Image(
                        painter = blinkPainters[i1],
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().graphicsLayer { alpha = blend * blinkVal }
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodSlider(value: Float, onValueChange: (Float) -> Unit) {
    val density = LocalDensity.current
    val trackHeight = 54.dp
    val thumbW = 62.dp
    val thumbH = 42.dp
    val inset = 6.dp
    val thumbColor = trackColorAt(value)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(RoundedCornerShape(trackHeight / 2))
            .background(Color.White.copy(alpha = 0.30f))
            .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(trackHeight / 2))
    ) {
        val maxTrackWidth = maxWidth
        val travelPx = with(density) { (maxTrackWidth - thumbW - inset * 2).toPx() }
        val thumbX = with(density) { (travelPx * value).toDp() } + inset

        fun setFromX(x: Float) {
            val start = with(density) { (inset + thumbW / 2).toPx() }
            onValueChange(((x - start) / travelPx).coerceIn(0f, 1f))
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(travelPx) { detectTapGestures { setFromX(it.x) } }
                .pointerInput(travelPx) {
                    detectHorizontalDragGestures(onDragStart = { setFromX(it.x) }) { change, _ ->
                        change.consume()
                        setFromX(change.position.x)
                    }
                }
        ) {
            // Gradient fill from the left edge smoothly into the thumb
            val fillWidth = (thumbX + thumbW / 2 + 10.dp).coerceAtMost(maxTrackWidth)
            Box(
                Modifier
                    .width(fillWidth)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(trackHeight / 2))
                    .background(
                        Brush.horizontalGradient(
                            colors = TrackGradient.take(
                                (1 + (value * (TrackGradient.size - 1)).roundToInt()).coerceIn(2, TrackGradient.size)
                            )
                        )
                    )
            )
            // Thumb: white pill with a rim that matches the current mood colour
            Box(
                Modifier
                    .offset(x = thumbX, y = (trackHeight - thumbH) / 2)
                    .size(thumbW, thumbH)
                    .clip(RoundedCornerShape(thumbH / 2))
                    .background(thumbColor)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(thumbH / 2))
                    .background(Color.White)
            )
        }
    }
}

/**
 * Profile / Home card showing today's vibe and quick access to check in.
 */
@Composable
fun TodayVibeCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { MoodCheckIn.openNow(MoodSource.PROFILE) }
) {
    val context = LocalContext.current
    var latestMood by remember { mutableStateOf(MoodCheckIn.getLatestMood(context)) }
    var latestLabel by remember { mutableStateOf(MoodCheckIn.getLatestMoodLabel(context)) }

    // Re-check whenever pending becomes null (user finished check-in)
    val pending by MoodCheckIn.pending.collectAsState()
    LaunchedEffect(pending) {
        if (pending == null) {
            latestMood = MoodCheckIn.getLatestMood(context)
            latestLabel = MoodCheckIn.getLatestMoodLabel(context)
        }
    }

    val mascotRes = MoodCheckIn.getMascotDrawableForMood(latestMood ?: 0.9f)
    val moodColor = trackColorAt(latestMood ?: 0.9f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF16181D))
            .border(1.dp, Color(0xFF262A34), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Mascot avatar preview
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF21252F))
                    .border(1.5.dp, moodColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(mascotRes),
                    contentDescription = "Mascot mood",
                    modifier = Modifier.size(46.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Today's Vibe",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(moodColor)
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (latestMood != null) latestLabel else "How are you feeling right now?",
                    color = Color(0xFF9EA3AE),
                    fontSize = 13.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF232732))
                    .border(1.dp, Color(0xFF333846), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (latestMood != null) "Update" else "Check in",
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
