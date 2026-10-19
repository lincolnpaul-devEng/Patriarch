package com.campus.network.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.campus.network.core.model.LocationPrivacyScope
import com.campus.network.core.model.OnboardingPreferences
import com.campus.network.core.model.StudentProfile
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "campusprofile")
data class StudentProfileEntity(
    @PrimaryKey val id: String = "primary_profile",
    val studentId: String = "",
    val fullName: String = "",
    val avatarUrl: String? = null,
    val universityEmail: String = "",
    val universityName: String = "",
    val degreeProgram: String = "",
    val academicYear: String = "",
    val campusResidence: String = "",
    val phoneNumber: String = "",
    val bio: String = "",
    val consistencyStreakDays: Int = 0,
    val behavioralTrustScore: Int = 50,
    val campusMeritPoints: Int = 0,
    val preferredRendezvousPoint: String = "",
    val personaRole: String = "Undergraduate Student",
    val statusNote: String? = null,
    val statusNoteEmoji: String? = null,
    val statusNoteExpiry: String? = null,
    val storageUsedBytes: Long = 0L,
    val storageQuotaBytes: Long = 1073741824L,
    val notificationsEnabled: Boolean = true,
    val escrowAlertsEnabled: Boolean = true,
    val questRemindersEnabled: Boolean = true,
    val isTrustScorePublic: Boolean = true,
    val isOnlineStatusVisible: Boolean = true,
    val activityLedgerVisibility: String = "Public to Campus",
    val locationPrivacyScope: String = "VERIFIED_CAMPUS_PEERS",
    val walletBalance: Double = 0.0,
    val vaultProtectedBalance: Double = 0.0,
    val onboardingPreferencesJson: String = "",
    val interestsCsv: String = "",
    val savedListingIdsCsv: String = "",
    val watchlistIdsCsv: String = ""
) {
    fun toDomain(): StudentProfile {
        val interestsList = if (interestsCsv.isBlank()) emptyList() else interestsCsv.split(";;").map { it.trim() }.filter { it.isNotEmpty() }
        val savedList = if (savedListingIdsCsv.isBlank()) emptyList() else savedListingIdsCsv.split(";;").map { it.trim() }.filter { it.isNotEmpty() }
        val watchlist = if (watchlistIdsCsv.isBlank()) emptyList() else watchlistIdsCsv.split(";;").map { it.trim() }.filter { it.isNotEmpty() }
        val scope = runCatching { LocationPrivacyScope.valueOf(locationPrivacyScope) }.getOrDefault(LocationPrivacyScope.VERIFIED_CAMPUS_PEERS)
        val deserializedPrefs = deserializeOnboardingPreferences(onboardingPreferencesJson)

        return StudentProfile(
            studentId = studentId,
            fullName = fullName,
            avatarUrl = avatarUrl,
            universityEmail = universityEmail,
            universityName = universityName,
            degreeProgram = degreeProgram,
            academicYear = academicYear,
            campusResidence = campusResidence,
            phoneNumber = phoneNumber,
            bio = bio,
            consistencyStreakDays = consistencyStreakDays,
            behavioralTrustScore = behavioralTrustScore,
            campusMeritPoints = campusMeritPoints,
            preferredRendezvousPoint = preferredRendezvousPoint,
            savedListingIds = savedList,
            watchlistIds = watchlist,
            personaRole = personaRole,
            statusNote = statusNote,
            statusNoteEmoji = statusNoteEmoji,
            statusNoteExpiry = statusNoteExpiry,
            storageUsedBytes = storageUsedBytes,
            storageQuotaBytes = storageQuotaBytes,
            notificationsEnabled = notificationsEnabled,
            escrowAlertsEnabled = escrowAlertsEnabled,
            questRemindersEnabled = questRemindersEnabled,
            isTrustScorePublic = isTrustScorePublic,
            isOnlineStatusVisible = isOnlineStatusVisible,
            activityLedgerVisibility = activityLedgerVisibility,
            locationPrivacyScope = scope,
            walletBalance = walletBalance,
            vaultProtectedBalance = vaultProtectedBalance,
            interests = interestsList,
            verifiedSkillBadges = emptyList(),
            privateQuestDeck = emptyList(),
            alternateProfiles = emptyList(),
            onboardingPreferences = deserializedPrefs
        )
    }

    companion object {
        fun serializeOnboardingPreferences(prefs: OnboardingPreferences): String {
            val obj = JSONObject()
            obj.put("languageCode", prefs.languageCode)
            obj.put("countryCode", prefs.countryCode)
            obj.put("countryName", prefs.countryName)
            obj.put("currencyCode", prefs.currencyCode)
            obj.put("regionName", prefs.regionName)
            obj.put("universityName", prefs.universityName)
            obj.put("universityDomain", prefs.universityDomain)
            obj.put("isCustomUniversity", prefs.isCustomUniversity)
            obj.put("isVerifiedStudentEmail", prefs.isVerifiedStudentEmail)
            obj.put("faculty", prefs.faculty)
            obj.put("yearOfStudy", prefs.yearOfStudy)
            obj.put("intents", JSONArray(prefs.intents))
            obj.put("favoriteCategories", JSONArray(prefs.favoriteCategories))
            obj.put("priorities", JSONArray(prefs.priorities))
            obj.put("pushNotificationsOptIn", prefs.pushNotificationsOptIn)
            obj.put("personalizedNewsConsent", prefs.personalizedNewsConsent)
            obj.put("hasCompletedSurvey", prefs.hasCompletedSurvey)
            return obj.toString()
        }

        fun deserializeOnboardingPreferences(json: String): OnboardingPreferences {
            if (json.isBlank()) return OnboardingPreferences()
            return runCatching {
                val obj = JSONObject(json)
                val intentsArray = obj.optJSONArray("intents")
                val intentsList = if (intentsArray != null) {
                    (0 until intentsArray.length()).map { intentsArray.getString(it) }
                } else emptyList()
                val catArray = obj.optJSONArray("favoriteCategories")
                val catList = if (catArray != null) {
                    (0 until catArray.length()).map { catArray.getString(it) }
                } else emptyList()
                val priArray = obj.optJSONArray("priorities")
                val priList = if (priArray != null) {
                    (0 until priArray.length()).map { priArray.getString(it) }
                } else emptyList()
                OnboardingPreferences(
                    languageCode = obj.optString("languageCode", "en"),
                    countryCode = obj.optString("countryCode", "KE"),
                    countryName = obj.optString("countryName", "Kenya"),
                    currencyCode = obj.optString("currencyCode", "KES"),
                    regionName = obj.optString("regionName", "Nairobi"),
                    universityName = obj.optString("universityName", "Strathmore University"),
                    universityDomain = obj.optString("universityDomain", "strathmore.edu"),
                    isCustomUniversity = obj.optBoolean("isCustomUniversity", false),
                    isVerifiedStudentEmail = obj.optBoolean("isVerifiedStudentEmail", false),
                    faculty = obj.optString("faculty", "Computing & Informatics"),
                    yearOfStudy = obj.optString("yearOfStudy", "Year 3"),
                    intents = intentsList,
                    favoriteCategories = catList,
                    priorities = priList,
                    pushNotificationsOptIn = obj.optBoolean("pushNotificationsOptIn", false),
                    personalizedNewsConsent = obj.optBoolean("personalizedNewsConsent", true),
                    hasCompletedSurvey = obj.optBoolean("hasCompletedSurvey", false)
                )
            }.getOrDefault(OnboardingPreferences())
        }

        fun fromDomain(domain: StudentProfile, id: String = "primary_profile"): StudentProfileEntity {
            return StudentProfileEntity(
                id = id,
                studentId = domain.studentId,
                fullName = domain.fullName,
                avatarUrl = domain.avatarUrl,
                universityEmail = domain.universityEmail,
                universityName = domain.universityName,
                degreeProgram = domain.degreeProgram,
                academicYear = domain.academicYear,
                campusResidence = domain.campusResidence,
                phoneNumber = domain.phoneNumber,
                bio = domain.bio,
                consistencyStreakDays = domain.consistencyStreakDays,
                behavioralTrustScore = domain.behavioralTrustScore,
                campusMeritPoints = domain.campusMeritPoints,
                preferredRendezvousPoint = domain.preferredRendezvousPoint,
                personaRole = domain.personaRole,
                statusNote = domain.statusNote,
                statusNoteEmoji = domain.statusNoteEmoji,
                statusNoteExpiry = domain.statusNoteExpiry,
                storageUsedBytes = domain.storageUsedBytes,
                storageQuotaBytes = domain.storageQuotaBytes,
                notificationsEnabled = domain.notificationsEnabled,
                escrowAlertsEnabled = domain.escrowAlertsEnabled,
                questRemindersEnabled = domain.questRemindersEnabled,
                isTrustScorePublic = domain.isTrustScorePublic,
                isOnlineStatusVisible = domain.isOnlineStatusVisible,
                activityLedgerVisibility = domain.activityLedgerVisibility,
                locationPrivacyScope = domain.locationPrivacyScope.name,
                walletBalance = domain.walletBalance,
                vaultProtectedBalance = domain.vaultProtectedBalance,
                onboardingPreferencesJson = serializeOnboardingPreferences(domain.onboardingPreferences),
                interestsCsv = domain.interests.joinToString(";;"),
                savedListingIdsCsv = domain.savedListingIds.joinToString(";;"),
                watchlistIdsCsv = domain.watchlistIds.joinToString(";;")
            )
        }
    }
}
