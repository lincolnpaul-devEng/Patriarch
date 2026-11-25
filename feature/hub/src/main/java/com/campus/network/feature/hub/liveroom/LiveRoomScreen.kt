package com.campus.network.feature.hub.liveroom

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAddAlt1
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campus.network.core.data.LiveRoomPresence
import com.campus.network.core.data.LiveStreamRepository
import com.campus.network.core.data.StudentProfileRepository
import com.campus.network.core.model.LiveStream
import com.campus.network.core.model.StudentProfile
import com.campus.network.core.model.SyncStatus
import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.model.stableUserId
import com.campus.network.core.network.call.LiveMediaConnectionState
import com.campus.network.core.network.call.LiveMediaRole
import com.campus.network.core.network.call.LiveMediaState
import com.campus.network.core.network.call.LiveStreamMediaRegistry
import com.campus.network.core.ui.components.CampusStudentAvatar
import com.campus.network.core.ui.theme.SquadaOne
import com.campus.network.feature.hub.golive.GoLiveCameraPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val Live = Color(0xFFE6F23C)
private val Glass = Color.White.copy(alpha = .20f)
private val Ink = Color(0xFF111111)
private val AppFont = SquadaOne

internal data class LiveRoomParticipant(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val backdropUrl: String? = null,
    val streamId: String? = null,
    val streamTitle: String? = null,
    val live: Boolean = false,
    val hand: Boolean = false,
    val muted: Boolean = false,
    val isFeaturedHost: Boolean = false,
    val isSelf: Boolean = false,
    val viewerCount: Int = 0,
    val likeCount: Int = 0,
)

internal fun displayFirstName(fullName: String, handle: String = ""): String {
    val clean = fullName.trim()
    if (clean.isRealProfileDisplayName()) {
        val token = clean.split(Regex("\\s+")).firstOrNull()?.trim().orEmpty()
        if (token.isNotEmpty()) return token
    }
    val cleanHandle = handle.trim().removePrefix("@").trim()
    if (cleanHandle.isNotEmpty() && !cleanHandle.equals("broadcaster", ignoreCase = true)) {
        return cleanHandle
    }
    return "Host"
}

