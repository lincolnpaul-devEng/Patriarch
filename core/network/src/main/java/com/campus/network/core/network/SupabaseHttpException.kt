package com.campus.network.core.network

class SupabaseHttpException(
    val statusCode: Int,
    responseBody: String
) : IllegalStateException("Supabase request failed (HTTP $statusCode): ${responseBody.take(500)}") {
    val isRetryable: Boolean
        get() = statusCode == 408 || statusCode == 429 || statusCode >= 500
}
