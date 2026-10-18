package com.campus.network.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.BuildConfig
import com.campus.network.R
import com.campus.network.core.network.SupabaseGoogleAuthClient
import com.campus.network.core.ui.theme.BrandBlueAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

private val BrandAuthBlue = BrandBlueAuth

/**
 * Auth Screen incorporating SocialPulse's Google Sign-In / Sign-Up + Supabase Email/Password Auth flow:
 * - Native Google Sign-In (`GoogleSignIn` configured with `BuildConfig.GOOGLE_CLIENT_ID_WEB`
 *   exchanging `idToken` via `supabase.auth.signInWithIdToken({ provider: 'google', token })`).
 * - Unified Log In (`activeTab = 0`) and Sign Up (`activeTab = 1`) flow with Google button on both tabs,
 *   `"or"` divider, Email/Password fields, loading indicator, friendly error messages, and one-tap tab switching.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onLoginSuccess: (email: String, fullName: String?, avatarUrl: String?) -> Unit,
    onContinueAsGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val authClient = remember(context) { SupabaseGoogleAuthClient(context) }

    var showAuthSheet by remember { mutableStateOf(false) }
    // 0 = Log In, 1 = Sign Up (Register) - matching SocialPulse app/auth/flow.tsx
    var activeTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var statusBannerMessage by remember { mutableStateOf<String?>(null) }
    var isStatusError by remember { mutableStateOf(false) }

    // Form States (matching SocialPulse app/auth/flow.tsx)
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var regFullName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun showFeedback(message: String, isError: Boolean = false) {
        statusBannerMessage = message
        isStatusError = isError
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    fun handleGoogleAccountAuthenticated(account: GoogleSignInAccount) {
        coroutineScope.launch {
            isLoading = true
            statusBannerMessage = null
            val idToken = account.idToken
            val email = account.email ?: ""
            val displayName = account.displayName
            val photoUrl = account.photoUrl?.toString()

            val result = if (!idToken.isNullOrBlank()) {
                authClient.signInWithGoogleIdToken(
                    idToken = idToken,
                    fallbackEmail = email,
                    fallbackName = displayName,
                    fallbackAvatarUrl = photoUrl
                )
            } else if (email.isNotBlank()) {
                authClient.signInWithGoogleAccountProfile(
                    email = email,
                    fullName = displayName,
                    avatarUrl = photoUrl,
                    googleId = account.id
                )
            } else {
                Result.failure(IllegalStateException("Unable to retrieve Google account email."))
            }

            isLoading = false
            result.onSuccess { sessionUser ->
                showAuthSheet = false
                onLoginSuccess(
                    sessionUser.email,
                    sessionUser.fullName ?: displayName,
                    sessionUser.avatarUrl ?: photoUrl
                )
            }.onFailure { err ->
                showFeedback(SupabaseGoogleAuthClient.getFriendlyErrorMessage(err), isError = true)
            }
        }
    }

    // Fallback launcher if Play Services returns DEVELOPER_ERROR (code 10) due to SHA-1 not yet added in Google Cloud Console
    val fallbackGoogleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            handleGoogleAccountAuthenticated(account)
        } catch (e: ApiException) {
            isLoading = false
            if (e.statusCode != 12501) { // 12501 = SIGN_IN_CANCELLED
                showFeedback(
                    SupabaseGoogleAuthClient.getFriendlyErrorMessage(e),
                    isError = true
                )
            }
        }
    }

    // Primary Native Google Sign-In Launcher configured with SocialPulse's GOOGLE_CLIENT_ID_WEB
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            handleGoogleAccountAuthenticated(account)
        } catch (e: ApiException) {
            if (e.statusCode == 10) {
                // Code 10 = DEVELOPER_ERROR (SHA-1 fingerprint or package name not yet registered for requestIdToken).
                // Automatically fall back to basic Google Sign-In profile picker so Google Sign-In still works on device.
                val fallbackGso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestProfile()
                    .build()
                val fallbackClient = GoogleSignIn.getClient(context, fallbackGso)
                fallbackClient.signOut().addOnCompleteListener {
                    fallbackGoogleLauncher.launch(fallbackClient.signInIntent)
                }
            } else {
                isLoading = false
                if (e.statusCode != 12501 && result.resultCode != Activity.RESULT_CANCELED) {
                    showFeedback(
                        SupabaseGoogleAuthClient.getFriendlyErrorMessage(e),
                        isError = true
                    )
                }
            }
        }
    }

    fun launchGoogleSignIn() {
        isLoading = true
        statusBannerMessage = null
        val webClientId = BuildConfig.GOOGLE_CLIENT_ID_WEB
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
        if (webClientId.isNotBlank()) {
            gsoBuilder.requestIdToken(webClientId)
        }
        val googleSignInClient = GoogleSignIn.getClient(context, gsoBuilder.build())
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    fun handleEmailLogin() {
        if (loginEmail.isBlank() || loginPassword.isBlank()) {
            showFeedback("Missing credentials", isError = true)
            return
        }
        coroutineScope.launch {
            isLoading = true
            statusBannerMessage = null
            val result = authClient.signInWithPassword(
                email = loginEmail.trim(),
                password = loginPassword
            )
            isLoading = false
            result.onSuccess { user ->
                showAuthSheet = false
                onLoginSuccess(user.email, user.fullName, user.avatarUrl)
            }.onFailure { err ->
                showFeedback(SupabaseGoogleAuthClient.getFriendlyErrorMessage(err), isError = true)
            }
        }
    }

    fun handleEmailRegister() {
        if (regFullName.isBlank() || regEmail.isBlank() || regPassword.isBlank()) {
            showFeedback("Fill all fields", isError = true)
            return
        }
        coroutineScope.launch {
            isLoading = true
            statusBannerMessage = null
            val result = authClient.signUpWithEmail(
                fullName = regFullName.trim(),
                email = regEmail.trim(),
                password = regPassword
            )
            isLoading = false
            result.onSuccess { user ->
                if (user.requiresEmailConfirmation) {
                    loginEmail = regEmail.trim()
                    loginPassword = regPassword
                    activeTab = 0
                    showFeedback("Check your email to verify your account!", isError = false)
                } else {
                    showAuthSheet = false
                    onLoginSuccess(
                        user.email.ifBlank { regEmail.trim() },
                        user.fullName ?: regFullName.trim(),
                        user.avatarUrl
                    )
                }
            }.onFailure { err ->
                showFeedback(SupabaseGoogleAuthClient.getFriendlyErrorMessage(err), isError = true)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BrandAuthBlue)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.9f))

            // Brand Logo & Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_logo_transparent),
                    contentDescription = "Patriarch Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(104.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Patriarch",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Solid White "Continue with Google" Pill Button
                Button(
                    onClick = { launchGoogleSignIn() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF0F172A)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        GoogleLogoCanvas(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Continue with Google",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }

                // 2. Solid White/Translucent "Log in" Pill Button
                Button(
                    onClick = {
                        activeTab = 0
                        statusBannerMessage = null
                        showAuthSheet = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.92f),
                        contentColor = BrandAuthBlue
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Log in with Email",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandAuthBlue
                    )
                }

                // 3. Outlined "Continue as guest" Pill Button
                OutlinedButton(
                    onClick = onContinueAsGuest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.5.dp, Color.White),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Continue as guest",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 4. "Join the Patriarch program ->" Link (Opens Sign Up tab)
                Row(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            activeTab = 1
                            statusBannerMessage = null
                            showAuthSheet = true
                        }
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Join the Patriarch\u00AE program",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1.1f))

            // Footer Link: "Privacy policy"
            Text(
                text = "Privacy policy",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clickable {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Privacy policy: Your data is secure with Patriarch.")
                        }
                    }
                    .padding(vertical = 16.dp, horizontal = 24.dp)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )

        // Unified SocialPulse-style Auth Modal Sheet (Log In <-> Sign Up)
        if (showAuthSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    if (!isLoading) {
                        showAuthSheet = false
                    }
                },
                sheetState = sheetState,
                containerColor = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                    },
                    label = "AuthTabTransition"
                ) { tab ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .navigationBarsPadding()
                    ) {
                        // Header matching SocialPulse flow.tsx
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (tab == 0) "Hi There!" else "Welcome!",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A),
                                letterSpacing = (-0.8).sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (tab == 0) {
                                    "Log in to manage your campus pulse."
                                } else {
                                    "Create your verified campus account today."
                                },
                                fontSize = 15.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Status / Error Banner inside Sheet
                        statusBannerMessage?.let { msg ->
                            Surface(
                                color = if (isStatusError) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Text(
                                    text = msg,
                                    color = if (isStatusError) Color(0xFFB91C1C) else Color(0xFF15803D),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }

                        // Social Sign-In / Sign-Up Button (Google) matching SocialPulse's socialRow
                        OutlinedButton(
                            onClick = { launchGoogleSignIn() },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF0F172A)
                            )
                        ) {
                            GoogleLogoCanvas(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (tab == 0) "Sign in with Google" else "Sign up with Google",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        // "or" Divider Row matching SocialPulse
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE2E8F0)
                            )
                            Text(
                                text = "or",
                                color = Color(0xFF64748B),
                                fontSize = 12.5.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE2E8F0)
                            )
                        }

                        if (tab == 0) {
                            // LOGIN VIEW
                            OutlinedTextField(
                                value = loginEmail,
                                onValueChange = { loginEmail = it },
                                placeholder = { Text("Email", color = Color(0xFF64748B)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Email,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(28.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = BrandAuthBlue,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A)
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = { loginPassword = it },
                                placeholder = { Text("Password", color = Color(0xFF64748B)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password visibility",
                                            tint = Color(0xFF64748B)
                                        )
                                    }
                                },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(28.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = BrandAuthBlue,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A)
                                )
                            )

                            Spacer(modifier = Modifier.height(22.dp))

                            Button(
                                onClick = { handleEmailLogin() },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandAuthBlue)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Log In",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "New here? ",
                                    color = Color(0xFF64748B),
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Sign Up",
                                    color = BrandAuthBlue,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        statusBannerMessage = null
                                        activeTab = 1
                                    }
                                )
                            }
                        } else {
                            // REGISTER VIEW
                            OutlinedTextField(
                                value = regFullName,
                                onValueChange = { regFullName = it },
                                placeholder = { Text("Full Name", color = Color(0xFF64748B)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(28.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = BrandAuthBlue,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A)
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = regEmail,
                                onValueChange = { regEmail = it },
                                placeholder = { Text("Email", color = Color(0xFF64748B)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Email,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(28.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = BrandAuthBlue,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A)
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = regPassword,
                                onValueChange = { regPassword = it },
                                placeholder = { Text("Password", color = Color(0xFF64748B)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password visibility",
                                            tint = Color(0xFF64748B)
                                        )
                                    }
                                },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(28.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = BrandAuthBlue,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A)
                                )
                            )

                            Spacer(modifier = Modifier.height(22.dp))

                            Button(
                                onClick = { handleEmailRegister() },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandAuthBlue)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Create Account",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Have an account? ",
                                    color = Color(0xFF64748B),
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Log In",
                                    color = BrandAuthBlue,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        statusBannerMessage = null
                                        activeTab = 0
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

/**
 * Custom multi-color Google "G" emblem drawn in Compose Canvas.
 */
@Composable
private fun GoogleLogoCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.22f
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
        val arcSize = Size(diameter, diameter)

        // Red top arc
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 215f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Yellow left arc
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 140f,
            sweepAngle = 75f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Green bottom arc
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Blue right arc
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -10f,
            sweepAngle = 55f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Blue horizontal bar of the "G"
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(size.width * 0.50f, size.height * 0.50f),
            end = Offset(size.width * 0.92f, size.height * 0.50f),
            strokeWidth = strokeWidth * 0.95f,
            cap = StrokeCap.Butt
        )
    }
}