internal fun buildLiveRoomParticipants(
    activeStreams: List<LiveStream>,
    allStreams: List<LiveStream>,
    knownProfiles: Map<String, StudentProfile>,
    currentProfile: StudentProfile?,
    roomPresence: List<LiveRoomPresence> = emptyList(),
): List<LiveRoomParticipant> {
    val result = mutableListOf<LiveRoomParticipant>()
    val seenIds = mutableSetOf<String>()
    val seenNames = mutableSetOf<String>()
    val selfId = currentProfile?.stableUserId()?.trim().orEmpty()

    val presenceById = roomPresence.associateBy { it.userId.trim().lowercase() }
    val presenceByName = roomPresence
        .filter { it.displayName.isRealProfileDisplayName() }
        .associateBy { it.displayName.trim().lowercase() }
    val activeStreamByBroadcaster = activeStreams
        .filter { it.isLive }
        .associateBy { it.broadcasterId.trim().lowercase() }

    fun findPresence(userId: String, displayName: String): LiveRoomPresence? {
        val trimmedId = userId.trim().lowercase()
        if (trimmedId.isNotEmpty()) {
            presenceById[trimmedId]?.let { return it }
        }
        val cleanName = displayName.trim().lowercase()
        if (cleanName.isNotEmpty()) {
            presenceByName[cleanName]?.let { return it }
        }
        return null
    }

    fun findProfile(userId: String, displayName: String): StudentProfile? {
        val trimmedId = userId.trim()
        if (trimmedId.isNotEmpty()) {
            knownProfiles[trimmedId]?.let { return it }
            knownProfiles[trimmedId.lowercase()]?.let { return it }
        }
        val cleanName = displayName.trim()
        if (cleanName.isNotEmpty()) {
            knownProfiles[cleanName]?.let { return it }
            knownProfiles[cleanName.lowercase()]?.let { return it }
        }
        return knownProfiles.values.firstOrNull { profile ->
            (trimmedId.isNotEmpty() && profile.stableUserId().equals(trimmedId, ignoreCase = true)) ||
                (cleanName.isNotEmpty() && profile.fullName.equals(cleanName, ignoreCase = true))
        }
    }

    fun registerSeen(keyId: String, rawName: String, matched: StudentProfile?) {
        seenIds += keyId.lowercase()
        val matchedId = matched?.stableUserId()?.trim()?.lowercase().orEmpty()
        if (matchedId.isNotEmpty()) seenIds += matchedId
        val normalizedName = rawName.trim().lowercase()
        if (normalizedName.isNotEmpty()) seenNames += normalizedName
        val matchedName = matched?.fullName?.trim()?.lowercase().orEmpty()
        if (matchedName.isNotEmpty()) seenNames += matchedName
    }

    fun addFromStream(stream: LiveStream, isLive: Boolean) {
        val rawBroadcasterId = stream.broadcasterId.trim()
        val keyId = rawBroadcasterId.ifBlank { stream.id }.lowercase()
        val matched = findProfile(rawBroadcasterId, stream.broadcasterName)
        val rawName = stream.broadcasterName.takeIf(String::isRealProfileDisplayName)
            ?: matched?.fullName?.takeIf(String::isRealProfileDisplayName)
            ?: ""
        val nameKey = rawName.trim().lowercase()
        if (keyId in seenIds || (nameKey.isNotEmpty() && nameKey in seenNames)) return

        val presence = findPresence(rawBroadcasterId, rawName)
        val isSelf = selfId.isNotEmpty() && rawBroadcasterId.equals(selfId, ignoreCase = true)
        val avatar = stream.broadcasterAvatarUrl?.takeIf { it.isNotBlank() && !it.startsWith("avatar_") }
            ?: matched?.avatarUrl?.takeIf { it.isNotBlank() }
            ?: presence?.avatarUrl?.takeIf { it.isNotBlank() }
            ?: (if (isSelf) currentProfile?.avatarUrl?.takeIf { it.isNotBlank() } else null)
        val backdrop = stream.backdropImageUrl?.takeIf { it.isNotBlank() } ?: avatar

        registerSeen(keyId, rawName, matched)
        result += LiveRoomParticipant(
            id = keyId,
            name = displayFirstName(rawName, stream.broadcasterHandle),
            avatarUrl = avatar,
            backdropUrl = backdrop,
            streamId = if (isLive) stream.id else null,
            streamTitle = stream.title.takeIf { it.isNotBlank() },
            live = isLive,
            hand = presence?.isHandRaised == true,
            muted = presence?.isMuted == true,
            viewerCount = stream.viewerCount,
            likeCount = stream.likeCount,
        )
    }

    if (currentProfile != null && currentProfile.fullName.isRealProfileDisplayName()) {
        val normalizedSelfName = currentProfile.fullName.trim().lowercase()
        val myActiveStream = activeStreams
            .filter { it.isLive }
            .firstOrNull { stream ->
                val bId = stream.broadcasterId.trim()
                (selfId.isNotEmpty() && bId.equals(selfId, ignoreCase = true)) ||
                    stream.broadcasterName.trim().equals(normalizedSelfName, ignoreCase = true)
            }
        val myKey = selfId.lowercase().ifBlank { normalizedSelfName }
        seenIds += myKey
        val streamBroadcasterId = myActiveStream?.broadcasterId?.trim()?.lowercase().orEmpty()
        if (streamBroadcasterId.isNotEmpty()) seenIds += streamBroadcasterId
        seenNames += normalizedSelfName

        val selfPresence = findPresence(myKey, currentProfile.fullName)
        val selfAvatar = currentProfile.avatarUrl?.takeIf { it.isNotBlank() }
            ?: myActiveStream?.broadcasterAvatarUrl?.takeIf { it.isNotBlank() && !it.startsWith("avatar_") }
        val selfBackdrop = myActiveStream?.backdropImageUrl?.takeIf { it.isNotBlank() } ?: selfAvatar

        result += LiveRoomParticipant(
            id = myKey,
            name = displayFirstName(currentProfile.fullName),
            avatarUrl = selfAvatar,
            backdropUrl = selfBackdrop,
            streamId = myActiveStream?.id,
            streamTitle = myActiveStream?.title?.takeIf { it.isNotBlank() },
            live = myActiveStream != null,
            hand = selfPresence?.isHandRaised == true,
            muted = selfPresence?.isMuted == true,
            isSelf = true,
            viewerCount = myActiveStream?.viewerCount ?: 0,
            likeCount = myActiveStream?.likeCount ?: 0,
        )
    }

    activeStreams
        .filter { it.isLive }
        .sortedWith(compareByDescending<LiveStream> { it.viewerCount }.thenByDescending { it.startedAtEpochMs })
        .forEach { addFromStream(it, isLive = true) }

    allStreams
        .filter { !it.isLive }
        .sortedByDescending { it.endedAtEpochMs ?: it.startedAtEpochMs }
        .forEach { addFromStream(it, isLive = false) }

    knownProfiles.values
        .filter { it.fullName.isRealProfileDisplayName() }
        .distinctBy { prof -> prof.stableUserId().ifBlank { prof.fullName }.trim().lowercase() }
        .forEach { peer ->
            val peerId = peer.stableUserId().trim().ifBlank { peer.fullName.trim() }.lowercase()
            if (selfId.isNotEmpty() && peerId == selfId.lowercase()) return@forEach
            val peerNameKey = peer.fullName.trim().lowercase()
            if (peerId in seenIds || peerNameKey in seenNames) return@forEach
            val peerPresence = findPresence(peerId, peer.fullName)
            registerSeen(peerId, peer.fullName, peer)
            result += LiveRoomParticipant(
                id = peerId,
                name = displayFirstName(peer.fullName),
                avatarUrl = peer.avatarUrl?.takeIf { it.isNotBlank() && !it.startsWith("avatar_") },
                backdropUrl = peer.avatarUrl?.takeIf { it.isNotBlank() && !it.startsWith("avatar_") },
                live = false,
                hand = peerPresence?.isHandRaised == true,
                muted = peerPresence?.isMuted == true,
            )
        }

    roomPresence
        .filter { it.displayName.isRealProfileDisplayName() }
        .forEach { p ->
            val uid = p.userId.trim().lowercase()
            val nameKey = p.displayName.trim().lowercase()
            if (uid.isBlank() || uid in seenIds || nameKey in seenNames) return@forEach
            val activeStream = activeStreamByBroadcaster[uid]
            seenIds += uid
            seenNames += nameKey
            val avatar = p.avatarUrl?.takeIf { it.isNotBlank() && !it.startsWith("avatar_") }
            result += LiveRoomParticipant(
                id = uid,
                name = displayFirstName(p.displayName),
                avatarUrl = avatar,
                backdropUrl = activeStream?.backdropImageUrl?.takeIf { it.isNotBlank() } ?: avatar,
                streamId = activeStream?.id,
                streamTitle = activeStream?.title?.takeIf { it.isNotBlank() },
                live = activeStream != null,
                hand = p.isHandRaised,
                muted = p.isMuted,
                viewerCount = activeStream?.viewerCount ?: 0,
                likeCount = activeStream?.likeCount ?: 0,
            )
        }

    val featuredIdx = result.indexOfFirst { !it.isSelf && it.live }
        .takeIf { it >= 0 }
        ?: if (result.size >= 2) 1 else -1
    return if (featuredIdx >= 0) {
        result.mapIndexed { idx, p -> if (idx == featuredIdx) p.copy(isFeaturedHost = true) else p }
    } else {
        result
    }
}

