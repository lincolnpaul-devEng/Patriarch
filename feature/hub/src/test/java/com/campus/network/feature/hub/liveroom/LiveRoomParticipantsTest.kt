package com.campus.network.feature.hub.liveroom

import com.campus.network.core.data.LiveRoomPresence
import com.campus.network.core.model.LiveStream
import com.campus.network.core.model.StudentProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveRoomParticipantsTest {

    private fun stream(
        id: String,
        broadcasterId: String,
        name: String,
        handle: String = "handle",
        isLive: Boolean = true,
        viewerCount: Int = 10,
        startedAt: Long = 1000L,
        avatarUrl: String? = null,
    ) = LiveStream(
        id = id,
        broadcasterId = broadcasterId,
        broadcasterName = name,
        broadcasterHandle = handle,
        broadcasterAvatarUrl = avatarUrl,
        title = "Title $id",
        description = "Desc",
        category = "Spotlight",
        isLive = isLive,
        viewerCount = viewerCount,
        startedAtEpochMs = startedAt,
    )

    private fun profile(
        id: String,
        name: String,
        email: String = "$id@strathmore.edu",
        avatarUrl: String? = null,
    ) = StudentProfile(
        studentId = id,
        fullName = name,
        universityEmail = email,
        avatarUrl = avatarUrl,
    )

    @Test
    fun currentProfilePinnedFirstAsSelf_evenWhenOtherStreamsHaveHigherViewers() {
        val me = profile("u_me", "Lincoln Paul", avatarUrl = "https://cdn.example/me.jpg")
        val known = mapOf(
            "u_imani" to profile("u_imani", "Imani Okafor", avatarUrl = "https://cdn.example/imani.jpg"),
            "u_chun" to profile("u_chun", "Chun Li", avatarUrl = "https://cdn.example/chun.jpg"),
        )
        val active = listOf(
            stream("s_chun", "u_chun", "Chun Li", viewerCount = 24, startedAt = 100L),
            stream("s_imani", "u_imani", "Imani Okafor", viewerCount = 74, startedAt = 200L),
        )

        val result = buildLiveRoomParticipants(active, active, known, currentProfile = me)

        assertEquals(3, result.size)
        assertEquals("Lincoln", result[0].name)
        assertTrue(result[0].isSelf)
        assertFalse(result[0].live)
        assertFalse(result[0].isFeaturedHost)

        assertEquals("Imani", result[1].name)
        assertEquals("s_imani", result[1].streamId)
        assertTrue(result[1].live)
        assertFalse(result[1].isSelf)
        assertEquals("https://cdn.example/imani.jpg", result[1].avatarUrl)
        assertTrue(result[1].isFeaturedHost)

        assertEquals("Chun", result[2].name)
        assertEquals("s_chun", result[2].streamId)
        assertTrue(result[2].live)
        assertFalse(result[2].isFeaturedHost)
    }

    @Test
    fun deduplication_preventsSelfAndBroadcastersAppearingTwice() {
        val me = profile("u_me", "Lincoln Paul", avatarUrl = "https://cdn.example/me.jpg")
        val jamesProfile = profile("u_james", "James Kariuki", avatarUrl = "https://cdn.example/james.jpg")
        val ericaProfile = profile("u_erica", "Erica Mensah", avatarUrl = "https://cdn.example/erica.jpg")
        // Simulate StudentProfileRepository storing multiple map keys for the same StudentProfile
        val known = mapOf(
            "u_me" to me,
            "lincoln paul" to me,
            "u_james" to jamesProfile,
            "james kariuki" to jamesProfile,
            "u_erica" to ericaProfile,
            "erica mensah" to ericaProfile,
        )
        val active = listOf(
            stream("s_me", "u_me", "Lincoln Paul", isLive = true, viewerCount = 99),
            stream("s_james", "u_james", "James Kariuki", isLive = true, viewerCount = 30),
        )
        val all = active + listOf(
            stream("s_james_old", "u_james", "James Kariuki", isLive = false, viewerCount = 5),
        )

        val result = buildLiveRoomParticipants(active, all, known, currentProfile = me)

        assertEquals(3, result.size)
        assertEquals("Lincoln", result[0].name)
        assertTrue(result[0].isSelf)
        assertTrue(result[0].live)
        assertEquals("s_me", result[0].streamId)
        assertFalse(result[0].isFeaturedHost)

        assertEquals("James", result[1].name)
        assertTrue(result[1].live)
        assertFalse(result[1].isSelf)
        assertTrue(result[1].isFeaturedHost)

        assertEquals("Erica", result[2].name)
        assertFalse(result[2].live)
        assertNull(result[2].streamId)
    }

    @Test
    fun nonLivePeersSortAfterCurrentUser_withNullStreamId() {
        val me = profile("u_me", "Lincoln Paul", avatarUrl = "https://cdn.example/me.jpg")
        val known = mapOf(
            "u_alex" to profile("u_alex", "Alex Otieno"),
            "u_tessa" to profile("u_tessa", "Tessa Wanjiku"),
        )

        val result = buildLiveRoomParticipants(emptyList(), emptyList(), known, currentProfile = me)

        assertEquals(3, result.size)
        assertTrue(result[0].isSelf)
        assertTrue(result.none { it.live })
        assertTrue(result.all { it.streamId == null })
        assertEquals(listOf("Lincoln", "Alex", "Tessa"), result.map { it.name })
    }

    @Test
    fun reconcileStageIds_pinsSelfInSlot0_promotesLiveOnlyWhenRequested_andPreservesManualGridPick() {
        assertEquals(emptyList<String>(), reconcileStageIds(emptyList(), emptyList()))

        val selfOnly = listOf(LiveRoomParticipant(id = "u_me", name = "Lincoln", isSelf = true))
        assertEquals(listOf("u_me"), reconcileStageIds(emptyList(), selfOnly))

        val participants = listOf(
            LiveRoomParticipant(id = "u_me", name = "Lincoln", isSelf = true),
            LiveRoomParticipant(id = "u_alex", name = "Alex", live = false),
            LiveRoomParticipant(id = "u_imani", name = "Imani", live = true, streamId = "s3"),
        )
        // Auto-sync promotes top live streamer into non-live slot 1
        assertEquals(
            listOf("u_me", "u_imani"),
            reconcileStageIds(listOf("u_me", "u_alex"), participants, promoteLiveToSlot1 = true),
        )
        // Manual grid selection in slot 1 is preserved even when another user is live
        assertEquals(
            listOf("u_me", "u_alex"),
            reconcileStageIds(listOf("u_me", "u_alex"), participants, promoteLiveToSlot1 = false),
        )
        // Missing IDs fall back cleanly to self in slot 0 and live streamer in slot 1
        assertEquals(
            listOf("u_me", "u_imani"),
            reconcileStageIds(listOf("missing", "u_me"), participants, promoteLiveToSlot1 = true),
        )
    }

    @Test
    fun buildLiveRoomParticipants_mergesRealtimePresenceHandAndMute_andAppendsActivePeers() {
        val me = profile("u_me", "Lincoln Paul")
        val known = mapOf(
            "u_imani" to profile("u_imani", "Imani Okafor"),
        )
        val presence = listOf(
            LiveRoomPresence(
                userId = "u_imani",
                displayName = "Imani Okafor",
                isHandRaised = true,
                isMuted = true,
                lastSeenEpochMs = 5_000L,
            ),
            LiveRoomPresence(
                userId = "u_tessa",
                displayName = "Tessa Wanjiku",
                isHandRaised = true,
                isMuted = false,
                lastSeenEpochMs = 6_000L,
            ),
        )

        val result = buildLiveRoomParticipants(
            activeStreams = emptyList(),
            allStreams = emptyList(),
            knownProfiles = known,
            currentProfile = me,
            roomPresence = presence,
        )

        assertEquals(3, result.size)
        val imani = result.first { it.id == "u_imani" }
        assertTrue(imani.hand)
        assertTrue(imani.muted)

        val tessa = result.first { it.id == "u_tessa" }
        assertEquals("Tessa", tessa.name)
        assertTrue(tessa.hand)
        assertFalse(tessa.muted)
    }

    @Test
    fun resolveFocusedStream_prefersStageSlot1ThenSlot0ThenTopActiveStream() {
        val streamSlot0 = stream("s_me", "u_me", "Lincoln Paul", viewerCount = 2)
        val streamSlot1 = stream("s_imani", "u_imani", "Imani Okafor", viewerCount = 15)
        val otherStream = stream("s_alex", "u_alex", "Alex Otieno", viewerCount = 50)
        val activeStreams = listOf(otherStream, streamSlot0, streamSlot1)

        val pMeLive = LiveRoomParticipant(id = "u_me", name = "Lincoln", live = true, streamId = "s_me", isSelf = true)
        val pImaniLive = LiveRoomParticipant(id = "u_imani", name = "Imani", live = true, streamId = "s_imani")
        val pAlexOffline = LiveRoomParticipant(id = "u_alex", name = "Alex", live = false, streamId = null)

        assertEquals(streamSlot1, resolveFocusedStream(listOf(pMeLive, pImaniLive), activeStreams))
        assertEquals(streamSlot0, resolveFocusedStream(listOf(pMeLive, pAlexOffline), activeStreams))
        assertEquals(otherStream, resolveFocusedStream(listOf(pAlexOffline), activeStreams))
        assertNull(resolveFocusedStream(listOf(pMeLive), emptyList()))
    }
}
