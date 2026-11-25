package com.campus.network.core.data

import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.CampusPlaceEntity
import com.campus.network.core.model.CampusPlace
import com.campus.network.core.model.PlaceCategory
import com.campus.network.core.model.PlaceReview
import com.campus.network.core.model.PlaceStats
import com.campus.network.core.model.SavedByUser
import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.network.SupabaseRealtimeChange
import com.campus.network.core.network.SupabaseSdkClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Serializable
data class CampusPlaceRow(
    val id: String = "",
    val name: String = "",
    val subtitle: String = "",
    val address: String = "",
    val category: String = PlaceCategory.STUDY_ZONES.name,
    val campus_region: String = "Central Campus",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val rating: Float = 4.8f,
    val review_count: Int = 0,
    val is_open_now: Boolean = true,
    val opening_hours: String = "Open 24/7",
    val is_verified_safe_spot: Boolean = false,
    val image_url: String? = null,
    val features: JsonElement? = null,
    val neighborhood: String = "",
    val security_level: String = "CCTV Monitored · 24/7 Guard",
    val has_wifi: Boolean = true,
    val has_power_outlets: Boolean = true
)

@Serializable
data class CampusPlaceBookmarkRow(
    val place_id: String = "",
    val user_id: String = "",
    val status: String = STATUS_SAVED,
    val created_at_epoch_ms: Long = 0L
) {
    companion object {
        const val STATUS_SAVED = "SAVED"
        const val STATUS_BEEN = "BEEN"
    }
}

@Serializable
data class CampusPlaceReviewRow(
    val id: String = "",
    val place_id: String = "",
    val user_id: String = "",
    val author_name: String = "",
    val comment: String = "",
    val created_at_epoch_ms: Long = 0L
)

/**
 * Online-first repository for campus SafeTrade checkpoints, study zones, labs,
 * user bookmarks/check-ins, and peer reviews.
 */
