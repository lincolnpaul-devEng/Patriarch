package com.campus.network.core.network.payment

import com.campus.network.core.model.EscrowHold
import com.campus.network.core.model.EscrowHoldRequest
import com.campus.network.core.model.EscrowRefundRequest
import com.campus.network.core.model.EscrowReleaseRequest
import com.campus.network.core.model.EscrowSettlement
import com.campus.network.core.model.PaymentErrorCode
import com.campus.network.core.model.PaymentMethodKind
import com.campus.network.core.model.PaymentMethodOption
import com.campus.network.core.model.PaymentResult

/**
 * Default socket occupant when no payment API credentials are configured.
 * Never fakes a successful fund lock — callers surface [PaymentErrorCode.PROVIDER_NOT_CONFIGURED].
 */
class UnconfiguredPaymentProvider : PaymentProvider {
    override val providerId: String = "unconfigured"
    override val displayName: String = "Payment provider not connected"
    override val isConfigured: Boolean = false

    override fun availableMethods(): List<PaymentMethodOption> = listOf(
        PaymentMethodOption(
            id = "pending_provider",
            kind = PaymentMethodKind.OTHER,
            title = "Connect a payment API",
            subtitle = "Add PAYMENT_API_BASE_URL + keys in secrets.properties to enable SafeTrade escrow"
        )
    )

    override suspend fun createEscrowHold(request: EscrowHoldRequest): PaymentResult<EscrowHold> =
        notConfigured()

    override suspend fun releaseToSeller(request: EscrowReleaseRequest): PaymentResult<EscrowSettlement> =
        notConfigured()

    override suspend fun refundBuyer(request: EscrowRefundRequest): PaymentResult<EscrowSettlement> =
        notConfigured()

    override suspend fun getHoldStatus(providerHoldId: String): PaymentResult<EscrowHold> =
        notConfigured()

    private fun notConfigured(): PaymentResult.Failure = PaymentResult.Failure(
        code = PaymentErrorCode.PROVIDER_NOT_CONFIGURED,
        message = "No payment API is configured. Set PAYMENT_API_BASE_URL, PAYMENT_API_KEY, " +
            "and PAYMENT_PROVIDER_ID in secrets.properties, then rebuild. " +
            "HttpEscrowPaymentProvider will call your provider's escrow endpoints.",
        retryable = false
    )
}
