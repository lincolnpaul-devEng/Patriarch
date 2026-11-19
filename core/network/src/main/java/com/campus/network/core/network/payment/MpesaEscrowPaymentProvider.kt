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
import com.campus.network.core.network.SupabaseSessionStore
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
 * M-Pesa (Safaricom Daraja) escrow socket backed by the `mpesa` Supabase Edge Function.
 *
 * Daraja credentials live only in Supabase function secrets — the APK holds nothing.
 * Every call carries the signed-in buyer's JWT so the server can enforce identity
 * (buyer-only release, bounded code attempts, item status flips).
 *
 * Enable by setting `MPESA_ENABLED=true` in secrets.properties and deploying the function:
 * `supabase functions deploy mpesa` + `supabase secrets set MPESA_...`.
 */
class MpesaEscrowPaymentProvider(
    private val edgeFunctionUrl: String =
        BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/mpesa",
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PaymentProvider {

    private val client = HttpClient(OkHttp)

    override val providerId: String = "mpesa"
    override val displayName: String = "M-Pesa Safe Trade"
    override val isConfigured: Boolean =
        edgeFunctionUrl.isNotBlank() && BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.MPESA_ENABLED

    override fun availableMethods(): List<PaymentMethodOption> = listOf(
        PaymentMethodOption(
            id = "MPESA_STK",
            kind = PaymentMethodKind.MOBILE_MONEY_STK,
            title = "M-Pesa (STK Push)",
            subtitle = "Pay into Safe Trade escrow with your Safaricom line",
            requiresPhoneNumber = true
        )
    )

    override suspend fun createEscrowHold(request: EscrowHoldRequest): PaymentResult<EscrowHold> {
        if (!isConfigured) {
            return PaymentResult.Failure(
                PaymentErrorCode.PROVIDER_NOT_CONFIGURED,
                "M-Pesa escrow is not deployed. Set MPESA_ENABLED=true in secrets.properties, deploy the mpesa edge function, and rebuild."
            )
        }
        val body = JSONObject().apply {
            put("action", "initiate")
            put("itemId", request.marketItemId)
            put("amount", request.amount)
            put("buyerUserId", request.buyerUserId)
            put("orderNumber", request.orderReference)
            put("rendezvousSpot", request.rendezvousSpot)
            put("buyerNote", request.buyerNote)
            put("paymentMethodId", request.paymentMethodId)
            request.buyerPhoneE164?.takeIf { it.isNotBlank() }?.let { put("buyerPhone", it) }
        }
        return apiCall(body) { json ->
            val order = json.optJSONObject("order")
            val holdId = order?.optString("id").orEmpty()
            if (holdId.isBlank()) {
                return@apiCall PaymentResult.Failure(
                    PaymentErrorCode.PROVIDER_REJECTED,
                    "Escrow response missing order id"
                )
            }
            PaymentResult.Success(
                EscrowHold(
                    orderId = holdId,
                    providerHoldId = order?.optString("providerHoldId").orEmpty().ifBlank { holdId },
                    providerId = providerId,
                    status = EscrowOrderStatus.PENDING_PAYMENT,
                    amount = request.amount,
                    currencyCode = request.currencyCode,
                    nextActionUrl = null,
                    rawProviderReference = json.optString("customerMessage").ifBlank { holdId }
                )
            )
        }
    }

    override suspend fun releaseToSeller(request: EscrowReleaseRequest): PaymentResult<EscrowSettlement> {
        if (!isConfigured) {
            return PaymentResult.Failure(
                PaymentErrorCode.PROVIDER_NOT_CONFIGURED,
                "M-Pesa escrow is not deployed on this build"
            )
        }
        if (request.scannedCode.isBlank()) {
            return PaymentResult.Failure(
                PaymentErrorCode.RELEASE_CODE_REQUIRED,
                "Scan the seller's release code before the payout can be sent"
            )
        }
        val body = JSONObject().apply {
            put("action", "release")
            put("itemId", request.marketItemId)
            put("scannedCode", request.scannedCode)
        }
        return apiCall(body) { _ ->
            PaymentResult.Success(
                EscrowSettlement(
                    orderId = request.orderId,
                    providerHoldId = request.providerHoldId,
                    providerId = providerId,
                    status = EscrowOrderStatus.RELEASED_TO_SELLER,
                    amount = request.amount,
                    currencyCode = request.currencyCode,
                    settlementReference = request.orderId
                )
            )
        }
    }

    override suspend fun refundBuyer(request: EscrowRefundRequest): PaymentResult<EscrowSettlement> {
        if (!isConfigured) {
            return PaymentResult.Failure(
                PaymentErrorCode.PROVIDER_NOT_CONFIGURED,
                "M-Pesa escrow is not deployed on this build"
            )
        }
        val body = JSONObject().apply {
            put("action", "refund")
            put("orderId", request.orderId)
            put("reason", request.reason)
        }
        return apiCall(body) { _ ->
            PaymentResult.Success(
                EscrowSettlement(
                    orderId = request.orderId,
                    providerHoldId = request.providerHoldId,
                    providerId = providerId,
                    status = EscrowOrderStatus.REFUNDED_TO_BUYER,
                    amount = request.amount,
                    currencyCode = request.currencyCode,
                    settlementReference = request.orderId
                )
            )
        }
    }

    override suspend fun getHoldStatus(providerHoldId: String): PaymentResult<EscrowHold> =
        PaymentResult.Failure(
            PaymentErrorCode.PROVIDER_REJECTED,
            "M-Pesa status polling is handled by the Daraja callback pipeline"
        )

    private suspend fun <T> apiCall(
        body: JSONObject,
        map: (JSONObject) -> PaymentResult<T>
    ): PaymentResult<T> = withContext(ioDispatcher) {
        val token = SupabaseSessionStore.accessToken()
        try {
            val response: HttpResponse = client.request(edgeFunctionUrl) {
                this.method = HttpMethod.Post
                header("Authorization", "Bearer ${token ?: ""}")
                header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                header("Accept", "application/json")
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
            val text = response.bodyAsText()
            if (!response.status.isSuccess()) {
                val failureJson = runCatching { JSONObject(text) }.getOrNull()
                return@withContext PaymentResult.Failure(
                    code = when (response.status.value) {
                        401, 403 -> PaymentErrorCode.AUTHENTICATION_FAILED
                        in 400..499 -> PaymentErrorCode.PROVIDER_REJECTED
                        else -> PaymentErrorCode.NETWORK
                    },
                    message = failureJson?.optString("message")?.ifBlank { null }
                        ?: text.ifBlank { "M-Pesa escrow HTTP ${response.status.value}" },
                    retryable = response.status.value >= 500
                )
            }
            val json = if (text.isBlank()) JSONObject() else JSONObject(text)
            map(json)
        } catch (t: Throwable) {
            PaymentResult.Failure(
                code = PaymentErrorCode.NETWORK,
                message = t.message ?: "M-Pesa escrow network failure",
                retryable = true,
                cause = t
            )
        }
    }
}
