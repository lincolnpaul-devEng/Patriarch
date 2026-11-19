package com.campus.network.core.data

import android.content.Context
import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.MarketItemEntity
import com.campus.network.core.database.entity.MicroGigEntity
import com.campus.network.core.model.GigUrgency
import com.campus.network.core.model.MarketItem
import com.campus.network.core.model.MicroGig
import com.campus.network.core.model.cleanDisplayName
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
    val category: String = "",
    val condition: String = "",
    val seller_name: String = "",
    val seller_id: String = "",
    val seller_rating: Double = 0.0,
    val is_campus_verified_seller: Boolean = false,
    val campus_hall_location: String = "",
    val image_url: String? = null,
    val timestamp_ms: Long = 0L,
    val description: String = "",
    val pickup_spot_name: String = "",
    val pickup_security_level: String = "",
    val pickup_latitude: Double = 0.0,
    val pickup_longitude: Double = 0.0,
    val pickup_timing: String = "",
    val payout_method: String = "",
    val requires_qr_release_code: Boolean = true,
    val qr_release_code: String? = null,
    val status: String = "ACTIVE"
)

@Serializable
data class EscrowOrderRow(
    val id: String = "",
    val order_number: String = "",
    val market_item_id: String = "",
    val buyer_user_id: String = "",
    val seller_user_id: String = "",
    val item_title: String = "",
    val amount: Double = 0.0,
    val currency_code: String = "KES",
    val payment_provider: String = "",
    val payment_method: String = "",
    val provider_hold_id: String = "",
    val status: String = "",
    val rendezvous_spot: String = "",
    val buyer_note: String = "",
    val created_at_epoch_ms: Long = 0L,
    val funded_at_epoch_ms: Long? = null,
    val released_at_epoch_ms: Long? = null
)

