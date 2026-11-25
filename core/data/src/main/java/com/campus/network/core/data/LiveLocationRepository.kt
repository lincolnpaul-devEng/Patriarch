package com.campus.network.core.data

import com.campus.network.core.model.FriendLiveLocation
import com.campus.network.core.model.LocationPrivacyScope
import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.network.RealtimeEventType
import com.campus.network.core.network.SupabaseRealtimeChange
import com.campus.network.core.network.SupabaseSdkClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.longOrNull
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Serializable
data class LiveLocationRow(
    val user_id: String = "",
    val display_name: String = "",
    val avatar_url: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy_meters: Float? = null,
    val heading: Float? = null,
    val privacy_scope: String = LocationPrivacyScope.GHOST_MODE_HIDDEN.name,
    val is_sharing: Boolean = false,
    val share_token: String? = null,
    val expires_at_epoch_ms: Long = 0L,
    val updated_at_epoch_ms: Long = 0L
)

@Serializable
private data class StartLiveLocationShareParams(
    val p_scope: String,
    val p_ttl_minutes: Int,
    val p_lat: Double,
    val p_lng: Double,
    val p_accuracy: Float? = null
)

@Serializable
private data class RedeemShareTokenParams(
    val p_token: String
)

private val liveLocationJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}

/**
 * Coordinates real-time friend live location streaming, scoped privacy sharing,
 * and revocable token redemption backed by `campus_live_locations`.
 */
