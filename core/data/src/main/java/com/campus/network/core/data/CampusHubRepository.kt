package com.campus.network.core.data

import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.MarketItemEntity
import com.campus.network.core.database.entity.MicroGigEntity
import com.campus.network.core.model.GigUrgency
import com.campus.network.core.model.MarketItem
import com.campus.network.core.model.MicroGig
import com.campus.network.core.network.OpenRouterAiClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for Tab 4: Hub Layer (Campus Micro-Commerce).
 * Powers the dual-view switcher hosting hyper-local item marketplace and real-time micro-gig board.
 */
class CampusHubRepository(
    private val database: CampusDatabase,
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
        location: String,
        description: String = "",
        pickupSpotName: String = "Quad Library Ground Floor Lockers",
        pickupSecurityLevel: String = "CCTV Monitored Safe Zone",
        pickupLatitude: Double = -1.3090,
        pickupLongitude: Double = 36.8120,
        pickupTiming: String = "Immediate (within 1 hr)",
        payoutMethod: String = "Campus SafePay Escrow",
        requiresQrReleaseCode: Boolean = true,
        qrReleaseCode: String = "CP-849201",
        imageUrl: String? = null,
        imageUrls: List<String> = emptyList()
    ): MarketItem = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val combinedUrls = imageUrls
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOfNotNull(imageUrl?.trim()?.takeIf { it.isNotEmpty() }) }
        val serializedImageField = combinedUrls.joinToString("|").ifBlank { null }

        val entity = MarketItemEntity(
            id = "item-$now",
            title = title,
            price = price,
            currency = "KES",
            category = category,
            condition = condition,
            sellerName = sellerName,
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
            qrReleaseCode = qrReleaseCode
        )
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
        urgency: GigUrgency
    ): MicroGig = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val entity = MicroGigEntity(
            id = "gig-$now",
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
        gigDao.insertGigs(listOf(entity))
        return@withContext entity.toDomain()
    }

    /**
     * Queries OpenRouter AI for real-time search suggestions.
     */
    suspend fun getAiSearchSuggestions(query: String): List<String> {
        return aiClient.getSearchSuggestions(query)
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
