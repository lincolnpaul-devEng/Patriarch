package com.campus.network.feature.messages.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campus.network.core.network.call.CallMediaProviderRegistry
import com.campus.network.core.network.call.StreamVideoCallMediaProvider
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call

/**
 * Renders Stream Video tracks inside Campus custom call chrome.
 * Local CameraX preview must be unbound while this is active (camera exclusive).
 */
@Composable
fun StreamRemoteParticipantVideo(
    modifier: Modifier = Modifier
) {
    val call = peekActiveStreamCall()
    if (call == null) {
        Box(modifier = modifier.background(Color(0xFF0F172A)), contentAlignment = Alignment.Center) {
            Text("Connecting peer video…", color = Color(0x99FFFFFF), fontSize = 13.sp)
        }
        return
    }
    val participants by call.state.participants.collectAsStateWithLifecycle()
    val me by call.state.me.collectAsStateWithLifecycle()
    val remote = participants.firstOrNull { it.sessionId != me?.sessionId } ?: participants.firstOrNull()
    VideoTheme {
        if (remote == null) {
            Box(modifier = modifier.background(Color(0xFF0F172A)), contentAlignment = Alignment.Center) {
                Text("Waiting for peer…", color = Color(0x99FFFFFF), fontSize = 13.sp)
            }
        } else {
            val remoteVideo by remote.video.collectAsStateWithLifecycle()
            val videoTrack = remoteVideo
            if (videoTrack != null) {
                VideoRenderer(
                    modifier = modifier.fillMaxSize(),
                    call = call,
                    video = videoTrack
                )
            } else {
                Box(modifier = modifier.background(Color(0xFF0F172A)), contentAlignment = Alignment.Center) {
                    Text("Peer camera off", color = Color(0x99FFFFFF), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun StreamLocalParticipantVideo(
    modifier: Modifier = Modifier
) {
    val call = peekActiveStreamCall()
    if (call == null) {
        Box(modifier = modifier.background(Color(0xFF101722)))
        return
    }
    val me by call.state.me.collectAsStateWithLifecycle()
    VideoTheme {
        val local = me
        if (local == null) {
            Box(modifier = modifier.background(Color(0xFF101722)))
        } else {
            val localVideo by local.video.collectAsStateWithLifecycle()
            val videoTrack = localVideo
            if (videoTrack != null) {
                VideoRenderer(
                    modifier = modifier.fillMaxSize(),
                    call = call,
                    video = videoTrack
                )
            } else {
                Box(modifier = modifier.background(Color(0xFF101722)))
            }
        }
    }
}

fun peekActiveStreamCall(): Call? {
    return (CallMediaProviderRegistry.resolve() as? StreamVideoCallMediaProvider)?.activeCall
}
