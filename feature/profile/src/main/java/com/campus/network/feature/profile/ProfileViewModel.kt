package com.campus.network.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.network.core.data.StudentProfileRepository
import com.campus.network.core.model.StudentProfile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: StudentProfile? = null,
    val isLoading: Boolean = false,
    val isDeleted: Boolean = false
)

sealed interface ProfileEvent {
    data class ClaimQuest(val questId: String) : ProfileEvent
    data class SetStatusNote(val note: String, val emoji: String, val expiry: String) : ProfileEvent
    data object ClearStatusNote : ProfileEvent
    data class UpdateProfile(
        val fullName: String,
        val universityName: String,
        val degreeProgram: String,
        val academicYear: String,
        val campusResidence: String,
        val phoneNumber: String,
        val bio: String,
        val interests: List<String>
    ) : ProfileEvent
    data class UpdateAvatar(val avatarUrl: String) : ProfileEvent
    data class AddProfile(val profile: StudentProfile) : ProfileEvent
    data class SwitchProfile(val studentId: String) : ProfileEvent
    data class UpdateNotifications(
        val notificationsEnabled: Boolean,
        val escrowAlertsEnabled: Boolean,
        val questRemindersEnabled: Boolean
    ) : ProfileEvent
    data class UpdatePrivacy(
        val isTrustScorePublic: Boolean,
        val isOnlineStatusVisible: Boolean,
        val activityLedgerVisibility: String
    ) : ProfileEvent
    data object ClearCache : ProfileEvent
    data object UpgradeStorage : ProfileEvent
    data class DeleteAccount(val reason: String) : ProfileEvent
}

class ProfileViewModel(
    private val repository: StudentProfileRepository
) : ViewModel() {

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    val uiState: StateFlow<ProfileUiState> = repository.getProfile()
        .map { profile -> ProfileUiState(profile = profile, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfileUiState(isLoading = true)
        )

    fun onEvent(event: ProfileEvent) {
        viewModelScope.launch {
            when (event) {
                is ProfileEvent.ClaimQuest -> {
                    repository.completeQuest(event.questId)
                    _userMessage.emit("Quest rewards claimed!")
                }
                is ProfileEvent.SetStatusNote -> {
                    repository.setStatusNote(event.note, event.emoji, event.expiry)
                    _userMessage.emit("Status note updated")
                }
                is ProfileEvent.ClearStatusNote -> {
                    repository.clearStatusNote()
                    _userMessage.emit("Status note cleared")
                }
                is ProfileEvent.UpdateProfile -> {
                    repository.updateProfile(
                        fullName = event.fullName,
                        universityName = event.universityName,
                        degreeProgram = event.degreeProgram,
                        academicYear = event.academicYear,
                        campusResidence = event.campusResidence,
                        phoneNumber = event.phoneNumber,
                        bio = event.bio,
                        interests = event.interests
                    )
                    _userMessage.emit("Profile details saved successfully")
                }
                is ProfileEvent.UpdateAvatar -> {
                    repository.updateAvatar(event.avatarUrl)
                    _userMessage.emit("Profile avatar updated successfully")
                }
                is ProfileEvent.AddProfile -> {
                    repository.addProfile(event.profile)
                    _userMessage.emit("Added secondary student persona: ${event.profile.personaRole}")
                }
                is ProfileEvent.SwitchProfile -> {
                    repository.switchProfile(event.studentId)
                    _userMessage.emit("Switched active persona")
                }
                is ProfileEvent.UpdateNotifications -> {
                    repository.updateNotificationSettings(
                        notificationsEnabled = event.notificationsEnabled,
                        escrowAlertsEnabled = event.escrowAlertsEnabled,
                        questRemindersEnabled = event.questRemindersEnabled
                    )
                    _userMessage.emit("Notification preferences updated")
                }
                is ProfileEvent.UpdatePrivacy -> {
                    repository.updatePrivacySettings(
                        isTrustScorePublic = event.isTrustScorePublic,
                        isOnlineStatusVisible = event.isOnlineStatusVisible,
                        activityLedgerVisibility = event.activityLedgerVisibility
                    )
                    _userMessage.emit("Privacy settings updated")
                }
                is ProfileEvent.ClearCache -> {
                    val freed = repository.clearCache()
                    val freedMb = String.format("%.1f", freed / (1024f * 1024f))
                    _userMessage.emit("Freed $freedMb MB of local temporary cached media")
                }
                is ProfileEvent.UpgradeStorage -> {
                    repository.upgradeStorage()
                    _userMessage.emit("Upgraded storage by +1 GB! New Quota: 2 GB")
                }
                is ProfileEvent.DeleteAccount -> {
                    repository.deleteAccount(event.reason)
                    _userMessage.emit("Account has been permanently scheduled for deletion.")
                }
            }
        }
    }
}
