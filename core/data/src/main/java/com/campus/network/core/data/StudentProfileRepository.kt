package com.campus.network.core.data

import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.StudentProfileEntity
import com.campus.network.core.model.BadgeLevel
import com.campus.network.core.model.ConsistencyQuest
import com.campus.network.core.model.OnboardingPreferences
import com.campus.network.core.model.QuestCategory
import com.campus.network.core.model.StudentProfile
import com.campus.network.core.model.UniversityDirectoryEntry
import com.campus.network.core.model.VerifiedSkillBadge
import com.campus.network.core.model.isRealProfileDisplayName
import com.campus.network.core.model.stableUserId
import com.campus.network.core.network.BuildConfig
import com.campus.network.core.network.SupabaseSdkClientProvider
import com.campus.network.core.network.SupabaseSessionStore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * Serializable row shape returned by the Supabase `campusprofile` table.
 * Matches the PostgREST column names exactly.
 */
@Serializable
data class CampusProfileRow(
    val id: String = "",
    val user_id: String = "",
    val student_id: String = "",
    val full_name: String = "",
    val university_email: String = "",
    val university_name: String = "",
    val avatar_url: String? = null,
    val degree_program: String? = null,
    val academic_year: String? = null,
    val bio: String? = null,
    val campus_residence: String? = null,
    val phone_number: String? = null,
    val preferred_rendezvous_point: String? = null,
    val notifications_enabled: Boolean = true,
    val escrow_alerts_enabled: Boolean = true,
    val quest_reminders_enabled: Boolean = true,
    val is_trust_score_public: Boolean = false,
    val is_online_status_visible: Boolean = false,
    val activity_ledger_visibility: String = "FRIENDS",
    val location_privacy_scope: String = "CAMPUS_WIDE",
    val behavioral_trust_score: Int = 50,
    val campus_merit_points: Int = 0,
    val created_at: String? = null,
    val updated_at: String? = null
)

/**
 * Repository for Tab 5: Profile (Gamified Persona).
 * Tracks student metadata, verified skill tags, and private Quest Deck.
 * Backed by Room database cache and Supabase PostgreSQL persistence via the
 * official Supabase Kotlin SDK.
 */
