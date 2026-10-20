package com.campus.network.core.data

import com.campus.network.core.model.BadgeLevel
import com.campus.network.core.model.ConsistencyQuest
import com.campus.network.core.model.OnboardingPreferences
import com.campus.network.core.model.QuestCategory
import com.campus.network.core.model.StudentProfile
import com.campus.network.core.model.UniversityDirectoryEntry
import com.campus.network.core.model.VerifiedSkillBadge
import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.StudentProfileEntity
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
                        _studentProfile.value = cachedEntity.toDomain()
                        if (cachedEntity.universityEmail.isNotBlank()) {
                            _isAuthenticatedSession.value = true
                        }
                    }
                }
            }
        }
        repositoryScope.launch {
            refreshKnownProfiles()
        }
    }

    private val _knownProfiles = MutableStateFlow<Map<String, StudentProfile>>(emptyMap())
    val knownProfiles = _knownProfiles.asStateFlow()

    suspend fun refreshKnownProfiles(): Map<String, StudentProfile> = withContext(ioDispatcher) {
        runCatching {
            val client = supabaseClient ?: return@runCatching _knownProfiles.value
            val result = client.queryTable("campusprofile?select=*").getOrNull()
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
                    if (name.isNotBlank() && !name.equals("Student", ignoreCase = true)) {
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
                map
            } else {
                _knownProfiles.value
            }
        }.getOrDefault(_knownProfiles.value)
    }

    private suspend fun persistToDatabase(profile: StudentProfile): Result<Unit> = runCatching {
        val client = supabaseClient
        val cleanEmail = profile.universityEmail.trim()

        if (client != null && cleanEmail.isNotBlank()) {
            val json = org.json.JSONObject().apply {
                put("student_id", profile.studentId)
                put("full_name", profile.fullName)
                put("university_email", cleanEmail)
                put("university_name", profile.universityName)
                put("degree_program", profile.degreeProgram)
                put("academic_year", profile.academicYear)
                put("behavioral_trust_score", profile.behavioralTrustScore)
                put("campus_merit_points", profile.campusMeritPoints)
                put("bio", profile.bio)
                if (profile.avatarUrl != null) put("avatar_url", profile.avatarUrl)
                put("campus_residence", profile.campusResidence)
                put("phone_number", profile.phoneNumber)
                put("wallet_balance", profile.walletBalance)
                put("vault_balance", profile.vaultProtectedBalance)
                put("preferred_rendezvous_point", profile.preferredRendezvousPoint)
                put("notifications_enabled", profile.notificationsEnabled)
                put("escrow_alerts_enabled", profile.escrowAlertsEnabled)
                put("quest_reminders_enabled", profile.questRemindersEnabled)
                put("is_trust_score_public", profile.isTrustScorePublic)
                put("is_online_status_visible", profile.isOnlineStatusVisible)
                put("activity_ledger_visibility", profile.activityLedgerVisibility)
                put("location_privacy_scope", profile.locationPrivacyScope.name)
                put("interests", org.json.JSONArray(profile.interests))
                put("onboarding_preferences", org.json.JSONObject(StudentProfileEntity.serializeOnboardingPreferences(profile.onboardingPreferences)))
            }
            val payload = json.toString()
            val encodedEmail = java.net.URLEncoder.encode(cleanEmail, "UTF-8")

            var updated = false
            var remoteError: Throwable? = null

            val patchIlikeResult = client.updateRecord(
                "campusprofile?university_email=ilike.$encodedEmail",
                payload
            )
            if (patchIlikeResult.isSuccess) {
                val rows = runCatching { org.json.JSONArray(patchIlikeResult.getOrNull()) }.getOrNull()
                if (rows != null && rows.length() > 0) {
                    updated = true
                }
            } else {
                remoteError = patchIlikeResult.exceptionOrNull()
            }

            if (!updated) {
                val patchEqResult = client.updateRecord(
                    "campusprofile?university_email=eq.$encodedEmail",
                    payload
                )
                if (patchEqResult.isSuccess) {
                    val rows = runCatching { org.json.JSONArray(patchEqResult.getOrNull()) }.getOrNull()
                    if (rows != null && rows.length() > 0) {
                        updated = true
                    }
                } else if (remoteError == null) {
                    remoteError = patchEqResult.exceptionOrNull()
                }
            }

            if (!updated) {
                val insertResult = client.insertRecord("campusprofile", payload)
                if (insertResult.isSuccess) {
                    updated = true
                } else if (remoteError == null) {
                    remoteError = insertResult.exceptionOrNull()
                }
            }

            // If remote database transfer failed, abort and DO NOT save locally!
            if (!updated) {
                throw remoteError ?: IllegalStateException("Database transaction failed. Profile changes were not saved.")
            }
        }

        // ONLY store locally when database transaction is truly successful!
        profileDao?.insertOrUpdateProfile(StudentProfileEntity.fromDomain(profile))
        _studentProfile.value = profile

        // Keep knownProfiles synchronized locally in memory across channels/feeds
        if (profile.fullName.isNotBlank()) {
            val updatedMap = _knownProfiles.value.toMutableMap()
            if (profile.studentId.isNotBlank()) updatedMap[profile.studentId] = profile
            if (profile.universityEmail.isNotBlank()) {
                updatedMap[profile.universityEmail] = profile
                updatedMap[profile.universityEmail.lowercase()] = profile
            }
            _knownProfiles.value = updatedMap
        }
    }

    fun getProfile(): Flow<StudentProfile> = _studentProfile.asStateFlow()

    suspend fun getProfileSync(): StudentProfile? = withContext(ioDispatcher) {
        profileDao?.getProfileSync()?.toDomain() ?: _studentProfile.value.takeIf { it.universityEmail.isNotBlank() }
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
                    val current = _studentProfile.value
                    val remoteStudentId = row.optString("student_id").ifBlank { row.optString("id") }.takeIf { it.isNotBlank() } ?: current.studentId
                    val remoteName = row.optString("full_name").takeIf { it.isNotBlank() } ?: current.fullName
                    val remoteAvatar = row.optString("avatar_url").takeIf { it.isNotBlank() } ?: current.avatarUrl
                    val remoteMerit = row.optInt("campus_merit_points", current.campusMeritPoints)
                    val remoteTrust = row.optInt("behavioral_trust_score", current.behavioralTrustScore)
                    val remoteUni = row.optString("university_name").takeIf { it.isNotBlank() } ?: current.universityName
                    val remoteResidence = row.optString("campus_residence").takeIf { it.isNotBlank() } ?: current.campusResidence
                    val remotePrefsObj = row.optJSONObject("onboarding_preferences")
                    val remotePrefs = if (remotePrefsObj != null) {
                        StudentProfileEntity.deserializeOnboardingPreferences(remotePrefsObj.toString())
                    } else {
                        current.onboardingPreferences
                    }
                    val updated = current.copy(
                        studentId = remoteStudentId,
                        fullName = remoteName,
                        avatarUrl = remoteAvatar,
                        campusMeritPoints = remoteMerit,
                        behavioralTrustScore = remoteTrust,
                        universityName = remoteUni,
                        campusResidence = remoteResidence,
                        onboardingPreferences = if (remotePrefs.hasCompletedSurvey) remotePrefs else current.onboardingPreferences
                    )
                    _studentProfile.value = updated
                    profileDao?.insertOrUpdateProfile(StudentProfileEntity.fromDomain(updated))
                    return@runCatching updated.onboardingPreferences.hasCompletedSurvey
                } else {
                    // Empty array returned from Supabase means user was deleted from the database!
                    signOut()
                    return@runCatching false
                }
            }
            false
        }.getOrDefault(false)
    }

    /**
     * Verifies against Supabase whether a user record exists in the remote database.
     * If user does not exist in the database, automatically signs out and purges local cache.
     */
    suspend fun validateUserExistsInDatabase(email: String): Result<Boolean> = withContext(ioDispatcher) {
        runCatching {
            val cleanEmail = email.trim()
            if (cleanEmail.isBlank()) {
                signOut()
                return@runCatching false
            }
            val encodedEmail = java.net.URLEncoder.encode(cleanEmail, "UTF-8")
            val client = supabaseClient ?: return@runCatching true
            val queryResult = client.queryTable("campusprofile?university_email=ilike.$encodedEmail&select=id,university_email").getOrThrow()
            val jsonArray = org.json.JSONArray(queryResult.trim())
            val exists = jsonArray.length() > 0
            if (!exists) {
                // User has been deleted from Supabase! Purge local session and database cache
                signOut()
            }
            exists
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

    suspend fun saveOnboardingPreferences(preferences: OnboardingPreferences) = withContext(ioDispatcher) {
        val current = _studentProfile.value
        val updatedInterests = if (preferences.favoriteCategories.isNotEmpty()) {
            preferences.favoriteCategories
        } else {
            current.interests
        }
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
        _studentProfile.value = current.copy(
            universityName = updatedUni,
            degreeProgram = updatedDegree,
            academicYear = updatedYear,
            campusResidence = updatedResidence,
            interests = updatedInterests,
            notificationsEnabled = preferences.pushNotificationsOptIn || current.notificationsEnabled,
            onboardingPreferences = preferences
        )
        persistToDatabase(_studentProfile.value)
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
        val current = _studentProfile.value
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
        _studentProfile.value = current.copy(
            fullName = cleanName,
            universityEmail = cleanEmail,
            avatarUrl = cleanAvatar,
            universityName = matchedUniversity?.name ?: current.universityName,
            personaRole = if (current.personaRole.isBlank() || current.personaRole.contains("Guest", ignoreCase = true)) "Undergraduate Student" else current.personaRole,
            onboardingPreferences = updatedPrefs
        )
        profileDao?.insertOrUpdateProfile(StudentProfileEntity.fromDomain(_studentProfile.value))
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
    ) = withContext(ioDispatcher) {
        val current = _studentProfile.value
        _studentProfile.value = current.copy(
            statusNote = note.trim(),
            statusNoteEmoji = emoji.ifBlank { "Focus" },
            statusNoteExpiry = expiry
        )
        persistToDatabase(_studentProfile.value)
    }

    suspend fun clearStatusNote() = withContext(ioDispatcher) {
        val current = _studentProfile.value
        _studentProfile.value = current.copy(
            statusNote = null,
            statusNoteEmoji = null,
            statusNoteExpiry = null
        )
        persistToDatabase(_studentProfile.value)
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
    ) = withContext(ioDispatcher) {
        val current = _studentProfile.value
        _studentProfile.value = current.copy(
            notificationsEnabled = notificationsEnabled,
            escrowAlertsEnabled = escrowAlertsEnabled,
            questRemindersEnabled = questRemindersEnabled
        )
        persistToDatabase(_studentProfile.value)
    }

    suspend fun updatePrivacySettings(
        isTrustScorePublic: Boolean,
        isOnlineStatusVisible: Boolean,
        activityLedgerVisibility: String
    ) = withContext(ioDispatcher) {
        val current = _studentProfile.value
        _studentProfile.value = current.copy(
            isTrustScorePublic = isTrustScorePublic,
            isOnlineStatusVisible = isOnlineStatusVisible,
            activityLedgerVisibility = activityLedgerVisibility
        )
        persistToDatabase(_studentProfile.value)
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
        val current = _studentProfile.value
        _studentProfile.value = current.copy(
            fullName = "Deleted Student",
            universityEmail = "deleted@campus.internal",
            bio = "Account permanently deleted.",
            statusNote = null,
            behavioralTrustScore = 0,
            consistencyStreakDays = 0,
            storageUsedBytes = 0L,
            privateQuestDeck = emptyList(),
            verifiedSkillBadges = emptyList()
        )
        persistToDatabase(_studentProfile.value)
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
