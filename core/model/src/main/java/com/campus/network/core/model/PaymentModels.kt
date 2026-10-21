package com.campus.network.core.model

/**
 * Domain contracts for Campus SafeTrade escrow payments.
 *
 * The UI and Hub layer talk only to these types. Concrete free/paid payment APIs
 * (Paystack, Flutterwave, Stripe, M-Pesa Daraja, etc.) plug in behind [PaymentProvider]
 * in `:core:network` without changing checkout UI.
 */

enum class EscrowOrderStatus {
    PENDING_PAYMENT,
    FUNDS_HELD,
    RELEASED_TO_SELLER,
    REFUNDED_TO_BUYER,
    FAILED,
    CANCELLED
}

enum class PaymentMethodKind {
    MOBILE_MONEY_STK,
    CARD,
    WALLET,
    BANK_TRANSFER,
    OTHER
}

data class PaymentMethodOption(
    val id: String,
    val kind: PaymentMethodKind,
    val title: String,
    val subtitle: String,
    val requiresPhoneNumber: Boolean = false
)

data class EscrowHoldRequest(
    val orderId: String,
    val orderReference: String,
    val marketItemId: String,
    val itemTitle: String,
    val amount: Double,
    val currencyCode: String,
    val buyerUserId: String,
    val sellerUserId: String,
    val buyerPhoneE164: String? = null,
    val paymentMethodId: String,
    val rendezvousSpot: String,
    val buyerNote: String = "",
    val metadata: Map<String, String> = emptyMap()
)

data class EscrowReleaseRequest(
    val orderId: String,
    val providerHoldId: String,
    val marketItemId: String,
    val sellerUserId: String,
    val amount: Double,
    val currencyCode: String,
    val scannedCode: String
)

data class EscrowRefundRequest(
    val orderId: String,
    val providerHoldId: String,
    val buyerUserId: String,
    val amount: Double,
    val currencyCode: String,
    val reason: String
)

data class EscrowHold(
    val orderId: String,
    val providerHoldId: String,
    val providerId: String,
    val status: EscrowOrderStatus,
    val amount: Double,
    val currencyCode: String,
    /** Provider checkout URL / STK prompt reference when user action is still required. */
    val nextActionUrl: String? = null,
    val rawProviderReference: String? = null
)

data class EscrowSettlement(
    val orderId: String,
    val providerHoldId: String,
    val providerId: String,
    val status: EscrowOrderStatus,
    val amount: Double,
    val currencyCode: String,
    val settlementReference: String? = null
)

data class EscrowOrder(
    val id: String,
    val orderNumber: String,
    val marketItemId: String,
    val buyerUserId: String,
    val sellerUserId: String,
    val itemTitle: String,
    val amount: Double,
    val currencyCode: String,
    val paymentProvider: String,
    val paymentMethodId: String,
    val providerHoldId: String? = null,
    val status: EscrowOrderStatus = EscrowOrderStatus.PENDING_PAYMENT,
    val rendezvousSpot: String = "",
    val buyerNote: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val fundedAtEpochMs: Long? = null,
    val releasedAtEpochMs: Long? = null
)

sealed class PaymentResult<out T> {
    data class Success<T>(val value: T) : PaymentResult<T>()
    data class Failure(
        val code: PaymentErrorCode,
        val message: String,
        val retryable: Boolean = false,
        val cause: Throwable? = null
    ) : PaymentResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    fun getOrNull(): T? = (this as? Success)?.value
}

enum class PaymentErrorCode {
    PROVIDER_NOT_CONFIGURED,
    INVALID_REQUEST,
    AUTHENTICATION_FAILED,
    INSUFFICIENT_FUNDS,
    PROVIDER_REJECTED,
    NETWORK,
    PERSISTENCE_FAILED,
    RELEASE_CODE_REQUIRED,
    UNKNOWN
}