class PlacesRepository(
    private val database: CampusDatabase? = null,
    private val supabase: SupabaseClient = SupabaseSdkClientProvider.defaultClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val placeDao = database?.campusPlaceDao()
    private val profileNameResolver = SupabaseProfileNameResolver(supabase)

    private val _memoryPlaces = MutableStateFlow<List<CampusPlace>>(emptyList())
    private val _savedPlaceIds = MutableStateFlow<Set<String>>(emptySet())
    val savedPlaceIds: StateFlow<Set<String>> = _savedPlaceIds.asStateFlow()

    private val _beenPlaceIds = MutableStateFlow<Set<String>>(emptySet())
    val beenPlaceIds: StateFlow<Set<String>> = _beenPlaceIds.asStateFlow()

    private val _friendSavedPlaceIds = MutableStateFlow<Set<String>>(emptySet())
    val friendSavedPlaceIds: StateFlow<Set<String>> = _friendSavedPlaceIds.asStateFlow()

    private val _savedByPlace = MutableStateFlow<Map<String, List<SavedByUser>>>(emptyMap())
    private val _reviewsByPlace = MutableStateFlow<Map<String, List<PlaceReview>>>(emptyMap())
    val reviewsByPlace: StateFlow<Map<String, List<PlaceReview>>> = _reviewsByPlace.asStateFlow()

    companion object {
        val LEGACY_SEEDED_PLACE_IDS = setOf(
            "strat_quad_b",
            "main_library",
            "innovation_hub_auditorium",
            "student_guild_hub",
            "mombasa_marine_commons",
            "kisumu_lakeside_lab",
            "kisumu_lakeview_lab"
        )
    }

    private val basePlacesFlow: Flow<List<CampusPlace>> = if (placeDao != null) {
        placeDao.getAllPlaces().map { list ->
            list.filterNot { it.id in LEGACY_SEEDED_PLACE_IDS }.map { it.toDomain() }
        }
    } else {
        _memoryPlaces.map { list -> list.filterNot { it.id in LEGACY_SEEDED_PLACE_IDS } }
    }

    val places: Flow<List<CampusPlace>> = combine(
        basePlacesFlow,
        _savedPlaceIds,
        _beenPlaceIds,
        _savedByPlace,
        _reviewsByPlace
    ) { base, savedIds, beenIds, savedByMap, reviewsMap ->
        base.map { place ->
            val users = savedByMap[place.id].orEmpty()
            val reviews = reviewsMap[place.id].orEmpty()
            val savesCount = maxOf(place.savesCount, users.count { it.isBookmarked })
            place.copy(
                isSavedToTry = place.id in savedIds,
                hasBeen = place.id in beenIds,
                savesCount = savesCount,
                savedByUsers = users,
                reviews = reviews,
                stats = place.stats.copy(saves = savesCount, stars = reviews.size)
            )
        }
    }.flowOn(ioDispatcher)

    suspend fun syncPlaces(
        currentUserId: String? = null,
        friendIds: Set<String> = emptySet()
    ): Result<Long> = withContext(ioDispatcher) {
        runCatching {
            val rows: List<CampusPlaceRow> = supabase.from("campus_places")
                .select(columns = Columns.ALL) {
                    order("name", order = Order.ASCENDING)
                    limit(150)
                }
                .decodeList()

            val entities = rows
                .filterNot { it.id in LEGACY_SEEDED_PLACE_IDS }
                .map { it.toEntity() }
            if (placeDao != null) {
                if (entities.isNotEmpty()) {
                    placeDao.insertPlaces(entities)
                    placeDao.deleteGhostPlaces(entities.map { it.id })
                } else {
                    placeDao.clearAllPlaces()
                }
            } else {
                _memoryPlaces.value = entities.map { it.toDomain() }
            }

            syncBookmarksAndReviews(currentUserId = currentUserId, friendIds = friendIds)
            System.currentTimeMillis()
        }
    }

    suspend fun syncBookmarksAndReviews(
        currentUserId: String? = null,
        friendIds: Set<String> = emptySet()
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val bookmarks: List<CampusPlaceBookmarkRow> = runCatching {
                supabase.from("campus_place_bookmarks")
                    .select(columns = Columns.ALL) {
                        order("created_at_epoch_ms", order = Order.DESCENDING)
                        limit(400)
                    }
                    .decodeList<CampusPlaceBookmarkRow>()
            }.getOrDefault(emptyList())

            val reviews: List<CampusPlaceReviewRow> = runCatching {
                supabase.from("campus_place_reviews")
                    .select(columns = Columns.ALL) {
                        order("created_at_epoch_ms", order = Order.DESCENDING)
                        limit(300)
                    }
                    .decodeList<CampusPlaceReviewRow>()
            }.getOrDefault(emptyList())

            val cleanCurrentId = currentUserId?.trim().orEmpty()
            val lowerFriendIds = friendIds.map { it.trim().lowercase() }.toSet()

            val userIdsToResolve = (bookmarks.map { it.user_id } + reviews.map { it.user_id }).distinct()
            val resolvedProfiles = if (userIdsToResolve.isNotEmpty()) {
                runCatching { profileNameResolver.resolve(userIdsToResolve) }.getOrDefault(emptyMap())
            } else {
                emptyMap()
            }

            if (cleanCurrentId.isNotBlank()) {
                _savedPlaceIds.value = bookmarks
                    .filter {
                        it.user_id.equals(cleanCurrentId, ignoreCase = true) &&
                            it.status == CampusPlaceBookmarkRow.STATUS_SAVED
                    }
                    .map { it.place_id }
                    .toSet()

                _beenPlaceIds.value = bookmarks
                    .filter {
                        it.user_id.equals(cleanCurrentId, ignoreCase = true) &&
                            it.status == CampusPlaceBookmarkRow.STATUS_BEEN
                    }
                    .map { it.place_id }
                    .toSet()
            }

            if (lowerFriendIds.isNotEmpty()) {
                _friendSavedPlaceIds.value = (
                    bookmarks.filter { it.user_id.trim().lowercase() in lowerFriendIds }.map { it.place_id } +
                        reviews.filter { it.user_id.trim().lowercase() in lowerFriendIds }.map { it.place_id }
                    ).toSet()
            }

            _savedByPlace.value = bookmarks.groupBy { it.place_id }
                .mapValues { (_, list) ->
                    list.distinctBy { it.user_id.trim().lowercase() }
                        .take(10)
                        .mapNotNull { row ->
                            val name = resolvedProfiles[row.user_id.trim().lowercase()]
                                ?.takeIf(String::isRealProfileDisplayName)
                                ?: return@mapNotNull null
                            SavedByUser(
                                id = row.user_id,
                                username = name,
                                avatarResName = "",
                                isLiked = row.status == CampusPlaceBookmarkRow.STATUS_BEEN,
                                isBookmarked = row.status == CampusPlaceBookmarkRow.STATUS_SAVED
                            )
                        }
                }

            _reviewsByPlace.value = reviews.groupBy { it.place_id }
                .mapValues { (_, list) ->
                    list.mapNotNull { row ->
                        val author = resolvedProfiles[row.user_id.trim().lowercase()]
                            ?.takeIf(String::isRealProfileDisplayName)
                            ?: row.author_name.takeIf(String::isRealProfileDisplayName)
                            ?: "Campus Peer"
                        if (row.comment.isBlank()) return@mapNotNull null
                        PlaceReview(
                            id = row.id,
                            placeId = row.place_id,
                            userId = row.user_id,
                            authorName = author,
                            comment = row.comment,
                            createdAtEpochMs = row.created_at_epoch_ms
                        )
                    }
                }
        }
    }

    fun getPlacesByCategory(category: PlaceCategory?): Flow<List<CampusPlace>> {
        return places.map { list ->
            if (category == null || category == PlaceCategory.TOP_PICKS) {
                list
            } else {
                list.filter { it.category == category }
            }
        }
    }

    fun getPlacesSortedByProximity(userLat: Double, userLng: Double): Flow<List<CampusPlace>> {
        return places.map { list ->
            list.sortedBy { place ->
                haversineDistanceKm(userLat, userLng, place.latitude, place.longitude)
            }
        }
    }

    suspend fun toggleSavedToTry(
        placeId: String,
        userId: String = ""
    ): Result<Boolean> = toggleBookmarkStatus(
        placeId = placeId,
        userId = userId,
        status = CampusPlaceBookmarkRow.STATUS_SAVED,
        stateFlow = _savedPlaceIds
    )

    suspend fun toggleBeen(
        placeId: String,
        userId: String = ""
    ): Result<Boolean> = toggleBookmarkStatus(
        placeId = placeId,
        userId = userId,
        status = CampusPlaceBookmarkRow.STATUS_BEEN,
        stateFlow = _beenPlaceIds
    )

    private suspend fun toggleBookmarkStatus(
        placeId: String,
        userId: String,
        status: String,
        stateFlow: MutableStateFlow<Set<String>>
    ): Result<Boolean> = withContext(ioDispatcher) {
        val cleanUserId = userId.trim()
        if (cleanUserId.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Please sign in to save places to your account.")
            )
        }
        val wasPresent = placeId in stateFlow.value
        val nextState = !wasPresent
        stateFlow.update { if (nextState) it + placeId else it - placeId }

        val remoteResult = runCatching {
            if (nextState) {
                supabase.from("campus_place_bookmarks").upsert(
                    CampusPlaceBookmarkRow(
                        place_id = placeId,
                        user_id = cleanUserId,
                        status = status,
                        created_at_epoch_ms = System.currentTimeMillis()
                    ),
                    onConflict = "place_id,user_id,status"
                )
            } else {
                supabase.from("campus_place_bookmarks").delete {
                    filter {
                        eq("place_id", placeId)
                        eq("user_id", cleanUserId)
                        eq("status", status)
                    }
                }
            }
            nextState
        }

        remoteResult.onFailure {
            stateFlow.update { if (wasPresent) it + placeId else it - placeId }
        }
        remoteResult
    }

    suspend fun submitReview(
        placeId: String,
        userId: String,
        authorName: String,
        comment: String
    ): Result<PlaceReview> = withContext(ioDispatcher) {
        val cleanComment = comment.trim()
        val cleanUserId = userId.trim()
        if (cleanComment.isBlank() || cleanUserId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User ID and review text are required"))
        }
        val reviewId = "rev_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val cleanAuthor = authorName.takeIf(String::isRealProfileDisplayName) ?: ""
        val optimistic = PlaceReview(
            id = reviewId,
            placeId = placeId,
            userId = cleanUserId,
            authorName = cleanAuthor.ifBlank { "You" },
            comment = cleanComment,
            createdAtEpochMs = now
        )
        _reviewsByPlace.update { map ->
            map + (placeId to (listOf(optimistic) + map[placeId].orEmpty()))
        }

        val remoteResult = runCatching {
            supabase.from("campus_place_reviews").upsert(
                CampusPlaceReviewRow(
                    id = reviewId,
                    place_id = placeId,
                    user_id = cleanUserId,
                    author_name = cleanAuthor,
                    comment = cleanComment,
                    created_at_epoch_ms = now
                )
            )
            optimistic
        }
        remoteResult.onFailure {
            _reviewsByPlace.update { map ->
                map + (placeId to map[placeId].orEmpty().filterNot { it.id == reviewId })
            }
        }
        remoteResult
    }

    suspend fun applyRealtimeChange(
        change: SupabaseRealtimeChange,
        currentUserId: String? = null,
        friendIds: Set<String> = emptySet()
    ) {
        when (change.table.lowercase()) {
            "campus_places" -> syncPlaces(currentUserId = currentUserId, friendIds = friendIds)
            "campus_place_bookmarks",
            "campus_place_reviews" -> syncBookmarksAndReviews(currentUserId = currentUserId, friendIds = friendIds)
        }
    }

    fun setPlaces(places: List<CampusPlace>) {
        _memoryPlaces.value = places
    }

    fun addPlace(place: CampusPlace) {
        _memoryPlaces.value = _memoryPlaces.value + place
    }

    private fun CampusPlaceRow.toEntity(): CampusPlaceEntity {
        val parsedFeatures = extractFeatures(features)
        val hasWifiFlag = has_wifi || parsedFeatures.any { it.equals("wifi", ignoreCase = true) }
        val hasPowerFlag = has_power_outlets || parsedFeatures.any { it.equals("power", ignoreCase = true) }
        val resolvedCategory = runCatching { PlaceCategory.valueOf(category.trim().uppercase()) }
            .getOrElse {
                if (is_verified_safe_spot) PlaceCategory.RENDEZVOUS else PlaceCategory.STUDY_ZONES
            }

        return CampusPlaceEntity(
            id = id,
            name = name,
            subtitle = subtitle,
            address = address,
            categoryName = resolvedCategory.name,
            neighborhood = neighborhood.ifBlank { campus_region },
            campusRegion = campus_region.ifBlank { "Central Campus" },
            description = subtitle.ifBlank { address },
            latitude = latitude,
            longitude = longitude,
            savesCount = review_count.coerceAtLeast(0),
            rating = rating,
            reviewCount = review_count,
            hoursText = opening_hours.ifBlank { if (is_open_now) "Open Now" else "Closed" },
            isOpenNow = is_open_now,
            securityLevel = security_level.ifBlank {
                if (is_verified_safe_spot) "CCTV Monitored · Verified SafeTrade" else "Campus Hub Access"
            },
            isVerifiedSafeTrade = is_verified_safe_spot || resolvedCategory == PlaceCategory.RENDEZVOUS,
            hasWifi = hasWifiFlag,
            hasPowerOutlets = hasPowerFlag,
            imageUrl = image_url?.takeIf { it.isNotBlank() }
        )
    }

    private fun CampusPlaceEntity.toDomain(): CampusPlace {
        val cat = runCatching { PlaceCategory.valueOf(categoryName) }.getOrDefault(PlaceCategory.STUDY_ZONES)
        return CampusPlace(
            id = id,
            name = name,
            category = cat,
            subtitle = subtitle,
            address = address,
            neighborhood = neighborhood,
            campusRegion = campusRegion,
            description = description,
            latitude = latitude,
            longitude = longitude,
            savesCount = savesCount,
            rankingTag = if (isVerifiedSafeTrade) "SAFETRADE SPOT" else "VERIFIED HUB",
            hoursText = hoursText,
            isOpenNow = isOpenNow,
            securityLevel = securityLevel,
            isVerifiedSafeTrade = isVerifiedSafeTrade,
            hasWifi = hasWifi,
            hasPowerOutlets = hasPowerOutlets,
            imageUrl = imageUrl,
            stats = PlaceStats(saves = savesCount, stars = reviewCount, likes = savesCount, dislikes = 0)
        )
    }

    private fun extractFeatures(element: JsonElement?): List<String> {
        if (element is JsonArray) {
            return element.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim() }
        }
        return emptyList()
    }

    private fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
