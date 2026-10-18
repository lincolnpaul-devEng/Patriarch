package com.campus.network.core.data

import com.campus.network.core.model.BadgeLevel
import com.campus.network.core.model.ConsistencyQuest
import com.campus.network.core.model.OnboardingPreferences
import com.campus.network.core.model.QuestCategory
import com.campus.network.core.model.StudentProfile
import com.campus.network.core.model.UniversityDirectoryEntry
import com.campus.network.core.model.VerifiedSkillBadge
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repository for Tab 5: Profile (Gamified Persona).
 * Tracks student metadata, verified skill tags, and private Quest Deck.
 */
class StudentProfileRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
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
            verifiedSkillBadges = emptyList(),
            privateQuestDeck = emptyList(),
            alternateProfiles = emptyList(),
            onboardingPreferences = OnboardingPreferences()
        )
    )

    private val _isAuthenticatedSession = MutableStateFlow(true)
    val isAuthenticatedSession = _isAuthenticatedSession.asStateFlow()

    private val _isGuestSession = MutableStateFlow(false)
    val isGuestSession = _isGuestSession.asStateFlow()

    private val _locationPrivacyScope = MutableStateFlow(
        com.campus.network.core.model.LocationPrivacyScope.VERIFIED_CAMPUS_PEERS
    )
    val locationPrivacyScope = _locationPrivacyScope.asStateFlow()

    fun getProfile(): Flow<StudentProfile> = _studentProfile.asStateFlow()

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
        val cleanAvatar = avatarUrl?.trim()?.takeIf { it.isNotEmpty() } ?: current.avatarUrl
        val currentPrefs = current.onboardingPreferences
        val isAcademicDomain = cleanEmail.lowercase().endsWith(".ac.ke") ||
            cleanEmail.lowercase().endsWith("strathmore.edu") ||
            matchedUniversity != null
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
            currentPrefs.copy(isVerifiedStudentEmail = isAcademicDomain)
        }
        _studentProfile.value = current.copy(
            fullName = cleanName,
            universityEmail = cleanEmail,
            avatarUrl = cleanAvatar,
            universityName = matchedUniversity?.name ?: current.universityName,
            personaRole = if (isAcademicDomain) "Undergraduate Student" else "Guest Preview (Unverified)",
            onboardingPreferences = updatedPrefs
        )
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
    }

    suspend fun updateProfile(
        fullName: String,
        universityName: String,
        degreeProgram: String,
        academicYear: String,
        campusResidence: String,
        phoneNumber: String,
        bio: String,
        interests: List<String>
    ) = withContext(ioDispatcher) {
        val current = _studentProfile.value
        _studentProfile.value = current.copy(
            fullName = fullName.trim(),
            universityName = universityName.trim(),
            degreeProgram = degreeProgram.trim(),
            academicYear = academicYear.trim(),
            campusResidence = campusResidence.trim(),
            phoneNumber = phoneNumber.trim(),
            bio = bio.trim(),
            interests = interests
        )
    }

    suspend fun updateAvatar(avatarUrl: String) = withContext(ioDispatcher) {
        val current = _studentProfile.value
        _studentProfile.value = current.copy(avatarUrl = avatarUrl.trim())
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
    }

    suspend fun clearStatusNote() = withContext(ioDispatcher) {
        val current = _studentProfile.value
        _studentProfile.value = current.copy(
            statusNote = null,
            statusNoteEmoji = null,
            statusNoteExpiry = null
        )
    }

    suspend fun addProfile(newProfile: StudentProfile) = withContext(ioDispatcher) {
        val current = _studentProfile.value
        val updatedAlternates = current.alternateProfiles + newProfile
        _studentProfile.value = current.copy(alternateProfiles = updatedAlternates)
    }

    suspend fun switchProfile(targetStudentId: String) = withContext(ioDispatcher) {
        val current = _studentProfile.value
        val target = current.alternateProfiles.find { it.studentId == targetStudentId }
        if (target != null) {
            val updatedAlternates = current.alternateProfiles.filterNot { it.studentId == targetStudentId } + current
            _studentProfile.value = target.copy(alternateProfiles = updatedAlternates)
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
    }

    suspend fun clearCache(): Long = withContext(ioDispatcher) {
        val current = _studentProfile.value
        val freedBytes = 38_500_000L // 38.5 MB cleared
        val newStorage = (current.storageUsedBytes - freedBytes).coerceAtLeast(85_000_000L)
        _studentProfile.value = current.copy(storageUsedBytes = newStorage)
        freedBytes
    }

    suspend fun upgradeStorage(additionalBytes: Long = 1_073_741_824L) = withContext(ioDispatcher) {
        val current = _studentProfile.value
        _studentProfile.value = current.copy(storageQuotaBytes = current.storageQuotaBytes + additionalBytes)
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
    }
}
