package com.campus.network.core.network.payment

import com.campus.network.core.network.BuildConfig

/**
 * Resolves the active [PaymentProvider] socket.
 *
 * Plug-in rule:
 * - If `PAYMENT_API_BASE_URL` (+ key/secret) is set → [HttpEscrowPaymentProvider]
 * - Else → [UnconfiguredPaymentProvider] (checkout refuses fake success)
 *
 * To add a vendor-native SDK later (e.g. Paystack Android SDK), register it here
 * behind the same [PaymentProvider] interface — Hub code stays unchanged.
 */
object PaymentProviderRegistry {

    @Volatile
    private var overrideProvider: PaymentProvider? = null

    /** Test / DI hook to force a specific provider. */
    fun install(provider: PaymentProvider?) {
        overrideProvider = provider
    }

    fun resolve(): PaymentProvider {
        overrideProvider?.let { return it }

        val http = HttpEscrowPaymentProvider()
        return if (http.isConfigured) {
            http
        } else {
            UnconfiguredPaymentProvider()
        }
    }

    fun isPaymentReady(): Boolean = resolve().isConfigured

    fun configurationHint(): String {
        val base = BuildConfig.PAYMENT_API_BASE_URL.trim()
        val id = BuildConfig.PAYMENT_PROVIDER_ID.trim().ifBlank { "(unset)" }
        return if (base.isBlank()) {
            "Payment socket empty. Add PAYMENT_API_BASE_URL, PAYMENT_API_KEY, " +
                "PAYMENT_API_SECRET, PAYMENT_PROVIDER_ID to secrets.properties."
        } else {
            "Payment socket ready for provider=$id at $base"
        }
    }
}