@Serializable
data class MicroGigRow(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val payout_amount: Double = 0.0,
    val currency: String = "KES",
    val estimated_duration: String = "",
    val poster_name: String = "",
    val poster_id: String = "",
    val poster_trust_score: Int = 0,
    val tag: String = "",
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
        pickupSpotName: String,
        pickupSecurityLevel: String,
        pickupLatitude: Double,
        pickupLongitude: Double,
        pickupTiming: String,
        payoutMethod: String,
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
            sellerRating = 0f,
            isCampusVerifiedSeller = false,
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
            status = "ACTIVE",
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
                    seller_rating = 0.0,
                    is_campus_verified_seller = false,
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
                    qr_release_code = generatedReleaseCode,
                    status = "ACTIVE"
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

    /** Listings owned by [sellerId] for the My Selling screen (local-first, live rows). */
    suspend fun getSellerListings(sellerId: String): List<MarketItem> = withContext(ioDispatcher) {
        val resolvedId = sellerId.trim()
        if (resolvedId.isBlank()) return@withContext emptyList()
        val local = itemDao.getItemsBySeller(resolvedId).map { it.toDomain() }
        runCatching {
            supabase.from("campus_market_items")
                .select { filter { eq("seller_id", resolvedId) } }
                .decodeList<MarketItemRow>()
        }.getOrDefault(emptyList()).map { obj ->
            MarketItemEntity(
                id = obj.id,
                title = obj.title,
                description = obj.description,
                price = obj.price,
                currency = obj.currency,
                category = obj.category,
                condition = obj.condition,
                sellerName = obj.seller_name,
                sellerUserId = obj.seller_id,
                sellerRating = obj.seller_rating.toFloat(),
                isCampusVerifiedSeller = obj.is_campus_verified_seller,
                campusHallLocation = obj.pickup_spot_name,
                imageUrl = obj.image_url,
                timestampMs = obj.timestamp_ms,
                pickupSpotName = obj.pickup_spot_name,
                pickupSecurityLevel = obj.pickup_security_level,
                pickupLatitude = obj.pickup_latitude,
                pickupLongitude = obj.pickup_longitude,
                pickupTiming = obj.pickup_timing,
                payoutMethod = obj.payout_method,
                requiresQrReleaseCode = obj.requires_qr_release_code,
                status = obj.status,
                qrReleaseCode = obj.qr_release_code.orEmpty(),
                syncStatus = com.campus.network.core.model.SyncStatus.SYNCED.name,
                syncId = null
            )
        }.let { remoteEntities ->
            if (remoteEntities.isNotEmpty()) {
                itemDao.insertItems(remoteEntities)
            }
            // Fresh remote truth, ordered newest first, filtered to this seller.
            remoteEntities.sortedByDescending { it.timestampMs }.map { it.toDomain() }.ifEmpty { local }
        }
    }

    /** Deletes a listing locally + remotely. RLS blocks non-owners on the remote write. */
    suspend fun deleteMarketItem(itemId: String): Result<Unit> = withContext(ioDispatcher) {
        itemDao.deleteItemById(itemId)
        val remote = runCatching {
            supabase.from("campus_market_items").delete { filter { eq("id", itemId) } }
        }
        if (remote.isSuccess) Result.success(Unit)
        else Result.failure(remote.exceptionOrNull() ?: Exception("Unable to delete listing"))
    }

    /** Flips a listing to SOLD (local + remote). Server-side buyers also flip it on release. */
    suspend fun markMarketItemSold(itemId: String): Result<Unit> = withContext(ioDispatcher) {
        itemDao.updateItemStatus(itemId, "SOLD")
        val remote = runCatching {
            supabase.from("campus_market_items").update(
                {
                    set("status", "SOLD")
                }
            ) { filter { eq("id", itemId) } }
        }
        if (remote.isSuccess) Result.success(Unit)
        else Result.failure(remote.exceptionOrNull() ?: Exception("Unable to mark listing sold"))
    }

    suspend fun fetchEscrowOrdersAsSeller(sellerId: String): List<com.campus.network.core.model.EscrowOrder> =
        withContext(ioDispatcher) {
            val resolvedId = sellerId.trim()
            if (resolvedId.isBlank()) return@withContext emptyList()
            runCatching {
                supabase.from("campus_escrow_orders")
                    .select {
                        filter { eq("seller_user_id", resolvedId) }
                        order("created_at_epoch_ms", order = Order.DESCENDING)
                    }
                    .decodeList<EscrowOrderRow>()
            }.getOrDefault(emptyList()).map { row ->
                com.campus.network.core.model.EscrowOrder(
                    id = row.id,
                    orderNumber = row.order_number,
                    marketItemId = row.market_item_id,
                    buyerUserId = row.buyer_user_id,
                    sellerUserId = row.seller_user_id,
                    itemTitle = row.item_title,
                    amount = row.amount,
                    currencyCode = row.currency_code,
                    paymentProvider = row.payment_provider,
                    paymentMethodId = row.payment_method,
                    providerHoldId = row.provider_hold_id,
                    status = runCatching {
                        com.campus.network.core.model.EscrowOrderStatus.valueOf(row.status)
                    }.getOrDefault(com.campus.network.core.model.EscrowOrderStatus.PENDING_PAYMENT),
                    rendezvousSpot = row.rendezvous_spot,
                    buyerNote = row.buyer_note,
                    createdAtEpochMs = row.created_at_epoch_ms,
                    fundedAtEpochMs = row.funded_at_epoch_ms,
                    releasedAtEpochMs = row.released_at_epoch_ms
                )
            }
        }

    /** Fetches the escrow order attached to a listing for the signed-in buyer (RLS participant read). */
    suspend fun fetchEscrowOrderForItem(itemId: String): com.campus.network.core.model.EscrowOrder? =
        withContext(ioDispatcher) {
            val resolvedId = itemId.trim()
            if (resolvedId.isBlank()) return@withContext null
            runCatching {
                supabase.from("campus_escrow_orders")
                    .select {
                        filter { eq("market_item_id", resolvedId) }
                        order("created_at_epoch_ms", order = Order.DESCENDING)
                        limit(1)
                    }
                    .decodeList<EscrowOrderRow>()
            }.getOrDefault(emptyList()).firstOrNull()?.let { row ->
                com.campus.network.core.model.EscrowOrder(
                    id = row.id,
                    orderNumber = row.order_number,
                    marketItemId = row.market_item_id,
                    buyerUserId = row.buyer_user_id,
                    sellerUserId = row.seller_user_id,
                    itemTitle = row.item_title,
                    amount = row.amount,
                    currencyCode = row.currency_code,
                    paymentProvider = row.payment_provider,
                    paymentMethodId = row.payment_method,
                    providerHoldId = row.provider_hold_id,
                    status = runCatching {
                        com.campus.network.core.model.EscrowOrderStatus.valueOf(row.status)
                    }.getOrDefault(com.campus.network.core.model.EscrowOrderStatus.PENDING_PAYMENT),
                    rendezvousSpot = row.rendezvous_spot,
                    buyerNote = row.buyer_note,
                    createdAtEpochMs = row.created_at_epoch_ms,
                    fundedAtEpochMs = row.funded_at_epoch_ms,
                    releasedAtEpochMs = row.released_at_epoch_ms
                )
            }
        }

    /** All Safe Trade orders where [userId] is buyer or seller (activity ledger / wallet history). */
    suspend fun fetchEscrowOrdersForUser(userId: String): List<com.campus.network.core.model.EscrowOrder> =
        withContext(ioDispatcher) {
            val resolvedId = userId.trim()
            if (resolvedId.isBlank()) return@withContext emptyList()
            runCatching {
                supabase.from("campus_escrow_orders")
                    .select {
                        filter { eq("buyer_user_id", resolvedId) }
                        order("created_at_epoch_ms", order = Order.DESCENDING)
                    }
                    .decodeList<EscrowOrderRow>()
            }.getOrDefault(emptyList()) + runCatching {
                supabase.from("campus_escrow_orders")
                    .select {
                        filter { eq("seller_user_id", resolvedId) }
                        order("created_at_epoch_ms", order = Order.DESCENDING)
                    }
                    .decodeList<EscrowOrderRow>()
            }.getOrDefault(emptyList())
        }.distinctBy { it.id }.map { row ->
            com.campus.network.core.model.EscrowOrder(
                id = row.id,
                orderNumber = row.order_number,
                marketItemId = row.market_item_id,
                buyerUserId = row.buyer_user_id,
                sellerUserId = row.seller_user_id,
                itemTitle = row.item_title,
                amount = row.amount,
                currencyCode = row.currency_code,
                paymentProvider = row.payment_provider,
                paymentMethodId = row.payment_method,
                providerHoldId = row.provider_hold_id,
                status = runCatching {
                    com.campus.network.core.model.EscrowOrderStatus.valueOf(row.status)
                }.getOrDefault(com.campus.network.core.model.EscrowOrderStatus.PENDING_PAYMENT),
                rendezvousSpot = row.rendezvous_spot,
                buyerNote = row.buyer_note,
                createdAtEpochMs = row.created_at_epoch_ms,
                fundedAtEpochMs = row.funded_at_epoch_ms,
                releasedAtEpochMs = row.released_at_epoch_ms
            )
        }.sortedByDescending { it.createdAtEpochMs }

    suspend fun postMicroGig(
        title: String,
        description: String,
        payoutAmount: Double,
        estimatedDuration: String,
        posterUserId: String,
        tag: String,
        urgency: GigUrgency,
        spotsAvailable: Int = 1,
        bannerUrl: String? = null,
        posterTrustScore: Int = 0,
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
            posterTrustScore = posterTrustScore,
            tag = tag,
            urgencyName = urgency.name,
            spotsAvailable = spotsAvailable,
            bannerUrl = bannerUrl,
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
                    poster_trust_score = posterTrustScore,
                    tag = tag,
                    urgency_name = urgency.name,
                    spots_available = spotsAvailable,
                    banner_url = bannerUrl,
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
            val cleanObjName = cleanDisplayName(obj.seller_name)
            val resolvedSellerName = sellerNames[sellerId.lowercase()]?.takeIf { it.isNotBlank() }
                ?: cleanObjName.takeIf { it.isNotBlank() }
                ?: "Campus Peer"
            MarketItemEntity(
                id = itemId,
                title = obj.title,
                price = obj.price,
                currency = obj.currency,
                category = obj.category,
                condition = obj.condition,
                sellerName = resolvedSellerName,
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
                qrReleaseCode = if (isOwner) ownerSecrets[itemId].orEmpty() else "",
                status = obj.status.ifBlank { "ACTIVE" }
            )
        }
        if (entities.isNotEmpty()) {
            itemDao.insertItems(entities)
        }
        if (sinceMs == 0L) {
            val remoteIds = entities.map { it.id }
            if (remoteIds.isNotEmpty()) {
                itemDao.deleteGhostItems(remoteIds)
            } else {
                itemDao.clearSyncedItems()
            }
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
        if (sinceMs == 0L) {
            val remoteIds = entities.map { it.id }
            if (remoteIds.isNotEmpty()) {
                gigDao.deleteGhostGigs(remoteIds)
            } else {
                gigDao.clearSyncedGigs()
            }
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
    /**
     * Removed client-side release verification: `verify_market_release_code` is revoked from the
     * app and release codes are only checked server-side inside the `mpesa` Edge Function's
     * release action (bounded attempts, buyer-only). Kept for source-compat as always-false.
     */
    suspend fun verifyReleaseCode(itemId: String, scannedCode: String): Boolean {
        itemId; scannedCode
        return false
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
            status = status,
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