internal fun resolveFocusedStream(
    stageParticipants: List<LiveRoomParticipant>,
    activeStreams: List<LiveStream>,
): LiveStream? {
    val slot1StreamId = stageParticipants.getOrNull(1)?.takeIf { it.live }?.streamId
    if (!slot1StreamId.isNullOrBlank()) {
        activeStreams.firstOrNull { it.id == slot1StreamId }?.let { return it }
    }
    val slot0StreamId = stageParticipants.getOrNull(0)?.takeIf { it.live }?.streamId
    if (!slot0StreamId.isNullOrBlank()) {
        activeStreams.firstOrNull { it.id == slot0StreamId }?.let { return it }
    }
    return activeStreams.firstOrNull { it.isLive }
}

internal fun reconcileStageIds(
    currentStageIds: List<String>,
    participants: List<LiveRoomParticipant>,
    promoteLiveToSlot1: Boolean = true,
): List<String> {
    if (participants.isEmpty()) return emptyList()
    val byId = participants.associateBy { it.id }
    val selfParticipant = participants.firstOrNull { it.isSelf }

    val slot0 = selfParticipant?.id
        ?: currentStageIds.firstOrNull { it in byId }
        ?: participants.first().id

    val existingSlot1 = currentStageIds
        .getOrNull(1)
        ?.takeIf { it in byId && it != slot0 }

    val slot1 = if (existingSlot1 != null) {
        if (!promoteLiveToSlot1 || byId[existingSlot1]?.live == true) {
            existingSlot1
        } else {
            participants.firstOrNull { it.id != slot0 && it.live }?.id ?: existingSlot1
        }
    } else {
        participants.firstOrNull { it.id != slot0 && it.live }?.id
            ?: participants.firstOrNull { it.id != slot0 }?.id
    }

    return listOfNotNull(slot0, slot1)
}

