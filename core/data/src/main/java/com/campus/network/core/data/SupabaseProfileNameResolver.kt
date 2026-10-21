package com.campus.network.core.data

import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.network.SupabaseClientProvider
import org.json.JSONArray
import java.net.URLEncoder

class SupabaseProfileNameResolver(
    private val supabaseClient: SupabaseClientProvider
) {
    suspend fun resolve(userIds: Collection<String>): Map<String, String> {
        val requestedIds = userIds.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
        if (requestedIds.isEmpty()) return emptyMap()

        val filters = requestedIds.flatMap { userId ->
            val encodedId = URLEncoder.encode(userId, Charsets.UTF_8.name())
            buildList {
                if (userId.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))) {
                    add("user_id.eq.$encodedId")
                }
                add("student_id.eq.$encodedId")
                if ('@' in userId) add("university_email.eq.$encodedId")
            }
        }
        val query = "campusprofile?or=(${filters.joinToString(",")})&limit=200"
        val raw = supabaseClient.queryTable(
            query,
            selectQuery = "user_id,student_id,university_email,full_name"
        ).getOrThrow()
        val profiles = JSONArray(raw)
        val namesById = mutableMapOf<String, String>()
        for (index in 0 until profiles.length()) {
            val profile = profiles.getJSONObject(index)
            val name = profile.optString("full_name").takeIf(String::isRealProfileDisplayName) ?: continue
            listOf("user_id", "student_id", "university_email").forEach { field ->
                profile.optString(field)
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let { namesById[it.lowercase()] = name.trim() }
            }
        }
        return requestedIds.mapNotNull { id ->
            namesById[id.lowercase()]?.let { id.lowercase() to it }
        }.toMap()
    }
}
