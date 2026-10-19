package com.campus.network.ui.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.core.network.AuthenticatedSessionUser
import com.campus.network.core.network.SupabaseGoogleAuthClient
import com.campus.network.core.ui.theme.BrandBlueAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Verification Screen replicating the exact layout of the reference image:
 * - Top back button (white circular button with `<`)
 * - Centered bold lowercase "verify" heading
 * - 6 distinct white pill/oval code input bubbles
 * - "resend code ↻" action row
 * - Subtitle: "sent to <target>. wrong number/email?"
 * - Primary CTA pill button with loading spinner + "next"
 * - Styled with BrandBlueAuth background and white/auth-matching CTA button
 * - Custom 3x4 numeric keypad with rounded keys (1-9 with letters, 0, backspace)
 */
@Composable
fun VerifyScreen(
    target: String,
    isPhone: Boolean = false,
    onBack: () -> Unit,
    onVerificationSuccess: (AuthenticatedSessionUser) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val authClient = remember(context) { SupabaseGoogleAuthClient(context) }

    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resendCooldown by remember { mutableStateOf(0) }

    // Countdown timer for resend
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000L)
            resendCooldown -= 1
        }
    }

    fun submitVerification() {
        if (code.length < 6) {
            errorMessage = "Please enter the complete 6-digit code."
            return
        }
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            val result = authClient.verifyOtp(
                email = target,
                token = code,
                type = if (isPhone) "sms" else "signup"
            )
            isLoading = false
            result.onSuccess { sessionUser ->
                onVerificationSuccess(sessionUser)
            }.onFailure { err ->
                val friendly = SupabaseGoogleAuthClient.getFriendlyErrorMessage(err)
                errorMessage = friendly
                snackbarHostState.showSnackbar(friendly)
            }
        }
    }

    // Auto-submit when 6 digits are reached
    LaunchedEffect(code) {
        if (code.length == 6 && !isLoading) {
            submitVerification()
        }
    }

    fun handleResend() {
        if (resendCooldown > 0 || isResending) return
        coroutineScope.launch {
            isResending = true
            errorMessage = null
            val result = authClient.resendOtp(
                email = target,
                type = if (isPhone) "sms" else "signup"
            )
            isResending = false
            result.onSuccess {
                resendCooldown = 30
                snackbarHostState.showSnackbar("A fresh 6-digit code was sent to $target.")
            }.onFailure { err ->
                val friendly = SupabaseGoogleAuthClient.getFriendlyErrorMessage(err)
                errorMessage = friendly
                snackbarHostState.showSnackbar(friendly)
            }
        }
    }

    fun onKeyPress(key: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        errorMessage = null
        if (key == "BACKSPACE") {
            if (code.isNotEmpty()) {
                code = code.dropLast(1)
            }
        } else if (code.length < 6 && key.all { it.isDigit() }) {
            code += key
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "SpinnerTransition")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinnerRotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBlueAuth)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation Bar: Circular white back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 3.dp,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier
                                .size(18.dp)
                                .padding(start = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Main "verify" Header
            Text(
                text = "verify",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-0.6).sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 6 Distinct Pill Bubbles for OTP digits
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                for (i in 0 until 6) {
                    val digit = code.getOrNull(i)?.toString() ?: ""
                    val isCurrent = i == code.length
                    OtpBubble(
                        digit = digit,
                        isCurrent = isCurrent
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Resend Code Row with refresh icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        enabled = resendCooldown == 0 && !isResending,
                        onClick = { handleResend() }
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (resendCooldown > 0) "resend code in ${resendCooldown}s" else "resend code",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Resend Code",
                    tint = Color.White,
                    modifier = Modifier
                        .size(17.dp)
                        .then(if (isResending) Modifier.rotate(rotationAngle) else Modifier)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Subtitle Row: "sent to <target>. wrong email/number?"
            val wrongText = if (isPhone) "wrong number?" else "wrong email?"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "sent to $target. ",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 14.5.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = wrongText,
                    color = Color.White,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onBack() }
                )
            }

            // Inline error message if any
            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = error,
                        color = Color(0xFFB91C1C),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Primary CTA: White Pill Button with spinner icon + "next"
            Button(
                onClick = { submitVerification() },
                enabled = !isLoading,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = BrandBlueAuth
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(54.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = BrandBlueAuth,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        // Radial-spoke spinner matching reference image
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = BrandBlueAuth,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(rotationAngle)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "next",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlueAuth
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Custom On-Screen 3x4 Numeric Keypad
            CustomVerificationKeypad(
                onKeyPress = { onKeyPress(it) }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )
    }
}

/**
 * 6 Pill/Oval Bubbles matching the reference image's tall rounded digits.
 */
@Composable
private fun OtpBubble(
    digit: String,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        shadowElevation = if (isCurrent) 6.dp else 2.dp,
        modifier = modifier
            .width(48.dp)
            .height(64.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (digit.isNotEmpty()) {
                Text(
                    text = digit,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
            } else if (isCurrent) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(22.dp)
                        .background(BrandBlueAuth.copy(alpha = 0.5f), RoundedCornerShape(1.dp))
                )
            }
        }
    }
}

/**
 * Custom 3x4 On-Screen Keypad matching iOS / modern numeric entry pad in reference image:
 * Row 1: 1, 2 (ABC), 3 (DEF)
 * Row 2: 4 (GHI), 5 (JKL), 6 (MNO)
 * Row 3: 7 (PQRS), 8 (TUV), 9 (WXYZ)
 * Row 4: Empty, 0, Backspace (⌫)
 */
@Composable
private fun CustomVerificationKeypad(
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf("1" to "", "2" to "A B C", "3" to "D E F"),
        listOf("4" to "G H I", "5" to "J K L", "6" to "M N O"),
        listOf("7" to "P Q R S", "8" to "T U V", "9" to "W X Y Z"),
        listOf("" to "", "0" to "", "BACKSPACE" to "")
    )

    Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { (digit, sublabel) ->
                        if (digit.isEmpty()) {
                            // Transparent spacer for bottom-left empty key
                            Spacer(modifier = Modifier.weight(1f))
                        } else if (digit == "BACKSPACE") {
                            // Backspace key
                            Surface(
                                onClick = { onKeyPress("BACKSPACE") },
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                shadowElevation = 1.5.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Backspace",
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        } else {
                            // Number Key with letters
                            Surface(
                                onClick = { onKeyPress(digit) },
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                shadowElevation = 1.5.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = digit,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A),
                                        lineHeight = 24.sp
                                    )
                                    if (sublabel.isNotEmpty()) {
                                        Text(
                                            text = sublabel,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B),
                                            letterSpacing = 1.2.sp,
                                            lineHeight = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
