package com.campus.network.core.model

/**
 * Cross-device call signaling models (Supabase-backed).
 * Media (WebRTC / Stream Video) plugs in separately via CallMediaProvider.
 */

enum class CallSessionStatus {
    RINGING,
    ACCEPTED,
    DECLINED,
    ENDED,
    MISSED,
    FAILED
}

enum class CallMediaKind {
    AUDIO,
    VIDEO
}

data class CallSession(
    val id: String,
    val channelId: String,
    val callerId: String,
    val callerName: String,
    val calleeId: String,
    val calleeName: String,
    val mediaKind: CallMediaKind,
    val status: CallSessionStatus,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

data class OutgoingCallInvite(
    val channelId: String,
    val callerId: String,
    val callerName: String,
    val calleeId: String,
    val calleeName: String,
    val mediaKind: CallMediaKind
)
