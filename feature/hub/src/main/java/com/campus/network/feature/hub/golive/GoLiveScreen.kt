package com.campus.network.feature.hub.golive

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.campus.network.core.network.OpenRouterAiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private val LiveBrush = Brush.horizontalGradient(listOf(Color(0xFFFF512F), Color(0xFFDD2476)))
private val PanelBg = Color(0xF2101014)
private val FieldBg = Color(0xFF1D1D24)
private val Muted = Color(0xFF9A9AA6)
private val Accent = Color(0xFFFF512F)

private val categories = listOf("Spotlight", "Campus Debate", "Tech Sprint", "Study Jam", "Music Live")

private enum class Phase { SETUP, COUNTDOWN, LIVE, SUMMARY }
private enum class Audience(val label: String, val icon: ImageVector) {
    CAMPUS("Whole campus", Icons.Default.Public),
    FOLLOWERS("Followers", Icons.Default.Group),
    SERVER("My server", Icons.Default.Groups)
}

private data class StreamSummary(val seconds: Int, val peakViewers: Int, val likes: Int, val newFollowers: Int)

/** Draft values survive rotation / process recreation via rememberSaveable. */
private class DraftState(
    private val titleState: MutableState<String>,
    private val descriptionState: MutableState<String>,
    private val categoryState: MutableState<String>,
    private val audienceState: MutableState<Audience>,
    private val commentsState: MutableState<Boolean>,
    private val replayState: MutableState<Boolean>
) {
    var title by titleState
    var description by descriptionState
    var category by categoryState
    var audience by audienceState
    var commentsOn by commentsState
    var saveReplay by replayState
}

@Composable
private fun rememberDraftState() = DraftState(
    rememberSaveable { mutableStateOf("") },
    rememberSaveable { mutableStateOf("") },
    rememberSaveable { mutableStateOf(categories.first()) },
    rememberSaveable { mutableStateOf(Audience.CAMPUS) },
    rememberSaveable { mutableStateOf(true) },
    rememberSaveable { mutableStateOf(true) }
)

/**
 * End-to-end Go Live flow: permissions -> details (with AI assist) -> 3-2-1 -> live host view -> summary.
 * One camera preview sits behind every phase so there is no flicker between steps.
 *
 * NOTE: there is no media backend wired up yet. Viewer counts, likes and comments in the
 * live phase are simulated until a broadcast service (RTMP/WebRTC) is connected.
 */
