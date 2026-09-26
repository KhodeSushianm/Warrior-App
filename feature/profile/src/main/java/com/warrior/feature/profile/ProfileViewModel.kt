package com.warrior.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warrior.domain.auth.LocalAccount
import com.warrior.domain.auth.usecase.GetAccount
import com.warrior.domain.auth.usecase.Logout
import com.warrior.domain.auth.usecase.ObserveSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeSession: ObserveSession,
    private val getAccount: GetAccount,
    private val logout: Logout,
) : ViewModel() {

    data class UiState(
        val account: LocalAccount? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { userId ->
                val account = userId?.let { getAccount(it) }
                _state.update { it.copy(account = account) }
            }
        }
    }

    fun onLogout() {
        viewModelScope.launch { logout() }
    }
}