class LiveLocationRepository(
    private val supabase: SupabaseClient = SupabaseSdkClientProvider.defaultClient,
    private val studentProfileRepository: StudentProfileRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val profileNameResolver = SupabaseProfileNameResolver(supabase)

    private val _friendLocations = MutableStateFlow<List<FriendLiveLocation>>(emptyList())
    val friendLocations: StateFlow<List<FriendLiveLocation>> = _friendLocations.asStateFlow()

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    private val _activeShareToken = MutableStateFlow<String?>(null)
    val activeShareToken: StateFlow<String?> = _activeShareToken.asStateFlow()

    private val _shareExpiresAtEpochMs = MutableStateFlow(0L)
    val shareExpiresAtEpochMs: StateFlow<Long> = _shareExpiresAtEpochMs.asStateFlow()

    @Volatile private var lastPublishedLat: Double? = null
    @Volatile private var lastPublishedLng: Double? = null
    @Volatile private var lastPublishedAtMs: Long = 0L

    data class ShareSessionResult(
        val userId: String,
        val shareToken: String?,
        val expiresAtEpochMs: Long,
        val privacyScope: LocationPrivacyScope,
        val isSharing: Boolean
    )

    suspend fun startSharing(
        scope: LocationPrivacyScope,
        ttlMinutes: Int = 30,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float? = null
    ): Result<ShareSessionResult> = withContext(ioDispatcher) {
        runCatching {
            val resultObj = supabase.postgrest.rpc(
                "start_live_location_share",
                StartLiveLocationShareParams(
                    p_scope = scope.name,
                    p_ttl_minutes = ttlMinutes,
                    p_lat = latitude,
                    p_lng = longitude,
                    p_accuracy = accuracyMeters
                )
            ).decodeAs<JsonObject>()

            val userId = (resultObj["user_id"] as? JsonPrimitive)?.contentOrNull.orEmpty()
            val token = (resultObj["share_token"] as? JsonPrimitive)?.contentOrNull
                ?.takeIf { it.isNotBlank() && it != "null" }
            val expiresAt = (resultObj["expires_at_epoch_ms"] as? JsonPrimitive)?.longOrNull ?: 0L
            val resolvedScope = (resultObj["privacy_scope"] as? JsonPrimitive)?.contentOrNull
                ?.let { raw -> runCatching { LocationPrivacyScope.valueOf(raw) }.getOrNull() }
                ?: scope
            val sharingActive = (resultObj["is_sharing"] as? JsonPrimitive)?.booleanOrNull
                ?: resolvedScope.allowsCoordinatesBroadcast

            _isSharing.value = sharingActive
            _activeShareToken.value = if (sharingActive) token else null
            _shareExpiresAtEpochMs.value = if (sharingActive) expiresAt else 0L
            if (sharingActive) {
                lastPublishedLat = latitude
                lastPublishedLng = longitude
                lastPublishedAtMs = System.currentTimeMillis()
            } else {
                lastPublishedLat = null
                lastPublishedLng = null
                lastPublishedAtMs = 0L
            }

            runCatching {
                studentProfileRepository?.updateLocationPrivacyScope(resolvedScope)
            }

            ShareSessionResult(
                userId = userId,
                shareToken = _activeShareToken.value,
                expiresAtEpochMs = _shareExpiresAtEpochMs.value,
                privacyScope = resolvedScope,
                isSharing = sharingActive
            )
        }
    }

    suspend fun updateScope(
        scope: LocationPrivacyScope,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float? = null
    ): Result<ShareSessionResult> {
        if (!scope.allowsCoordinatesBroadcast || scope == LocationPrivacyScope.GHOST_MODE_HIDDEN) {
            return stopSharing().map {
                ShareSessionResult(
                    userId = "",
                    shareToken = null,
                    expiresAtEpochMs = 0L,
                    privacyScope = LocationPrivacyScope.GHOST_MODE_HIDDEN,
                    isSharing = false
                )
            }
        }
        return startSharing(
            scope = scope,
            ttlMinutes = if (scope == LocationPrivacyScope.TIMED_SESSION_30M) 30 else 120,
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracyMeters
        )
    }

    fun shouldPublishUpdate(
        latitude: Double,
        longitude: Double,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val prevLat = lastPublishedLat ?: return true
        val prevLng = lastPublishedLng ?: return true
        val elapsed = nowMs - lastPublishedAtMs
        if (elapsed >= MIN_PUBLISH_INTERVAL_MS) return true
        val distanceMeters = haversineMeters(prevLat, prevLng, latitude, longitude)
        return distanceMeters >= MIN_PUBLISH_DISTANCE_METERS
    }

    suspend fun publishLocationUpdate(
        userId: String,
        displayName: String,
        avatarUrl: String?,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float? = null,
        heading: Float? = null,
        scope: LocationPrivacyScope,
        nowMs: Long = System.currentTimeMillis()
    ): Result<Boolean> = withContext(ioDispatcher) {
        val cleanId = userId.trim()
        if (cleanId.isBlank() || !_isSharing.value || !scope.allowsCoordinatesBroadcast) {
            return@withContext Result.success(false)
        }
        val expiresAt = _shareExpiresAtEpochMs.value
        if (expiresAt in 1L..nowMs) {
            stopSharing()
            return@withContext Result.success(false)
        }
        if (!shouldPublishUpdate(latitude, longitude, nowMs)) {
            return@withContext Result.success(false)
        }

        val resolvedExpires = if (expiresAt > nowMs) expiresAt else nowMs + DEFAULT_SHARE_TTL_MS
        runCatching {
            supabase.from("campus_live_locations").upsert(
                LiveLocationRow(
                    user_id = cleanId,
                    display_name = displayName.takeIf(String::isRealProfileDisplayName).orEmpty(),
                    avatar_url = avatarUrl,
                    latitude = latitude,
                    longitude = longitude,
                    accuracy_meters = accuracyMeters,
                    heading = heading,
                    privacy_scope = scope.name,
                    is_sharing = true,
                    share_token = _activeShareToken.value,
                    expires_at_epoch_ms = resolvedExpires,
                    updated_at_epoch_ms = nowMs
                ),
                onConflict = "user_id"
            )
            lastPublishedLat = latitude
            lastPublishedLng = longitude
            lastPublishedAtMs = nowMs
            true
        }
    }

    suspend fun stopSharing(): Result<Unit> = withContext(ioDispatcher) {
        _isSharing.value = false
        _activeShareToken.value = null
        _shareExpiresAtEpochMs.value = 0L
        lastPublishedLat = null
        lastPublishedLng = null
        lastPublishedAtMs = 0L

        runCatching {
            supabase.postgrest.rpc("stop_live_location_share")
            Unit
        }.also {
            runCatching {
                studentProfileRepository?.updateLocationPrivacyScope(LocationPrivacyScope.GHOST_MODE_HIDDEN)
            }
        }
    }

    suspend fun redeemShareToken(
        token: String,
        currentUserId: String? = null
    ): Result<FriendLiveLocation> = withContext(ioDispatcher) {
        runCatching {
            val cleanToken = token.trim()
            require(cleanToken.isNotBlank()) { "Share token is required" }
            val obj = supabase.postgrest.rpc(
                "redeem_location_share_token",
                RedeemShareTokenParams(p_token = cleanToken)
            ).decodeAs<JsonObject>()

            val ownerId = (obj["user_id"] as? JsonPrimitive)?.contentOrNull.orEmpty()
            val rawName = (obj["display_name"] as? JsonPrimitive)?.contentOrNull.orEmpty()
            val avatar = (obj["avatar_url"] as? JsonPrimitive)?.contentOrNull
                ?.takeIf { it.isNotBlank() && !it.startsWith("avatar_") }
            val lat = (obj["latitude"] as? JsonPrimitive)?.doubleOrNull ?: 0.0
            val lng = (obj["longitude"] as? JsonPrimitive)?.doubleOrNull ?: 0.0
            val accuracy = (obj["accuracy_meters"] as? JsonPrimitive)?.floatOrNull
            val heading = (obj["heading"] as? JsonPrimitive)?.floatOrNull
            val scope = (obj["privacy_scope"] as? JsonPrimitive)?.contentOrNull
                ?.let { runCatching { LocationPrivacyScope.valueOf(it) }.getOrNull() }
                ?: LocationPrivacyScope.TIMED_SESSION_30M
            val expiresAt = (obj["expires_at_epoch_ms"] as? JsonPrimitive)?.longOrNull ?: 0L
            val updatedAt = (obj["updated_at_epoch_ms"] as? JsonPrimitive)?.longOrNull
                ?: System.currentTimeMillis()

            val resolved = if (ownerId.isNotBlank()) {
                runCatching { profileNameResolver.resolveWithAvatars(listOf(ownerId)) }
                    .getOrDefault(emptyMap())[ownerId.lowercase()]
            } else {
                null
            }

            val location = FriendLiveLocation(
                userId = ownerId,
                displayName = resolved?.name?.takeIf(String::isRealProfileDisplayName)
                    ?: rawName.takeIf(String::isRealProfileDisplayName)
                    ?: "Campus Peer",
                avatarUrl = avatar ?: resolved?.avatarUrl,
                latitude = lat,
                longitude = lng,
                accuracyMeters = accuracy,
                heading = heading,
                privacyScope = scope,
                expiresAtEpochMs = expiresAt,
                updatedAtEpochMs = updatedAt
            )
            if (currentUserId == null || !ownerId.equals(currentUserId, ignoreCase = true)) {
                _friendLocations.update { existing -> mergeFriendLocation(existing, location) }
            }
            syncVisibleFriendLocations(currentUserId)
            location
        }
    }

    suspend fun syncVisibleFriendLocations(
        currentUserId: String? = null
    ): Result<List<FriendLiveLocation>> = withContext(ioDispatcher) {
        runCatching {
            val now = System.currentTimeMillis()
            val rows: List<LiveLocationRow> = supabase.from("campus_live_locations")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("is_sharing", true)
                        gt("expires_at_epoch_ms", now)
                    }
                    limit(100)
                }
                .decodeList()

            val selfRow = currentUserId?.takeIf { it.isNotBlank() }?.let { me ->
                rows.firstOrNull { it.user_id.equals(me, ignoreCase = true) }
            }
            if (selfRow != null) {
                val scope = runCatching { LocationPrivacyScope.valueOf(selfRow.privacy_scope) }
                    .getOrDefault(LocationPrivacyScope.GHOST_MODE_HIDDEN)
                val active = selfRow.is_sharing &&
                    scope.allowsCoordinatesBroadcast &&
                    selfRow.expires_at_epoch_ms > now
                _isSharing.value = active
                _activeShareToken.value = if (active) selfRow.share_token else null
                _shareExpiresAtEpochMs.value = if (active) selfRow.expires_at_epoch_ms else 0L
            }

            val peerRows = rows.filter { row ->
                val isSelf = !currentUserId.isNullOrBlank() &&
                    row.user_id.equals(currentUserId, ignoreCase = true)
                !isSelf && isRowCurrentlyActive(row, now)
            }

            val resolved = if (peerRows.isNotEmpty()) {
                runCatching {
                    profileNameResolver.resolveWithAvatars(peerRows.map { it.user_id })
                }.getOrDefault(emptyMap())
            } else {
                emptyMap()
            }

            val existingByUser = _friendLocations.value.associateBy { it.userId.trim().lowercase() }
            val mapped = peerRows.mapNotNull { row ->
                val prev = existingByUser[row.user_id.trim().lowercase()]
                toDomainIfActive(row, resolved[row.user_id.trim().lowercase()], now, prev)
            }.sortedByDescending { it.updatedAtEpochMs }

            _friendLocations.value = mapped
            mapped
        }
    }

    suspend fun applyRealtimeChange(
        change: SupabaseRealtimeChange,
        currentUserId: String? = null
    ) = withContext(ioDispatcher) {
        if (!change.table.equals("campus_live_locations", ignoreCase = true)) return@withContext

        if (change.eventType == RealtimeEventType.DELETE) {
            val deletedUserId = extractJsonField(change.oldRecordJson, "user_id") ?: return@withContext
            _friendLocations.update { current ->
                current.filterNot { it.userId.equals(deletedUserId, ignoreCase = true) }
            }
            return@withContext
        }

        val jsonStr = change.recordJson ?: return@withContext
        val row = runCatching {
            liveLocationJson.decodeFromString<LiveLocationRow>(jsonStr)
        }.getOrNull() ?: return@withContext
        if (row.user_id.isBlank()) return@withContext

        val now = System.currentTimeMillis()
        if (!currentUserId.isNullOrBlank() && row.user_id.equals(currentUserId, ignoreCase = true)) {
            val scope = runCatching { LocationPrivacyScope.valueOf(row.privacy_scope) }
                .getOrDefault(LocationPrivacyScope.GHOST_MODE_HIDDEN)
            val active = row.is_sharing && scope.allowsCoordinatesBroadcast && row.expires_at_epoch_ms > now
            _isSharing.value = active
            _activeShareToken.value = if (active) row.share_token else null
            _shareExpiresAtEpochMs.value = if (active) row.expires_at_epoch_ms else 0L
            return@withContext
        }

        if (!isRowCurrentlyActive(row, now)) {
            _friendLocations.update { current ->
                current.filterNot { it.userId.equals(row.user_id, ignoreCase = true) }
            }
            return@withContext
        }

        val resolved = runCatching {
            profileNameResolver.resolveWithAvatars(listOf(row.user_id))
        }.getOrDefault(emptyMap())[row.user_id.trim().lowercase()]

        val prev = _friendLocations.value.firstOrNull { it.userId.equals(row.user_id, ignoreCase = true) }
        val item = toDomainIfActive(row, resolved, now, prev) ?: return@withContext
        _friendLocations.update { current -> mergeFriendLocation(current, item, now) }
    }

    internal fun pruneStaleLocations(nowMs: Long = System.currentTimeMillis()) {
        _friendLocations.update { current ->
            current.filter { loc ->
                loc.privacyScope.allowsCoordinatesBroadcast &&
                    loc.expiresAtEpochMs > nowMs &&
                    (loc.updatedAtEpochMs == 0L || nowMs - loc.updatedAtEpochMs <= STALE_LOCATION_MS)
            }
        }
    }

    private fun isRowCurrentlyActive(row: LiveLocationRow, nowMs: Long): Boolean {
        val scope = runCatching { LocationPrivacyScope.valueOf(row.privacy_scope) }
            .getOrDefault(LocationPrivacyScope.GHOST_MODE_HIDDEN)
        if (!row.is_sharing || !scope.allowsCoordinatesBroadcast) return false
        if (row.expires_at_epoch_ms <= nowMs) return false
        if (row.updated_at_epoch_ms > 0L && nowMs - row.updated_at_epoch_ms > STALE_LOCATION_MS) return false
        return true
    }

    private fun toDomainIfActive(
        row: LiveLocationRow,
        resolved: ResolvedCampusProfile?,
        nowMs: Long,
        previous: FriendLiveLocation? = null
    ): FriendLiveLocation? {
        if (!isRowCurrentlyActive(row, nowMs)) return null
        val scope = runCatching { LocationPrivacyScope.valueOf(row.privacy_scope) }
            .getOrDefault(LocationPrivacyScope.GHOST_MODE_HIDDEN)
        val avatar = row.avatar_url?.takeIf { it.isNotBlank() && !it.startsWith("avatar_") }
            ?: resolved?.avatarUrl
        val name = resolved?.name?.takeIf(String::isRealProfileDisplayName)
            ?: row.display_name.takeIf(String::isRealProfileDisplayName)
            .orEmpty()
        if (name.isBlank()) return null

        val rowUpdatedMs = if (row.updated_at_epoch_ms > 0L) row.updated_at_epoch_ms else nowMs
        var computedSpeedKmh: Float? = previous?.speedKmh
        var stationarySinceMs: Long = previous?.stationarySinceEpochMs?.takeIf { it > 0L } ?: rowUpdatedMs

        if (previous != null) {
            val distMeters = haversineMeters(previous.latitude, previous.longitude, row.latitude, row.longitude)
            val dtSec = ((rowUpdatedMs - previous.updatedAtEpochMs) / 1000.0).coerceAtLeast(0.0)
            if (dtSec >= 1.5) {
                val rawKmh = ((distMeters / dtSec) * 3.6).toFloat()
                if (distMeters >= 8.0 && rawKmh in 1.5f..140f) {
                    computedSpeedKmh = rawKmh
                    stationarySinceMs = rowUpdatedMs
                } else if (distMeters < 6.0) {
                    computedSpeedKmh = 0f
                }
            } else if (distMeters < 5.0) {
                stationarySinceMs = previous.stationarySinceEpochMs.takeIf { it > 0L } ?: previous.updatedAtEpochMs
            }
        } else {
            val defaultSessionStart = (row.expires_at_epoch_ms - DEFAULT_SHARE_TTL_MS).coerceIn(1L, rowUpdatedMs)
            stationarySinceMs = defaultSessionStart
        }

        return FriendLiveLocation(
            userId = row.user_id,
            displayName = name,
            avatarUrl = avatar,
            latitude = row.latitude,
            longitude = row.longitude,
            accuracyMeters = row.accuracy_meters,
            heading = row.heading,
            speedKmh = computedSpeedKmh,
            stationarySinceEpochMs = stationarySinceMs,
            privacyScope = scope,
            expiresAtEpochMs = row.expires_at_epoch_ms,
            updatedAtEpochMs = row.updated_at_epoch_ms
        )
    }

    private fun mergeFriendLocation(
        existing: List<FriendLiveLocation>,
        incoming: FriendLiveLocation,
        nowMs: Long = System.currentTimeMillis()
    ): List<FriendLiveLocation> {
        return (existing.filterNot {
            it.userId.equals(incoming.userId, ignoreCase = true) ||
                it.expiresAtEpochMs <= nowMs ||
                (it.updatedAtEpochMs > 0L && nowMs - it.updatedAtEpochMs > STALE_LOCATION_MS)
        } + incoming).sortedByDescending { it.updatedAtEpochMs }
    }

    private fun extractJsonField(jsonStr: String?, key: String): String? {
        if (jsonStr.isNullOrBlank()) return null
        val obj = runCatching {
            liveLocationJson.parseToJsonElement(jsonStr) as? JsonObject
        }.getOrNull() ?: return null
        return (obj[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
    }

    companion object {
        const val MIN_PUBLISH_INTERVAL_MS = 5_000L
        const val MIN_PUBLISH_DISTANCE_METERS = 10.0
        const val DEFAULT_SHARE_TTL_MS = 30L * 60_000L
        const val STALE_LOCATION_MS = 3L * 60_000L

        internal fun haversineMeters(
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double
        ): Double {
            val r = 6_371_000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }
    }
}
