package com.campus.network.core.data

import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.MarketItemEntity
import com.campus.network.core.database.entity.MicroGigEntity
import com.campus.network.core.model.GigUrgency
import com.campus.network.core.model.MarketItem
import com.campus.network.core.model.MicroGig
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
        location: String
    ): MarketItem = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
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
            imageUrl = null,
            timestampMs = now
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

    private fun MarketItemEntity.toDomain() = MarketItem(
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
        imageUrl = imageUrl,
        timestampMs = timestampMs
    )

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
        timestampMs = timestampMs
    )
}
