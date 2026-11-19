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
        // M-Pesa socket: enabled via secrets.properties (Daraja creds live only in
        // Supabase function secrets, never in the APK).
        if (BuildConfig.MPESA_ENABLED && BuildConfig.SUPABASE_URL.isNotBlank()) {
            return MpesaEscrowPaymentProvider()
        }
        return UnconfiguredPaymentProvider()
    }

    /** Explicitly wire a provider from out-of-band configuration (never app BuildConfig). */
    fun installHttpProvider(baseUrl: String, apiKey: String, apiSecret: String,
                            providerId: String, displayName: String) {
        overrideProvider = HttpEscrowPaymentProvider(baseUrl, apiKey, apiSecret, providerId, displayName)
    }

    fun isPaymentReady(): Boolean = resolve().isConfigured

    fun configurationHint(): String =
        "Payment socket empty. Set MPESA_ENABLED=true in secrets.properties and deploy the `mpesa` " +
            "Supabase Edge Function (or install an HTTP provider via PaymentProviderRegistry.installHttpProvider(...)). " +
            "Never ship payment secrets in BuildConfig."
}
