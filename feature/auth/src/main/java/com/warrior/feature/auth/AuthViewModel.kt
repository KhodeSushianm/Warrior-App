package com.warrior.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warrior.domain.auth.DuplicateUsernameException
import com.warrior.domain.auth.InvalidCredentialsException
import com.warrior.domain.auth.usecase.CreateLocalAccount
import com.warrior.domain.auth.usecase.Login
import com.warrior.domain.auth.validation.AuthErrorCode
import com.warrior.domain.auth.validation.AuthValidationException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val createLocalAccount: CreateLocalAccount,
    private val login: Login,
) : ViewModel() {

    enum class Mode { LOGIN, REGISTER }

    data class UiState(
        val mode: Mode = Mode.LOGIN,
        val username: String = "",
        val displayName: String = "",
        val password: String = "",
        val isSubmitting: Boolean = false,
        val errorCodes: List<AuthErrorCode> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onUsernameChange(value: String) = _state.update { it.copy(username = value, errorCodes = emptyList()) }

    fun onDisplayNameChange(value: String) = _state.update { it.copy(displayName = value, errorCodes = emptyList()) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, errorCodes = emptyList()) }

    fun onToggleMode() = _state.update {
        it.copy(
            mode = if (it.mode == Mode.LOGIN) Mode.REGISTER else Mode.LOGIN,
            errorCodes = emptyList(),
        )
    }

    fun onSubmit() {
        if (_state.value.isSubmitting) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorCodes = emptyList()) }
            val snapshot = _state.value
            val result = if (snapshot.mode == Mode.LOGIN) {
                login(snapshot.username, snapshot.password)
            } else {
                createLocalAccount(snapshot.username, snapshot.displayName, snapshot.password)
            }
            result.onFailure { error ->
                _state.update { it.copy(isSubmitting = false, errorCodes = codesFor(error)) }
            }
            // On success the device session starts; root navigation observes it
            // and swaps the tree, so no local navigation state is needed.
        }
    }

    /** Maps failures to stable codes; the screen resolves them via string resources (i18n-ready). */
    private fun codesFor(error: Throwable): List<AuthErrorCode> = when (error) {
        is AuthValidationException -> error.codes.ifEmpty { listOf(AuthErrorCode.UNEXPECTED) }
        is DuplicateUsernameException -> listOf(AuthErrorCode.DUPLICATE_USERNAME)
        is InvalidCredentialsException -> listOf(AuthErrorCode.INVALID_CREDENTIALS)
        else -> listOf(AuthErrorCode.UNEXPECTED)
    }
}