@Composable
fun GoLiveScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val ai = remember { OpenRouterAiClient() }

    val required = remember { arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO) }
    fun permissionsGranted() = required.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    var hasPermission by remember { mutableStateOf(permissionsGranted()) }
    var permanentlyDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermission = result.values.all { it }
        permanentlyDenied = !hasPermission
    }

    // Ask once on entry; re-check on resume so returning from system settings unlocks the flow.
    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(required) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) hasPermission = permissionsGranted()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Never let the screen sleep while setting up or broadcasting.
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val draft = rememberDraftState()
    var phase by rememberSaveable { mutableStateOf(Phase.SETUP) }
    var useFrontCamera by rememberSaveable { mutableStateOf(true) }
    var micOn by rememberSaveable { mutableStateOf(true) }
    var cameraUnavailable by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<StreamSummary?>(null) }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF08080B))) {
        if (hasPermission && phase != Phase.SUMMARY) {
            GoLiveCameraPreview(
                useFront = useFrontCamera,
                onUnavailable = { cameraUnavailable = true },
                modifier = Modifier.fillMaxSize()
            )
        }

        when {
            !hasPermission -> PermissionGate(
                permanentlyDenied = permanentlyDenied,
                onAllow = { permissionLauncher.launch(required) },
                onOpenSettings = { openAppSettings(context) },
                onClose = onExit
            )

            phase == Phase.SETUP -> SetupPhase(
                draft = draft,
                micOn = micOn,
                cameraUnavailable = cameraUnavailable,
                onToggleMic = { micOn = !micOn },
                onFlip = { useFrontCamera = !useFrontCamera },
                onClose = onExit,
                onWriteWithAi = { idea, category -> ai.generateStreamDraft(idea, category) },
                onGoLive = { phase = Phase.COUNTDOWN }
            )

            phase == Phase.COUNTDOWN -> CountdownPhase(
                onCancel = { phase = Phase.SETUP },
                onFinished = { phase = Phase.LIVE }
            )

            phase == Phase.LIVE -> LivePhase(
                title = draft.title,
                commentsEnabled = draft.commentsOn,
                micOn = micOn,
                onToggleMic = { micOn = !micOn },
                onFlip = { useFrontCamera = !useFrontCamera },
                onEnded = {
                    summary = it
                    phase = Phase.SUMMARY
                }
            )

            phase == Phase.SUMMARY -> summary?.let {
                SummaryPhase(
                    title = draft.title,
                    saveReplay = draft.saveReplay,
                    summary = it,
                    onShare = { shareStream(context, draft.title) },
                    onDone = onExit
                )
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

private fun shareStream(context: Context, title: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Catch the replay of \"$title\" on Campus Network!")
    }
    context.startActivity(Intent.createChooser(send, "Share stream").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

// ============================================================================
// Permission gate
// ============================================================================
@Composable
private fun PermissionGate(
    permanentlyDenied: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
        GlassButton(Icons.Default.Close, "Close", onClose, Modifier.align(Alignment.TopStart))
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(84.dp).clip(CircleShape).background(LiveBrush),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Videocam, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
            Spacer(Modifier.height(24.dp))
            Text("Allow camera & microphone", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(
                "So your campus can see and hear you live. We only use them while you're on the Go Live screen.",
                color = Muted, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            PrimaryButton(
                text = if (permanentlyDenied) "Open settings" else "Allow access",
                onClick = if (permanentlyDenied) onOpenSettings else onAllow
            )
        }
    }
}

// ============================================================================
// Phase 1: Setup (details + AI assist)
// ============================================================================
@Composable
private fun SetupPhase(
    draft: DraftState,
    micOn: Boolean,
    cameraUnavailable: Boolean,
    onToggleMic: () -> Unit,
    onFlip: () -> Unit,
    onClose: () -> Unit,
    onWriteWithAi: suspend (idea: String, category: String) -> OpenRouterAiClient.AiStreamDraft,
    onGoLive: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var aiLoading by remember { mutableStateOf(false) }
    val maxPanel = (LocalConfiguration.current.screenHeightDp * 0.66f).dp

    BackHandler(onBack = onClose)

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrims keep icons and the panel legible over any camera frame.
        Box(
            Modifier.fillMaxWidth().height(140.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(0.55f), Color.Transparent)))
        )

        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassButton(Icons.Default.Close, "Close", onClose)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(Icons.Default.FlipCameraAndroid, "Flip camera", onFlip)
                GlassButton(if (micOn) Icons.Default.Mic else Icons.Default.MicOff, "Toggle microphone", onToggleMic)
            }
        }

        if (cameraUnavailable) {
            Text(
                "No camera available on this device",
                color = Color.White, fontSize = 14.sp,
                modifier = Modifier.align(Alignment.Center).clip(CircleShape)
                    .background(Color.Black.copy(0.6f)).padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .heightIn(max = maxPanel)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(PanelBg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text("Go Live", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text("Tell your campus what's happening", color = Muted, fontSize = 13.sp)

            Spacer(Modifier.height(16.dp))

            // Title + AI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("Title")
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Accent.copy(alpha = 0.16f))
                        .clickable(enabled = !aiLoading) {
                            aiLoading = true
                            scope.launch {
                                val result = onWriteWithAi(draft.title, draft.category)
                                draft.title = result.title
                                draft.description = buildString {
                                    append(result.description)
                                    if (result.hashtags.isNotEmpty()) {
                                        append("\n")
                                        append(result.hashtags.joinToString(" ") { "#$it" })
                                    }
                                }.trim()
                                aiLoading = false
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (aiLoading) {
                        CircularProgressIndicator(Modifier.size(14.dp), color = Accent, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, null, tint = Accent, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(if (aiLoading) "Writing…" else "Write with AI", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            DarkField(
                value = draft.title,
                onValueChange = { draft.title = it.take(60) },
                placeholder = "e.g. Student founders showcase live",
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))
            SectionLabel("Category")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { tag ->
                    val selected = draft.category == tag
                    Text(
                        text = tag,
                        color = if (selected) Color.White else Muted,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .then(if (selected) Modifier.background(LiveBrush) else Modifier.background(FieldBg))
                            .clickable { draft.category = tag }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("Description (optional)")
            Spacer(Modifier.height(8.dp))
            DarkField(
                value = draft.description,
                onValueChange = { draft.description = it.take(200) },
                placeholder = "What should viewers expect?",
                singleLine = false,
                minLines = 2
            )

            Spacer(Modifier.height(16.dp))
            SectionLabel("Who can watch")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Audience.values().forEach { option ->
                    val selected = draft.audience == option
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) Accent.copy(alpha = 0.18f) else FieldBg)
                            .border(1.dp, if (selected) Accent else Color.Transparent, RoundedCornerShape(14.dp))
                            .clickable { draft.audience = option }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(option.icon, null, tint = if (selected) Accent else Muted, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(option.label, color = if (selected) Color.White else Muted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            ToggleRow("Live comments", "Let viewers chat while you stream", Icons.Default.Chat, draft.commentsOn) { draft.commentsOn = it }
            ToggleRow("Save replay", "Keep the stream on your profile after it ends", Icons.Default.Videocam, draft.saveReplay) { draft.saveReplay = it }

            Spacer(Modifier.height(14.dp))
            PrimaryButton(
                text = "Go Live",
                icon = Icons.Default.Videocam,
                enabled = draft.title.isNotBlank() && !cameraUnavailable,
                onClick = onGoLive
            )
            if (draft.title.isBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Add a title, or tap Write with AI, to go live",
                    color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ============================================================================
// Phase 2: 3-2-1 countdown
// ============================================================================
@Composable
private fun CountdownPhase(onCancel: () -> Unit, onFinished: () -> Unit) {
    var count by remember { mutableIntStateOf(3) }
    val scale = remember(count) { Animatable(1.7f) }

    BackHandler(onBack = onCancel)
    LaunchedEffect(Unit) {
        for (n in 3 downTo 1) {
            count = n
            delay(1000)
        }
        onFinished()
    }
    LaunchedEffect(count) { scale.animateTo(1f, tween(650)) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                color = Color.White,
                fontSize = 120.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
            )
            Text("You're going live", color = Color.White.copy(0.85f), fontSize = 16.sp)
        }
        TextButton(
            onClick = onCancel,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 32.dp)
        ) { Text("Cancel", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
}

// ============================================================================
// Phase 3: Live host view
// ============================================================================
private val simulatedViewers = listOf("Amina_M", "Kevin.Dev", "Wanjiku.Codes", "Brian_Tech", "Grace_N", "Dan_Finance", "Esther_B", "Chris_Mech")
private val simulatedComments = listOf(
    "Loving this!", "Can you hear me clearly?", "Hi from the library \uD83D\uDC4B", "Great topic",
    "Drop the link please", "First time here, so good", "Audio is crisp", "Can you go over that again?"
)

@Composable
private fun LivePhase(
    title: String,
    commentsEnabled: Boolean,
    micOn: Boolean,
    onToggleMic: () -> Unit,
    onFlip: () -> Unit,
    onEnded: (StreamSummary) -> Unit
) {
    var seconds by remember { mutableIntStateOf(0) }
    var viewers by remember { mutableIntStateOf(1) }
    var peak by remember { mutableIntStateOf(1) }
    var likes by remember { mutableIntStateOf(0) }
    var showEndDialog by remember { mutableStateOf(false) }
    val comments = remember { mutableStateListOf<Pair<String, String>>() }

    BackHandler { showEndDialog = true }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            seconds++
        }
    }
    // Simulated engagement until a real broadcast backend exists.
    LaunchedEffect(Unit) {
        while (true) {
            delay(2200)
            viewers = (viewers + Random.nextInt(-1, 6)).coerceAtLeast(1)
            peak = maxOf(peak, viewers)
            likes += Random.nextInt(0, 4)
        }
    }
    LaunchedEffect(commentsEnabled) {
        while (commentsEnabled) {
            delay(Random.nextLong(1800, 4200))
            comments.add(simulatedViewers.random() to simulatedComments.random())
            if (comments.size > 30) comments.removeAt(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxWidth().height(150.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(0.6f), Color.Transparent)))
        )
        Box(
            Modifier.fillMaxWidth().height(300.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f))))
        )

        // Top bar: LIVE + timer + viewers, title underneath
        Column(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "LIVE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.clip(CircleShape).background(LiveBrush).padding(horizontal = 12.dp, vertical = 5.dp)
                )
                StatPill(formatDuration(seconds))
                StatPill(viewers.toString(), Icons.Default.Visibility)
            }
            Spacer(Modifier.height(10.dp))
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2)
        }

        // Comment feed + controls
        Column(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 16.dp)
        ) {
            if (commentsEnabled) {
                val visible = comments.takeLast(5)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth(0.78f)) {
                    visible.forEachIndexed { index, (name, text) ->
                        val alpha = 0.45f + 0.55f * (index + 1) / visible.size
                        Text(
                            text = "$name  $text",
                            color = Color.White.copy(alpha = alpha),
                            fontSize = 13.sp,
                            modifier = Modifier.clip(RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(0.35f)).padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassButton(Icons.Default.FlipCameraAndroid, "Flip camera", onFlip)
                    GlassButton(if (micOn) Icons.Default.Mic else Icons.Default.MicOff, "Toggle microphone", onToggleMic)
                    StatPill(likes.toString(), Icons.Default.Favorite)
                }
                Text(
                    "End", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(CircleShape).background(Color(0xFFE5252A))
                        .clickable { showEndDialog = true }.padding(horizontal = 26.dp, vertical = 11.dp)
                )
            }
        }
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            containerColor = Color(0xFF1B1B22),
            title = { Text("End live stream?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Your viewers will be disconnected. You can't resume this stream.", color = Muted) },
            confirmButton = {
                TextButton(onClick = {
                    showEndDialog = false
                    onEnded(StreamSummary(seconds, peak, likes, newFollowers = peak / 5))
                }) { Text("End stream", color = Color(0xFFFF5A5F), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) { Text("Keep streaming", color = Color.White) }
            }
        )
    }
}

// ============================================================================
// Phase 4: Summary
// ============================================================================
@Composable
private fun SummaryPhase(
    title: String,
    saveReplay: Boolean,
    summary: StreamSummary,
    onShare: () -> Unit,
    onDone: () -> Unit
) {
    BackHandler(onBack = onDone)
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF34D399), modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Stream ended", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(title, color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 2)
        if (saveReplay) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Replay saved to your profile", color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).background(Color(0xFF34D399).copy(0.14f)).padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryStat("Duration", formatDuration(summary.seconds), Modifier.weight(1f))
            SummaryStat("Peak viewers", summary.peakViewers.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryStat("Likes", summary.likes.toString(), Modifier.weight(1f))
            SummaryStat("New followers", "+${summary.newFollowers}", Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
        if (saveReplay) {
            Row(
                modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, Color.White.copy(0.25f)), RoundedCornerShape(16.dp))
                    .clickable(onClick = onShare),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share replay", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
        }
        PrimaryButton(text = "Done", onClick = onDone)
    }
}

// ============================================================================
// Shared bits
// ============================================================================
private fun formatDuration(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Color(0xFFD0D0DA), fontSize = 13.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun GlassButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, description, tint = Color.White, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun StatPill(text: String, icon: ImageVector? = null) {
    Row(
        modifier = Modifier.clip(CircleShape).background(Color.Black.copy(0.45f)).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).background(FieldBg).padding(16.dp)
    ) {
        Text(label, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Muted, fontSize = 11.5.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Accent, checkedThumbColor = Color.White)
        )
    }
}

@Composable
private fun DarkField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    minLines: Int = 1
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Muted, fontSize = 14.sp) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = if (singleLine) ImeAction.Done else ImeAction.Default
        ),
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = FieldBg, unfocusedContainerColor = FieldBg,
            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            cursorColor = Accent,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, icon: ImageVector? = null, enabled: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(if (enabled) Modifier.background(LiveBrush) else Modifier.background(FieldBg))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (enabled) Color.White else Muted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = if (enabled) Color.White else Muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
