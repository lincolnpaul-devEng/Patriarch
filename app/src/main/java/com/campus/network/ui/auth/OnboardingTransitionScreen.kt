package com.campus.network.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.campus.network.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 3D Satellite Token descriptor.
 * Strictly uses genuine 3D cutouts with no backgrounds or box engulfment.
 */
private data class SatelliteToken(
    val drawableRes: Int,
    val initialXRatio: Float,
    val initialYRatio: Float,
    val sizeDp: Int,
    val floatPhase: Float,
    val rotationOffset: Float,
    val isInitial: Boolean = false,
    val zIndexOrder: Float = 1.0f
)

/**
 * Photographic Cloud descriptor for multi-layer atmospheric drift.
 */
private data class DriftingCloud(
    val initialXRatio: Float,
    val yRatio: Float,
    val widthDp: Int,
    val heightDp: Int,
    val speed: Float,
    val baseAlpha: Float,
    val isMirrored: Boolean = false
)

/**
 * Second Onboarding Screen:
 * - Pure realistic blue sky background with genuine photographic, see-through drifting cumulus clouds.
 * - Intact 3D Silver Metallic Inflatable Foil Balloon Typography ("PATRIARCH") in the center.
 * - Genuine 3D transparent campus assets floating completely free (zero rectangular shadows, borders, or backgrounds).
 * - "loading your new map" directly beneath the intact balloon typography with dot progression.
 * - Entire cluster ascends together rapidly through the top bleed.
 * - Concludes with a realistic photographic volumetric cloud bank billowing upward to close over the sky (Frame 7).
 */
