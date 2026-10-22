package com.campus.network.core.data

import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.network.SupabaseClientProvider
import org.json.JSONArray
import java.net.URLEncoder

data class ResolvedCampusProfile(
    val name: String,
    val avatarUrl: String?
)

class SupabaseProfileNameResolver(
    private val supabaseClient: SupabaseClientProvider
) {
    suspend fun resolve(userIds: Collection<String>): Map<String, String> =
        resolveProfiles(userIds, includeAvatar = false).mapValues { it.value.name }

    suspend fun resolveWithAvatars(userIds: Collection<String>): Map<String, ResolvedCampusProfile> =
        resolveProfiles(userIds, includeAvatar = true)

    private suspend fun resolveProfiles(
        userIds: Collection<String>,
        includeAvatar: Boolean
    ): Map<String, ResolvedCampusProfile> {
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
            selectQuery = if (includeAvatar) {
                "user_id,student_id,university_email,full_name,avatar_url"
            } else {
                "user_id,student_id,university_email,full_name"
            }
        ).getOrThrow()
        val profiles = JSONArray(raw)
        val profilesById = mutableMapOf<String, ResolvedCampusProfile>()
        for (index in 0 until profiles.length()) {
            val profile = profiles.getJSONObject(index)
            val name = profile.optString("full_name").takeIf(String::isRealProfileDisplayName) ?: continue
            val avatarUrl = if (includeAvatar) {
                profile.optString("avatar_url")
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.startsWith("avatar_") }
            } else {
                null
            }
            val resolved = ResolvedCampusProfile(name.trim(), avatarUrl)
            listOf("user_id", "student_id", "university_email").forEach { field ->
                profile.optString(field)
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let { profilesById[it.lowercase()] = resolved }
            }
        }
        return requestedIds.mapNotNull { id ->
            profilesById[id.lowercase()]?.let { id.lowercase() to it }
        }.toMap()
    }
}
