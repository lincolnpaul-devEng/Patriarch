package com.campus.network.feature.messages

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.campus.network.core.ui.theme.AlertRed
import com.campus.network.core.ui.theme.BrandBlueMid
import com.campus.network.core.ui.theme.BrandBluePrimary
import com.campus.network.core.ui.theme.BrandGreen
import com.campus.network.core.ui.theme.TextWhite
import com.campus.network.feature.messages.audio.VoiceFilterPreset
import com.campus.network.feature.messages.components.InCallMoreOptionsSheet
import com.campus.network.feature.messages.components.StreamLocalParticipantVideo
import com.campus.network.feature.messages.components.StreamRemoteParticipantVideo
import com.campus.network.feature.messages.components.VoiceMorphSheet
import com.campus.network.feature.messages.components.peekActiveStreamCall

@Composable
fun VideoCallScreen(
    callState: CallUiState,
    onEvent: (MessagesEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // When connected, user can tap PiP to swap who is fullscreen vs in the floating PiP window.
    // Default when connected: remote caller is fullscreen, local camera is in PiP (isLocalFullscreen = false).
    // While CALLING or RINGING: local camera preview fills the entire screen, and PiP is completely hidden.
    var isLocalFullscreen by remember { mutableStateOf(false) }
    var isMoreOptionsOpen by remember { mutableStateOf(false) }
    var isVoiceFilterOpen by remember { mutableStateOf(false) }
    val useStreamMedia = callState.status == CallStatus.CONNECTED &&
        callState.mediaProviderReady &&
        peekActiveStreamCall() != null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Fullscreen Media Viewport
        val showLocalInFullscreen = (callState.status != CallStatus.CONNECTED) || isLocalFullscreen

        if (showLocalInFullscreen) {
            if (useStreamMedia) {
                StreamLocalParticipantVideo(modifier = Modifier.fillMaxSize())
            } else {
                CameraPreviewSurface(
                    isFrontCamera = callState.isFrontCamera,
                    isVideoEnabled = callState.isVideoEnabled,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            if (useStreamMedia) {
                StreamRemoteParticipantVideo(modifier = Modifier.fillMaxSize())
            } else {
                RemoteParticipantSurface(
                    callState = callState,
                    isPip = false,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 2. Picture-in-Picture (PiP) Window
        // Strictly displayed ONLY after the other caller accepts the call (CallStatus.CONNECTED)
        AnimatedVisibility(
            visible = callState.status == CallStatus.CONNECTED,
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 70.dp, start = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E293B),
                shadowElevation = 10.dp,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0x66FFFFFF)),
                modifier = Modifier
                    .size(width = 105.dp, height = 150.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        isLocalFullscreen = !isLocalFullscreen
                    }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isLocalFullscreen) {
                        // Remote is in PiP
                        if (useStreamMedia) {
                            StreamRemoteParticipantVideo(modifier = Modifier.fillMaxSize())
                        } else {
                            RemoteParticipantSurface(
                                callState = callState,
                                isPip = true,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0x99000000),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = callState.peerName,
                                fontSize = 9.sp,
                                color = TextWhite,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    } else {
                        // Local Camera is in PiP
                        if (useStreamMedia) {
                            StreamLocalParticipantVideo(modifier = Modifier.fillMaxSize())
                        } else {
                            CameraPreviewSurface(
                                isFrontCamera = callState.isFrontCamera,
                                isVideoEnabled = callState.isVideoEnabled,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0x99000000),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "You",
                                fontSize = 9.sp,
                                color = TextWhite,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Top Left: Minimize / Return
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp)
        ) {
            IconButton(
                onClick = { onEvent(MessagesEvent.EndCall) },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x80181F26))
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Minimize Call",
                    tint = TextWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 4. Top Center: Peer Name + Call Status / Duration
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 68.dp, end = 68.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = callState.peerName,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.75f),
                        blurRadius = 8f
                    )
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when (callState.status) {
                    CallStatus.CALLING -> "Calling"
                    CallStatus.RINGING -> "Ringing"
                    CallStatus.INCOMING -> "Incoming call"
                    CallStatus.CONNECTED -> formatCallDuration(callState.durationSeconds)
                    CallStatus.FAILED -> callState.connectionState.ifBlank { "Call ended" }
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xEEFFFFFF),
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.75f),
                        blurRadius = 8f
                    )
                )
            )

            if (callState.activeVoiceFilter != VoiceFilterPreset.NATURAL) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFDD2476).copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFF512F)),
                    modifier = Modifier.clickable { isVoiceFilterOpen = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFFFF8DA1),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Voice: ${callState.activeVoiceFilter.title}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF8DA1)
                        )
                    }
                }
            }
        }

        // 5. Top Right: Vertical action buttons (Add Participant + Camera Switch)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = { /* Add participant */ },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xB3181F26))
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Add Participant",
                    tint = TextWhite,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = { onEvent(MessagesEvent.FlipCamera) },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xB3181F26))
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Flip Camera",
                    tint = TextWhite,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 6. Bottom dock — Answer/Decline for incoming, otherwise in-call controls
        if (callState.status == CallStatus.INCOMING || callState.isIncoming) {
            IncomingCallActions(
                onDecline = { onEvent(MessagesEvent.DeclineCall) },
                onAccept = { onEvent(MessagesEvent.AcceptCall) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 28.dp)
                    .fillMaxWidth()
            )
        } else {
            Surface(
                shape = RoundedCornerShape(36.dp),
                color = Color(0xF0181F26),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .fillMaxWidth()
                    .height(72.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isMoreOptionsOpen = true },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (isMoreOptionsOpen || isVoiceFilterOpen || (callState.activeVoiceFilter != VoiceFilterPreset.NATURAL)) {
                                    Color(0xFFDD2476).copy(alpha = 0.4f)
                                } else {
                                    Color(0x33FFFFFF)
                                }
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "More Options",
                            tint = TextWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = { onEvent(MessagesEvent.ToggleSpeaker) },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (callState.isSpeakerOn) Color.White else Color(0x33FFFFFF))
                    ) {
                        Icon(
                            imageVector = if (callState.isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Speaker",
                            tint = if (callState.isSpeakerOn) Color(0xFF181F26) else TextWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = { onEvent(MessagesEvent.ToggleVideo) },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (callState.isVideoEnabled) Color(0x33FFFFFF) else AlertRed.copy(alpha = 0.85f))
                    ) {
                        Icon(
                            imageVector = if (callState.isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Toggle Video",
                            tint = TextWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = { onEvent(MessagesEvent.ToggleMute) },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                    ) {
                        Icon(
                            imageVector = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Toggle Microphone",
                            tint = TextWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = { onEvent(MessagesEvent.EndCall) },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(AlertRed)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = TextWhite,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        // In-Call More Options Sheet
        if (isMoreOptionsOpen) {
            InCallMoreOptionsSheet(
                callState = callState,
                onOpenVoiceFilter = {
                    isMoreOptionsOpen = false
                    isVoiceFilterOpen = true
                },
                onDismiss = { isMoreOptionsOpen = false }
            )
        }

        // Dedicated Voice Morphing Sheet
        if (isVoiceFilterOpen) {
            VoiceMorphSheet(
                callState = callState,
                onEvent = onEvent,
                onDismiss = { isVoiceFilterOpen = false }
            )
        }
    }
}

@Composable
fun CameraPreviewSurface(
    isFrontCamera: Boolean,
    isVideoEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (_: Exception) {}
        }
    }

    if (!isVideoEnabled) {
        Box(
            modifier = modifier.background(Color(0xFF101722)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.VideocamOff,
                    contentDescription = "Camera Off",
                    tint = Color(0x66FFFFFF),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Camera Off",
                    fontSize = 11.sp,
                    color = Color(0x99FFFFFF)
                )
            }
        }
    } else if (!hasCameraPermission) {
        Box(
            modifier = modifier.background(Color(0xFF101722)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Camera Permission",
                    tint = Color(0x66FFFFFF),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Camera permission required",
                    fontSize = 11.sp,
                    color = Color(0x99FFFFFF)
                )
            }
        }
    } else {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                bindCamera(ctx, lifecycleOwner, previewView, isFrontCamera)
                previewView
            },
            update = { previewView ->
                bindCamera(previewView.context, lifecycleOwner, previewView, isFrontCamera)
            }
        )
    }
}