/**
 * Live Room screen reproducing the exact 393 x 846 spatial layout while wired to
 * real campus streams and profiles.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LiveRoomScreen(
    studentProfileRepository: StudentProfileRepository? = null,
    liveStreamRepository: LiveStreamRepository? = null,
    onNavigateBack: () -> Unit = {},
    onNavigateToCreateLive: () -> Unit = {},
    onOpenLiveStream: (streamId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val activeProfile by (
        studentProfileRepository?.studentProfile?.collectAsState(initial = null)
            ?: remember { mutableStateOf<StudentProfile?>(null) }
        )
    val knownProfiles by (
        studentProfileRepository?.knownProfiles?.collectAsState()
            ?: remember { mutableStateOf(emptyMap<String, StudentProfile>()) }
        )
    val activeStreams by (
        liveStreamRepository?.getActiveStreams()?.collectAsState(initial = emptyList())
            ?: remember { mutableStateOf(emptyList<LiveStream>()) }
        )
    val allStreams by (
        liveStreamRepository?.getAllStreams()?.collectAsState(initial = emptyList())
            ?: remember { mutableStateOf(emptyList<LiveStream>()) }
        )
    val roomPresence by (
        liveStreamRepository?.roomPresence?.collectAsState()
            ?: remember { mutableStateOf(emptyList()) }
        )

    val mediaManager = remember { LiveStreamMediaRegistry.resolve() }
    val mediaState by (
        mediaManager?.state?.collectAsStateWithLifecycle()
            ?: remember { mutableStateOf(LiveMediaState()) }
        )

    fun checkPermission(perm: String): Boolean =
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

    var hasCameraPermission by remember { mutableStateOf(checkPermission(Manifest.permission.CAMERA)) }
    var hasAudioPermission by remember { mutableStateOf(checkPermission(Manifest.permission.RECORD_AUDIO)) }
    var isNavigatingAway by remember { mutableStateOf(false) }

    // Session dwell tracker for Live Room participants
    com.campus.network.core.ui.components.MoodSessionTracker(
        source = com.campus.network.core.ui.components.MoodSource.LIVE_ROOM
    )

    val participants = remember(activeStreams, allStreams, knownProfiles, activeProfile, roomPresence) {
        buildLiveRoomParticipants(activeStreams, allStreams, knownProfiles, activeProfile, roomPresence)
    }

    val mutedIds = remember { mutableStateMapOf<String, Boolean>() }
    var stageIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var micOn by remember { mutableStateOf(hasAudioPermission) }
    var camOn by remember { mutableStateOf(hasCameraPermission) }
    var useFrontCamera by remember { mutableStateOf(true) }
    var flipped by remember { mutableStateOf(false) }
    var myHand by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    var showChat by remember { mutableStateOf(false) }
    var showPeopleSheet by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        camOn = granted
        toast = if (granted) "Camera on" else "Camera permission denied"
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        micOn = granted
        if (granted) {
            coroutineScope.launch { mediaManager?.setMicEnabled(true) }
        }
        toast = if (granted) "Mic on" else "Microphone permission denied"
    }

    LaunchedEffect(liveStreamRepository, studentProfileRepository) {
        liveStreamRepository?.syncRemoteLiveStreams(0L)
        liveStreamRepository?.syncRoomPresence()
        if (knownProfiles.isEmpty()) {
            studentProfileRepository?.refreshKnownProfiles()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    isNavigatingAway = true
                }
                Lifecycle.Event.ON_RESUME -> {
                    isNavigatingAway = false
                    hasCameraPermission = checkPermission(Manifest.permission.CAMERA)
                    hasAudioPermission = checkPermission(Manifest.permission.RECORD_AUDIO)
                    coroutineScope.launch {
                        liveStreamRepository?.syncRemoteLiveStreams(0L)
                        liveStreamRepository?.syncRoomPresence()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(participants.map { it.id to it.live }) {
        stageIds = reconcileStageIds(stageIds, participants, promoteLiveToSlot1 = true)
    }

    val resolvedStageIds = remember(stageIds, participants) {
        reconcileStageIds(stageIds, participants, promoteLiveToSlot1 = false)
    }
    val participantMap = remember(participants) { participants.associateBy { it.id } }
    val stageParticipants = remember(resolvedStageIds, participantMap) {
        resolvedStageIds.mapNotNull { participantMap[it] }
    }
    val gridParticipants = remember(participants, resolvedStageIds) {
        participants.filter { it.id !in resolvedStageIds }
    }

    val focusedStream = remember(stageParticipants, activeStreams) {
        resolveFocusedStream(stageParticipants, activeStreams)
    }
    val chatStreamId = focusedStream?.id ?: LiveStreamRepository.LIVE_ROOM_LOBBY_STREAM_ID

    val remoteComments by (
        liveStreamRepository?.getCommentsForStream(chatStreamId)?.collectAsState(initial = emptyList())
            ?: remember { mutableStateOf(emptyList()) }
        )

    LaunchedEffect(chatStreamId, liveStreamRepository) {
        liveStreamRepository?.syncRemoteCommentsForStream(chatStreamId)
    }

    val selfId = activeProfile?.stableUserId().orEmpty()
    val selfIdKey = selfId.trim().lowercase()
    val selfName = activeProfile?.fullName?.takeIf(String::isRealProfileDisplayName).orEmpty()
    val myActiveStreamId = activeStreams.firstOrNull {
        it.isLive && selfId.isNotBlank() && it.broadcasterId.equals(selfId, ignoreCase = true)
    }?.id

    // Sync self Live Room presence in real time (hand raised, mic, camera, active heartbeat)
    LaunchedEffect(selfId, selfName, myHand, micOn, camOn, myActiveStreamId, focusedStream?.id) {
        if (selfId.isNotBlank() && selfName.isNotBlank() && liveStreamRepository != null) {
            liveStreamRepository.upsertRoomPresence(
                userId = selfId,
                displayName = selfName,
                avatarUrl = activeProfile?.avatarUrl,
                streamId = myActiveStreamId ?: focusedStream?.id,
                isHandRaised = myHand,
                isMuted = !micOn,
                isCameraOn = camOn,
                isOnStage = true
            )
        }
    }

    LaunchedEffect(selfId, selfName, liveStreamRepository) {
        if (selfId.isBlank() || selfName.isBlank() || liveStreamRepository == null) return@LaunchedEffect
        while (isActive) {
            delay(30_000L)
            liveStreamRepository.syncRemoteLiveStreams(0L)
            liveStreamRepository.syncRoomPresence()
            liveStreamRepository.upsertRoomPresence(
                userId = selfId,
                displayName = selfName,
                avatarUrl = activeProfile?.avatarUrl,
                streamId = myActiveStreamId ?: focusedStream?.id,
                isHandRaised = myHand,
                isMuted = !micOn,
                isCameraOn = camOn,
                isOnStage = true
            )
        }
    }

    DisposableEffect(selfId) {
        onDispose {
            if (selfId.isNotBlank()) {
                CoroutineScope(Dispatchers.IO + NonCancellable).launch {
                    liveStreamRepository?.removeRoomPresence(selfId)
                }
            }
        }
    }

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(1600)
            toast = null
        }
    }

    val viewers = maxOf(
        activeStreams.sumOf { it.viewerCount.coerceAtLeast(1) },
        roomPresence.size.coerceAtLeast(if (activeProfile != null) 1 else 0)
    )
    val likes = focusedStream?.likeCount ?: activeStreams.sumOf { it.likeCount }
    val hands = participants.count { p ->
        if (p.isSelf || p.id == selfIdKey) myHand else p.hand
    }
    val chatCount = remoteComments.size
    val meritPoints = activeProfile?.campusMeritPoints ?: 0
    val rewardProgress = if (meritPoints <= 0) {
        0.08f
    } else {
        val remainder = meritPoints % 100
        ((if (remainder == 0) 100 else remainder) / 100f).coerceIn(0.08f, 1f)
    }

    fun exitRoom() {
        isNavigatingAway = true
        if (selfId.isNotBlank()) {
            coroutineScope.launch(NonCancellable) {
                liveStreamRepository?.removeRoomPresence(selfId)
            }
        }
        com.campus.network.core.ui.components.MoodCheckIn.request(
            context,
            com.campus.network.core.ui.components.MoodSource.LIVE_ROOM
        )
        onNavigateBack()
    }

    fun openStreamFullscreen(streamId: String) {
        isNavigatingAway = true
        onOpenLiveStream(streamId)
    }

    fun navigateToCreateLive() {
        isNavigatingAway = true
        onNavigateToCreateLive()
    }

    fun shareFocusedOrRoom() {
        val shareText = if (focusedStream != null) {
            "Watch ${focusedStream.broadcasterName} live on Patriarch: \"${focusedStream.title}\""
        } else {
            "Join me live in the Patriarch Live Room!"
        }
        runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Patriarch Live Invite", shareText))
        }
        runCatching {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(send, "Share live stream").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        toast = "Invite copied & ready to share"
    }

    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            val s = minOf(maxWidth / 393.dp, maxHeight / 846.dp)
            Box(
                Modifier
                    .requiredSize(393.dp, 846.dp)
                    .graphicsLayer {
                        scaleX = s
                        scaleY = s
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
                    .clip(RoundedCornerShape(52.dp))
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0xFF9AA5AE),
                            .22f to Color(0xFF8E96A1),
                            .42f to Color(0xFF8A8189),
                            .58f to Color(0xFF706468),
                            .80f to Color(0xFF55505A),
                            1f to Color(0xFF5A4F55),
                        )
                    )
                    .drawBehind {
                        glow(Color(0xFFC8604A), size.width, size.height * .91f, size.width * .75f, size.height * .20f)
                        glow(Color(0xFFB3604D), 0f, size.height * .63f, size.width * .45f, size.height * .20f)
                        glow(Color(0xFF45434A), size.width * .28f, size.height * .90f, size.width * .70f, size.height * .26f)
                    }
            ) {
                // Back button
                Box(
                    Modifier
                        .offset(22.dp, 69.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Glass)
                        .clickable { exitRoom() },
                    Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                // Reward row
                Row(
                    Modifier
                        .offset(72.dp, 76.dp)
                        .height(22.dp)
                        .clickable(remember { MutableInteractionSource() }, null) {
                            toast = "$meritPoints merit pts · ${activeStreams.size} live"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Txt("REWARD", 14.sp, FontWeight.Medium, Modifier.width(65.dp), Color.White.copy(alpha = .6f))
                    Spacer(Modifier.width(8.dp))
                    ULogo()
                    Spacer(Modifier.width(3.dp))
                    Txt("0.1 for every", 15.sp, FontWeight.Medium)
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.size(18.dp).clip(CircleShape).background(Color.White), Alignment.Center) {
                        Icon(Icons.Rounded.Person, null, tint = Color(0xFF8E96A1), modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.size(36.dp, 8.dp).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = .3f))) {
                        Box(Modifier.fillMaxWidth(rewardProgress).fillMaxHeight().background(Live, RoundedCornerShape(5.dp)))
                    }
                }

                // Stage
                Row(
                    Modifier.offset(22.dp, 134.dp).graphicsLayer { scaleX = if (flipped) -1f else 1f },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    stageParticipants.forEachIndexed { stageIndex, p ->
                        val isMuted = if (p.isSelf) !micOn else (mutedIds[p.id] == true || p.muted)
                        fun toggleMute() {
                            val nowMuted = if (p.isSelf) {
                                if (!micOn && !hasAudioPermission) {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    return
                                }
                                micOn = !micOn
                                coroutineScope.launch { mediaManager?.setMicEnabled(micOn) }
                                !micOn
                            } else {
                                val next = !(mutedIds[p.id] == true)
                                mutedIds[p.id] = next
                                next
                            }
                            toast = if (nowMuted) "${p.name} muted" else "${p.name} unmuted"
                        }
                        Box(
                            Modifier
                                .size(171.dp, 241.dp)
                                .clip(RoundedCornerShape(30.dp))
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        if (stageIndex == 1 && p.live && p.streamId != null) {
                                            openStreamFullscreen(p.streamId)
                                        } else {
                                            toggleMute()
                                        }
                                    },
                                    onLongClick = { toggleMute() },
                                ),
                        ) {
                            if (p.isSelf && camOn && hasCameraPermission && !isNavigatingAway) {
                                GoLiveCameraPreview(
                                    useFront = useFrontCamera,
                                    onUnavailable = {
                                        camOn = false
                                        toast = "Camera unavailable"
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                ParticipantPhoto(
                                    photoUrl = p.backdropUrl ?: p.avatarUrl,
                                    fullName = p.name,
                                    shape = RoundedCornerShape(30.dp),
                                    modifier = Modifier.fillMaxSize(),
                                    fontSize = 28.sp,
                                )
                            }
                            if (isMuted) {
                                Icon(
                                    Icons.Rounded.MicOff,
                                    null,
                                    tint = Color.White.copy(alpha = .85f),
                                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 22.dp, end = 23.dp).size(22.dp),
                                )
                            }
                            Row(
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 40.dp)
                                    .graphicsLayer { scaleX = if (flipped) -1f else 1f },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Txt(
                                    p.name,
                                    15.sp,
                                    FontWeight.Medium,
                                    lineHeight = 16.sp,
                                    shadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 2f), 6f),
                                )
                                if (stageIndex == 1 && (p.isFeaturedHost || p.live)) {
                                    Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }

                    repeat((2 - stageParticipants.size).coerceAtLeast(0)) {
                        Box(
                            Modifier
                                .size(171.dp, 241.dp)
                                .clip(RoundedCornerShape(30.dp))
                                .background(Glass)
                                .clickable { navigateToCreateLive() },
                            Alignment.Center,
                        ) {
                            Column(
                                modifier = Modifier.graphicsLayer { scaleX = if (flipped) -1f else 1f },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Rounded.Videocam, null, tint = Color.White, modifier = Modifier.size(32.dp))
                                Txt("Tap to Go Live", 14.sp, FontWeight.Medium)
                            }
                        }
                    }
                }

                // Grid
                Column(
                    Modifier
                        .offset(22.dp, 397.dp)
                        .width(350.dp)
                        .height(268.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (gridParticipants.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(134.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Txt(
                                "Waiting for peers to join",
                                14.sp,
                                FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                        }
                    } else {
                        gridParticipants.chunked(3).forEach { row ->
                            Row(Modifier.width(350.dp).height(134.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                row.forEach { p ->
                                    val showHand = p.hand || (p.id == selfIdKey && myHand)
                                    Box(
                                        Modifier
                                            .width(80.dp)
                                            .fillMaxHeight()
                                            .clickable(remember { MutableInteractionSource() }, null) {
                                                val slot0 = resolvedStageIds.firstOrNull()
                                                stageIds = listOfNotNull(slot0, p.id).distinct()
                                                toast = if (p.live) {
                                                    "${p.name} on stage — tap tile to watch"
                                                } else {
                                                    "${p.name} joined stage"
                                                }
                                            },
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            ParticipantPhoto(
                                                photoUrl = p.avatarUrl,
                                                fullName = p.name,
                                                shape = RoundedCornerShape(26.dp),
                                                modifier = Modifier.size(80.dp),
                                                fontSize = 20.sp,
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Txt(p.name, 14.sp, FontWeight.SemiBold, lineHeight = 20.sp)
                                                if (p.live) {
                                                    Box(Modifier.padding(start = 5.dp).size(8.dp).background(Live, CircleShape))
                                                }
                                            }
                                        }
                                        if (showHand) HandBubble()
                                    }
                                }
                                repeat(3 - row.size) {
                                    Spacer(Modifier.width(80.dp))
                                }
                            }
                        }
                    }
                }

                // Controls
                Row(Modifier.offset(19.dp, 693.dp).width(355.dp)) {
                    Ctrl(Icons.Rounded.Cached, true) {
                        useFrontCamera = !useFrontCamera
                        if (!camOn || !hasCameraPermission) {
                            flipped = !flipped
                        } else {
                            flipped = false
                        }
                        coroutineScope.launch { mediaManager?.flipCamera() }
                        toast = if (useFrontCamera) "Front camera" else "Back camera"
                    }
                    Ctrl(if (camOn) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff, camOn) {
                        if (!hasCameraPermission) {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            camOn = !camOn
                            coroutineScope.launch { mediaManager?.setCameraEnabled(camOn) }
                            toast = if (camOn) "Camera on" else "Camera off"
                        }
                    }
                    Ctrl(if (micOn) Icons.Rounded.Mic else Icons.Rounded.MicOff, micOn) {
                        if (!micOn && !hasAudioPermission) {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            micOn = !micOn
                            coroutineScope.launch { mediaManager?.setMicEnabled(micOn) }
                            toast = if (micOn) "Mic on" else "Mic muted"
                        }
                    }
                    Ctrl(Icons.Rounded.PersonAddAlt1, true) { navigateToCreateLive() }
                    Box(
                        Modifier
                            .size(71.dp, 44.dp)
                            .clickable(remember { MutableInteractionSource() }, null) {
                                val isLiveBroadcaster = mediaState.role == LiveMediaRole.BROADCASTER &&
                                    mediaState.connectionState == LiveMediaConnectionState.LIVE
                                if (isLiveBroadcaster && mediaManager != null) {
                                    coroutineScope.launch {
                                        mediaManager.toggleRecording()
                                            .onSuccess { recordingNow ->
                                                toast = if (recordingNow) "Cloud recording started" else "Recording stopped"
                                            }
                                            .onFailure {
                                                shareFocusedOrRoom()
                                            }
                                    }
                                } else {
                                    shareFocusedOrRoom()
                                }
                            },
                        Alignment.Center,
                    ) {
                        Box(Modifier.size(26.dp).background(Color.White, RoundedCornerShape(6.dp)), Alignment.Center) {
                            Icon(Icons.Rounded.Download, null, tint = Color(0xFF8A6A62), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Pills
                Row(
                    Modifier
                        .offset(y = 754.dp)
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        Modifier
                            .height(56.dp)
                            .requiredWidth(93.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { showPeopleSheet = true }
                            .padding(start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        ParticipantPhoto(
                            photoUrl = activeProfile?.avatarUrl,
                            fullName = activeProfile?.fullName.orEmpty().ifBlank { "Me" },
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp),
                            fontSize = 12.sp,
                        )
                        Txt("$viewers", 20.sp, FontWeight.SemiBold, color = Ink)
                    }
                    Pill(Icons.Rounded.ChatBubble, "$chatCount", 93.dp) { showChat = true }
                    Pill(Icons.Rounded.WavingHand, "$hands", 88.dp, active = myHand) {
                        myHand = !myHand
                        toast = if (myHand) "Hand raised" else "Hand lowered"
                    }
                    Pill(Icons.Rounded.ThumbUp, "$likes", 90.dp) {
                        val targetStream = focusedStream
                        if (targetStream != null && liveStreamRepository != null) {
                            coroutineScope.launch {
                                liveStreamRepository.incrementLikes(targetStream.id)
                                    .onFailure {
                                        toast = "Unable to save like to database"
                                    }
                            }
                        } else {
                            toast = "Start or select a live stream to like"
                        }
                    }
                }

                // Toast
                AnimatedVisibility(toast != null, Modifier.align(Alignment.TopCenter).offset(y = 58.dp)) {
                    Box(Modifier.background(Color(0xD9141414), RoundedCornerShape(18.dp)).padding(horizontal = 15.dp, vertical = 8.dp)) {
                        Txt(toast ?: "", 13.sp, FontWeight.Medium)
                    }
                }
            }
        }

        if (showPeopleSheet) {
            ModalBottomSheet(onDismissRequest = { showPeopleSheet = false }, containerColor = Color(0xE6181616)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .heightIn(min = 240.dp, max = 440.dp),
                ) {
                    Txt("Live & Listening ($viewers)", 16.sp, FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(
                        Modifier.fillMaxWidth().weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (activeStreams.isEmpty()) {
                            item {
                                Text(
                                    text = "No one is broadcasting video yet. Tap + to start a live stream.",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp,
                                    fontFamily = AppFont,
                                )
                            }
                        } else {
                            items(activeStreams, key = { it.id }) { stream ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Glass)
                                        .clickable {
                                            showPeopleSheet = false
                                            openStreamFullscreen(stream.id)
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    ParticipantPhoto(
                                        photoUrl = stream.broadcasterAvatarUrl,
                                        fullName = stream.broadcasterName.ifBlank { "Host" },
                                        shape = CircleShape,
                                        modifier = Modifier.size(38.dp),
                                        fontSize = 14.sp,
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Txt(stream.broadcasterName.ifBlank { "Host" }, 14.sp, FontWeight.SemiBold)
                                        Txt(
                                            "${stream.title} · ${stream.viewerCount.coerceAtLeast(1)} watching · ${stream.likeCount} likes",
                                            12.sp,
                                            FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.75f),
                                        )
                                    }
                                    Box(
                                        Modifier
                                            .clip(CircleShape)
                                            .background(Live)
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                    ) {
                                        Txt("Watch", 12.sp, FontWeight.SemiBold, color = Ink)
                                    }
                                }
                            }
                        }

                        if (roomPresence.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(6.dp))
                                Txt("Active in Live Room (${roomPresence.size})", 14.sp, FontWeight.SemiBold)
                            }
                            items(roomPresence, key = { it.userId }) { member ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .clickable {
                                            val targetId = member.userId.trim().lowercase()
                                            val slot0 = resolvedStageIds.firstOrNull()
                                            if (targetId != slot0) {
                                                stageIds = listOfNotNull(slot0, targetId).distinct()
                                            }
                                            showPeopleSheet = false
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    ParticipantPhoto(
                                        photoUrl = member.avatarUrl,
                                        fullName = member.displayName.ifBlank { "Student" },
                                        shape = CircleShape,
                                        modifier = Modifier.size(32.dp),
                                        fontSize = 12.sp,
                                    )
                                    Txt(
                                        member.displayName.ifBlank { "Student" },
                                        14.sp,
                                        FontWeight.Medium,
                                        Modifier.weight(1f),
                                    )
                                    if (member.isHandRaised) {
                                        Icon(Icons.Rounded.PanTool, "Hand raised", tint = Live, modifier = Modifier.size(16.dp))
                                    }
                                    if (member.isMuted) {
                                        Icon(Icons.Rounded.MicOff, "Muted", tint = Color.White.copy(alpha = 0.65f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showChat) {
            var draft by remember { mutableStateOf("") }
            val chatListState = rememberLazyListState()
            val sheetMessages = remember(remoteComments, chatStreamId) {
                remoteComments.map { c ->
                    val sender = c.senderName.takeIf { it.isRealProfileDisplayName() } ?: "Student"
                    c.id to "${displayFirstName(sender)}: ${c.commentText}"
                }
            }
            LaunchedEffect(sheetMessages.size) {
                if (sheetMessages.isNotEmpty()) {
                    runCatching { chatListState.animateScrollToItem(sheetMessages.size - 1) }
                }
            }
            ModalBottomSheet(onDismissRequest = { showChat = false }, containerColor = Color(0xE6181616)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp)
                        .heightIn(min = 320.dp, max = 460.dp),
                ) {
                    Txt(
                        if (focusedStream != null) "Chat · ${displayFirstName(focusedStream.broadcasterName)}" else "Live Room Chat",
                        16.sp,
                        FontWeight.SemiBold,
                    )
                    LazyColumn(
                        state = chatListState,
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (sheetMessages.isEmpty()) {
                            item {
                                Txt("No messages yet", 14.sp, FontWeight.Medium, color = Color.White.copy(alpha = 0.6f))
                            }
                        } else {
                            items(sheetMessages, key = { it.first }) { (_, line) ->
                                Text(
                                    text = line,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontFamily = AppFont,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BasicTextField(
                            draft,
                            { draft = it },
                            Modifier
                                .weight(1f)
                                .height(42.dp)
                                .background(Glass, CircleShape)
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontFamily = AppFont),
                            cursorBrush = SolidColor(Live),
                            singleLine = true,
                        )
                        Box(
                            Modifier
                                .height(42.dp)
                                .background(Live, CircleShape)
                                .clickable {
                                    val trimmed = draft.trim()
                                    if (trimmed.isNotEmpty()) {
                                        val profile = activeProfile
                                        val senderId = profile?.stableUserId().orEmpty()
                                        if (liveStreamRepository != null &&
                                            senderId.isNotBlank() &&
                                            profile?.fullName?.isRealProfileDisplayName() == true
                                        ) {
                                            draft = ""
                                            coroutineScope.launch {
                                                runCatching {
                                                    liveStreamRepository.postComment(
                                                        streamId = chatStreamId,
                                                        senderId = senderId,
                                                        senderAvatarRes = 0,
                                                        commentText = trimmed,
                                                        senderAvatarUrl = profile.avatarUrl,
                                                        senderDisplayName = profile.fullName,
                                                    )
                                                }.onSuccess { saved ->
                                                    if (saved.syncStatus != SyncStatus.SYNCED.name) {
                                                        toast = "Queued offline — will sync"
                                                    }
                                                }.onFailure {
                                                    toast = "Could not send message"
                                                }
                                            }
                                        } else {
                                            toast = "Sign in with your profile to chat"
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp),
                            Alignment.Center,
                        ) { Txt("Send", 14.sp, FontWeight.SemiBold, color = Ink) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantPhoto(
    photoUrl: String?,
    fullName: String,
    shape: Shape,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 18.sp,
) {
    CampusStudentAvatar(
        avatarUrl = photoUrl,
        fullName = fullName,
        shape = shape,
        backgroundColor = Color(0xFF4A454D),
        initialsColor = Color.White,
        fontSize = fontSize,
        modifier = modifier,
    )
}

@Composable
private fun Txt(
    text: String,
    size: TextUnit,
    weight: FontWeight,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    lineHeight: TextUnit = TextUnit.Unspecified,
    shadow: Shadow? = null,
) = Text(
    text = text,
    modifier = modifier,
    color = color,
    fontSize = size,
    fontFamily = AppFont,
    fontWeight = weight,
    lineHeight = lineHeight,
    maxLines = 1,
    softWrap = false,
    style = if (shadow != null) TextStyle(shadow = shadow) else TextStyle.Default,
)

@Composable
private fun Ctrl(icon: ImageVector, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(71.dp, 44.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        Alignment.Center,
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = if (on) 1f else .45f), modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun Pill(icon: ImageVector, label: String, width: Dp, active: Boolean = false, onClick: () -> Unit) {
    val fg = if (active) Ink else Color.White
    Row(
        Modifier
            .height(56.dp)
            .requiredWidth(width)
            .clip(CircleShape)
            .background(if (active) Color.White else Glass)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(30.dp))
        Txt(label, 20.sp, FontWeight.SemiBold, color = fg)
    }
}

@Composable
private fun HandBubble() {
    val sway by rememberInfiniteTransition(label = "sway").animateFloat(
        0f,
        10f,
        infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "r",
    )
    Box(
        Modifier
            .offset(54.dp, (-9).dp)
            .size(37.dp)
            .graphicsLayer { rotationZ = sway }
            .background(Color.White, CircleShape),
        Alignment.Center,
    ) {
        Icon(Icons.Rounded.PanTool, null, tint = Ink, modifier = Modifier.size(20.dp))
    }
    Box(Modifier.offset(85.dp, 21.dp).size(6.dp).background(Color.White, CircleShape))
}

@Composable
private fun ULogo() = Canvas(Modifier.size(20.dp, 18.dp)) {
    val u = size.width / 20f
    val p = Path().apply {
        moveTo(3f * u, 1.5f * u)
        relativeLineTo(0f, 8f * u)
        relativeCubicTo(0f, 5f * u, 3f * u, 7f * u, 6.5f * u, 7f * u)
        relativeCubicTo(3f * u, 0f, 5.5f * u, -2f * u, 6f * u, -5.5f * u)
        lineTo(15.5f * u, 1.5f * u)
        moveTo(15.5f * u, 12.5f * u)
        relativeLineTo(0f, 4f * u)
    }
    drawPath(p, Color.White, style = Stroke(2.2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Elliptical glow: color at center fading to transparent at 72% of radiusX. */
private fun DrawScope.glow(color: Color, cx: Float, cy: Float, rx: Float, ry: Float) {
    if (rx <= 0f || ry <= 0f) return
    withTransform({ translate(cx, cy); scale(1f, ry / rx, Offset.Zero) }) {
        drawCircle(
            Brush.radialGradient(0f to color, .72f to Color.Transparent, center = Offset.Zero, radius = rx),
            radius = rx,
            center = Offset.Zero,
        )
    }
}
