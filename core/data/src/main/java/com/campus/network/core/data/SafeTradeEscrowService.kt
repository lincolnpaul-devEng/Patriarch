package com.campus.network.core.data

import com.campus.network.core.model.EscrowHoldRequest
import com.campus.network.core.model.EscrowOrder
import com.campus.network.core.model.EscrowOrderStatus
import com.campus.network.core.model.EscrowRefundRequest
import com.campus.network.core.model.EscrowReleaseRequest
import com.campus.network.core.model.MarketItem
import com.campus.network.core.model.PaymentErrorCode
import com.campus.network.core.model.PaymentMethodOption
import com.campus.network.core.model.PaymentResult
import com.campus.network.core.network.SupabaseClientProvider
import com.campus.network.core.network.payment.PaymentProvider
import com.campus.network.core.network.payment.PaymentProviderRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

/**
 * Orchestrates SafeTrade escrow: payment socket → provider hold → Supabase order row.
 *
 * Never marks an order FUNDS_HELD unless [PaymentProvider.createEscrowHold] succeeds.
 */
class SafeTradeEscrowService(
    private val supabaseClient: SupabaseClientProvider = SupabaseClientProvider(),
    private val paymentProvider: PaymentProvider = PaymentProviderRegistry.resolve(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    fun isPaymentReady(): Boolean = paymentProvider.isConfigured

    fun providerDisplayName(): String = paymentProvider.displayName

    fun availablePaymentMethods(): List<PaymentMethodOption> = paymentProvider.availableMethods()

    data class LockEscrowParams(
        val item: MarketItem,
        val buyerUserId: String,
        val amount: Double,
        val currencyCode: String,
        val orderReference: String,
        val paymentMethodId: String,
        val buyerPhoneE164: String? = null,
        val handoverTitle: String = "",
        val packageTitle: String = "",
        val buyerNote: String = "",
        val quantity: Int = 1
    )

    data class LockEscrowSuccess(
        val order: EscrowOrder,
        val nextActionUrl: String?
    )

    suspend fun lockFundsInEscrow(params: LockEscrowParams): PaymentResult<LockEscrowSuccess> =
        withContext(ioDispatcher) {
            if (params.buyerUserId.isBlank()) {
                return@withContext PaymentResult.Failure(
                    PaymentErrorCode.INVALID_REQUEST,
                    "Buyer identity is required before locking escrow"
                )
            }
            val sellerId = params.item.sellerUserId.trim()
            if (sellerId.isBlank()) {
                return@withContext PaymentResult.Failure(
                    PaymentErrorCode.INVALID_REQUEST,
                    "Listing has no sellerUserId — cannot open escrow"
                )
            }
            if (sellerId.equals(params.buyerUserId, ignoreCase = true)) {
                return@withContext PaymentResult.Failure(
                    PaymentErrorCode.INVALID_REQUEST,
                    "You cannot escrow your own listing"
                )
            }
            if (!paymentProvider.isConfigured) {
                return@withContext PaymentResult.Failure(
                    PaymentErrorCode.PROVIDER_NOT_CONFIGURED,
                    PaymentProviderRegistry.configurationHint()
                )
            }

            val orderId = "escrow_${UUID.randomUUID()}"
            val holdRequest = EscrowHoldRequest(
                orderId = orderId,
                orderReference = params.orderReference,
                marketItemId = params.item.id,
                itemTitle = params.item.title,
                amount = params.amount,
                currencyCode = params.currencyCode,
                buyerUserId = params.buyerUserId,
                sellerUserId = sellerId,
                buyerPhoneE164 = params.buyerPhoneE164,
                paymentMethodId = params.paymentMethodId,
                rendezvousSpot = params.item.pickupSpotName,
                buyerNote = params.buyerNote,
                metadata = mapOf(
                    "handover" to params.handoverTitle,
                    "package" to params.packageTitle,
                    "quantity" to params.quantity.toString()
                )
            )

            when (val holdResult = paymentProvider.createEscrowHold(holdRequest)) {
                is PaymentResult.Failure -> return@withContext holdResult
                is PaymentResult.Success -> {
                    val hold = holdResult.value
                    val funded = hold.status == EscrowOrderStatus.FUNDS_HELD ||
                        hold.status == EscrowOrderStatus.PENDING_PAYMENT
                    if (!funded && hold.providerHoldId.isBlank()) {
                        return@withContext PaymentResult.Failure(
                            PaymentErrorCode.PROVIDER_REJECTED,
                            "Payment provider did not return a hold id"
                        )
                    }
                    val order = EscrowOrder(
                        id = orderId,
                        orderNumber = params.orderReference,
                        marketItemId = params.item.id,
                        buyerUserId = params.buyerUserId,
                        sellerUserId = sellerId,
                        itemTitle = params.item.title,
                        amount = params.amount,
                        currencyCode = params.currencyCode,
                        paymentProvider = paymentProvider.providerId,
                        paymentMethodId = params.paymentMethodId,
                        providerHoldId = hold.providerHoldId,
                        status = hold.status,
                        rendezvousSpot = params.item.pickupSpotName,
                        buyerNote = params.buyerNote,
                        fundedAtEpochMs = if (hold.status == EscrowOrderStatus.FUNDS_HELD) {
                            System.currentTimeMillis()
                        } else {
                            null
                        }
                    )
                    val persist = persistOrder(order)
                    if (persist is PaymentResult.Failure) {
                        return@withContext persist
                    }
                    PaymentResult.Success(
                        LockEscrowSuccess(order = order, nextActionUrl = hold.nextActionUrl)
                    )
                }
            }
        }

    suspend fun releaseFundsToSeller(
        order: EscrowOrder,
        releaseCodeVerified: Boolean
    ): PaymentResult<EscrowOrder> = withContext(ioDispatcher) {
        val holdId = order.providerHoldId?.trim().orEmpty()
        if (holdId.isBlank()) {
            return@withContext PaymentResult.Failure(
                PaymentErrorCode.INVALID_REQUEST,
                "Escrow order has no provider hold id"
            )
        }
        if (!releaseCodeVerified) {
            return@withContext PaymentResult.Failure(
                PaymentErrorCode.RELEASE_CODE_REQUIRED,
                "Scan/verify the seller release QR before payout"
            )
        }
        when (
            val release = paymentProvider.releaseToSeller(
                EscrowReleaseRequest(
                    orderId = order.id,
                    providerHoldId = holdId,
                    marketItemId = order.marketItemId,
                    sellerUserId = order.sellerUserId,
                    amount = order.amount,
                    currencyCode = order.currencyCode,
                    releaseCodeVerified = true
                )
            )
        ) {
            is PaymentResult.Failure -> release
            is PaymentResult.Success -> {
                val updated = order.copy(
                    status = EscrowOrderStatus.RELEASED_TO_SELLER,
                    releasedAtEpochMs = System.currentTimeMillis()
                )
                val persist = persistOrder(updated)
                if (persist is PaymentResult.Failure) persist else PaymentResult.Success(updated)
            }
        }
    }

    suspend fun refundBuyer(order: EscrowOrder, reason: String): PaymentResult<EscrowOrder> =
        withContext(ioDispatcher) {
            val holdId = order.providerHoldId?.trim().orEmpty()
            if (holdId.isBlank()) {
                return@withContext PaymentResult.Failure(
                    PaymentErrorCode.INVALID_REQUEST,
                    "Escrow order has no provider hold id"
                )
            }
            when (
                val refund = paymentProvider.refundBuyer(
                    EscrowRefundRequest(
                        orderId = order.id,
                        providerHoldId = holdId,
                        buyerUserId = order.buyerUserId,
                        amount = order.amount,
                        currencyCode = order.currencyCode,
                        reason = reason
                    )
                )
            ) {
                is PaymentResult.Failure -> refund
                is PaymentResult.Success -> {
                    val updated = order.copy(status = EscrowOrderStatus.REFUNDED_TO_BUYER)
                    val persist = persistOrder(updated)
                    if (persist is PaymentResult.Failure) persist else PaymentResult.Success(updated)
                }
            }
        }

    private suspend fun persistOrder(order: EscrowOrder): PaymentResult<Unit> {
        val payload = JSONObject().apply {
            put("id", order.id)
            put("order_number", order.orderNumber)
            put("market_item_id", order.marketItemId)
            put("buyer_user_id", order.buyerUserId)
            put("seller_user_id", order.sellerUserId)
            put("item_title", order.itemTitle)
            put("amount", order.amount)
            put("currency_code", order.currencyCode)
            put("payment_provider", order.paymentProvider)
            put("payment_method", order.paymentMethodId)
            put("provider_hold_id", order.providerHoldId)
            put("status", order.status.name)
            put("rendezvous_spot", order.rendezvousSpot)
            put("buyer_note", order.buyerNote)
            put("created_at_epoch_ms", order.createdAtEpochMs)
            order.fundedAtEpochMs?.let { put("funded_at_epoch_ms", it) }
            order.releasedAtEpochMs?.let { put("released_at_epoch_ms", it) }
        }.toString()

        val result = supabaseClient.upsertRecord(
            tableName = "campus_escrow_orders",
            jsonPayload = payload,
            onConflict = "id"
        )
        return if (result.isSuccess) {
            PaymentResult.Success(Unit)
        } else {
            PaymentResult.Failure(
                code = PaymentErrorCode.PERSISTENCE_FAILED,
                message = result.exceptionOrNull()?.message
                    ?: "Failed to persist escrow order",
                retryable = true,
                cause = result.exceptionOrNull()
            )
        }
    }
}