@Composable
fun OnboardingTransitionScreen(
    onTransitionComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 14 Genuine 3D Campus Assets (strictly transparent cutouts, no flat pictures)
    val satellites = remember {
        listOf(
            // Frame 0 & 1 Initial Ambient Satellites
            SatelliteToken(
                drawableRes = R.drawable.cat_cafes, // 3D takeaway coffee cup
                initialXRatio = -0.28f,
                initialYRatio = 0.25f,
                sizeDp = 66,
                floatPhase = 0.5f,
                rotationOffset = -12f,
                isInitial = true,
                zIndexOrder = 2.0f
            ),
            SatelliteToken(
                drawableRes = R.drawable.cat_events, // 3D mirror disco ball
                initialXRatio = 0.33f,
                initialYRatio = 0.17f,
                sizeDp = 78,
                floatPhase = 1.8f,
                rotationOffset = 6f,
                isInitial = true,
                zIndexOrder = 1.5f
            ),
            SatelliteToken(
                drawableRes = R.drawable.pin_cookie, // 3D dessert flan with cherry
                initialXRatio = -0.36f,
                initialYRatio = 0.08f,
                sizeDp = 60,
                floatPhase = 2.4f,
                rotationOffset = -8f,
                isInitial = true,
                zIndexOrder = 1.2f
            ),
            SatelliteToken(
                drawableRes = R.drawable.pin_dumpling, // 3D sushi / dumpling piece
                initialXRatio = -0.32f,
                initialYRatio = -0.30f,
                sizeDp = 54,
                floatPhase = 0.0f,
                rotationOffset = 14f,
                isInitial = true,
                zIndexOrder = 1.0f
            ),

            // Frame 2-4 Cascading 3D Satellites
            SatelliteToken(
                drawableRes = R.drawable.pin_event_stage, // 3D campus event stage
                initialXRatio = -0.35f,
                initialYRatio = -0.42f,
                sizeDp = 76,
                floatPhase = 1.2f,
                rotationOffset = -16f,
                isInitial = false,
                zIndexOrder = 0.8f
            ),
            SatelliteToken(
                drawableRes = R.drawable.pin_bakery, // 3D golden croissant
                initialXRatio = 0.31f,
                initialYRatio = -0.38f,
                sizeDp = 74,
                floatPhase = 2.1f,
                rotationOffset = 22f,
                isInitial = false,
                zIndexOrder = 0.9f
            ),
            SatelliteToken(
                drawableRes = R.drawable.cat_bars, // 3D cocktail glass
                initialXRatio = 0.02f,
                initialYRatio = -0.34f,
                sizeDp = 56,
                floatPhase = 0.9f,
                rotationOffset = 18f,
                isInitial = false,
                zIndexOrder = 1.8f
            ),
            SatelliteToken(
                drawableRes = R.drawable.pin_chilli, // 3D chilli pepper
                initialXRatio = 0.36f,
                initialYRatio = -0.16f,
                sizeDp = 52,
                floatPhase = 3.0f,
                rotationOffset = -10f,
                isInitial = false,
                zIndexOrder = 2.2f
            ),
            SatelliteToken(
                drawableRes = R.drawable.cat_shops, // 3D pastel shopping bag
                initialXRatio = -0.38f,
                initialYRatio = -0.12f,
                sizeDp = 64,
                floatPhase = 1.5f,
                rotationOffset = 8f,
                isInitial = false,
                zIndexOrder = 1.3f
            ),
            SatelliteToken(
                drawableRes = R.drawable.pin_pasta, // 3D pasta dish
                initialXRatio = 0.34f,
                initialYRatio = 0.02f,
                sizeDp = 62,
                floatPhase = 2.7f,
                rotationOffset = 10f,
                isInitial = false,
                zIndexOrder = 1.4f
            ),
            SatelliteToken(
                drawableRes = R.drawable.pin_steak, // 3D steak dish
                initialXRatio = -0.10f,
                initialYRatio = 0.32f,
                sizeDp = 60,
                floatPhase = 0.3f,
                rotationOffset = -6f,
                isInitial = false,
                zIndexOrder = 1.6f
            ),
            SatelliteToken(
                drawableRes = R.drawable.cat_top_picks, // 3D top picks emblem
                initialXRatio = 0.40f,
                initialYRatio = -0.28f,
                sizeDp = 54,
                floatPhase = 1.1f,
                rotationOffset = -14f,
                isInitial = false,
                zIndexOrder = 1.1f
            ),
            SatelliteToken(
                drawableRes = R.drawable.cat_go_out, // 3D night meetup drink
                initialXRatio = -0.18f,
                initialYRatio = -0.38f,
                sizeDp = 58,
                floatPhase = 2.6f,
                rotationOffset = 12f,
                isInitial = false,
                zIndexOrder = 0.7f
            ),
            SatelliteToken(
                drawableRes = R.drawable.ic_clock_open_now, // 3D translucent neon clock
                initialXRatio = 0.22f,
                initialYRatio = 0.30f,
                sizeDp = 50,
                floatPhase = 0.7f,
                rotationOffset = 15f,
                isInitial = false,
                zIndexOrder = 1.7f
            )
        )
    }

    // Drifting Photographic Cumulus Clouds (ambient layers, see-through to blue sky)
    val driftingClouds = remember {
        listOf(
            DriftingCloud(
                initialXRatio = -0.20f,
                yRatio = 0.18f,
                widthDp = 280,
                heightDp = 150,
                speed = 0.40f,
                baseAlpha = 0.55f
            ),
            DriftingCloud(
                initialXRatio = 0.45f,
                yRatio = 0.34f,
                widthDp = 340,
                heightDp = 180,
                speed = 0.65f,
                baseAlpha = 0.70f,
                isMirrored = true
            ),
            DriftingCloud(
                initialXRatio = -0.10f,
                yRatio = 0.62f,
                widthDp = 420,
                heightDp = 220,
                speed = 0.90f,
                baseAlpha = 0.85f
            ),
            DriftingCloud(
                initialXRatio = 0.35f,
                yRatio = 0.75f,
                widthDp = 480,
                heightDp = 250,
                speed = 1.10f,
                baseAlpha = 0.90f,
                isMirrored = true
            )
        )
    }

    // Dynamic Loading Text Sequence
    var loadingText by remember { mutableStateOf("") }

    // Master Animation State Controllers
    val cascadeProgress = remember { Animatable(0f) }
    val clusterProgress = remember { Animatable(0f) }
    val launchTranslationY = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val cloudClosingProgress = remember { Animatable(0f) }

    // Ambient floating harmonic oscillation
    val infiniteTransition = rememberInfiniteTransition(label = "SkyDynamics")
    val ambientBob by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AmbientBob"
    )

    // Parallax continuous cloud drift
    val cloudOffsetFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 35000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CloudDrift"
    )

    // Sequence Choreography strictly matching the 8 reference frames (>= 8 seconds duration)
    LaunchedEffect(Unit) {
        // --- Frames 0 & 1: Initial Ambient State (0ms to 2400ms = 2.4s) ---
        // Clean sky, intact "PATRIARCH" balloon typography floats with initial satellites, captivating the viewer
        delay(2400)

        // --- Frame 2: Ingress & Loading Label Appears (2400ms to 4000ms = 1.6s) ---
        loadingText = "loading your new map"
        launch {
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
        cascadeProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing)
        )

        // --- Frame 3: Dot Progression (4000ms to 4900ms = 0.9s) ---
        loadingText = "loading your new map."
        delay(900)

        // --- Frame 4: Clumping & Final Ellipsis (4900ms to 6600ms = 1.7s) ---
        loadingText = "loading your new map.."
        delay(600)
        loadingText = "loading your new map..."
        clusterProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
        )

        // --- Frame 5: Upward Pull-Up & Text Dissolve (6600ms to 7200ms = 0.6s) ---
        launch {
            textAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 400, easing = FastOutLinearInEasing)
            )
        }
        delay(400)

        // --- Frame 6: Rocket Ascending Exit (7000ms to 8000ms = 1.0s) ---
        launch {
            launchTranslationY.animateTo(
                targetValue = -2200f,
                animationSpec = tween(durationMillis = 1000, easing = FastOutLinearInEasing)
            )
        }

        // --- Frame 7: Clouds Surge Upward to Close Over Clear Sky (7400ms to 8800ms = 1.4s) ---
        delay(400)
        cloudClosingProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing)
        )

        // Scene fully closed with soft photographic volumetric cloud banks - hold momentarily
        delay(300)
        onTransitionComplete()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Instant bypass tap
                onTransitionComplete()
            }
    ) {
        val screenWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val screenHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

        // 1. Realistic Sky Blue Gradient Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF3880D4), // Atmospheric high-altitude azure blue
                            Color(0xFF5294DC), // Radiant mid sky
                            Color(0xFF76ADE8), // Soft lower sky
                            Color(0xFFAFD1F5)  // Horizon haze
                        )
                    )
                )
        )

        // 2. Realistic Drifting Photographic Clouds (See-through to blue sky)
        driftingClouds.forEach { cloud ->
            val cloudWidthPx = with(LocalDensity.current) { cloud.widthDp.dp.toPx() }
            val driftDistance = screenWidthPx + cloudWidthPx
            val rawX = (cloud.initialXRatio * screenWidthPx) + (cloudOffsetFactor * cloud.speed * driftDistance)
            val wrappedX = (rawX % (screenWidthPx + cloudWidthPx)) - cloudWidthPx
            val posY = cloud.yRatio * screenHeightPx

            Image(
                painter = painterResource(R.drawable.cloud_cumulus_drift),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .offset {
                        IntOffset(wrappedX.roundToInt(), posY.roundToInt())
                    }
                    .size(cloud.widthDp.dp, cloud.heightDp.dp)
                    .graphicsLayer {
                        alpha = cloud.baseAlpha
                        if (cloud.isMirrored) scaleX = -1f
                    }
            )
        }

        // 3. Kinetic Rocket Cluster Container (Translates upward together in Frames 5-7)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = launchTranslationY.value
                },
            contentAlignment = Alignment.Center
        ) {
            // A. 3D Floating Campus Satellites (Clean 3D cutouts, zero background, zero box shadow)
            satellites.forEach { token ->
                val baseX = token.initialXRatio * screenWidthPx
                val baseY = token.initialYRatio * screenHeightPx

                // Gravitational clumping: tokens contract tightly toward the central anchor
                val contractFactor = 1f - (clusterProgress.value * 0.74f)
                val currentX = baseX * contractFactor
                val currentY = baseY * contractFactor

                // Harmonic individual floating displacement
                val floatOffsetY = sin(ambientBob * PI.toFloat() + token.floatPhase) * 8f
                val floatOffsetX = cos(ambientBob * PI.toFloat() + token.floatPhase) * 4f

                // Ingress fade/scale for secondary satellites
                val alpha = if (token.isInitial) 1f else cascadeProgress.value.coerceIn(0f, 1f)
                val scale = if (token.isInitial) 1f else (0.4f + (cascadeProgress.value * 0.6f))

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (currentX + floatOffsetX).roundToInt(),
                                y = (currentY + floatOffsetY).roundToInt()
                            )
                        }
                        .size(token.sizeDp.dp)
                        .zIndex(token.zIndexOrder)
                        .graphicsLayer {
                            this.alpha = alpha
                            this.scaleX = scale
                            this.scaleY = scale
                            this.rotationZ = token.rotationOffset + (ambientBob * 2.5f)
                            // Strictly NO shadowElevation or background - completely free cutout
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(token.drawableRes),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // B. Central Intact 3D Silver Metallic Foil Balloon Typography ("PATRIARCH")
            // Completely free-floating, zero rectangular shadow or background engulfment
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset {
                        IntOffset(
                            x = 0,
                            y = (ambientBob * 8).roundToInt()
                        )
                    }
                    .zIndex(1.0f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.patriarch_balloon_silver),
                    contentDescription = "Patriarch Silver Metallic 3D Balloon Typography",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .graphicsLayer {
                            rotationZ = ambientBob * 1.0f
                            // Strictly NO shadowElevation - pristine transparent 3D asset
                        }
                )

                Spacer(modifier = Modifier.height(28.dp))

                // C. Loading Label directly underneath the intact balloon typography (Frames 2-5)
                if (loadingText.isNotEmpty()) {
                    Text(
                        text = loadingText,
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.95f),
                            letterSpacing = 0.2.sp,
                            shadow = Shadow(
                                color = Color(0x66000000),
                                offset = Offset(0f, 2f),
                                blurRadius = 4f
                            )
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.alpha(textAlpha.value)
                    )
                }
            }
        }

        // 4. Final Frame 7 Sequence: Photographic Volumetric Cloud Bank Sweeps Upward to Close the Scene
        if (cloudClosingProgress.value > 0f) {
            val progress = cloudClosingProgress.value
            val ease = FastOutSlowInEasing.transform(progress)

            // Primary volumetric cloud bank surges upward from bottom-right (Frame 7)
            // Preserves natural 1:1 aspect ratio with ContentScale.Crop (strictly eliminates vertical stretch)
            val bankOffsetY = (screenHeightPx * 0.95f) * (1f - ease) - (screenHeightPx * 0.12f * ease)
            val bankOffsetX = (screenWidthPx * 0.25f) * (1f - ease) - (screenWidthPx * 0.10f * ease)
            val bankScale = 1.0f + (ease * 0.25f)
            val bankAlpha = (ease * 1.5f).coerceIn(0f, 1f)

            // Secondary volumetric cloud layer surges from bottom-left for realistic multi-layer atmospheric depth
            val frontOffsetY = (screenHeightPx * 1.10f) * (1f - ease) + (screenHeightPx * 0.05f * ease)
            val frontOffsetX = (-screenWidthPx * 0.30f) * (1f - ease) + (screenWidthPx * 0.05f * ease)
            val frontScale = 0.95f + (ease * 0.35f)
            val frontAlpha = (ease * 1.3f).coerceIn(0f, 0.95f)

            // Background volumetric cloud bank (strictly ContentScale.Crop to maintain natural cumulus billows)
            Image(
                painter = painterResource(R.drawable.cloud_bank_closing),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(bankOffsetX.roundToInt(), bankOffsetY.roundToInt())
                    }
                    .graphicsLayer {
                        scaleX = bankScale
                        scaleY = bankScale
                        alpha = bankAlpha
                    }
                    .zIndex(10f)
            )

            // Foreground volumetric cumulus layer (adds authentic photographic depth and soft wisps)
            Image(
                painter = painterResource(R.drawable.cloud_cumulus_drift),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth(1.4f)
                    .height(maxHeight * 0.85f)
                    .offset {
                        IntOffset(frontOffsetX.roundToInt(), frontOffsetY.roundToInt())
                    }
                    .graphicsLayer {
                        scaleX = frontScale
                        scaleY = frontScale
                        alpha = frontAlpha
                    }
                    .zIndex(11f)
            )
        }
    }
}
