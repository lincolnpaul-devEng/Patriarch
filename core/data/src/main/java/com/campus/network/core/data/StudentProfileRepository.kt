package com.campus.network.core.data

import com.campus.network.core.model.BadgeLevel
import com.campus.network.core.model.ConsistencyQuest
import com.campus.network.core.model.QuestCategory
import com.campus.network.core.model.StudentProfile
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
            studentId = "ST-2024-8842",
            fullName = "Lincoln Kiprotich",
            universityEmail = "l.kiprotich@strathmore.edu",
            universityName = "Strathmore University",
            degreeProgram = "B.Sc. Informatics & Computer Science",
            academicYear = "3rd Year (Semester 1)",
            consistencyStreakDays = 24,
            behavioralTrustScore = 96,
            verifiedSkillBadges = listOf(
                VerifiedSkillBadge(
                    id = "badge-1",
                    name = "Jetpack Compose Architect",
                    facultyVerifier = "Dr. Odhiambo (School of Computing)",
                    verifiedDate = "Sep 2026",
                    endorsementCount = 18,
                    level = BadgeLevel.GOLD
                ),
                VerifiedSkillBadge(
                    id = "badge-2",
                    name = "Kotlin Coroutines & Flow Guru",
                    facultyVerifier = "Prof. Mwangi (Software Eng)",
                    verifiedDate = "Aug 2026",
                    endorsementCount = 27,
                    level = BadgeLevel.CAMPUS_MASTER
                ),
                VerifiedSkillBadge(
                    id = "badge-3",
                    name = "Peer Escrow Arbitrator",
                    facultyVerifier = "Student Guild Commerce Council",
                    verifiedDate = "Jul 2026",
                    endorsementCount = 12,
                    level = BadgeLevel.SILVER
                )
            ),
            privateQuestDeck = listOf(
                ConsistencyQuest(
                    id = "quest-1",
                    title = "3-Day Focus Library Streak",
                    description = "Check in to campus library / study lounge for at least 45 minutes across 3 consecutive days.",
                    xpReward = 150,
                    currentProgress = 2,
                    targetProgress = 3,
                    isCompleted = false,
                    category = QuestCategory.FOCUS_STREAK
                ),
                ConsistencyQuest(
                    id = "quest-2",
                    title = "Academic Workspace Helper",
                    description = "Answer 2 peer questions in your enrolled course code threads (#cs-201 or #cs-305).",
                    xpReward = 200,
                    currentProgress = 2,
                    targetProgress = 2,
                    isCompleted = true,
                    category = QuestCategory.ACADEMIC_PEER_HELP
                ),
                ConsistencyQuest(
                    id = "quest-3",
                    title = "Zero-Dispute Marketplace Settler",
                    description = "Complete 3 peer transactions via inline settlement cards with 100% mutual confirmation.",
                    xpReward = 300,
                    currentProgress = 2,
                    targetProgress = 3,
                    isCompleted = false,
                    category = QuestCategory.COMMERCE_RELIABILITY
                ),
                ConsistencyQuest(
                    id = "quest-4",
                    title = "Campus Meet Host",
                    description = "Organize a flash study-jam or hack meetup attended by at least 3 verified students.",
                    xpReward = 250,
                    currentProgress = 1,
                    targetProgress = 1,
                    isCompleted = true,
                    category = QuestCategory.COMMUNITY_ENGAGEMENT
                )
            )
        )
    )

    fun getProfile(): Flow<StudentProfile> = _studentProfile.asStateFlow()

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
}
