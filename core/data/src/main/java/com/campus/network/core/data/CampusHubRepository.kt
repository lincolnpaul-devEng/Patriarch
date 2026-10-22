package com.campus.network.core.data

import android.content.Context
import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.MarketItemEntity
import com.campus.network.core.database.entity.MicroGigEntity
import com.campus.network.core.model.GigUrgency
import com.campus.network.core.model.MarketItem
import com.campus.network.core.model.MicroGig
import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.network.OpenRouterAiClient
import com.campus.network.core.network.SupabaseSdkClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class MarketItemRow(
    val id: String = "",
    val title: String = "",
    val price: Double = 0.0,
    val currency: String = "KES",
    val category: String = "General",
    val condition: String = "Good",
    val seller_name: String = "",
    val seller_id: String = "",
    val seller_rating: Double = 5.0,
    val is_campus_verified_seller: Boolean = true,
    val campus_hall_location: String = "Main Campus",
    val image_url: String? = null,
    val timestamp_ms: Long = 0L,
    val description: String = "",
    val pickup_spot_name: String = "Quad Library Ground Floor Lockers",
    val pickup_security_level: String = "CCTV Monitored Safe Zone",
    val pickup_latitude: Double = -1.3090,
    val pickup_longitude: Double = 36.8120,
    val pickup_timing: String = "Immediate (within 1 hr)",
    val payout_method: String = "Campus SafePay Escrow",
    val requires_qr_release_code: Boolean = true,
    val qr_release_code: String? = null
)

@Serializable
data class MicroGigRow(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val payout_amount: Double = 0.0,
    val currency: String = "KES",
    val estimated_duration: String = "1 hour",
    val poster_name: String = "",
    val poster_id: String = "",
    val poster_trust_score: Int = 95,
    val tag: String = "General",
    val urgency_name: String = "NORMAL",
    val spots_available: Int = 1,
    val timestamp_ms: Long = 0L,
    val banner_url: String? = null
)

@Serializable
data class GetReleaseCodeParams(
    val p_item_id: String,
    val p_caller_id: String
)

@Serializable
data class VerifyReleaseCodeParams(
    val p_item_id: String,
    val p_scanned_code: String
)

/**
 * Repository for Tab 4: Hub Layer (Campus Micro-Commerce).
 * Powers the dual-view switcher hosting hyper-local item marketplace and real-time micro-gig board.
 */
