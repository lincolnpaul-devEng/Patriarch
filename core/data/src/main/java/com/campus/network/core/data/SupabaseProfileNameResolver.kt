package com.campus.network.core.data

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

        val selectColumns = if (includeAvatar) {
            "user_id,student_id,university_email,full_name,avatar_url"
        } else {
            "user_id,student_id,university_email,full_name"
        }

        val profiles: List<CampusProfileRow> = runCatching {
            supabase.from("campusprofile")
                .select(columns = Columns.raw(selectColumns)) {
                    filter {
                        or {
                            requestedIds.forEach { userId ->
                                eq("user_id", userId)
                                eq("student_id", userId)
                                if ('@' in userId) eq("university_email", userId)
                            }
                        }
                    }
                    limit(200)
                }
                .decodeList<CampusProfileRow>()
        }.getOrDefault(emptyList())

        val profilesById = mutableMapOf<String, ResolvedCampusProfile>()
        for (profile in profiles) {
            val name = profile.full_name.takeIf(String::isRealProfileDisplayName) ?: continue
            val avatarUrl = if (includeAvatar) {
                profile.avatar_url?.takeIf { it.isNotBlank() && !it.startsWith("avatar_") }
            } else {
                null
            }
            val resolved = ResolvedCampusProfile(name.trim(), avatarUrl)
            listOf(profile.user_id, profile.student_id, profile.university_email).forEach { id ->
                if (id.isNotBlank()) {
                    profilesById[id] = resolved
                    profilesById[id.lowercase()] = resolved
                }
            }
        }
        return profilesById
    }
}
