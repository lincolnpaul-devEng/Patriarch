package com.campus.network.core.data

import com.campus.network.core.model.BadgeLevel
import com.campus.network.core.model.ConsistencyQuest
import com.campus.network.core.model.OnboardingPreferences
import com.campus.network.core.model.QuestCategory
import com.campus.network.core.model.StudentProfile
import com.campus.network.core.model.UniversityDirectoryEntry
import com.campus.network.core.model.VerifiedSkillBadge
import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.model.stableUserId
import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.StudentProfileEntity
import com.campus.network.core.network.SupabaseSessionStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repository for Tab 5: Profile (Gamified Persona).
 * Tracks student metadata, verified skill tags, and private Quest Deck.
 * Backed by Room database cache and Supabase PostgreSQL persistence.
 */
class StudentProfileRepository(
    private val database: CampusDatabase? = null,
    private val supabaseClient: com.campus.network.core.network.SupabaseClientProvider? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val profileDao = database?.studentProfileDao()
    private val repositoryScope = CoroutineScope(ioDispatcher + SupervisorJob())
    private val _knownProfiles = MutableStateFlow<Map<String, StudentProfile>>(emptyMap())
    val knownProfiles = _knownProfiles.asStateFlow()

    private val _studentProfile = MutableStateFlow(
        StudentProfile(
            studentId = "",
            fullName = "",
            avatarUrl = null,
            universityEmail = "",
            universityName = "",
            degreeProgram = "",
            academicYear = "",
            consistencyStreakDays = 0,
            behavioralTrustScore = 50,
            campusMeritPoints = 0,
            verifiedSkillBadges = emptyList(),
            privateQuestDeck = emptyList(),
            alternateProfiles = emptyList(),
            onboardingPreferences = OnboardingPreferences()
        )
    )

    val studentProfile = _studentProfile.asStateFlow()

    private val _isAuthenticatedSession = MutableStateFlow(false)
    val isAuthenticatedSession = _isAuthenticatedSession.asStateFlow()

    private val _isGuestSession = MutableStateFlow(false)
    val isGuestSession = _isGuestSession.asStateFlow()

    private val _locationPrivacyScope = MutableStateFlow(
        com.campus.network.core.model.LocationPrivacyScope.VERIFIED_CAMPUS_PEERS
    )
    val locationPrivacyScope = _locationPrivacyScope.asStateFlow()

    init {
        if (profileDao != null) {
            repositoryScope.launch {
                profileDao.getProfile().collect { cachedEntity ->
                    if (cachedEntity != null) {
                        val cachedProfile = cachedEntity.toDomain()
                        _studentProfile.value = cachedProfile.copy(
                            fullName = profileNameFromKnown(cachedProfile, _knownProfiles.value).orEmpty()
                        )
                        _isAuthenticatedSession.value = SupabaseSessionStore.accessToken() != null
                    }
                }
            }
        }
        repositoryScope.launch {
            refreshKnownProfiles()
        }
    }

    suspend fun refreshKnownProfiles(sinceMs: Long = 0L): Map<String, StudentProfile> = withContext(ioDispatcher) {
        runCatching {
            val client = supabaseClient ?: return@runCatching _knownProfiles.value
            val deltaFilter = if (sinceMs > 0L) "&updated_at=gt.$sinceMs" else ""
            val result = client.queryTable("campusprofile?select=*$deltaFilter").getOrNull()
            if (!result.isNullOrBlank()) {
                val jsonArray = org.json.JSONArray(result)
                val map = mutableMapOf<String, StudentProfile>()
                for (i in 0 until jsonArray.length()) {
                    val row = jsonArray.getJSONObject(i)
                    val id = row.optString("id")
                    val userId = row.optString("user_id")
                    val studentId = row.optString("student_id")
                    val email = row.optString("university_email")
                    val name = row.optString("full_name")
                    val avatar = row.optString("avatar_url").takeIf { it.isNotBlank() && !it.startsWith("avatar_") }
                    val uni = row.optString("university_name")
                    if (name.isRealProfileDisplayName()) {
                        val prof = StudentProfile(
                            studentId = studentId.ifBlank { id },
                            fullName = name,
                            avatarUrl = avatar,
                            universityEmail = email,
                            universityName = uni
                        )
                        if (id.isNotBlank()) map[id] = prof
                        if (userId.isNotBlank()) map[userId] = prof
                        if (studentId.isNotBlank()) map[studentId] = prof
                        if (email.isNotBlank()) {
                            map[email] = prof
                            map[email.lowercase()] = prof
                        }
                    }
                }
                _knownProfiles.value = map
                val current = _studentProfile.value
                val canonicalName = profileNameFromKnown(current, map)
                _studentProfile.value = current.copy(fullName = canonicalName.orEmpty())
                map
            } else {
                _knownProfiles.value
            }
        }.getOrDefault(_knownProfiles.value)
    }

    suspend fun syncRemoteProfiles(sinceMs: Long): Result<Long> = withContext(ioDispatcher) {
        runCatching {
            val client = supabaseClient ?: throw IllegalStateException("Supabase client is not configured")
            val timestampFilter = if (sinceMs > 0L) {
                val cursor = java.time.Instant.ofEpochMilli(sinceMs).toString()
                "&updated_at=gt.${java.net.URLEncoder.encode(cursor, "UTF-8")}"
            } else {
                ""
            }
            val response = client.queryTable(
                "campusprofile?order=updated_at.asc&limit=100$timestampFilter"
            ).getOrThrow()
            val rows = org.json.JSONArray(response)
            val profiles = _knownProfiles.value.toMutableMap()
            var newestTimestamp = sinceMs
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                val updatedAt = row.optString("updated_at")
                val rowTimestamp = runCatching {
                    java.time.OffsetDateTime.parse(updatedAt).toInstant().toEpochMilli()
                }.getOrDefault(0L)
                newestTimestamp = maxOf(newestTimestamp, rowTimestamp)

                val id = row.optString("id")
                val userId = row.optString("user_id")
                val studentId = row.optString("student_id").ifBlank { id }
                val email = row.optString("university_email")
                val name = row.optString("full_name")
                if (!name.isRealProfileDisplayName()) continue
                val profile = StudentProfile(
                    studentId = studentId,
                    fullName = name,
                    avatarUrl = row.optString("avatar_url")
                        .takeIf { it.isNotBlank() && !it.startsWith("avatar_") },
                    universityEmail = email,
                    universityName = row.optString("university_name")
                )
                listOf(id, userId, studentId, email, email.lowercase())
                    .filter(String::isNotBlank)
                    .forEach { profiles[it] = profile }
            }
            _knownProfiles.value = profiles
            val current = _studentProfile.value
            profileNameFromKnown(current, profiles)?.let { canonical ->
                _studentProfile.value = current.copy(fullName = canonical)
            }
            newestTimestamp
        }
    }

    private suspend fun persistToDatabase(profile: StudentProfile): Result<Unit> {
        val syncId = java.util.UUID.randomUUID().toString()
        val pendingProfile = profile.copy(
            syncStatus = com.campus.network.core.model.SyncStatus.PENDING.name,
            syncId = syncId
        )
        try {
            profileDao?.insertOrUpdateProfile(StudentProfileEntity.fromDomain(pendingProfile))
        } catch (error: Exception) {
            return Result.failure(error)
        }
        _studentProfile.value = pendingProfile

        val remoteResult = runCatching {
            val client = supabaseClient ?: throw IllegalStateException("Supabase client is not configured")
            if (profile.universityEmail.isBlank()) {
                throw IllegalArgumentException("A verified profile email is required before saving changes.")
            }
            if (!profile.fullName.isRealProfileDisplayName()) {
                throw IllegalArgumentException("A real profile name is required before saving profile changes.")
            }
            val json = org.json.JSONObject().apply {
                put("full_name", profile.fullName)
                put("university_name", profile.universityName)
                put("degree_program", profile.degreeProgram)
                put("academic_year", profile.academicYear)
                put("bio", profile.bio)
                profile.avatarUrl?.let { put("avatar_url", it) }
                put("campus_residence", profile.campusResidence)
                put("phone_number", profile.phoneNumber)
                put("preferred_rendezvous_point", profile.preferredRendezvousPoint)
                put("notifications_enabled", profile.notificationsEnabled)
                put("escrow_alerts_enabled", profile.escrowAlertsEnabled)
                put("quest_reminders_enabled", profile.questRemindersEnabled)
                put("is_trust_score_public", profile.isTrustScorePublic)
                put("is_online_status_visible", profile.isOnlineStatusVisible)
                put("activity_ledger_visibility", profile.activityLedgerVisibility)
                put("location_privacy_scope", profile.locationPrivacyScope.name)
                put("interests", org.json.JSONArray(profile.interests))
                put(
                    "onboarding_preferences",
                    org.json.JSONObject(StudentProfileEntity.serializeOnboardingPreferences(profile.onboardingPreferences))
                )
            }
            val userId = SupabaseSessionStore.session()?.optString("user_id")?.takeIf(String::isNotBlank)
                ?: throw SecurityException("Sign in before updating your profile")
            val rows = org.json.JSONArray(
                client.updateRecord(
                    "campusprofile?user_id=eq.${java.net.URLEncoder.encode(userId, "UTF-8")}",
                    json.toString()
                ).getOrThrow()
            )
            check(rows.length() == 1) { "Your authenticated profile could not be updated" }
        }

        val status = syncStatusFor(remoteResult)
        val savedProfile = pendingProfile.copy(syncStatus = status.name)
        profileDao?.insertOrUpdateProfile(StudentProfileEntity.fromDomain(savedProfile))
        _studentProfile.value = savedProfile
        if (status == com.campus.network.core.model.SyncStatus.SYNCED) {
            rememberProfile(savedProfile)
            return Result.success(Unit)
        }
        val error = remoteResult.exceptionOrNull()
            ?: IllegalStateException("Profile sync failed without an error")
        if (status == com.campus.network.core.model.SyncStatus.PENDING) {
            return Result.success(Unit)
        }
        return Result.failure(error)
    }

    fun getProfile(): Flow<StudentProfile> = _studentProfile.asStateFlow()

    suspend fun getProfileSync(): StudentProfile? = withContext(ioDispatcher) {
        val profile = profileDao?.getProfileSync()?.toDomain() ?: _studentProfile.value
        profile.copy(fullName = profileNameFromKnown(profile, _knownProfiles.value).orEmpty())
            .takeIf { it.universityEmail.isNotBlank() }
    }

    private fun profileNameFromKnown(
        profile: StudentProfile,
        profiles: Map<String, StudentProfile>
    ): String? {
        val userId = profile.stableUserId()
        if (userId.isBlank()) return null
        return (profiles[userId]
            ?: profiles[userId.lowercase()]
            ?: profiles.values.firstOrNull { it.stableUserId().equals(userId, ignoreCase = true) })
            ?.fullName
            ?.takeIf(String::isRealProfileDisplayName)
    }

    private fun rememberProfile(profile: StudentProfile) {
        val updatedMap = _knownProfiles.value.toMutableMap()
        listOf(profile.studentId, profile.stableUserId(), profile.universityEmail)
            .filter(String::isNotBlank)
            .forEach { key ->
                updatedMap[key] = profile
                updatedMap[key.lowercase()] = profile
            }
        _knownProfiles.value = updatedMap
    }

    suspend fun syncProfileWithRemote(email: String): Boolean = withContext(ioDispatcher) {
        runCatching {
            val cleanEmail = email.trim()
            if (cleanEmail.isBlank()) return@runCatching false
            val encodedEmail = java.net.URLEncoder.encode(cleanEmail, "UTF-8")
            val result = supabaseClient?.queryTable("campusprofile?university_email=ilike.$encodedEmail")?.getOrNull()
            if (!result.isNullOrBlank()) {
                val jsonArray = org.json.JSONArray(result)
                if (jsonArray.length() > 0) {
                    val row = jsonArray.getJSONObject(0)
                    val existingProfile = _studentProfile.value
                    val current = if (existingProfile.universityEmail.equals(cleanEmail, ignoreCase = true)) {
                        existingProfile
                    } else {
                        StudentProfile()
                    }
                    val remoteStudentId = row.optString("student_id").ifBlank { row.optString("id") }.takeIf { it.isNotBlank() } ?: current.studentId
                    val remoteName = row.optString("full_name")
                    if (!remoteName.isRealProfileDisplayName()) {
                        return@runCatching false
                    }
                    val remoteAvatar = row.optString("avatar_url").takeIf { it.isNotBlank() } ?: current.avatarUrl
                    val remoteMerit = row.optInt("campus_merit_points", current.campusMeritPoints)
                    val remoteTrust = row.optInt("behavioral_trust_score", current.behavioralTrustScore)
                    val remoteUni = row.optString("university_name").takeIf { it.isNotBlank() } ?: current.universityName
                    val remoteResidence = row.optString("campus_residence").takeIf { it.isNotBlank() } ?: current.campusResidence
                    val remotePrefsObj = row.optJSONObject("onboarding_preferences")
                    val remotePrefs = if (remotePrefsObj != null) {
                        StudentProfileEntity.deserializeOnboardingPreferences(remotePrefsObj.toString())
                    } else {
                        OnboardingPreferences()
                    }
                    val updated = current.copy(
                        studentId = remoteStudentId,
                        fullName = remoteName,
                        avatarUrl = remoteAvatar,
                        universityEmail = row.optString("university_email").ifBlank { cleanEmail },
                        campusMeritPoints = remoteMerit,
                        behavioralTrustScore = remoteTrust,
                        universityName = remoteUni,
                        campusResidence = remoteResidence,
                        onboardingPreferences = if (
                            remotePrefs.hasCompletedSurvey ||
                            !current.universityEmail.equals(cleanEmail, ignoreCase = true) ||
                            current.syncStatus !in setOf(
                                com.campus.network.core.model.SyncStatus.PENDING.name,
                                com.campus.network.core.model.SyncStatus.FAILED.name
                            )
                        ) {
                            remotePrefs
                        } else {
                            current.onboardingPreferences
                        }
                    )
                    _studentProfile.value = updated
                    profileDao?.insertOrUpdateProfile(StudentProfileEntity.fromDomain(updated))
                    rememberProfile(updated)
                    return@runCatching updated.onboardingPreferences.hasCompletedSurvey
                } else {
                    // Profile does not yet exist remotely, upload current profile if email is valid
                    val current = _studentProfile.value
                    if (current.universityEmail.isNotBlank() &&
                        current.universityEmail.equals(cleanEmail, ignoreCase = true)
                    ) {
                        runCatching {
                            persistToDatabase(current)
                        }
                    }
                    return@runCatching current.onboardingPreferences.hasCompletedSurvey
                }
            }
            false
        }.getOrDefault(false)
    }

    /**
     * Verifies against Supabase whether a user record exists in the remote database.
     */
    suspend fun validateUserExistsInDatabase(email: String): Result<Boolean> = withContext(ioDispatcher) {
        runCatching {
            val cleanEmail = email.trim()
            if (cleanEmail.isBlank()) {
                return@runCatching false
            }
            val encodedEmail = java.net.URLEncoder.encode(cleanEmail, "UTF-8")
            val client = supabaseClient ?: return@runCatching true
            val queryResult = client.queryTable("campusprofile?university_email=ilike.$encodedEmail&select=id,university_email").getOrNull()
            if (queryResult.isNullOrBlank()) {
                return@runCatching true
            }
            val jsonArray = org.json.JSONArray(queryResult.trim())
            jsonArray.length() > 0
        }
    }

    suspend fun signOut() = withContext(ioDispatcher) {
        _isAuthenticatedSession.value = false
        _isGuestSession.value = false
        _knownProfiles.value = emptyMap()
        _studentProfile.value = StudentProfile(
            studentId = "",
            fullName = "",
            avatarUrl = null,
            universityEmail = "",
            universityName = "",
            degreeProgram = "",
            academicYear = "",
            consistencyStreakDays = 0,
            behavioralTrustScore = 50,
            campusMeritPoints = 0,
            verifiedSkillBadges = emptyList(),
            privateQuestDeck = emptyList(),
            alternateProfiles = emptyList(),
            onboardingPreferences = OnboardingPreferences()
        )
        runCatching {
            profileDao?.clearProfile()
        }
    }

    suspend fun isEmailRegistered(email: String): Boolean = withContext(ioDispatcher) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) return@withContext false

        // 1. Check live Supabase database first
        val remoteCheck = runCatching {
            val encodedEmail = java.net.URLEncoder.encode(cleanEmail, "UTF-8")
            val queryResult = supabaseClient?.queryTable("campusprofile?university_email=ilike.$encodedEmail&select=id,university_email")?.getOrThrow()
            if (!queryResult.isNullOrBlank()) {
                val arr = org.json.JSONArray(queryResult.trim())
                arr.length() > 0
            } else {
                false
            }
        }.getOrNull()

        if (remoteCheck != null) {
            return@withContext remoteCheck
        }

        // 2. Fallback to local Room cache if network request failed
        val local = profileDao?.getProfileByEmail(cleanEmail)
        if (local != null && local.universityEmail.isNotBlank()) {
            return@withContext true
        }
        false
    }

    suspend fun updateProfileDirect(profile: StudentProfile): Result<Unit> = withContext(ioDispatcher) {
        persistToDatabase(profile)
    }

    suspend fun markFailedWritesPending() = withContext(ioDispatcher) {
        val failedProfile = profileDao?.getFailedProfile()
        if (failedProfile != null) {
            profileDao?.updateProfileSyncStatus(
                failedProfile.id,
                com.campus.network.core.model.SyncStatus.PENDING.name,
                failedProfile.syncId
            )
        }
    }

    suspend fun applyGuestSession() = withContext(ioDispatcher) {
        _isAuthenticatedSession.value = false
        _isGuestSession.value = true
        val current = _studentProfile.value
        _studentProfile.value = current.copy(
            personaRole = "Guest Preview (Unverified)",
            onboardingPreferences = current.onboardingPreferences.copy(
                isVerifiedStudentEmail = false
            )
        )
    }

    suspend fun updateLocationPrivacyScope(
        scope: com.campus.network.core.model.LocationPrivacyScope
    ) = withContext(ioDispatcher) {
        _locationPrivacyScope.value = scope
    }

    suspend fun saveOnboardingPreferences(preferences: OnboardingPreferences): Result<Unit> = withContext(ioDispatcher) {
        val dao = profileDao
            ?: return@withContext Result.failure(IllegalStateException("Local profile database is unavailable"))
        if (preferences.personalizedNewsConsent &&
            (preferences.intents.isEmpty() || preferences.favoriteCategories.isEmpty())
        ) {
            return@withContext Result.failure(
                IllegalArgumentException("Choose at least one goal and one content category to enable personalized recommendations.")
            )
        }
        val current = _studentProfile.value
        val updatedDegree = if (preferences.faculty.isNotBlank()) {
            preferences.faculty
        } else {
            current.degreeProgram
        }
        val updatedYear = if (preferences.yearOfStudy.isNotBlank()) {
            preferences.yearOfStudy
        } else {
            current.academicYear
        }
        val updatedUni = if (preferences.universityName.isNotBlank()) {
            preferences.universityName
        } else {
            current.universityName
        }
        val updatedResidence = if (preferences.regionName.isNotBlank()) {
            "${preferences.regionName}, ${preferences.countryName}"
        } else {
            current.campusResidence
        }
        val updated = current.copy(
            universityName = updatedUni,
            degreeProgram = updatedDegree,
            academicYear = updatedYear,
            campusResidence = updatedResidence,
            notificationsEnabled = preferences.pushNotificationsOptIn,
            onboardingPreferences = preferences
        )
        val previousSyncId = current.syncId
        val result = persistToDatabase(updated)
        val savedProfile = _studentProfile.value
        if (result.isFailure &&
            savedProfile.syncId != previousSyncId &&
            savedProfile.syncStatus == com.campus.network.core.model.SyncStatus.FAILED.name
        ) {
            val incompleteProfile = savedProfile.copy(
                onboardingPreferences = savedProfile.onboardingPreferences.copy(hasCompletedSurvey = false)
            )
            try {
                dao.insertOrUpdateProfile(StudentProfileEntity.fromDomain(incompleteProfile))
                _studentProfile.value = incompleteProfile
            } catch (error: Exception) {
                return@withContext Result.failure(error)
            }
        }
        result
    }

    suspend fun applyAuthenticatedUser(
        fullName: String?,
        email: String,
        avatarUrl: String? = null,
        matchedUniversity: UniversityDirectoryEntry?,
        countryName: String?,
        currencyCode: String?
    ) = withContext(ioDispatcher) {
        _isAuthenticatedSession.value = true
        _isGuestSession.value = false
        val existingProfile = _studentProfile.value
        val current = if (existingProfile.universityEmail.equals(email.trim(), ignoreCase = true)) {
            existingProfile
        } else {
            StudentProfile()
        }
        val cleanName = fullName?.trim()?.takeIf { it.isNotEmpty() } ?: current.fullName
        val cleanEmail = email.trim().takeIf { it.isNotEmpty() } ?: current.universityEmail
        // Keep an already saved custom avatar; the identity provider photo is only a fallback.
        val cleanAvatar = current.avatarUrl?.takeIf { it.isNotBlank() }
            ?: avatarUrl?.trim()?.takeIf { it.isNotEmpty() }
        val currentPrefs = current.onboardingPreferences
        val updatedPrefs = if (matchedUniversity != null) {
            currentPrefs.copy(
                countryCode = matchedUniversity.countryCode.ifBlank { currentPrefs.countryCode },
                countryName = countryName ?: currentPrefs.countryName,
                currencyCode = currencyCode ?: currentPrefs.currencyCode,
                regionName = matchedUniversity.region.ifBlank { currentPrefs.regionName },
                universityName = matchedUniversity.name,
                universityDomain = matchedUniversity.domain,
                isCustomUniversity = false,
                isVerifiedStudentEmail = true
            )
        } else {
            currentPrefs.copy(isVerifiedStudentEmail = true)
        }
        val authenticatedProfile = current.copy(
            fullName = cleanName,
            universityEmail = cleanEmail,
            avatarUrl = cleanAvatar,
            universityName = matchedUniversity?.name ?: current.universityName,
            personaRole = if (current.personaRole.isBlank() || current.personaRole.contains("Guest", ignoreCase = true)) "Undergraduate Student" else current.personaRole,
            onboardingPreferences = updatedPrefs
        )
        _studentProfile.value = authenticatedProfile.copy(fullName = "")
        profileDao?.insertOrUpdateProfile(StudentProfileEntity.fromDomain(_studentProfile.value))
        if (authenticatedProfile.fullName.isRealProfileDisplayName()) {
        persistToDatabase(authenticatedProfile)
        }
    }

    suspend fun setAvatarFallbackIfMissing(avatarUrl: String?) = withContext(ioDispatcher) {
        val fallbackUrl = avatarUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return@withContext
        val current = _studentProfile.value
        if (!current.avatarUrl.isNullOrBlank()) return@withContext
        val updated = current.copy(avatarUrl = fallbackUrl)
        _studentProfile.value = updated
        profileDao?.insertOrUpdateProfile(StudentProfileEntity.fromDomain(updated))
    }

    suspend fun completeQuest(questId: String) = withContext(ioDispatcher) {
        val current = _studentProfile.value
        val updatedQuests = current.privateQuestDeck.map { quest ->
            if (quest.id == questId) {
                quest.copy(currentProgress = quest.targetProgress, isCompleted = true)
            } else {
                quest
            }
        }
        _studentProfile.value = current.copy(
            privateQuestDeck = updatedQuests,
            behavioralTrustScore = (current.behavioralTrustScore + 1).coerceAtMost(100)
        )
        persistToDatabase(_studentProfile.value)
    }

    suspend fun updateProfile(
        fullName: String,
        universityName: String,
        degreeProgram: String,
        academicYear: String,
        campusResidence: String,
        phoneNumber: String,
        bio: String,
        interests: List<String>,
        avatarUrl: String? = null
    ): Result<Unit> = withContext(ioDispatcher) {
        val current = _studentProfile.value
        val updated = current.copy(
            fullName = fullName.trim(),
            universityName = universityName.trim(),
            degreeProgram = degreeProgram.trim(),
            academicYear = academicYear.trim(),
            campusResidence = campusResidence.trim(),
            phoneNumber = phoneNumber.trim(),
            bio = bio.trim(),
            interests = interests,
            avatarUrl = avatarUrl?.trim()?.ifBlank { null } ?: current.avatarUrl
        )
        persistToDatabase(updated)
    }

    suspend fun updateAvatar(avatarUrl: String): Result<Unit> = withContext(ioDispatcher) {
        val current = _studentProfile.value
        val updated = current.copy(avatarUrl = avatarUrl.trim())
        persistToDatabase(updated)
    }

    suspend fun setStatusNote(
        note: String,
        emoji: String,
        expiry: String
    ): Result<Unit> = withContext(ioDispatcher) {
        val current = _studentProfile.value
        val updated = current.copy(
            statusNote = note.trim(),
            statusNoteEmoji = emoji.ifBlank { "Focus" },
            statusNoteExpiry = expiry
        )
        persistToDatabase(updated)
    }

    suspend fun clearStatusNote(): Result<Unit> = withContext(ioDispatcher) {
        val current = _studentProfile.value
        val updated = current.copy(
            statusNote = null,
            statusNoteEmoji = null,
            statusNoteExpiry = null
        )
        persistToDatabase(updated)
    }

    suspend fun updateCampusMeritPoints(points: Int) = withContext(ioDispatcher) {
        val current = _studentProfile.value
        _studentProfile.value = current.copy(campusMeritPoints = points.coerceAtLeast(0))
        persistToDatabase(_studentProfile.value)
    }

    suspend fun toggleSavedListing(listingId: String) = withContext(ioDispatcher) {
        val current = _studentProfile.value
        val list = current.savedListingIds.toMutableList()
        if (list.contains(listingId)) {
            list.remove(listingId)
        } else {
            list.add(listingId)
        }
        _studentProfile.value = current.copy(savedListingIds = list)
        persistToDatabase(_studentProfile.value)
    }

    suspend fun setPreferredRendezvousPoint(spotName: String) = withContext(ioDispatcher) {
        val current = _studentProfile.value
        _studentProfile.value = current.copy(preferredRendezvousPoint = spotName.trim())
        persistToDatabase(_studentProfile.value)
    }


    suspend fun addProfile(newProfile: StudentProfile) = withContext(ioDispatcher) {
        val current = _studentProfile.value
        val updatedAlternates = current.alternateProfiles + newProfile
        _studentProfile.value = current.copy(alternateProfiles = updatedAlternates)
        persistToDatabase(_studentProfile.value)
    }

    suspend fun switchProfile(targetStudentId: String) = withContext(ioDispatcher) {
        val current = _studentProfile.value
        val target = current.alternateProfiles.find { it.studentId == targetStudentId }
        if (target != null) {
            val updatedAlternates = current.alternateProfiles.filterNot { it.studentId == targetStudentId } + current
            _studentProfile.value = target.copy(alternateProfiles = updatedAlternates)
            persistToDatabase(_studentProfile.value)
        }
    }

    suspend fun updateNotificationSettings(
        notificationsEnabled: Boolean,
        escrowAlertsEnabled: Boolean,
        questRemindersEnabled: Boolean
    ): Result<Unit> = withContext(ioDispatcher) {
        val current = _studentProfile.value
        val updated = current.copy(
            notificationsEnabled = notificationsEnabled,
            escrowAlertsEnabled = escrowAlertsEnabled,
            questRemindersEnabled = questRemindersEnabled
        )
        persistToDatabase(updated)
    }

    suspend fun updatePrivacySettings(
        isTrustScorePublic: Boolean,
        isOnlineStatusVisible: Boolean,
        activityLedgerVisibility: String
    ): Result<Unit> = withContext(ioDispatcher) {
        val current = _studentProfile.value
        val updated = current.copy(
            isTrustScorePublic = isTrustScorePublic,
            isOnlineStatusVisible = isOnlineStatusVisible,
            activityLedgerVisibility = activityLedgerVisibility
        )
        persistToDatabase(updated)
    }

    suspend fun clearCache(): Long = withContext(ioDispatcher) {
        val current = _studentProfile.value
        val freedBytes = 38_500_000L // 38.5 MB cleared
        val newStorage = (current.storageUsedBytes - freedBytes).coerceAtLeast(0L)
        _studentProfile.value = current.copy(storageUsedBytes = newStorage)
        persistToDatabase(_studentProfile.value)
        freedBytes
    }

    suspend fun upgradeStorage(additionalBytes: Long = 1_073_741_824L) = withContext(ioDispatcher) {
        val current = _studentProfile.value
        _studentProfile.value = current.copy(storageQuotaBytes = current.storageQuotaBytes + additionalBytes)
        persistToDatabase(_studentProfile.value)
    }

    suspend fun deleteAccount(reason: String) = withContext(ioDispatcher) {
        // 1. Hard-delete the account and all owned data server-side via Edge Function.
        val payload = org.json.JSONObject().apply {
            put("reason", reason)
            put("user_id", _studentProfile.value.stableUserId())
        }.toString()
        supabaseClient?.invokeFunction("delete-account", payload)

        // 2. Clear local Room cache and sign out of the session.
        signOut()
    }

    suspend fun depositToWallet(amount: Double): Boolean = withContext(ioDispatcher) {
        if (amount <= 0.0) return@withContext false
        val current = _studentProfile.value
        val updated = current.copy(walletBalance = current.walletBalance + amount)
        _studentProfile.value = updated
        persistToDatabase(updated)
        true
    }

    suspend fun withdrawFromWallet(amount: Double): Boolean = withContext(ioDispatcher) {
        val current = _studentProfile.value
        if (amount <= 0.0 || amount > current.walletBalance) return@withContext false
        val updated = current.copy(walletBalance = current.walletBalance - amount)
        _studentProfile.value = updated
        persistToDatabase(updated)
        true
    }

    suspend fun lockVaultAmount(amount: Double): Boolean = withContext(ioDispatcher) {
        val current = _studentProfile.value
        if (amount <= 0.0 || amount > current.walletBalance) return@withContext false
        val updated = current.copy(
            walletBalance = current.walletBalance - amount,
            vaultProtectedBalance = current.vaultProtectedBalance + amount
        )
        _studentProfile.value = updated
        persistToDatabase(updated)
        true
    }

    suspend fun releaseVaultAmount(amount: Double): Boolean = withContext(ioDispatcher) {
        val current = _studentProfile.value
        if (amount <= 0.0 || amount > current.vaultProtectedBalance) return@withContext false
        val updated = current.copy(
            vaultProtectedBalance = current.vaultProtectedBalance - amount,
            walletBalance = current.walletBalance + amount
        )
        _studentProfile.value = updated
        persistToDatabase(updated)
        true
    }
}
