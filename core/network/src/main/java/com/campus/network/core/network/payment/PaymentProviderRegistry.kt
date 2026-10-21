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
        return overrideProvider ?: UnconfiguredPaymentProvider()
    }

    /** Explicitly wire a provider from out-of-band configuration (never app BuildConfig). */
    fun installHttpProvider(baseUrl: String, apiKey: String, apiSecret: String,
                            providerId: String, displayName: String) {
        overrideProvider = HttpEscrowPaymentProvider(baseUrl, apiKey, apiSecret, providerId, displayName)
    }

    fun isPaymentReady(): Boolean = resolve().isConfigured

    fun configurationHint(): String =
        "Payment socket empty. Configure an Edge Function and install the provider via " +
            "PaymentProviderRegistry.installHttpProvider(...) — never ship payment secrets in BuildConfig."
}