class StudentProfileRepository(
    private val database: CampusDatabase? = null,
    private val sdkClient: SupabaseClient = SupabaseSdkClientProvider.defaultClient,
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
    fun getProfile(): StateFlow<StudentProfile> = studentProfile

    suspend fun getProfileSync(): StudentProfile? = withContext(ioDispatcher) {
        val profile = profileDao?.getProfileSync()?.toDomain() ?: _studentProfile.value
        profile.copy(fullName = profileNameFromKnown(profile, _knownProfiles.value).orEmpty())
            .takeIf { it.universityEmail.isNotBlank() }
    }

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
        try {
            val rows: List<CampusProfileRow> = sdkClient
                .from("campusprofile")
                .select(columns = Columns.ALL) {
                    if (sinceMs > 0L) {
                        val cursor = java.time.Instant.ofEpochMilli(sinceMs).toString()
                        filter { gt("created_at", cursor) }
                    }
                    limit(200)
                }
                .decodeList()
            val map = mutableMapOf<String, StudentProfile>()
            for (row in rows) {
                val avatar = row.avatar_url?.takeIf { it.isNotBlank() && !it.startsWith("avatar_") }
                if (row.full_name.isRealProfileDisplayName()) {
                    val prof = StudentProfile(
                        studentId = row.student_id.ifBlank { row.id },
                        fullName = row.full_name,
                        avatarUrl = avatar,
                        universityEmail = row.university_email,
                        universityName = row.university_name
                    )
                    if (row.id.isNotBlank()) map[row.id] = prof
                    if (row.user_id.isNotBlank()) map[row.user_id] = prof
                    if (row.student_id.isNotBlank()) map[row.student_id] = prof
                    if (row.university_email.isNotBlank()) {
                        map[row.university_email] = prof
                        map[row.university_email.lowercase()] = prof
                    }
                }
            }
            _knownProfiles.value = map
            val current = _studentProfile.value
            val canonicalName = profileNameFromKnown(current, map)
            _studentProfile.value = current.copy(fullName = canonicalName.orEmpty())
            map
        } catch (e: Exception) {
            _knownProfiles.value
        }
    }

    suspend fun syncRemoteProfiles(sinceMs: Long): Result<Long> = withContext(ioDispatcher) {
        runCatching {
            val rows: List<CampusProfileRow> = sdkClient.from("campusprofile")
                .select(columns = Columns.ALL) {
                    if (sinceMs > 0L) {
                        val cursor = java.time.Instant.ofEpochMilli(sinceMs).toString()
                        filter { gt("created_at", cursor) }
                    }
                    order("created_at", order = Order.ASCENDING)
                    limit(100)
                }
                .decodeList()
            val profiles = _knownProfiles.value.toMutableMap()
            var newestTimestamp = sinceMs
            for (row in rows) {
                newestTimestamp = System.currentTimeMillis()

                val id = row.id
                val userId = row.user_id
                val studentId = row.student_id.ifBlank { id }
                val email = row.university_email
                val name = row.full_name
                if (!name.isRealProfileDisplayName()) continue
                val profile = StudentProfile(
                    studentId = studentId,
                    fullName = name,
                    avatarUrl = row.avatar_url?.takeIf { it.isNotBlank() && !it.startsWith("avatar_") },
                    universityEmail = email,
                    universityName = row.university_name,
                    campusResidence = row.campus_residence.orEmpty(),
                    degreeProgram = row.degree_program.orEmpty(),
                    academicYear = row.academic_year.orEmpty(),
                    bio = row.bio.orEmpty(),
                    phoneNumber = row.phone_number.orEmpty(),
                    preferredRendezvousPoint = row.preferred_rendezvous_point.orEmpty(),
                    notificationsEnabled = row.notifications_enabled,
                    escrowAlertsEnabled = row.escrow_alerts_enabled,
                    questRemindersEnabled = row.quest_reminders_enabled,
                    isTrustScorePublic = row.is_trust_score_public,
                    isOnlineStatusVisible = row.is_online_status_visible,
                    activityLedgerVisibility = row.activity_ledger_visibility,
                    behavioralTrustScore = row.behavioral_trust_score,
                    campusMeritPoints = row.campus_merit_points
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

        val sessionUserId = SupabaseSessionStore.session()?.optString("user_id")?.takeIf(String::isNotBlank)
        if (sessionUserId == null) {
            return Result.success(Unit)
        }

        val remoteResult = runCatching {
            if (profile.universityEmail.isBlank()) {
                throw IllegalArgumentException("A verified profile email is required before saving changes.")
            }
            if (!profile.fullName.isRealProfileDisplayName()) {
                throw IllegalArgumentException("A real profile name is required before saving profile changes.")
            }
            sdkClient.from("campusprofile").upsert(
                CampusProfileRow(
                    id = profile.stableUserId(),
                    user_id = sessionUserId,
                    student_id = profile.studentId,
                    full_name = profile.fullName,
                    university_email = profile.universityEmail,
                    university_name = profile.universityName,
                    avatar_url = profile.avatarUrl,
                    degree_program = profile.degreeProgram,
                    academic_year = profile.academicYear,
                    bio = profile.bio,
                    campus_residence = profile.campusResidence,
                    phone_number = profile.phoneNumber,
                    preferred_rendezvous_point = profile.preferredRendezvousPoint,
                    notifications_enabled = profile.notificationsEnabled,
                    escrow_alerts_enabled = profile.escrowAlertsEnabled,
                    quest_reminders_enabled = profile.questRemindersEnabled,
                    is_trust_score_public = profile.isTrustScorePublic,
                    is_online_status_visible = profile.isOnlineStatusVisible,
                    activity_ledger_visibility = profile.activityLedgerVisibility,
                    location_privacy_scope = profile.locationPrivacyScope.name,
                    behavioral_trust_score = profile.behavioralTrustScore,
                    campus_merit_points = profile.campusMeritPoints
                )
            )
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

    private fun syncStatusFor(remoteResult: Result<Any?>): com.campus.network.core.model.SyncStatus {
        if (remoteResult.isSuccess) {
            return com.campus.network.core.model.SyncStatus.SYNCED
        }
        val error = remoteResult.exceptionOrNull() ?: return com.campus.network.core.model.SyncStatus.FAILED
        var cause: Throwable? = error
        while (cause != null) {
            if (cause is java.io.IOException) return com.campus.network.core.model.SyncStatus.PENDING
            cause = cause.cause
        }
        return com.campus.network.core.model.SyncStatus.FAILED
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
            val rows: List<CampusProfileRow> = sdkClient.from("campusprofile")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("university_email", cleanEmail)
                    }
                    limit(1)
                }
                .decodeList()
            if (rows.isNotEmpty()) {
                val row = rows.first()
                val existingProfile = _studentProfile.value
                val current = if (existingProfile.universityEmail.equals(cleanEmail, ignoreCase = true)) {
                    existingProfile
                } else {
                    StudentProfile()
                }
                val remoteStudentId = row.student_id.ifBlank { row.id }.takeIf { it.isNotBlank() } ?: current.studentId
                val remoteName = row.full_name
                if (!remoteName.isRealProfileDisplayName()) {
                    return@runCatching false
                }
                val remoteAvatar = row.avatar_url?.takeIf { it.isNotBlank() && !it.startsWith("avatar_") } ?: current.avatarUrl
                val remoteMerit = if (row.campus_merit_points != 0) row.campus_merit_points else current.campusMeritPoints
                val remoteTrust = if (row.behavioral_trust_score != 0) row.behavioral_trust_score else current.behavioralTrustScore
                val remoteUni = row.university_name.takeIf { it.isNotBlank() } ?: current.universityName
                val remoteResidence = row.campus_residence?.takeIf { it.isNotBlank() } ?: current.campusResidence
                val remotePrefs = current.onboardingPreferences
                val updated = current.copy(
                    studentId = remoteStudentId,
                    fullName = remoteName,
                    avatarUrl = remoteAvatar,
                    universityEmail = row.university_email.ifBlank { cleanEmail },
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
        }.getOrDefault(false)
    }

    /**
     * Verifies against Supabase whether a user record exists in the remote database.
     */
    suspend fun validateUserExistsInDatabase(email: String): Result<Boolean> = withContext(ioDispatcher) {
        runCatching {
            val cleanEmail = email.trim()
            if (cleanEmail.isBlank()) return@runCatching false
            val rows: List<CampusProfileRow> = sdkClient.from("campusprofile")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("university_email", cleanEmail)
                    }
                    limit(1)
                }
                .decodeList()
            rows.isNotEmpty()
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

        val remoteCheck = runCatching {
            val rows: List<CampusProfileRow> = sdkClient.from("campusprofile")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("university_email", cleanEmail)
                    }
                    limit(1)
                }
                .decodeList()
            rows.isNotEmpty()
        }.getOrNull()

        if (remoteCheck != null) {
            return@withContext remoteCheck
        }

        val local = profileDao?.getProfileByEmail(cleanEmail)
        if (local != null && local.universityEmail.isNotBlank()) {
            return@withContext true
        }
        false
    }

    suspend fun saveProfile(
        studentId: String = _studentProfile.value.studentId,
        fullName: String = _studentProfile.value.fullName,
        universityEmail: String = _studentProfile.value.universityEmail,
        universityName: String = _studentProfile.value.universityName,
        degreeProgram: String = _studentProfile.value.degreeProgram,
        academicYear: String = _studentProfile.value.academicYear,
        campusResidence: String = _studentProfile.value.campusResidence,
        phoneNumber: String = _studentProfile.value.phoneNumber,
        bio: String = _studentProfile.value.bio,
        avatarUrl: String? = _studentProfile.value.avatarUrl,
        preferredRendezvousPoint: String = _studentProfile.value.preferredRendezvousPoint,
        notificationsEnabled: Boolean = _studentProfile.value.notificationsEnabled,
        escrowAlertsEnabled: Boolean = _studentProfile.value.escrowAlertsEnabled,
        questRemindersEnabled: Boolean = _studentProfile.value.questRemindersEnabled,
        isTrustScorePublic: Boolean = _studentProfile.value.isTrustScorePublic,
        isOnlineStatusVisible: Boolean = _studentProfile.value.isOnlineStatusVisible,
        activityLedgerVisibility: String = _studentProfile.value.activityLedgerVisibility,
        locationPrivacyScope: com.campus.network.core.model.LocationPrivacyScope = _studentProfile.value.locationPrivacyScope,
        interests: List<String> = _studentProfile.value.interests,
        onboardingPreferences: OnboardingPreferences = _studentProfile.value.onboardingPreferences
    ): Result<Unit> = withContext(ioDispatcher) {
        val updated = _studentProfile.value.copy(
            studentId = studentId,
            fullName = fullName,
            universityEmail = universityEmail,
            universityName = universityName,
            degreeProgram = degreeProgram,
            academicYear = academicYear,
            campusResidence = campusResidence,
            phoneNumber = phoneNumber,
            bio = bio,
            avatarUrl = avatarUrl,
            preferredRendezvousPoint = preferredRendezvousPoint,
            notificationsEnabled = notificationsEnabled,
            escrowAlertsEnabled = escrowAlertsEnabled,
            questRemindersEnabled = questRemindersEnabled,
            isTrustScorePublic = isTrustScorePublic,
            isOnlineStatusVisible = isOnlineStatusVisible,
            activityLedgerVisibility = activityLedgerVisibility,
            locationPrivacyScope = locationPrivacyScope,
            interests = interests,
            onboardingPreferences = onboardingPreferences
        )
        persistToDatabase(updated)
    }

    suspend fun retryFailedUploads() = withContext(ioDispatcher) {
        val current = _studentProfile.value
        if (current.syncStatus == com.campus.network.core.model.SyncStatus.FAILED.name ||
            current.syncStatus == com.campus.network.core.model.SyncStatus.PENDING.name
        ) {
            persistToDatabase(current)
        }
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

    suspend fun updateLocationPrivacyScope(scope: com.campus.network.core.model.LocationPrivacyScope) {
        _locationPrivacyScope.value = scope
        val current = _studentProfile.value
        persistToDatabase(current.copy(locationPrivacyScope = scope))
    }

    suspend fun updateStatusNote(note: String?, emoji: String?, expiry: String? = null) = withContext(ioDispatcher) {
        val current = _studentProfile.value
        val updated = current.copy(
            statusNote = note,
            statusNoteEmoji = emoji,
            statusNoteExpiry = expiry
        )
        persistToDatabase(updated)
    }

    suspend fun clearStatusNote() = withContext(ioDispatcher) {
        updateStatusNote(null, null, null)
    }

    suspend fun clearCache(): Long = withContext(ioDispatcher) {
        val current = _studentProfile.value
        val freedBytes = 38_500_000L
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
        val payload = JSONObject().apply {
            put("reason", reason)
            put("user_id", _studentProfile.value.stableUserId())
        }.toString()
        runCatching {
            val jwt = SupabaseSessionStore.accessToken() ?: return@runCatching
            val url = URL("${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/delete-account")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer $jwt")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            conn.responseCode
        }
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

private fun profileNameFromKnown(
    current: StudentProfile,
    profiles: Map<String, StudentProfile>
): String? {
    val candidates = listOf(
        current.studentId,
        current.stableUserId(),
        current.universityEmail,
        current.universityEmail.lowercase()
    ).filter(String::isNotBlank)
    for (candidate in candidates) {
        val resolved = profiles[candidate]?.fullName?.takeIf(String::isRealProfileDisplayName)
        if (resolved != null) return resolved
    }
    return current.fullName.takeIf(String::isRealProfileDisplayName)
}
