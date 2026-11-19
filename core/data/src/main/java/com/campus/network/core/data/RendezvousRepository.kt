package com.campus.network.core.data

import com.campus.network.core.network.SupabaseSdkClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class RendezvousSpotRow(
    val id: String = "",
    val spot_name: String = "",
    val description: String? = null,
    val security_level: String = "",
    val has_cctv: Boolean = false,
    val has_security_desk: Boolean = false,
    val operating_hours: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val is_active: Boolean = true,
    val university_name: String = ""
)

/** A campus official safe-meet spot surfaced in the sell sheet pickup picker. */
data class RendezvousSpot(
    val id: String,
    val name: String,
    val description: String,
    val securityLevel: String,
    val hasCctv: Boolean,
    val hasSecurityDesk: Boolean,
    val operatingHours: String,
    val latitude: Double?,
    val longitude: Double?
)

/**
 * Reads official campus rendezvous points (`campus_rendezvous_points`).
 * Rows are managed externally; the app never hardcodes spot lists.
 */
class RendezvousRepository(
    private val sdkClient: SupabaseClient = SupabaseSdkClientProvider.defaultClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun getActiveSpots(): List<RendezvousSpot> = withContext(ioDispatcher) {
        val result = runCatching {
            sdkClient.from("campus_rendezvous_points")
                .select {
                    filter { eq("is_active", true) }
                    order("spot_name", order = Order.ASCENDING)
                }
                .decodeList<RendezvousSpotRow>()
        }
        result.getOrDefault(emptyList()).map { row ->
            RendezvousSpot(
                id = row.id,
                name = row.spot_name,
                description = row.description.orEmpty(),
                securityLevel = row.security_level,
                hasCctv = row.has_cctv,
                hasSecurityDesk = row.has_security_desk,
                operatingHours = row.operating_hours,
                latitude = row.latitude,
                longitude = row.longitude
            )
        }
    }
}
