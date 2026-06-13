package cz.calmmoney.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.calmmoney.data.repo.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface RootUiState {
    data object Loading : RootUiState
    data object Onboarding : RootUiState
    data object Ready : RootUiState
}

@HiltViewModel
class RootViewModel @Inject constructor(
    accountRepository: AccountRepository,
) : ViewModel() {
    val state: StateFlow<RootUiState> = accountRepository.observeActive()
        .map { if (it.isEmpty()) RootUiState.Onboarding else RootUiState.Ready }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootUiState.Loading)
}
