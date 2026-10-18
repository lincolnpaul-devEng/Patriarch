package com.campus.network.core.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.campus.network.core.ui.R
import com.campus.network.core.ui.theme.BrandBlueMid
import com.campus.network.core.ui.theme.BrandBluePrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Stable
class CampusNetworkState(
    isInitiallyOnline: Boolean,
    isInitiallySlow: Boolean,
    private val onRetryRequest: () -> Unit
) {
    var isOnline by mutableStateOf(isInitiallyOnline)
        internal set
    var isSlowNetwork by mutableStateOf(isInitiallySlow)
        internal set
    var isRetrying by mutableStateOf(false)
        internal set
    var showRestoredBanner by mutableStateOf(false)
        internal set

    fun triggerRetry() {
        onRetryRequest()
    }
}

@SuppressLint("MissingPermission")
private fun queryNetworkSnapshot(connectivityManager: ConnectivityManager): Pair<Boolean, Boolean> {
    return runCatching {
        val activeNetwork = connectivityManager.activeNetwork ?: return false to false
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false to false
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isNotSuspended = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
        val downstreamKbps = caps.linkDownstreamBandwidthKbps
        val isSlow = downstreamKbps in 1..1800
        (hasInternet && !isNotSuspended) to isSlow
    }.getOrDefault(true to false)
}

/**
 * Real-time reactive observer for device internet connectivity and constrained/slow bandwidth.
 */
