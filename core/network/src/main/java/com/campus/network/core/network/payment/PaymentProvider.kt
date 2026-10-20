package com.campus.network.core.network.payment

import com.campus.network.core.model.EscrowHold
import com.campus.network.core.model.EscrowHoldRequest
import com.campus.network.core.model.EscrowRefundRequest
import com.campus.network.core.model.EscrowReleaseRequest
import com.campus.network.core.model.EscrowSettlement
import com.campus.network.core.model.PaymentMethodOption
import com.campus.network.core.model.PaymentResult

/**
 * Pluggable payment socket for Campus SafeTrade escrow.
 *
 * Production free/paid APIs plug in here. Checkout UI never talks to a vendor SDK directly —
 * swap providers by changing [PaymentProviderRegistry] / secrets, not Hub screens.
 *
 * Expected lifecycle:
 * 1. [createEscrowHold] — authorize/lock buyer funds (STK, card auth, wallet debit hold)
 * 2. [releaseToSeller] — after QR verify at meetup
 * 3. [refundBuyer] — cancel / dispute path
 */
interface PaymentProvider {
    /** Stable id used in DB rows, e.g. "http_escrow", "paystack", "flutterwave". */
    val providerId: String

    /** Human label shown in checkout. */
    val displayName: String

    /**
     * True only when API base URL + credentials are present.
     * When false, Hub must refuse to mark escrow as funded.
     */
    val isConfigured: Boolean

    fun availableMethods(): List<PaymentMethodOption>

    suspend fun createEscrowHold(request: EscrowHoldRequest): PaymentResult<EscrowHold>

    suspend fun releaseToSeller(request: EscrowReleaseRequest): PaymentResult<EscrowSettlement>

    suspend fun refundBuyer(request: EscrowRefundRequest): PaymentResult<EscrowSettlement>

    suspend fun getHoldStatus(providerHoldId: String): PaymentResult<EscrowHold>
}
