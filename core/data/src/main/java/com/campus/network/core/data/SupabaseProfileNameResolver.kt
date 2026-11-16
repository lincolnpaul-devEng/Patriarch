package com.campus.network.core.data

import com.campus.network.core.model.cleanDisplayName
import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.network.SupabaseSdkClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

data class ResolvedCampusProfile(
    val name: String,
    val avatarUrl: String?
)

class SupabaseProfileNameResolver(
    private val supabase: SupabaseClient = SupabaseSdkClientProvider.defaultClient
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

        val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        val selectColumns = if (includeAvatar) {
            "id,user_id,student_id,university_email,full_name,avatar_url"
        } else {
            "id,user_id,student_id,university_email,full_name"
        }

        val profiles: List<CampusProfileRow> = runCatching {
            supabase.from("campusprofile")
                .select(columns = Columns.raw(selectColumns)) {
                    filter {
                        or {
                            requestedIds.forEach { userId ->
                                val clean = cleanDisplayName(userId)
                                if (userId.matches(uuidRegex)) {
                                    eq("user_id", userId)
                                    eq("id", userId)
                                }
                                eq("student_id", userId)
                                if ('@' in userId) eq("university_email", userId)
                                if (clean.isNotBlank()) eq("full_name", clean)
                            }
                        }
                    }
                    limit(200)
                }
                .decodeList<CampusProfileRow>()
        }.getOrDefault(emptyList())

        val profilesById = mutableMapOf<String, ResolvedCampusProfile>()
        for (profile in profiles) {
            val cleanName = cleanDisplayName(profile.full_name)
            if (!cleanName.isRealProfileDisplayName()) continue
            val avatarUrl = if (includeAvatar) {
                profile.avatar_url?.takeIf { it.isNotBlank() && !it.startsWith("avatar_") }
            } else {
                null
            }
            val resolved = ResolvedCampusProfile(cleanName.trim(), avatarUrl)
            listOfNotNull(
                profile.id,
                profile.user_id,
                profile.student_id,
                profile.university_email,
                cleanName,
                cleanName.lowercase().replace(Regex("[^a-z0-9]"), "")
            ).forEach { id ->
                if (id.isNotBlank()) {
                    profilesById[id] = resolved
                    profilesById[id.lowercase()] = resolved
                    profilesById[id.trim().lowercase()] = resolved
                }
            }
        }
        return profilesById
    }
}