private fun bindCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    isFrontCamera: Boolean
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = if (isFrontCamera) {
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
            } else {
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
            }
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        } catch (_: Exception) {}
    }, ContextCompat.getMainExecutor(context))
}

@Composable
private fun RemoteParticipantSurface(
    callState: CallUiState,
    isPip: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1E2D44),
                    Color(0xFF0F172A),
                    Color(0xFF030712)
                )
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(if (isPip) 4.dp else 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (isPip) 48.dp else 110.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(BrandBluePrimary, BrandBlueMid)
                        )
                    )
                    .border(2.dp, Color(0x66FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (callState.peerInitials.isNotBlank()) {
                    Text(
                        text = callState.peerInitials,
                        fontSize = if (isPip) 18.sp else 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = callState.peerName,
                        tint = TextWhite,
                        modifier = Modifier.size(if (isPip) 24.dp else 60.dp)
                    )
                }
            }

            if (!isPip) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = callState.peerName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x40000000)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(BrandGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (callState.mediaProviderReady) {
                                "Stream Video • Connected"
                            } else {
                                "Signaling only • media pending"
                            },
                            fontSize = 11.sp,
                            color = Color(0xCCFFFFFF)
                        )
                    }
                }
            }
        }

        if (!isPip) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0x80000000),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 110.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(BrandGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${callState.peerName} (Remote)",
                        fontSize = 11.sp,
                        color = TextWhite
                    )
                }
            }
        }
    }
}
