package com.campus.network.core.network.payment

import com.campus.network.core.model.EscrowHold
import com.campus.network.core.model.EscrowHoldRequest
import com.campus.network.core.model.EscrowOrderStatus
import com.campus.network.core.model.EscrowRefundRequest
import com.campus.network.core.model.EscrowReleaseRequest
import com.campus.network.core.model.EscrowSettlement
import com.campus.network.core.model.PaymentErrorCode
import com.campus.network.core.model.PaymentMethodKind
import com.campus.network.core.model.PaymentMethodOption
import com.campus.network.core.model.PaymentResult
import com.campus.network.core.network.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Production-ready HTTP socket for any escrow-capable payment API.
 *
 * Configure in `secrets.properties` (then rebuild):
 * ```
 * PAYMENT_API_BASE_URL=https://your-provider.example/v1
 * PAYMENT_API_KEY=pk_live_...
 * PAYMENT_API_SECRET=sk_live_...
 * PAYMENT_PROVIDER_ID=paystack   # or flutterwave | mpesa_daraja | stripe | custom
 * PAYMENT_PROVIDER_DISPLAY_NAME=Paystack Escrow
 * ```
 *
 * Contract your backend (or a thin adapter Edge Function) must expose:
 *
 * | Method | Path | Body highlights | Success JSON |
 * |--------|------|-----------------|--------------|
 * | POST | `/escrow/holds` | orderId, amount, currency, buyerUserId, sellerUserId, paymentMethodId, phone? | `{ "holdId", "status":"FUNDS_HELD"\|"PENDING_PAYMENT", "nextActionUrl"? }` |
 * | POST | `/escrow/holds/{holdId}/release` | orderId, sellerUserId, amount, releaseCodeVerified | `{ "settlementId"?, "status":"RELEASED_TO_SELLER" }` |
 * | POST | `/escrow/holds/{holdId}/refund` | orderId, buyerUserId, amount, reason | `{ "settlementId"?, "status":"REFUNDED_TO_BUYER" }` |
 * | GET  | `/escrow/holds/{holdId}` | — | `{ "holdId", "status", "amount", "currency" }` |
 *
 * Auth header: `Authorization: Bearer {PAYMENT_API_SECRET}` and `X-Api-Key: {PAYMENT_API_KEY}`.
 *
 * When you obtain a free production API, point these env values at your adapter —
 * Hub checkout already calls this provider; no UI rewrite required.
 */
