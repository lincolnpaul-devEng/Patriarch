package com.campus.network.feature.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.network.core.data.DeltaSyncManager
import com.campus.network.core.data.StudentProfileRepository
import com.campus.network.core.model.StudentProfile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: StudentProfile? = null,
    val isLoading: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val isAccountDeleted: Boolean = false,
    val deletionProgress: Float = 0f,
    val deletionStageText: String = ""
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
        val interests: List<String>,
        val avatarUrl: String? = null
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
    data class DepositWallet(val amount: Double) : ProfileEvent
    data class WithdrawWallet(val amount: Double) : ProfileEvent
    data class ReleaseVault(val amount: Double) : ProfileEvent
    data object SignOut : ProfileEvent
}

class ProfileViewModel(
    context: Context,
    private val repository: StudentProfileRepository
) : ViewModel() {

    private val deltaSyncManager = DeltaSyncManager.get(context)

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _isDeletingAccount = MutableStateFlow(false)
    private val _isAccountDeleted = MutableStateFlow(false)
    private val _deletionProgress = MutableStateFlow(0f)
    private val _deletionStageText = MutableStateFlow("")

    val uiState: StateFlow<ProfileUiState> = combine(
        repository.getProfile(),
        _isDeletingAccount,
        _isAccountDeleted,
        _deletionProgress,
        _deletionStageText
    ) { profile, isDeleting, isDeleted, progress, stageText ->
        ProfileUiState(
            profile = profile,
            isLoading = false,
            isDeletingAccount = isDeleting,
            isAccountDeleted = isDeleted,
            deletionProgress = progress,
            deletionStageText = stageText
        )
    }.stateIn(
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
                    val result = repository.setStatusNote(event.note, event.emoji, event.expiry)
                    result.fold(
                        onSuccess = { _userMessage.emit("Status note updated") },
                        onFailure = { error ->
                            _userMessage.emit("Failed to update status note: ${error.localizedMessage ?: "Network error"}")
                        }
                    )
                }
                is ProfileEvent.ClearStatusNote -> {
                    val result = repository.clearStatusNote()
                    result.fold(
                        onSuccess = { _userMessage.emit("Status note cleared") },
                        onFailure = { error ->
                            _userMessage.emit("Failed to clear status note: ${error.localizedMessage ?: "Network error"}")
                        }
                    )
                }
                is ProfileEvent.UpdateProfile -> {
                    val result = repository.updateProfile(
                        fullName = event.fullName,
                        universityName = event.universityName,
                        degreeProgram = event.degreeProgram,
                        academicYear = event.academicYear,
                        campusResidence = event.campusResidence,
                        phoneNumber = event.phoneNumber,
                        bio = event.bio,
                        interests = event.interests,
                        avatarUrl = event.avatarUrl
                    )
                    _userMessage.emit(saveFeedback(result, "Profile details and image saved successfully"))
                }
                is ProfileEvent.UpdateAvatar -> {
                    val res = repository.updateAvatar(event.avatarUrl)
                    _userMessage.emit(saveFeedback(res, "Profile avatar updated successfully", "Failed to save avatar"))
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
                    val result = repository.updateNotificationSettings(
                        notificationsEnabled = event.notificationsEnabled,
                        escrowAlertsEnabled = event.escrowAlertsEnabled,
                        questRemindersEnabled = event.questRemindersEnabled
                    )
                    _userMessage.emit(saveFeedback(result, "Notification preferences updated", "Failed to update notification settings"))
                }
                is ProfileEvent.UpdatePrivacy -> {
                    val result = repository.updatePrivacySettings(
                        isTrustScorePublic = event.isTrustScorePublic,
                        isOnlineStatusVisible = event.isOnlineStatusVisible,
                        activityLedgerVisibility = event.activityLedgerVisibility
                    )
                    _userMessage.emit(saveFeedback(result, "Privacy settings updated", "Failed to update privacy settings"))
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
                    _isDeletingAccount.value = true
                    _deletionProgress.value = 0.08f
                    _deletionStageText.value = "Initiating secure deletion & verifying credentials..."

                    val stagesJob = viewModelScope.launch {
                        val stages = listOf(
                            0.22f to "Deleting chat messages, direct threads & reactions...",
                            0.45f to "Reconciling community servers & transferring memberships...",
                            0.65f to "Clearing marketplace listings, bids & campus wallet...",
                            0.80f to "Purging student profile, avatar & storage assets...",
                            0.92f to "Wiping local session cache & security tokens..."
                        )
                        for ((prog, text) in stages) {
                            kotlinx.coroutines.delay(450)
                            if (!_isDeletingAccount.value) break
                            _deletionProgress.value = prog
                            _deletionStageText.value = text
                        }
                    }

                    val result = repository.deleteAccount(event.reason)
                    stagesJob.cancel()

                    if (result.isSuccess) {
                        _deletionProgress.value = 1.0f
                        _deletionStageText.value = "Account deleted permanently."
                        kotlinx.coroutines.delay(650)
                        _isDeletingAccount.value = false
                        _isAccountDeleted.value = true
                        _userMessage.emit("Account deleted permanently.")
                    } else {
                        _isDeletingAccount.value = false
                        _deletionProgress.value = 0f
                        _deletionStageText.value = ""
                        _userMessage.emit(
                            "Deletion failed: ${result.exceptionOrNull()?.message ?: "try again later"}"
                        )
                    }
                }
                is ProfileEvent.DepositWallet -> {
                    val success = repository.depositToWallet(event.amount)
                    if (success) {
                        _userMessage.emit("Successfully deposited KES ${String.format("%.2f", event.amount)} to wallet")
                    } else {
                        _userMessage.emit("Deposit failed: invalid amount")
                    }
                }
                is ProfileEvent.WithdrawWallet -> {
                    val success = repository.withdrawFromWallet(event.amount)
                    if (success) {
                        _userMessage.emit("Successfully withdrawn KES ${String.format("%.2f", event.amount)} to M-Pesa")
                    } else {
                        _userMessage.emit("Withdrawal failed: insufficient available balance")
                    }
                }
                is ProfileEvent.ReleaseVault -> {
                    val success = repository.releaseVaultAmount(event.amount)
                    if (success) {
                        _userMessage.emit("Released KES ${String.format("%.2f", event.amount)} from SafePay Vault to wallet")
                    } else {
                        _userMessage.emit("Failed to release vault funds")
                    }
                }
                is ProfileEvent.SignOut -> {
                    _isDeletingAccount.value = false
                    _isAccountDeleted.value = false
                    _deletionProgress.value = 0f
                    _deletionStageText.value = ""
                    repository.signOut()
                    _userMessage.emit("Signed out of Patriarch")
                }
            }
        }
    }

    fun resetDeletionState() {
        _isDeletingAccount.value = false
        _isAccountDeleted.value = false
        _deletionProgress.value = 0f
        _deletionStageText.value = ""
    }

    suspend fun retryFailedUploads() {
        repository.markFailedWritesPending()
        _userMessage.emit("Retry queued. Profile upload will resume when connected.")
    }

    private suspend fun saveFeedback(
        result: Result<Unit>,
        successMessage: String,
        failureMessage: String = "Save failed"
    ): String {
        val profile = repository.getProfileSync()
        return when (profile?.syncStatus) {
            "PENDING" -> "Saved on this device; upload pending."
            "FAILED" -> "Saved on this device but sync failed. Tap to Retry."
            else -> result.fold(
                onSuccess = { successMessage },
                onFailure = { error ->
                    "$failureMessage: ${error.localizedMessage ?: "Please try again."}"
                }
            )
        }
    }

    suspend fun saveProfile(
        fullName: String,
        universityName: String,
        degreeProgram: String,
        academicYear: String,
        campusResidence: String,
        phoneNumber: String,
        bio: String,
        interests: List<String>,
        avatarUrl: String?
    ): Result<Unit> {
        val result = repository.updateProfile(
            fullName = fullName,
            universityName = universityName,
            degreeProgram = degreeProgram,
            academicYear = academicYear,
            campusResidence = campusResidence,
            phoneNumber = phoneNumber,
            bio = bio,
            interests = interests,
            avatarUrl = avatarUrl
        )
        _userMessage.emit(saveFeedback(result, "Profile details and image saved successfully", "Profile update failed"))
        return result
    }
}
