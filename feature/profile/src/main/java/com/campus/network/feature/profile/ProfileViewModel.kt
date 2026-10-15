package com.campus.network.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.network.core.data.StudentProfileRepository
import com.campus.network.core.model.StudentProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: StudentProfile? = null,
    val isLoading: Boolean = false
)

sealed interface ProfileEvent {
    data class ClaimQuest(val questId: String) : ProfileEvent
}

class ProfileViewModel(
    private val repository: StudentProfileRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = repository.getProfile()
        .map { profile -> ProfileUiState(profile = profile, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfileUiState(isLoading = true)
        )

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.ClaimQuest -> {
                viewModelScope.launch {
                    repository.completeQuest(event.questId)
                }
            }
        }
    }
}