@SuppressLint("MissingPermission")
@Composable
fun rememberCampusNetworkState(): CampusNetworkState {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val connectivityManager = remember(context) {
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    val initialSnapshot = remember { queryNetworkSnapshot(connectivityManager) }
    var stateRef by remember { mutableStateOf<CampusNetworkState?>(null) }

    val networkState = remember {
        CampusNetworkState(
            isInitiallyOnline = initialSnapshot.first,
            isInitiallySlow = initialSnapshot.second,
            onRetryRequest = {
                val target = stateRef ?: return@CampusNetworkState
                coroutineScope.launch {
                    target.isRetrying = true
                    delay(900L)
                    val (onlineNow, slowNow) = queryNetworkSnapshot(connectivityManager)
                    val wasOffline = !target.isOnline
                    target.isOnline = onlineNow
                    target.isSlowNetwork = slowNow
                    target.isRetrying = false
                    if (wasOffline && onlineNow) {
                        target.showRestoredBanner = true
                    }
                }
            }
        ).also { stateRef = it }
    }

    LaunchedEffect(networkState.showRestoredBanner) {
        if (networkState.showRestoredBanner) {
            delay(2600L)
            networkState.showRestoredBanner = false
        }
    }

    DisposableEffect(connectivityManager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val (onlineNow, slowNow) = queryNetworkSnapshot(connectivityManager)
                val wasOffline = !networkState.isOnline
                networkState.isOnline = onlineNow
                networkState.isSlowNetwork = slowNow
                if (wasOffline && onlineNow) {
                    networkState.showRestoredBanner = true
                }
            }

            override fun onLost(network: Network) {
                val (onlineNow, slowNow) = queryNetworkSnapshot(connectivityManager)
                networkState.isOnline = onlineNow
                networkState.isSlowNetwork = slowNow
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val downstream = networkCapabilities.linkDownstreamBandwidthKbps
                val wasOffline = !networkState.isOnline
                networkState.isOnline = hasInternet
                networkState.isSlowNetwork = downstream in 1..1800
                if (wasOffline && hasInternet) {
                    networkState.showRestoredBanner = true
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        runCatching {
            connectivityManager.registerNetworkCallback(request, callback)
        }

        onDispose {
            runCatching {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }
    }

    return networkState
}

/**
 * Pattern A: Global Floating Top Offline Toast Banner
 * (Inspired by Reference Image 3, styled in Patriarch's elevated card aesthetic).
 * Slides down smoothly when the device loses internet connectivity, and briefly confirms
 * when connectivity is restored.
 */
@Composable
fun CampusOfflineTopBanner(
    isOnline: Boolean,
    showRestoredBanner: Boolean = false,
    onRetryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val visible = !isOnline || showRestoredBanner

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(260)),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(220)),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = if (!isOnline) Color(0xFFFFFBEB) else Color(0xFFF0FDF4),
            shadowElevation = 10.dp,
            border = BorderStroke(
                width = 1.dp,
                color = if (!isOnline) Color(0xFFFDE68A) else Color(0xFFBBF7D0)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 15.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Leading Exclamation / Check Circle Badge drawn in Canvas
                Canvas(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(22.dp)
                ) {
                    val badgeColor = if (!isOnline) Color(0xFF78716C) else Color(0xFF15803D)
                    drawCircle(
                        color = badgeColor,
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                    if (!isOnline) {
                        // Exclamation stem
                        drawLine(
                            color = badgeColor,
                            start = Offset(size.width / 2f, size.height * 0.27f),
                            end = Offset(size.width / 2f, size.height * 0.56f),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        // Exclamation dot
                        drawCircle(
                            color = badgeColor,
                            radius = 1.3.dp.toPx(),
                            center = Offset(size.width / 2f, size.height * 0.72f)
                        )
                    } else {
                        // Checkmark
                        val checkPath = Path().apply {
                            moveTo(size.width * 0.30f, size.height * 0.52f)
                            lineTo(size.width * 0.45f, size.height * 0.67f)
                            lineTo(size.width * 0.72f, size.height * 0.36f)
                        }
                        drawPath(
                            path = checkPath,
                            color = badgeColor,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (!isOnline) "Oops, something went wrong" else "Back online",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (!isOnline) {
                            "Your network connection appears to be offline."
                        } else {
                            "Your campus network connection has been restored."
                        },
                        fontSize = 14.sp,
                        color = Color(0xFF57534E),
                        lineHeight = 19.sp
                    )
                }

                if (!isOnline) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = CircleShape,
                        color = BrandBluePrimary.copy(alpha = 0.10f),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .clickable(onClick = onRetryClick)
                    ) {
                        Text(
                            text = "Retry",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBluePrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pattern B: Centered "No internet connection" Empty State / Sheet View
 * Uses the user-provided `no_internet.json` Lottie animation in the middle,
 * followed by the "No internet connection" heading, explanatory subtitle,
 * and outlined "Try again" pill button.
 */
@Composable
fun CampusNoInternetEmptyState(
    onTryAgainClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "No internet connection",
    subtitle: String = "It looks like you're offline. Please check your internet connection and try again."
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.no_internet)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.size(210.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = title,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = subtitle,
            fontSize = 15.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onTryAgainClick,
            shape = CircleShape,
            border = BorderStroke(1.2.dp, Color(0xFFCBD5E1)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF0F172A)
            ),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Try again",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
        }
    }
}

/**
 * Pattern C: Inline Search / Action Network Alert Card
 * (Inspired by Reference Image 2: Rose-bordered inline problem alert card inside Search
 * or action flows with "Still need help?" + primary action button below).
 */
@Composable
fun CampusInlineNetworkAlertCard(
    featureName: String = "Search",
    description: String = "We could not reach the campus network right now. Please check your internet connection and try again.",
    actionLabel: String = "Try search again",
    onActionClick: () -> Unit,
    onSecondarySupportClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Rose-bordered Problem Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFFFF5F5),
            border = BorderStroke(1.2.dp, Color(0xFFFCA5A5)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Solid crimson exclamation circle icon
                Canvas(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(22.dp)
                ) {
                    drawCircle(color = Color(0xFFDC2626))
                    drawLine(
                        color = Color.White,
                        start = Offset(size.width / 2f, size.height * 0.26f),
                        end = Offset(size.width / 2f, size.height * 0.56f),
                        strokeWidth = 2.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 1.4.dp.toPx(),
                        center = Offset(size.width / 2f, size.height * 0.74f)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sorry, there seems to be a problem with $featureName",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = description,
                        fontSize = 13.5.sp,
                        color = Color(0xFF334155),
                        lineHeight = 19.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Still need help?",
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (onSecondarySupportClick != null) {
                    onSecondarySupportClick()
                } else {
                    onActionClick()
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = actionLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