class CampusHubRepository(
    private val context: Context,
    private val database: CampusDatabase,
    private val supabase: SupabaseClient = SupabaseSdkClientProvider.defaultClient,
    private val aiClient: OpenRouterAiClient = OpenRouterAiClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val itemDao = database.marketItemDao()
    private val gigDao = database.microGigDao()
    private val profileNameResolver = SupabaseProfileNameResolver(supabase)

    fun getMarketplaceItems(): Flow<List<MarketItem>> {
        return itemDao.getAllMarketItems()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    fun getMicroGigs(): Flow<List<MicroGig>> {
        return gigDao.getAllGigs()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    suspend fun postMarketItem(
        title: String,
        price: Double,
        category: String,
        condition: String,
        sellerUserId: String,
        location: String,
        description: String = "",
        pickupSpotName: String = "Quad Library Ground Floor Lockers",
        pickupSecurityLevel: String = "CCTV Monitored Safe Zone",
        pickupLatitude: Double = -1.3090,
        pickupLongitude: Double = 36.8120,
        pickupTiming: String = "Immediate (within 1 hr)",
        payoutMethod: String = "Campus SafePay Escrow",
        requiresQrReleaseCode: Boolean = true,
        qrReleaseCode: String = "",
        imageUrl: String? = null,
        imageUrls: List<String> = emptyList(),
        sellerDisplayName: String? = null,
        itemId: String = "item_${java.util.UUID.randomUUID()}"
    ): MarketItem = withContext(ioDispatcher) {
        val resolvedSellerId = sellerUserId.trim()
        require(resolvedSellerId.isNotBlank()) {
            "sellerUserId is required — marketplace ownership cannot rely on display names"
        }
        val resolvedSellerName = sellerDisplayName?.takeIf(String::isRealProfileDisplayName)
            ?: profileNameResolver.resolve(listOf(resolvedSellerId))[resolvedSellerId.lowercase()]
            ?: throw IllegalStateException("A Supabase profile name is required to publish a marketplace listing")
        val now = System.currentTimeMillis()
        val combinedUrls = imageUrls
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOfNotNull(imageUrl?.trim()?.takeIf { it.isNotEmpty() }) }
        val serializedImageField = combinedUrls.joinToString("|").ifBlank { null }
        val generatedReleaseCode = qrReleaseCode.trim().ifBlank {
            "CP-${(100000..999999).random()}"
        }

        val syncId = java.util.UUID.randomUUID().toString()
        val pendingEntity = MarketItemEntity(
            id = itemId,
            title = title,
            price = price,
            currency = "KES",
            category = category,
            condition = condition,
            sellerName = resolvedSellerName,
            sellerUserId = resolvedSellerId,
            sellerRating = 5.0f,
            isCampusVerifiedSeller = true,
            campusHallLocation = location,
            imageUrl = serializedImageField,
            timestampMs = now,
            description = description,
            pickupSpotName = pickupSpotName,
            pickupSecurityLevel = pickupSecurityLevel,
            pickupLatitude = pickupLatitude,
            pickupLongitude = pickupLongitude,
            pickupTiming = pickupTiming,
            payoutMethod = payoutMethod,
            requiresQrReleaseCode = requiresQrReleaseCode,
            qrReleaseCode = generatedReleaseCode,
            syncStatus = com.campus.network.core.model.SyncStatus.PENDING.name,
            syncId = syncId
        )
        itemDao.insertItems(listOf(pendingEntity))

        val remoteResult = runCatching {
            supabase.from("campus_market_items").upsert(
                MarketItemRow(
                    id = itemId,
                    title = title,
                    price = price,
                    currency = "KES",
                    category = category,
                    condition = condition,
                    seller_name = resolvedSellerName,
                    seller_id = resolvedSellerId,
                    seller_rating = 5.0,
                    is_campus_verified_seller = true,
                    campus_hall_location = location,
                    image_url = serializedImageField,
                    timestamp_ms = now,
                    description = description,
                    pickup_spot_name = pickupSpotName,
                    pickup_security_level = pickupSecurityLevel,
                    pickup_latitude = pickupLatitude,
                    pickup_longitude = pickupLongitude,
                    pickup_timing = pickupTiming,
                    payout_method = payoutMethod,
                    requires_qr_release_code = requiresQrReleaseCode,
                    qr_release_code = generatedReleaseCode
                )
            )
        }

        val finalStatus = if (remoteResult.isSuccess) {
            com.campus.network.core.model.SyncStatus.SYNCED
        } else {
            com.campus.network.core.model.SyncStatus.FAILED
        }
        val finalEntity = pendingEntity.copy(syncStatus = finalStatus.name)
        itemDao.insertItems(listOf(finalEntity))
        return@withContext finalEntity.toDomain()
    }

    suspend fun postMicroGig(
        title: String,
        description: String,
        payoutAmount: Double,
        estimatedDuration: String,
        posterUserId: String,
        tag: String,
        urgency: GigUrgency,
        posterDisplayName: String? = null,
        gigId: String = "gig_${java.util.UUID.randomUUID()}"
    ): MicroGig = withContext(ioDispatcher) {
        val resolvedPosterId = posterUserId.trim()
        require(resolvedPosterId.isNotBlank()) {
            "posterUserId is required to publish a campus gig"
        }
        val resolvedPosterName = posterDisplayName?.takeIf(String::isRealProfileDisplayName)
            ?: profileNameResolver.resolve(listOf(resolvedPosterId))[resolvedPosterId.lowercase()]
            ?: throw IllegalStateException("A Supabase profile name is required to publish a campus gig")
        val now = System.currentTimeMillis()
        val syncId = java.util.UUID.randomUUID().toString()
        val entity = MicroGigEntity(
            id = gigId,
            title = title,
            description = description,
            payoutAmount = payoutAmount,
            currency = "KES",
            estimatedDuration = estimatedDuration,
            posterName = resolvedPosterName,
            posterUserId = resolvedPosterId,
            posterTrustScore = 95,
            tag = tag,
            urgencyName = urgency.name,
            spotsAvailable = 1,
            timestampMs = now,
            syncStatus = com.campus.network.core.model.SyncStatus.PENDING.name,
            syncId = syncId
        )
        gigDao.insertGigs(listOf(entity))

        val remoteResult = runCatching {
            supabase.from("campus_micro_gigs").upsert(
                MicroGigRow(
                    id = gigId,
                    title = title,
                    description = description,
                    payout_amount = payoutAmount,
                    currency = "KES",
                    estimated_duration = estimatedDuration,
                    poster_name = resolvedPosterName,
                    poster_id = resolvedPosterId,
                    poster_trust_score = 95,
                    tag = tag,
                    urgency_name = urgency.name,
                    spots_available = 1,
                    timestamp_ms = now
                )
            )
        }

        val finalStatus = if (remoteResult.isSuccess) {
            com.campus.network.core.model.SyncStatus.SYNCED
        } else {
            com.campus.network.core.model.SyncStatus.FAILED
        }
        val saved = entity.copy(syncStatus = finalStatus.name)
        gigDao.insertGigs(listOf(saved))
        return@withContext saved.toDomain()
    }

    suspend fun markFailedWritesPending() = withContext(ioDispatcher) {
        itemDao.getFailedItems().forEach {
            itemDao.updateSyncStatus(it.id, it.syncId, com.campus.network.core.model.SyncStatus.PENDING.name)
        }
        gigDao.getFailedGigs().forEach {
            gigDao.updateSyncStatus(it.id, it.syncId, com.campus.network.core.model.SyncStatus.PENDING.name)
        }
        // Trigger full retry of all failed syncs via DeltaSyncManager
        DeltaSyncManager.get(context).retryFailedSyncs()
    }

    /**
     * Syncs marketplace listings. Release codes are only retained for listings owned by
     * [currentUserId]; all other rows store an empty [MarketItemEntity.qrReleaseCode].
     */
    suspend fun syncRemoteMarketItems(currentUserId: String? = null, sinceMs: Long = 0L): Result<Long> = withContext(ioDispatcher) {
        val itemsResult = runCatching {
            supabase.from("campus_market_items").select {
                filter {
                    if (sinceMs > 0L) {
                        gt("timestamp_ms", sinceMs)
                    }
                }
                order("timestamp_ms", order = Order.ASCENDING)
                limit(100)
            }.decodeList<MarketItemRow>()
        }
        if (itemsResult.isFailure) {
            return@withContext Result.failure(itemsResult.exceptionOrNull() ?: Exception("Failed to fetch market items"))
        }
        val items = itemsResult.getOrThrow()

        var newWatermark = sinceMs
        val sellerNames = try {
            profileNameResolver.resolve(items.map { it.seller_id })
        } catch (error: Exception) {
            return@withContext Result.failure(error)
        }

        val ownerSecrets = mutableMapOf<String, String>()
        val viewerId = currentUserId?.trim().orEmpty()
        if (viewerId.isNotBlank()) {
            for (row in items) {
                if (row.seller_id.equals(viewerId, ignoreCase = true)) {
                    val code = runCatching {
                        supabase.postgrest.rpc(
                            "get_market_item_release_code",
                            GetReleaseCodeParams(row.id, viewerId)
                        ).decodeAs<String>()
                    }.getOrNull()?.trim()?.trim('"').orEmpty()
                    if (code.isNotBlank() && code != "null") {
                        ownerSecrets[row.id] = code
                    }
                }
            }
        }

        val entities = items.map { obj ->
            if (obj.timestamp_ms > newWatermark) newWatermark = obj.timestamp_ms
            val itemId = obj.id
            val sellerId = obj.seller_id
            val isOwner = viewerId.isNotBlank() &&
                sellerId.isNotBlank() &&
                viewerId.equals(sellerId, ignoreCase = true)
            MarketItemEntity(
                id = itemId,
                title = obj.title,
                price = obj.price,
                currency = obj.currency,
                category = obj.category,
                condition = obj.condition,
                sellerName = sellerNames[sellerId.lowercase()].orEmpty(),
                sellerUserId = sellerId,
                sellerRating = obj.seller_rating.toFloat(),
                isCampusVerifiedSeller = obj.is_campus_verified_seller,
                campusHallLocation = obj.campus_hall_location,
                imageUrl = obj.image_url?.ifBlank { null },
                timestampMs = if (obj.timestamp_ms > 0L) obj.timestamp_ms else System.currentTimeMillis(),
                description = obj.description,
                pickupSpotName = obj.pickup_spot_name,
                pickupSecurityLevel = obj.pickup_security_level,
                pickupLatitude = obj.pickup_latitude,
                pickupLongitude = obj.pickup_longitude,
                pickupTiming = obj.pickup_timing,
                payoutMethod = obj.payout_method,
                requiresQrReleaseCode = obj.requires_qr_release_code,
                qrReleaseCode = if (isOwner) ownerSecrets[itemId].orEmpty() else ""
            )
        }
        if (entities.isNotEmpty()) {
            itemDao.insertItems(entities)
        }
        Result.success(newWatermark)
    }

    suspend fun syncRemoteMicroGigs(sinceMs: Long = 0L): Result<Long> = withContext(ioDispatcher) {
        val gigsResult = runCatching {
            supabase.from("campus_micro_gigs").select {
                filter {
                    if (sinceMs > 0L) {
                        gt("timestamp_ms", sinceMs)
                    }
                }
                order("timestamp_ms", order = Order.ASCENDING)
                limit(100)
            }.decodeList<MicroGigRow>()
        }
        if (gigsResult.isFailure) {
            return@withContext Result.failure(gigsResult.exceptionOrNull() ?: Exception("Failed to fetch micro gigs"))
        }
        val gigs = gigsResult.getOrThrow()

        var newWatermark = sinceMs
        val posterNames = try {
            profileNameResolver.resolve(gigs.map { it.poster_id })
        } catch (error: Exception) {
            return@withContext Result.failure(error)
        }
        val entities = gigs.map { obj ->
            if (obj.timestamp_ms > newWatermark) newWatermark = obj.timestamp_ms
            MicroGigEntity(
                id = obj.id,
                title = obj.title,
                description = obj.description,
                payoutAmount = obj.payout_amount,
                currency = obj.currency,
                estimatedDuration = obj.estimated_duration,
                posterName = posterNames[obj.poster_id.lowercase()].orEmpty(),
                posterUserId = obj.poster_id,
                posterTrustScore = obj.poster_trust_score,
                tag = obj.tag,
                urgencyName = obj.urgency_name,
                spotsAvailable = obj.spots_available,
                timestampMs = if (obj.timestamp_ms > 0L) obj.timestamp_ms else System.currentTimeMillis(),
                bannerUrl = obj.banner_url?.ifBlank { null }
            )
        }
        if (entities.isNotEmpty()) {
            gigDao.insertGigs(entities)
        }
        Result.success(newWatermark)
    }

    /**
     * Queries OpenRouter AI for real-time search suggestions.
     */
    suspend fun getAiSearchSuggestions(query: String): List<String> {
        return aiClient.getSearchSuggestions(query)
    }

    /**
     * Verifies a buyer-scanned release code server-side so the secret never needs to live on buyer devices.
     */
    suspend fun verifyReleaseCode(itemId: String, scannedCode: String): Boolean = withContext(ioDispatcher) {
        val result = runCatching {
            val res = supabase.postgrest.rpc(
                "verify_market_release_code",
                VerifyReleaseCodeParams(itemId, scannedCode.trim())
            )
            res.decodeAs<Boolean>()
        }
        result.getOrDefault(false)
    }

    private fun MarketItemEntity.toDomain(): MarketItem {
        val parsedUrls = imageUrl
            ?.split("|")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        return MarketItem(
            id = id,
            title = title,
            price = price,
            currency = currency,
            category = category,
            condition = condition,
            sellerName = sellerName.takeIf(String::isRealProfileDisplayName).orEmpty(),
            sellerUserId = sellerUserId,
            sellerRating = sellerRating,
            isCampusVerifiedSeller = isCampusVerifiedSeller,
            campusHallLocation = campusHallLocation,
            imageUrl = parsedUrls.firstOrNull(),
            imageUrls = parsedUrls,
            timestampMs = timestampMs,
            description = description,
            pickupSpotName = pickupSpotName,
            pickupSecurityLevel = pickupSecurityLevel,
            pickupLatitude = pickupLatitude,
            pickupLongitude = pickupLongitude,
            pickupTiming = pickupTiming,
            payoutMethod = payoutMethod,
            requiresQrReleaseCode = requiresQrReleaseCode,
            qrReleaseCode = qrReleaseCode,
            syncStatus = syncStatus,
            syncId = syncId
        )
    }

    private fun MicroGigEntity.toDomain() = MicroGig(
        id = id,
        title = title,
        description = description,
        payoutAmount = payoutAmount,
        currency = currency,
        estimatedDuration = estimatedDuration,
        posterName = posterName.takeIf(String::isRealProfileDisplayName).orEmpty(),
        posterUserId = posterUserId,
        posterTrustScore = posterTrustScore,
        tag = tag,
        urgencyLevel = runCatching { GigUrgency.valueOf(urgencyName) }.getOrDefault(GigUrgency.NORMAL),
        spotsAvailable = spotsAvailable,
        timestampMs = timestampMs,
        bannerUrl = bannerUrl,
        syncStatus = syncStatus,
        syncId = syncId
    )
}
