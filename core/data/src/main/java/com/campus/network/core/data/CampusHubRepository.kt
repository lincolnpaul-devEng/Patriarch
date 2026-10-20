package com.campus.network.core.data

import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.MarketItemEntity
import com.campus.network.core.database.entity.MicroGigEntity
import com.campus.network.core.model.GigUrgency
import com.campus.network.core.model.MarketItem
import com.campus.network.core.model.MicroGig
import com.campus.network.core.network.OpenRouterAiClient
import com.campus.network.core.network.SupabaseClientProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository for Tab 4: Hub Layer (Campus Micro-Commerce).
 * Powers the dual-view switcher hosting hyper-local item marketplace and real-time micro-gig board.
 */
class CampusHubRepository(
    private val database: CampusDatabase,
    private val supabaseClient: SupabaseClientProvider = SupabaseClientProvider(),
    private val aiClient: OpenRouterAiClient = OpenRouterAiClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val itemDao = database.marketItemDao()
    private val gigDao = database.microGigDao()

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
        sellerName: String,
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
        itemId: String = "item_${java.util.UUID.randomUUID()}"
    ): MarketItem = withContext(ioDispatcher) {
        val resolvedSellerId = sellerUserId.trim()
        require(resolvedSellerId.isNotBlank()) {
            "sellerUserId is required — marketplace ownership cannot rely on display names"
        }
        val now = System.currentTimeMillis()
        val combinedUrls = imageUrls
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOfNotNull(imageUrl?.trim()?.takeIf { it.isNotEmpty() }) }
        val serializedImageField = combinedUrls.joinToString("|").ifBlank { null }
        val generatedReleaseCode = qrReleaseCode.trim().ifBlank {
            "CP-${(100000..999999).random()}"
        }

        val entity = MarketItemEntity(
            id = itemId,
            title = title,
            price = price,
            currency = "KES",
            category = category,
            condition = condition,
            sellerName = sellerName,
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
            qrReleaseCode = generatedReleaseCode
        )

        val remotePayload = JSONObject().apply {
            put("id", itemId)
            put("title", title)
            put("price", price)
            put("currency", "KES")
            put("category", category)
            put("condition", condition)
            put("seller_name", sellerName)
            put("seller_id", resolvedSellerId)
            put("seller_rating", 5.0)
            put("is_campus_verified_seller", true)
            put("campus_hall_location", location)
            put("image_url", serializedImageField)
            put("timestamp_ms", now)
            put("description", description)
            put("pickup_spot_name", pickupSpotName)
            put("pickup_security_level", pickupSecurityLevel)
            put("pickup_latitude", pickupLatitude)
            put("pickup_longitude", pickupLongitude)
            put("pickup_timing", pickupTiming)
            put("payout_method", payoutMethod)
            put("requires_qr_release_code", requiresQrReleaseCode)
            put("qr_release_code", generatedReleaseCode)
        }.toString()

        val remoteResult = supabaseClient.upsertRecord("campus_market_items", remotePayload, onConflict = "id")
        if (remoteResult.isFailure) {
            throw remoteResult.exceptionOrNull() ?: Exception("Failed to publish market item to cloud")
        }

        itemDao.insertItems(listOf(entity))
        return@withContext entity.toDomain()
    }

    suspend fun postMicroGig(
        title: String,
        description: String,
        payoutAmount: Double,
        estimatedDuration: String,
        posterName: String,
        tag: String,
        urgency: GigUrgency,
        gigId: String = "gig_${java.util.UUID.randomUUID()}"
    ): MicroGig = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val entity = MicroGigEntity(
            id = gigId,
            title = title,
            description = description,
            payoutAmount = payoutAmount,
            currency = "KES",
            estimatedDuration = estimatedDuration,
            posterName = posterName,
            posterTrustScore = 95,
            tag = tag,
            urgencyName = urgency.name,
            spotsAvailable = 1,
            timestampMs = now
        )

        val remotePayload = JSONObject().apply {
            put("id", gigId)
            put("title", title)
            put("description", description)
            put("payout_amount", payoutAmount)
            put("currency", "KES")
            put("estimated_duration", estimatedDuration)
            put("poster_name", posterName)
            put("poster_trust_score", 95)
            put("tag", tag)
            put("urgency_name", urgency.name)
            put("spots_available", 1)
            put("timestamp_ms", now)
        }.toString()

        val remoteResult = supabaseClient.upsertRecord("campus_micro_gigs", remotePayload, onConflict = "id")
        if (remoteResult.isFailure) {
            throw remoteResult.exceptionOrNull() ?: Exception("Failed to publish micro gig to cloud")
        }

        gigDao.insertGigs(listOf(entity))
        return@withContext entity.toDomain()
    }

    /**
     * Syncs marketplace listings. Release codes are only retained for listings owned by
     * [currentUserId]; all other rows store an empty [MarketItemEntity.qrReleaseCode].
     */
    suspend fun syncRemoteMarketItems(currentUserId: String? = null): Result<Unit> = withContext(ioDispatcher) {
        // Explicit column list omits qr_release_code from the browse payload by default;
        // owner secrets are merged via a second owner-scoped select when currentUserId is set.
        val browseSelect = listOf(
            "id", "title", "price", "currency", "category", "condition",
            "seller_name", "seller_id", "seller_rating", "is_campus_verified_seller",
            "campus_hall_location", "image_url", "timestamp_ms", "description",
            "pickup_spot_name", "pickup_security_level", "pickup_latitude", "pickup_longitude",
            "pickup_timing", "payout_method", "requires_qr_release_code"
        ).joinToString(",")
        val queryResult = supabaseClient.queryTable(
            "campus_market_items?order=timestamp_ms.desc&limit=50",
            selectQuery = browseSelect
        )
        if (queryResult.isFailure) {
            return@withContext Result.failure(queryResult.exceptionOrNull() ?: Exception("Failed to fetch market items"))
        }
        val raw = queryResult.getOrNull() ?: "[]"
        val array = JSONArray(raw)

        val ownerSecrets = mutableMapOf<String, String>()
        val viewerId = currentUserId?.trim().orEmpty()
        if (viewerId.isNotBlank()) {
            val secretSelect = "id,qr_release_code"
            val ownedQuery = supabaseClient.queryTable(
                "campus_market_items?seller_id=eq.${java.net.URLEncoder.encode(viewerId, Charsets.UTF_8.name())}&limit=50",
                selectQuery = secretSelect
            )
            ownedQuery.getOrNull()?.let { ownedRaw ->
                val ownedArray = JSONArray(ownedRaw)
                for (i in 0 until ownedArray.length()) {
                    val row = ownedArray.getJSONObject(i)
                    val code = row.optString("qr_release_code", "")
                    if (code.isNotBlank()) {
                        ownerSecrets[row.getString("id")] = code
                    }
                }
            }
        }

        val entities = mutableListOf<MarketItemEntity>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val itemId = obj.getString("id")
            val sellerId = obj.optString("seller_id", "")
            val isOwner = viewerId.isNotBlank() &&
                sellerId.isNotBlank() &&
                viewerId.equals(sellerId, ignoreCase = true)
            entities.add(
                MarketItemEntity(
                    id = itemId,
                    title = obj.getString("title"),
                    price = obj.optDouble("price", 0.0),
                    currency = obj.optString("currency", "KES"),
                    category = obj.optString("category", "General"),
                    condition = obj.optString("condition", "Good"),
                    sellerName = obj.optString("seller_name", "Student Peer"),
                    sellerUserId = sellerId,
                    sellerRating = obj.optDouble("seller_rating", 5.0).toFloat(),
                    isCampusVerifiedSeller = obj.optBoolean("is_campus_verified_seller", true),
                    campusHallLocation = obj.optString("campus_hall_location", "Main Campus"),
                    imageUrl = obj.optString("image_url", "").ifBlank { null },
                    timestampMs = obj.optLong("timestamp_ms", System.currentTimeMillis()),
                    description = obj.optString("description", ""),
                    pickupSpotName = obj.optString("pickup_spot_name", "Quad Library Ground Floor Lockers"),
                    pickupSecurityLevel = obj.optString("pickup_security_level", "CCTV Monitored Safe Zone"),
                    pickupLatitude = obj.optDouble("pickup_latitude", -1.3090),
                    pickupLongitude = obj.optDouble("pickup_longitude", 36.8120),
                    pickupTiming = obj.optString("pickup_timing", "Immediate (within 1 hr)"),
                    payoutMethod = obj.optString("payout_method", "Campus SafePay Escrow"),
                    requiresQrReleaseCode = obj.optBoolean("requires_qr_release_code", true),
                    qrReleaseCode = if (isOwner) ownerSecrets[itemId].orEmpty() else ""
                )
            )
        }
        if (entities.isNotEmpty()) {
            itemDao.insertItems(entities)
        }
        Result.success(Unit)
    }

    suspend fun syncRemoteMicroGigs(): Result<Unit> = withContext(ioDispatcher) {
        val queryResult = supabaseClient.queryTable("campus_micro_gigs?order=timestamp_ms.desc&limit=50")
        if (queryResult.isFailure) {
            return@withContext Result.failure(queryResult.exceptionOrNull() ?: Exception("Failed to fetch micro gigs"))
        }
        val raw = queryResult.getOrNull() ?: "[]"
        val array = JSONArray(raw)
        val entities = mutableListOf<MicroGigEntity>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            entities.add(
                MicroGigEntity(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    description = obj.optString("description", ""),
                    payoutAmount = obj.optDouble("payout_amount", 0.0),
                    currency = obj.optString("currency", "KES"),
                    estimatedDuration = obj.optString("estimated_duration", "1 hour"),
                    posterName = obj.optString("poster_name", "Student Peer"),
                    posterTrustScore = obj.optInt("poster_trust_score", 95),
                    tag = obj.optString("tag", "General"),
                    urgencyName = obj.optString("urgency_name", "NORMAL"),
                    spotsAvailable = obj.optInt("spots_available", 1),
                    timestampMs = obj.optLong("timestamp_ms", System.currentTimeMillis()),
                    bannerUrl = obj.optString("banner_url", "").ifBlank { null }
                )
            )
        }
        if (entities.isNotEmpty()) {
            gigDao.insertGigs(entities)
        }
        Result.success(Unit)
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
        val payload = JSONObject().apply {
            put("p_item_id", itemId)
            put("p_scanned_code", scannedCode.trim())
        }.toString()
        val result = supabaseClient.callRpc("verify_market_release_code", payload)
        val body = result.getOrNull()?.trim().orEmpty()
        body.equals("true", ignoreCase = true) || body == "t"
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
            sellerName = sellerName,
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
            qrReleaseCode = qrReleaseCode
        )
    }

    private fun MicroGigEntity.toDomain() = MicroGig(
        id = id,
        title = title,
        description = description,
        payoutAmount = payoutAmount,
        currency = currency,
        estimatedDuration = estimatedDuration,
        posterName = posterName,
        posterTrustScore = posterTrustScore,
        tag = tag,
        urgencyLevel = runCatching { GigUrgency.valueOf(urgencyName) }.getOrDefault(GigUrgency.NORMAL),
        spotsAvailable = spotsAvailable,
        timestampMs = timestampMs,
        bannerUrl = bannerUrl
    )
}
