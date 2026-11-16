package com.campus.network.core.network

/**
 * Thrown when a Supabase REST (PostgREST) endpoint returns a non-2xx HTTP status.
 *
 * The supabase-kt 2.0.0 Postgrest plugin does not inspect response status codes for
 * write operations (upsert/update/delete): a 401/403/409 response is returned as a
 * successful [io.github.jan.supabase.postgrest.result.PostgrestResult] whose body is
 * the error JSON. This exception is raised by the network-layer plugin installed in
 * [SupabaseSdkClientProvider] so write failures surface loudly instead of silently.
 */
class PostgrestHttpException(
    val status: Int,
    val body: String
) : Exception("PostgREST error $status: $body") {

    /** True when the failure is plausibly temporary (server-side or rate limiting). */
    val isTransient: Boolean
        get() = status >= 500 || status == 429
}