class HttpEscrowPaymentProvider(
    private val baseUrl: String = BuildConfig.PAYMENT_API_BASE_URL.trim().trimEnd('/'),
    private val apiKey: String = BuildConfig.PAYMENT_API_KEY.trim(),
    private val apiSecret: String = BuildConfig.PAYMENT_API_SECRET.trim(),
    private val configuredProviderId: String = BuildConfig.PAYMENT_PROVIDER_ID.trim()
        .ifBlank { "http_escrow" },
    private val configuredDisplayName: String = BuildConfig.PAYMENT_PROVIDER_DISPLAY_NAME.trim()
        .ifBlank { "Campus SafePay" },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PaymentProvider {

    private val client = HttpClient(OkHttp)

    override val providerId: String = configuredProviderId
    override val displayName: String = configuredDisplayName
    override val isConfigured: Boolean =
        baseUrl.isNotBlank() && (apiKey.isNotBlank() || apiSecret.isNotBlank())

    override fun availableMethods(): List<PaymentMethodOption> = listOf(
        PaymentMethodOption(
            id = "mobile_money_stk",
            kind = PaymentMethodKind.MOBILE_MONEY_STK,
            title = "Mobile Money (STK Push)",
            subtitle = "Lock funds via your connected payment API",
            requiresPhoneNumber = true
        ),
        PaymentMethodOption(
            id = "card",
            kind = PaymentMethodKind.CARD,
            title = "Card",
            subtitle = "Authorize card hold through the payment API"
        ),
        PaymentMethodOption(
            id = "wallet",
            kind = PaymentMethodKind.WALLET,
            title = "Campus Wallet",
            subtitle = "Debit held balance when the provider supports wallets"
        )
    )

    override suspend fun createEscrowHold(request: EscrowHoldRequest): PaymentResult<EscrowHold> {
        if (!isConfigured) {
            return PaymentResult.Failure(
                PaymentErrorCode.PROVIDER_NOT_CONFIGURED,
                "PAYMENT_API_BASE_URL / keys are missing"
            )
        }
        val body = JSONObject().apply {
            put("orderId", request.orderId)
            put("orderReference", request.orderReference)
            put("marketItemId", request.marketItemId)
            put("itemTitle", request.itemTitle)
            put("amount", request.amount)
            put("currency", request.currencyCode)
            put("buyerUserId", request.buyerUserId)
            put("sellerUserId", request.sellerUserId)
            put("paymentMethodId", request.paymentMethodId)
            put("rendezvousSpot", request.rendezvousSpot)
            put("buyerNote", request.buyerNote)
            request.buyerPhoneE164?.takeIf { it.isNotBlank() }?.let { put("buyerPhone", it) }
            val meta = JSONObject()
            request.metadata.forEach { (key, value) -> meta.put(key, value) }
            put("metadata", meta)
        }
        return apiCall(HttpMethod.Post, "/escrow/holds", body) { json ->
            val holdId = json.optString("holdId").ifBlank { json.optString("id") }
            if (holdId.isBlank()) {
                return@apiCall PaymentResult.Failure(
                    PaymentErrorCode.PROVIDER_REJECTED,
                    "Provider response missing holdId"
                )
            }
            PaymentResult.Success(
                EscrowHold(
                    orderId = request.orderId,
                    providerHoldId = holdId,
                    providerId = providerId,
                    status = parseStatus(json.optString("status"), EscrowOrderStatus.FUNDS_HELD),
                    amount = request.amount,
                    currencyCode = request.currencyCode,
                    nextActionUrl = json.optString("nextActionUrl").ifBlank { null },
                    rawProviderReference = json.optString("reference").ifBlank { holdId }
                )
            )
        }
    }

    override suspend fun releaseToSeller(request: EscrowReleaseRequest): PaymentResult<EscrowSettlement> {
        if (!isConfigured) {
            return PaymentResult.Failure(
                PaymentErrorCode.PROVIDER_NOT_CONFIGURED,
                "PAYMENT_API_BASE_URL / keys are missing"
            )
        }
        if (!request.releaseCodeVerified) {
            return PaymentResult.Failure(
                PaymentErrorCode.RELEASE_CODE_REQUIRED,
                "Seller release QR must be verified before payout"
            )
        }
        val body = JSONObject().apply {
            put("orderId", request.orderId)
            put("sellerUserId", request.sellerUserId)
            put("marketItemId", request.marketItemId)
            put("amount", request.amount)
            put("currency", request.currencyCode)
            put("releaseCodeVerified", true)
        }
        return apiCall(
            HttpMethod.Post,
            "/escrow/holds/${request.providerHoldId}/release",
            body
        ) { json ->
            PaymentResult.Success(
                EscrowSettlement(
                    orderId = request.orderId,
                    providerHoldId = request.providerHoldId,
                    providerId = providerId,
                    status = parseStatus(json.optString("status"), EscrowOrderStatus.RELEASED_TO_SELLER),
                    amount = request.amount,
                    currencyCode = request.currencyCode,
                    settlementReference = json.optString("settlementId")
                        .ifBlank { json.optString("reference") }
                        .ifBlank { null }
                )
            )
        }
    }

    override suspend fun refundBuyer(request: EscrowRefundRequest): PaymentResult<EscrowSettlement> {
        if (!isConfigured) {
            return PaymentResult.Failure(
                PaymentErrorCode.PROVIDER_NOT_CONFIGURED,
                "PAYMENT_API_BASE_URL / keys are missing"
            )
        }
        val body = JSONObject().apply {
            put("orderId", request.orderId)
            put("buyerUserId", request.buyerUserId)
            put("amount", request.amount)
            put("currency", request.currencyCode)
            put("reason", request.reason)
        }
        return apiCall(
            HttpMethod.Post,
            "/escrow/holds/${request.providerHoldId}/refund",
            body
        ) { json ->
            PaymentResult.Success(
                EscrowSettlement(
                    orderId = request.orderId,
                    providerHoldId = request.providerHoldId,
                    providerId = providerId,
                    status = parseStatus(json.optString("status"), EscrowOrderStatus.REFUNDED_TO_BUYER),
                    amount = request.amount,
                    currencyCode = request.currencyCode,
                    settlementReference = json.optString("settlementId")
                        .ifBlank { json.optString("reference") }
                        .ifBlank { null }
                )
            )
        }
    }

    override suspend fun getHoldStatus(providerHoldId: String): PaymentResult<EscrowHold> {
        if (!isConfigured) {
            return PaymentResult.Failure(
                PaymentErrorCode.PROVIDER_NOT_CONFIGURED,
                "PAYMENT_API_BASE_URL / keys are missing"
            )
        }
        return apiCall(HttpMethod.Get, "/escrow/holds/$providerHoldId", null) { json ->
            val holdId = json.optString("holdId").ifBlank { providerHoldId }
            PaymentResult.Success(
                EscrowHold(
                    orderId = json.optString("orderId"),
                    providerHoldId = holdId,
                    providerId = providerId,
                    status = parseStatus(json.optString("status"), EscrowOrderStatus.FUNDS_HELD),
                    amount = json.optDouble("amount", 0.0),
                    currencyCode = json.optString("currency", "KES"),
                    nextActionUrl = json.optString("nextActionUrl").ifBlank { null },
                    rawProviderReference = json.optString("reference").ifBlank { holdId }
                )
            )
        }
    }

    private suspend fun <T> apiCall(
        method: HttpMethod,
        path: String,
        body: JSONObject?,
        map: (JSONObject) -> PaymentResult<T>
    ): PaymentResult<T> = withContext(ioDispatcher) {
        try {
            val response: HttpResponse = client.request("$baseUrl$path") {
                this.method = method
                header("Authorization", "Bearer ${apiSecret.ifBlank { apiKey }}")
                if (apiKey.isNotBlank()) header("X-Api-Key", apiKey)
                header("X-Payment-Provider", providerId)
                header("Accept", "application/json")
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body.toString())
                }
            }
            val text = response.bodyAsText()
            if (!response.status.isSuccess()) {
                return@withContext PaymentResult.Failure(
                    code = when (response.status.value) {
                        401, 403 -> PaymentErrorCode.AUTHENTICATION_FAILED
                        402 -> PaymentErrorCode.INSUFFICIENT_FUNDS
                        in 400..499 -> PaymentErrorCode.PROVIDER_REJECTED
                        else -> PaymentErrorCode.NETWORK
                    },
                    message = text.ifBlank { "Payment API HTTP ${response.status.value}" },
                    retryable = response.status.value >= 500
                )
            }
            val json = if (text.isBlank()) JSONObject() else JSONObject(text)
            map(json)
        } catch (t: Throwable) {
            PaymentResult.Failure(
                code = PaymentErrorCode.NETWORK,
                message = t.message ?: "Payment API network failure",
                retryable = true,
                cause = t
            )
        }
    }

    private fun parseStatus(raw: String, fallback: EscrowOrderStatus): EscrowOrderStatus {
        val normalized = raw.trim().uppercase().replace('-', '_')
        return runCatching { EscrowOrderStatus.valueOf(normalized) }.getOrDefault(fallback)
    }
}
