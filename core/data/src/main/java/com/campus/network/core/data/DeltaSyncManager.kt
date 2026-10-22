package com.campus.network.core.data

import android.content.Context
import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.*
import com.campus.network.core.model.SyncStatus
import com.campus.network.core.network.SupabaseSdkClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException

/** Coordinates remote delta reads and local-first pending writes. */
class DeltaSyncManager private constructor(
    private val serverRepository: ServerRepository,
    private val hubRepository: CampusHubRepository,
    private val flashMeetRepository: FlashMeetRepository,
    private val liveStreamRepository: LiveStreamRepository,
    private val profileRepository: StudentProfileRepository,
    private val database: CampusDatabase,
    private val supabase: SupabaseClient = SupabaseSdkClientProvider.defaultClient,
    private val watermarkStore: SyncWatermarkStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        @Volatile private var instance: DeltaSyncManager? = null

        fun get(context: Context): DeltaSyncManager = instance ?: synchronized(this) {
            instance ?: run {
                val appContext = context.applicationContext
                val database = CampusDatabase.getInstance(appContext)
                val supabase = SupabaseSdkClientProvider.defaultClient
                DeltaSyncManager(
                    serverRepository = ServerRepository(database, supabase),
                    hubRepository = CampusHubRepository(context, database, supabase),
                    flashMeetRepository = FlashMeetRepository(database, supabase),
                    liveStreamRepository = LiveStreamRepository(database, supabase),
                    profileRepository = StudentProfileRepository(database, supabase),
                    database = database,
                    supabase = supabase,
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
        result: Result<Unit>,
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
            if (cause is IOException) return true
            cause = cause.cause
        }
        return false
    }

    private suspend fun uploadProfile(row: StudentProfileEntity): Boolean {
        val userId = com.campus.network.core.network.SupabaseSessionStore.session()
            ?.optString("user_id")?.takeIf(String::isNotBlank)
        val result = if (userId == null) {
            Result.failure(SecurityException("Sign in before syncing profile changes"))
        } else {
            runCatching {
                supabase.from("campusprofile").update(
                    CampusProfileRow(
                        id = row.id,
                        user_id = userId,
                        full_name = row.fullName,
                        university_name = row.universityName,
                        degree_program = row.degreeProgram,
                        academic_year = row.academicYear,
                        bio = row.bio,
                        avatar_url = row.avatarUrl,
                        campus_residence = row.campusResidence,
                        phone_number = row.phoneNumber,
                        preferred_rendezvous_point = row.preferredRendezvousPoint,
                        notifications_enabled = row.notificationsEnabled,
                        escrow_alerts_enabled = row.escrowAlertsEnabled,
                        quest_reminders_enabled = row.questRemindersEnabled,
                        is_trust_score_public = row.isTrustScorePublic,
                        is_online_status_visible = row.isOnlineStatusVisible,
                        activity_ledger_visibility = row.activityLedgerVisibility,
                        location_privacy_scope = row.locationPrivacyScope
                    )
                ) {
                    filter { eq("user_id", userId) }
                }
                Unit
            }
        }
        return updateStatus(result) {
            database.studentProfileDao().updateProfileSyncStatus(row.id, it, row.syncId)
        }
    }

    private suspend fun uploadServer(row: ServerEntity): Boolean {
        val result = runCatching {
            supabase.from("servers").upsert(
                ServerRow(
                    id = row.id,
                    name = row.name,
                    description = row.description,
                    category = row.category,
                    owner_id = row.ownerId,
                    avatar_res_name = row.avatarResName,
                    banner_res_name = row.bannerResName,
                    is_verified = row.isVerified,
                    member_count = row.memberCount,
                    invite_code = row.inviteCode,
                    created_at_epoch_ms = row.createdAtEpochMs
                )
            )
            Unit
        }
        return updateStatus(result) { database.serverDao().updateSyncStatus(row.id, row.syncId, it) }
    }

    private suspend fun uploadChannel(row: ServerChannelEntity): Boolean {
        val result = runCatching {
            supabase.from("server_channels").upsert(
                ServerChannelRow(
                    id = row.id,
                    server_id = row.serverId,
                    name = row.name,
                    category_name = row.categoryName,
                    channel_type = row.channelType.lowercase(),
                    topic = row.topic,
                    is_unread = row.unreadCount > 0,
                    order_index = row.orderIndex
                )
            )
            Unit
        }
        return updateStatus(result) { database.serverChannelDao().updateSyncStatus(row.id, row.syncId, it) }
    }

    private suspend fun uploadRule(row: ServerRuleEntity): Boolean {
        val result = runCatching {
            supabase.from("server_rules").upsert(
                ServerRuleRow(
                    id = row.id,
                    server_id = row.serverId,
                    rule_index = row.ruleIndex,
                    title = row.title,
                    description = row.description
                )
            )
            Unit
        }
        return updateStatus(result) { database.serverRuleDao().updateSyncStatus(row.id, row.syncId, it) }
    }

    private suspend fun uploadMember(row: ServerMemberEntity): Boolean {
        val result = runCatching {
            supabase.from("server_members").upsert(
                ServerMemberRow(
                    id = row.id,
                    server_id = row.serverId,
                    user_id = row.userId,
                    display_name = row.displayName,
                    handle = row.handle,
                    role = row.role,
                    role_title = row.roleTitle,
                    role_color_hex = row.roleColorHex,
                    avatar_url = row.avatarUrl,
                    avatar_res_name = row.avatarResName,
                    is_online = row.isOnline,
                    joined_at_epoch_ms = row.joinedAtEpochMs
                )
            )
            Unit
        }
        return updateStatus(result) { database.serverMemberDao().updateSyncStatus(row.id, row.syncId, it) }
    }

    private suspend fun uploadFriend(row: FriendEntity): Boolean {
        val result = runCatching {
            supabase.from("campus_friends").upsert(
                FriendRow(
                    id = row.id,
                    user_id = row.userId,
                    friend_user_id = row.friendUserId,
                    friend_display_name = row.friendDisplayName,
                    friend_handle = row.friendHandle,
                    friend_avatar_res_name = row.friendAvatarResName,
                    status = row.status,
                    last_interaction_epoch_ms = row.lastInteractionEpochMs,
                    dm_channel_id = row.dmChannelId
                )
            )
            Unit
        }
        return updateStatus(result) { database.friendDao().updateSyncStatus(row.id, row.syncId, it) }
    }

    private suspend fun uploadMarketItem(row: MarketItemEntity): Boolean {
        val result = runCatching {
            supabase.from("campus_market_items").upsert(
                MarketItemRow(
                    id = row.id,
                    title = row.title,
                    price = row.price,
                    currency = row.currency,
                    category = row.category,
                    condition = row.condition,
                    seller_name = row.sellerName,
                    seller_id = row.sellerUserId,
                    seller_rating = row.sellerRating.toDouble(),
                    is_campus_verified_seller = row.isCampusVerifiedSeller,
                    campus_hall_location = row.campusHallLocation,
                    image_url = row.imageUrl,
                    timestamp_ms = row.timestampMs,
                    description = row.description,
                    pickup_spot_name = row.pickupSpotName,
                    pickup_security_level = row.pickupSecurityLevel,
                    pickup_latitude = row.pickupLatitude,
                    pickup_longitude = row.pickupLongitude,
                    pickup_timing = row.pickupTiming,
                    payout_method = row.payoutMethod,
                    requires_qr_release_code = row.requiresQrReleaseCode,
                    qr_release_code = row.qrReleaseCode
                )
            )
            Unit
        }
        return updateStatus(result) { database.marketItemDao().updateSyncStatus(row.id, row.syncId, it) }
    }

    private suspend fun uploadMicroGig(row: MicroGigEntity): Boolean {
        val result = runCatching {
            supabase.from("campus_micro_gigs").upsert(
                MicroGigRow(
                    id = row.id,
                    title = row.title,
                    description = row.description,
                    payout_amount = row.payoutAmount,
                    currency = row.currency,
                    estimated_duration = row.estimatedDuration,
                    poster_name = row.posterName,
                    poster_id = row.posterUserId,
                    poster_trust_score = row.posterTrustScore,
                    tag = row.tag,
                    urgency_name = row.urgencyName,
                    spots_available = row.spotsAvailable,
                    timestamp_ms = row.timestampMs,
                    banner_url = row.bannerUrl
                )
            )
            Unit
        }
        return updateStatus(result) { database.microGigDao().updateSyncStatus(row.id, row.syncId, it) }
    }

    private suspend fun uploadLiveStream(row: LiveStreamEntity): Boolean {
        val result = runCatching {
            supabase.from("campus_live_streams").upsert(
                LiveStreamRow(
                    id = row.id,
                    broadcaster_id = row.broadcasterId,
                    broadcaster_name = row.broadcasterName,
                    broadcaster_handle = row.broadcasterHandle,
                    broadcaster_avatar_res = row.broadcasterAvatarRes,
                    title = row.title,
                    description = row.description,
                    category = row.category,
                    audience = row.audience,
                    is_live = row.isLive,
                    stream_playback_url = row.streamPlaybackUrl,
                    viewer_count = row.viewerCount,
                    peak_viewers = row.peakViewers,
                    like_count = row.likeCount,
                    started_at_epoch_ms = row.startedAtEpochMs
                )
            )
            Unit
        }
        return updateStatus(result) { database.liveStreamDao().updateSyncStatus(row.id, row.syncId, it) }
    }

    private suspend fun uploadComment(row: StreamCommentEntity): Boolean {
        val result = runCatching {
            supabase.from("campus_stream_comments").upsert(
                StreamCommentRow(
                    id = row.id,
                    stream_id = row.streamId,
                    sender_id = row.senderId,
                    sender_name = row.senderName,
                    sender_avatar_url = row.senderAvatarUrl,
                    sender_avatar_res = row.senderAvatarRes,
                    comment_text = row.commentText,
                    timestamp_epoch_ms = row.timestampEpochMs
                )
            )
            Unit
        }
        return updateStatus(result) { database.streamCommentDao().updateSyncStatus(row.id, row.syncId, it) }
    }

    private suspend fun uploadFlashMeet(row: FlashMeetEntity): Boolean {
        val result = runCatching {
            supabase.from("campus_flash_meets").upsert(
                FlashMeetRow(
                    id = row.id,
                    title = row.title,
                    description = row.description,
                    category_name = row.categoryName,
                    latitude = row.latitude,
                    longitude = row.longitude,
                    location_name = row.locationName,
                    creator_name = row.creatorName,
                    creator_id = row.creatorUserId,
                    created_at_epoch_ms = row.createdAtEpochMs,
                    expires_at_epoch_ms = row.expiresAtEpochMs,
                    attendee_count = row.attendeeCount,
                    max_attendees = row.maxAttendees
                )
            )
            Unit
        }
        return updateStatus(result) { database.flashMeetDao().updateSyncStatus(row.id, row.syncId, it) }
    }

    private suspend fun uploadChannelMessage(row: ServerChannelMessageEntity): Boolean {
        val result = runCatching {
            supabase.from("server_channel_messages").upsert(
                ServerChannelMessageRow(
                    id = row.id,
                    server_id = row.serverId,
                    channel_id = row.channelId,
                    sender_id = row.senderId,
                    sender_name = row.senderName,
                    text = row.text,
                    image_urls = row.imageUrlsJson.takeIf { it.isNotBlank() },
                    reaction_count = row.reactionCount,
                    timestamp_epoch_ms = row.timestampEpochMs
                )
            )
            Unit
        }
        return updateStatus(result) {
            database.serverChannelMessageDao().updateSyncStatus(row.id, row.syncId, it)
        }
    }
}
