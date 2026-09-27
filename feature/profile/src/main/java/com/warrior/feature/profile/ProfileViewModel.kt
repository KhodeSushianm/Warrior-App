package com.warrior.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warrior.domain.auth.DuplicateUsernameException
import com.warrior.domain.auth.LocalAccount
import com.warrior.domain.auth.usecase.GetAccount
import com.warrior.domain.auth.usecase.Logout
import com.warrior.domain.auth.usecase.ObserveSession
import com.warrior.domain.auth.usecase.UpdateAccount
import com.warrior.domain.auth.validation.AuthErrorCode
import com.warrior.domain.auth.validation.AuthValidationException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Profile (Phase 9): identity display, edit (displayName + username with
 * on-device uniqueness), about info and logout. Errors surface as stable
 * [AuthErrorCode]s; the screen maps them to string resources (i18n-ready).
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val observeSession: ObserveSession,
    private val getAccount: GetAccount,
    private val updateAccount: UpdateAccount,
    private val logout: Logout,
) : ViewModel() {

    data class UiState(
        val account: LocalAccount? = null,
        val showEditDialog: Boolean = false,
        val editDisplayName: String = "",
        val editUsername: String = "",
        val editErrorCodes: List<AuthErrorCode> = emptyList(),
        val isSaving: Boolean = false,
        val showAbout: Boolean = false,
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

    fun onEditOpen() {
        val account = _state.value.account ?: return
        _state.update {
            it.copy(
                showEditDialog = true,
                editDisplayName = account.displayName,
                editUsername = account.username,
                editErrorCodes = emptyList(),
            )
        }
    }

    fun onEditDisplayNameChange(value: String) =
        _state.update { it.copy(editDisplayName = value, editErrorCodes = emptyList()) }

    fun onEditUsernameChange(value: String) =
        _state.update { it.copy(editUsername = value, editErrorCodes = emptyList()) }

    fun onEditCancel() = _state.update { it.copy(showEditDialog = false, editErrorCodes = emptyList()) }

    fun onSaveProfile() {
        if (_state.value.isSaving) return
        viewModelScope.launch {
            val userId = observeSession().first()
            if (userId == null) {
                _state.update { it.copy(editErrorCodes = listOf(AuthErrorCode.UNEXPECTED)) }
                return@launch
            }
            _state.update { it.copy(isSaving = true, editErrorCodes = emptyList()) }
            val snapshot = _state.value
            updateAccount(userId, snapshot.editDisplayName, snapshot.editUsername)
                .onSuccess {
                    // Refresh identity from the repository (single source of truth).
                    val account = getAccount(userId)
                    _state.update {
                        it.copy(isSaving = false, showEditDialog = false, account = account)
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isSaving = false, editErrorCodes = codesFor(error)) }
                }
        }
    }

    fun onAboutOpen() = _state.update { it.copy(showAbout = true) }

    fun onAboutClose() = _state.update { it.copy(showAbout = false) }

    fun onLogout() {
        viewModelScope.launch { logout() }
    }

    private fun codesFor(error: Throwable): List<AuthErrorCode> = when (error) {
        is AuthValidationException -> error.codes.ifEmpty { listOf(AuthErrorCode.UNEXPECTED) }
        is DuplicateUsernameException -> listOf(AuthErrorCode.DUPLICATE_USERNAME)
        else -> listOf(AuthErrorCode.UNEXPECTED)
    }
}
