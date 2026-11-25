package com.campus.network.feature.hub.liveroom

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campus.network.core.network.call.StreamVideoCallMediaProvider
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call

/**
 * Renders the host's live local WebRTC camera track inside a Stream Video livestream call.
 */
@Composable
fun BroadcasterLiveVideoSurface(
    call: Call?,
    modifier: Modifier = Modifier,
    onVideoReady: (Boolean) -> Unit = {}
) {
    if (call == null) {
        LaunchedEffect(Unit) { onVideoReady(false) }
        return
    }
    val me by call.state.me.collectAsStateWithLifecycle()
    val local = me
    if (local == null) {
        LaunchedEffect(call) { onVideoReady(false) }
        return
    }
    val localVideo by local.video.collectAsStateWithLifecycle()
    val track = localVideo
    LaunchedEffect(track != null) {
        onVideoReady(track != null)
    }
    if (track != null) {
        VideoTheme {
            VideoRenderer(
                modifier = modifier.fillMaxSize(),
                call = call,
                video = track
            )
        }
    }
}

/**
 * Renders the remote broadcaster's live WebRTC video track for livestream viewers.
 */
@Composable
fun ViewerLiveVideoSurface(
    call: Call?,
    broadcasterUserId: String?,
    modifier: Modifier = Modifier,
    onRemoteVideoState: (hasParticipant: Boolean, hasVideo: Boolean) -> Unit = { _, _ -> },
    onRemoteParticipantCount: (Int) -> Unit = {}
) {
    if (call == null) {
        LaunchedEffect(Unit) {
            onRemoteVideoState(false, false)
            onRemoteParticipantCount(0)
        }
        return
    }
    val participants by call.state.participants.collectAsStateWithLifecycle()
    val me by call.state.me.collectAsStateWithLifecycle()
    val remotes = participants.filter { it.sessionId != me?.sessionId && !it.isLocal }
    LaunchedEffect(participants.size) {
        onRemoteParticipantCount(participants.size)
    }

    val sanitizedBroadcaster = broadcasterUserId
        ?.takeIf { it.isNotBlank() }
        ?.let { StreamVideoCallMediaProvider.sanitizeUserId(it) }

    val remoteSnapshots = remotes.map { participant ->
        key(participant.sessionId) {
            val videoState by participant.video.collectAsStateWithLifecycle()
            val uid by participant.userId.collectAsStateWithLifecycle()
            Triple(participant, uid, videoState)
        }
    }

    val target = remoteSnapshots.firstOrNull { (_, uid, _) ->
        (sanitizedBroadcaster != null && uid.equals(sanitizedBroadcaster, ignoreCase = true)) ||
            (broadcasterUserId != null && uid.equals(broadcasterUserId, ignoreCase = true))
    } ?: remoteSnapshots.firstOrNull { (_, _, video) -> video != null }
        ?: remoteSnapshots.firstOrNull()

    val hasParticipant = target != null
    val videoTrack = target?.third

    LaunchedEffect(hasParticipant, videoTrack != null) {
        onRemoteVideoState(hasParticipant, videoTrack != null)
    }

    if (videoTrack != null) {
        VideoTheme {
            VideoRenderer(
                modifier = modifier.fillMaxSize(),
                call = call,
                video = videoTrack
            )
        }
    }
}
