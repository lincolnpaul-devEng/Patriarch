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
                    // Order persistence happens server-side after a real provider hold.
                    PaymentResult.Success(
                        LockEscrowSuccess(order = order, nextActionUrl = hold.nextActionUrl)
                    )
                }
            }
        }

    suspend fun releaseFundsToSeller(
        order: EscrowOrder,
        scannedCode: String
    ): PaymentResult<EscrowOrder> = withContext(ioDispatcher) {
        val holdId = order.providerHoldId?.trim().orEmpty()
        if (holdId.isBlank()) {
            return@withContext PaymentResult.Failure(
                PaymentErrorCode.INVALID_REQUEST,
                "Escrow order has no provider hold id"
            )
        }
        if (scannedCode.isBlank()) {
            return@withContext PaymentResult.Failure(
                PaymentErrorCode.RELEASE_CODE_REQUIRED,
                "Scan/verify the seller release QR before payout"
            )
        }
        // Payment release, code verification and order persistence all happen server-side
        // behind a JWT-guarded Edge Function; the client never holds payment secrets.
        val result = paymentProvider.releaseToSeller(
            EscrowReleaseRequest(
                orderId = order.id,
                providerHoldId = holdId,
                marketItemId = order.marketItemId,
                sellerUserId = order.sellerUserId,
                amount = order.amount,
                currencyCode = order.currencyCode,
                scannedCode = scannedCode
            )
        )
        when (result) {
            is PaymentResult.Failure -> result
            is PaymentResult.Success -> {
                val updated = order.copy(
                    status = EscrowOrderStatus.RELEASED_TO_SELLER,
                    releasedAtEpochMs = System.currentTimeMillis()
                )
                PaymentResult.Success(updated)
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
            val result = paymentProvider.refundBuyer(
                EscrowRefundRequest(
                    orderId = order.id,
                    providerHoldId = holdId,
                    buyerUserId = order.buyerUserId,
                    amount = order.amount,
                    currencyCode = order.currencyCode,
                    reason = reason
                )
            )
            if (result is PaymentResult.Failure) result else {
                val updated = order.copy(status = EscrowOrderStatus.REFUNDED_TO_BUYER)
                PaymentResult.Success(updated)
            }
        }
}
