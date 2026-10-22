package com.campus.network.core.data

import android.content.Context
import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.*
import com.campus.network.core.model.SyncStatus
import com.campus.network.core.network.SupabaseClientProvider
import com.campus.network.core.network.SupabaseHttpException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/** Coordinates remote delta reads and local-first pending writes. */
class DeltaSyncManager private constructor(
    private val serverRepository: ServerRepository,
    private val hubRepository: CampusHubRepository,
    private val flashMeetRepository: FlashMeetRepository,
    private val liveStreamRepository: LiveStreamRepository,
    private val profileRepository: StudentProfileRepository,
    private val database: CampusDatabase,
    private val supabaseClient: SupabaseClientProvider,
    private val watermarkStore: SyncWatermarkStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        @Volatile private var instance: DeltaSyncManager? = null

        fun get(context: Context): DeltaSyncManager = instance ?: synchronized(this) {
            instance ?: run {
                val appContext = context.applicationContext
                val database = CampusDatabase.getInstance(appContext)
                val supabase = SupabaseClientProvider()
                DeltaSyncManager(
                    serverRepository = ServerRepository(database, supabase),
                    hubRepository = CampusHubRepository(context, database, supabase),
                    flashMeetRepository = FlashMeetRepository(database, supabase),
                    liveStreamRepository = LiveStreamRepository(database, supabase),
                    profileRepository = StudentProfileRepository(database, supabase),
                    database = database,
                    supabaseClient = supabase,
                    watermarkStore = SyncWatermarkStore.apply { initialize(appContext) }
                ).also { instance = it }
            }
        }
    }

    /** Throws on a table failure; callers can safely retry because each table advances its own cursor. */
    suspend fun syncAll(): Map<String, Long> = withContext(ioDispatcher) {
        linkedMapOf(
            "campusprofile" to syncProfile(),
            "servers" to syncServers(),
            "campus_market_items" to syncMarketItems(),
            "campus_micro_gigs" to syncMicroGigs(),
            "campus_flash_meets" to syncFlashMeets(),
            "campus_live_streams" to syncLiveStreams()
        )
    }

    /**
     * Uploads pending local records. Transient failures remain PENDING and request WorkManager
     * retry; permanent failures transition to FAILED for an explicit user retry.
     */
    suspend fun syncPendingWrites(): Boolean = withContext(ioDispatcher) {
        var shouldRetry = false
        suspend fun accept(retry: Boolean) {
            shouldRetry = shouldRetry || retry
        }

        database.studentProfileDao().getPendingProfile()?.let { accept(uploadProfile(it)) }
        database.serverDao().getPendingServers().forEach { accept(uploadServer(it)) }
        database.serverChannelDao().getPendingChannels().forEach { accept(uploadChannel(it)) }
        database.serverRuleDao().getPendingRules().forEach { accept(uploadRule(it)) }
        database.serverMemberDao().getPendingMembers().forEach { accept(uploadMember(it)) }
        database.friendDao().getPendingFriends().forEach { accept(uploadFriend(it)) }
        database.marketItemDao().getPendingItems().forEach { accept(uploadMarketItem(it)) }
        database.microGigDao().getPendingGigs().forEach { accept(uploadMicroGig(it)) }
        database.liveStreamDao().getPendingStreams().forEach { accept(uploadLiveStream(it)) }
        database.streamCommentDao().getPendingComments().forEach { accept(uploadComment(it)) }
        database.flashMeetDao().getPendingMeets().forEach { accept(uploadFlashMeet(it)) }
        database.serverChannelMessageDao().getPendingSyncMessages().forEach { accept(uploadChannelMessage(it)) }
        shouldRetry
    }

    /** Transitions fatal rows back to PENDING, then attempts them once on explicit user action. */
    suspend fun retryFailedSyncs(): Boolean = withContext(ioDispatcher) {
        database.studentProfileDao().getFailedProfile()?.let {
            database.studentProfileDao().updateProfileSyncStatus(it.id, SyncStatus.PENDING.name, it.syncId)
        }
        database.serverDao().getFailedServers().forEach {
            database.serverDao().updateSyncStatus(it.id, it.syncId, SyncStatus.PENDING.name)
        }
        database.serverChannelDao().getFailedChannels().forEach {
            database.serverChannelDao().updateSyncStatus(it.id, it.syncId, SyncStatus.PENDING.name)
        }
        database.serverRuleDao().getFailedRules().forEach {
            database.serverRuleDao().updateSyncStatus(it.id, it.syncId, SyncStatus.PENDING.name)
        }
        database.serverMemberDao().getFailedMembers().forEach {
            database.serverMemberDao().updateSyncStatus(it.id, it.syncId, SyncStatus.PENDING.name)
        }
        database.friendDao().getFailedFriends().forEach {
            database.friendDao().updateSyncStatus(it.id, it.syncId, SyncStatus.PENDING.name)
        }
        database.marketItemDao().getFailedItems().forEach {
            database.marketItemDao().updateSyncStatus(it.id, it.syncId, SyncStatus.PENDING.name)
        }
        database.microGigDao().getFailedGigs().forEach {
            database.microGigDao().updateSyncStatus(it.id, it.syncId, SyncStatus.PENDING.name)
        }
        database.liveStreamDao().getFailedStreams().forEach {
            database.liveStreamDao().updateSyncStatus(it.id, it.syncId, SyncStatus.PENDING.name)
        }
        database.streamCommentDao().getFailedComments().forEach {
            database.streamCommentDao().updateSyncStatus(it.id, it.syncId, SyncStatus.PENDING.name)
        }
        database.flashMeetDao().getFailedMeets().forEach {
            database.flashMeetDao().updateSyncStatus(it.id, it.syncId, SyncStatus.PENDING.name)
        }
        database.serverChannelMessageDao().getFailedSyncMessages().forEach {
            database.serverChannelMessageDao().updateSyncStatus(it.id, it.syncId, SyncStatus.PENDING.name)
        }
        syncPendingWrites()
    }

    private suspend fun syncProfile(): Long = runDelta("campusprofile") {
        profileRepository.syncRemoteProfiles(it)
    }

    private suspend fun syncServers(): Long = runDelta("servers") {
        serverRepository.syncRemoteServers(it)
    }

    private suspend fun syncMarketItems(): Long = runDelta("campus_market_items") {
        hubRepository.syncRemoteMarketItems(sinceMs = it)
    }

    private suspend fun syncMicroGigs(): Long = runDelta("campus_micro_gigs") {
        hubRepository.syncRemoteMicroGigs(it)
    }

    private suspend fun syncFlashMeets(): Long = runDelta("campus_flash_meets") {
        flashMeetRepository.syncRemoteFlashMeets(it)
    }

    private suspend fun syncLiveStreams(): Long = runDelta("campus_live_streams") {
        liveStreamRepository.syncRemoteLiveStreams(it)
    }

    private suspend fun runDelta(table: String, operation: suspend (Long) -> Result<Long>): Long {
        val since = watermarkStore.getLastSyncedAt(table)
        val next = operation(since).getOrThrow()
        watermarkStore.putLastSyncedAt(table, next)
        return next
    }

    private suspend fun updateStatus(
        result: Result<String>,
        update: suspend (String) -> Unit
    ): Boolean {
        if (result.isSuccess) {
            update(SyncStatus.SYNCED.name)
            return false
        }
        val error = result.exceptionOrNull() ?: IllegalStateException("Remote sync failed without an error")
        val status = if (isTransient(error)) SyncStatus.PENDING else SyncStatus.FAILED
        update(status.name)
        return status == SyncStatus.PENDING
    }

    private fun isTransient(error: Throwable): Boolean {
        var cause: Throwable? = error
        while (cause != null) {
            if (cause is SupabaseHttpException) return cause.isRetryable
            if (cause is IOException) return true
            cause = cause.cause
        }
        return false
    }

    private suspend fun uploadProfile(row: StudentProfileEntity): Boolean {
        val payload = JSONObject().apply {
            put("full_name", row.fullName)
            put("university_name", row.universityName)
            put("degree_program", row.degreeProgram)
            put("academic_year", row.academicYear)
            put("bio", row.bio)
            row.avatarUrl?.let { put("avatar_url", it) }
            put("campus_residence", row.campusResidence)
            put("phone_number", row.phoneNumber)
            put("preferred_rendezvous_point", row.preferredRendezvousPoint)
            put("notifications_enabled", row.notificationsEnabled)
            put("escrow_alerts_enabled", row.escrowAlertsEnabled)
            put("quest_reminders_enabled", row.questRemindersEnabled)
            put("is_trust_score_public", row.isTrustScorePublic)
            put("is_online_status_visible", row.isOnlineStatusVisible)
            put("activity_ledger_visibility", row.activityLedgerVisibility)
            put("location_privacy_scope", row.locationPrivacyScope)
            put("interests", JSONArray(row.interestsCsv.split(";;").filter(String::isNotEmpty)))
            put(
                "onboarding_preferences",
                JSONObject(StudentProfileEntity.serializeOnboardingPreferences(row.toDomain().onboardingPreferences))
            )
        }.toString()
        val userId = com.campus.network.core.network.SupabaseSessionStore.session()
            ?.optString("user_id")?.takeIf(String::isNotBlank)
        val result = if (userId == null) {
            Result.failure(SecurityException("Sign in before syncing profile changes"))
        } else {
            supabaseClient.updateRecord(
                "campusprofile?user_id=eq.${java.net.URLEncoder.encode(userId, "UTF-8")}",
                payload
            ).requireSavedRow()
        }
        return updateStatus(result) {
            database.studentProfileDao().updateProfileSyncStatus(row.id, it, row.syncId)
        }
    }

    private suspend fun uploadServer(row: ServerEntity): Boolean {
        val payload = JSONObject().apply {
            put("id", row.id); put("name", row.name); put("description", row.description)
            put("category", row.category); put("owner_id", row.ownerId)
            put("avatar_res_name", row.avatarResName); row.bannerResName?.let { put("banner_res_name", it) }
            put("is_verified", row.isVerified); put("member_count", row.memberCount)
            put("invite_code", row.inviteCode)
        }.toString()
        val result = supabaseClient.upsertRecord("servers", payload, onConflict = "id").requireSavedRow()
        return updateStatus(result) { database.serverDao().updateSyncStatus(row.id, row.syncId, it) }
    }

    private suspend fun uploadChannel(row: ServerChannelEntity): Boolean {
        val payload = JSONObject().apply {
            put("id", row.id); put("server_id", row.serverId); put("name", row.name)
            put("category_name", row.categoryName); put("channel_type", row.channelType.lowercase())
            put("topic", row.topic); put("is_unread", row.unreadCount > 0)
            put("order_index", row.orderIndex)
        }.toString()
        val result = supabaseClient.upsertRecord("server_channels", payload, onConflict = "id").requireSavedRow()
        return updateStatus(result) { database.serverChannelDao().updateSyncStatus(row.id, row.syncId, it) }
    }

    private suspend fun uploadRule(row: ServerRuleEntity): Boolean {
        val payload = JSONObject().apply {
            put("id", row.id); put("server_id", row.serverId); put("rule_index", row.ruleIndex)
            put("title", row.title); put("description", row.description)
        }.toString()
        val result = supabaseClient.upsertRecord("server_rules", payload, onConflict = "id").requireSavedRow()
        return updateStatus(result) { database.serverRuleDao().updateSyncStatus(row.id, row.syncId, it) }
    }

    private suspend fun uploadMember(row: ServerMemberEntity): Boolean {
        val payload = JSONObject().apply {
            put("id", row.id); put("server_id", row.serverId); put("user_id", row.userId)
            put("display_name", row.displayName); put("handle", row.handle); put("role", row.role)
            put("role_title", row.roleTitle); put("role_color_hex", row.roleColorHex)
            row.avatarUrl?.let { put("avatar_url", it) }
            put("avatar_res_name", row.avatarResName); put("is_online", row.isOnline)
            put("joined_at_epoch_ms", row.joinedAtEpochMs)
        }.toString()
        val result = supabaseClient.upsertRecord("server_members", payload, onConflict = "id").requireSavedRow()
        return updateStatus(result) { database.serverMemberDao().updateSyncStatus(row.id, row.syncId, it) }
    }

    private suspend fun uploadFriend(row: FriendEntity): Boolean {
        val payload = JSONObject().apply {
            put("id", row.id); put("user_id", row.userId); put("friend_user_id", row.friendUserId)
            put("friend_display_name", row.friendDisplayName); put("friend_handle", row.friendHandle)
            put("friend_avatar_res_name", row.friendAvatarResName); put("status", row.status)
            put("last_interaction_epoch_ms", row.lastInteractionEpochMs)
            row.dmChannelId?.let { put("dm_channel_id", it) }
        }.toString()
        val result = supabaseClient.upsertRecord("campus_friends", payload, onConflict = "id").requireSavedRow()
        return updateStatus(result) { database.friendDao().updateSyncStatus(row.id, row.syncId, it) }
    }

    private suspend fun uploadMarketItem(row: MarketItemEntity): Boolean {
        val payload = JSONObject().apply {
            put("id", row.id); put("title", row.title); put("price", row.price)
            put("currency", row.currency); put("category", row.category); put("condition", row.condition)
            put("seller_name", row.sellerName); put("seller_id", row.sellerUserId)
            put("seller_rating", row.sellerRating); put("is_campus_verified_seller", row.isCampusVerifiedSeller)
            put("campus_hall_location", row.campusHallLocation); put("image_url", row.imageUrl)
            put("timestamp_ms", row.timestampMs); put("description", row.description)
            put("pickup_spot_name", row.pickupSpotName); put("pickup_security_level", row.pickupSecurityLevel)
            put("pickup_latitude", row.pickupLatitude); put("pickup_longitude", row.pickupLongitude)
            put("pickup_timing", row.pickupTiming); put("payout_method", row.payoutMethod)
            put("requires_qr_release_code", row.requiresQrReleaseCode); put("qr_release_code", row.qrReleaseCode)
        }.toString()
        val result = supabaseClient.upsertRecord("campus_market_items", payload, onConflict = "id").requireSavedRow()
        return updateStatus(result) { database.marketItemDao().updateSyncStatus(row.id, row.syncId, it) }
    }

    private suspend fun uploadMicroGig(row: MicroGigEntity): Boolean {
        val payload = JSONObject().apply {
            put("id", row.id); put("title", row.title); put("description", row.description)
            put("payout_amount", row.payoutAmount); put("currency", row.currency)
            put("estimated_duration", row.estimatedDuration); put("poster_name", row.posterName)
            put("poster_id", row.posterUserId); put("poster_trust_score", row.posterTrustScore)
            put("tag", row.tag); put("urgency_name", row.urgencyName)
            put("spots_available", row.spotsAvailable); put("timestamp_ms", row.timestampMs)
        }.toString()
        val result = supabaseClient.upsertRecord("campus_micro_gigs", payload, onConflict = "id").requireSavedRow()
        return updateStatus(result) { database.microGigDao().updateSyncStatus(row.id, row.syncId, it) }
    }

    private suspend fun uploadLiveStream(row: LiveStreamEntity): Boolean {
        val payload = JSONObject().apply {
            put("id", row.id); put("broadcaster_id", row.broadcasterId)
            put("broadcaster_name", row.broadcasterName); put("broadcaster_handle", row.broadcasterHandle)
            put("broadcaster_avatar_res", row.broadcasterAvatarRes); put("title", row.title)
            put("description", row.description); put("category", row.category); put("audience", row.audience)
            put("is_live", row.isLive); put("stream_playback_url", row.streamPlaybackUrl)
            put("viewer_count", row.viewerCount); put("peak_viewers", row.peakViewers)
            put("like_count", row.likeCount); put("started_at_epoch_ms", row.startedAtEpochMs)
        }.toString()
        val result = supabaseClient.upsertRecord("campus_live_streams", payload, onConflict = "id").requireSavedRow()
        return updateStatus(result) { database.liveStreamDao().updateSyncStatus(row.id, row.syncId, it) }
    }

    private suspend fun uploadComment(row: StreamCommentEntity): Boolean {
        val payload = JSONObject().apply {
            put("id", row.id); put("stream_id", row.streamId); put("sender_id", row.senderId)
            put("sender_name", row.senderName); put("sender_avatar_url", row.senderAvatarUrl)
            put("sender_avatar_res", row.senderAvatarRes); put("comment_text", row.commentText)
            put("timestamp_epoch_ms", row.timestampEpochMs)
        }.toString()
        val result = supabaseClient.upsertRecord("campus_stream_comments", payload, onConflict = "id").requireSavedRow()
        return updateStatus(result) { database.streamCommentDao().updateSyncStatus(row.id, row.syncId, it) }
    }

    private suspend fun uploadFlashMeet(row: FlashMeetEntity): Boolean {
        val payload = JSONObject().apply {
            put("id", row.id); put("title", row.title); put("description", row.description)
            put("category_name", row.categoryName); put("latitude", row.latitude); put("longitude", row.longitude)
            put("location_name", row.locationName); put("creator_name", row.creatorName)
            put("creator_id", row.creatorUserId); put("created_at_epoch_ms", row.createdAtEpochMs)
            put("expires_at_epoch_ms", row.expiresAtEpochMs); put("attendee_count", row.attendeeCount)
            put("max_attendees", row.maxAttendees)
        }.toString()
        val result = supabaseClient.upsertRecord("campus_flash_meets", payload, onConflict = "id").requireSavedRow()
        return updateStatus(result) { database.flashMeetDao().updateSyncStatus(row.id, row.syncId, it) }
    }

    private suspend fun uploadChannelMessage(row: ServerChannelMessageEntity): Boolean {
        val payload = JSONObject().apply {
            put("id", row.id); put("server_id", row.serverId); put("channel_id", row.channelId)
            put("sender_id", row.senderId); put("sender_name", row.senderName); put("text", row.text)
            put("image_urls", JSONArray(row.imageUrlsJson)); put("reaction_count", row.reactionCount)
            put("timestamp_epoch_ms", row.timestampEpochMs)
        }.toString()
        val result = supabaseClient.upsertRecord("server_channel_messages", payload, onConflict = "id").requireSavedRow()
        return updateStatus(result) {
            database.serverChannelMessageDao().updateSyncStatus(row.id, row.syncId, it)
        }
    }
}
