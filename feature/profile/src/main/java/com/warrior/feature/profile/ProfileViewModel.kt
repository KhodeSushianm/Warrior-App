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
import com.warrior.domain.training.BackupFormatException
import com.warrior.domain.training.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Profile (Phase 9 + Season 2 Phase 12): identity display/edit, about info,
 * logout, and the whole-device backup flows (export payload / import with
 * explicit confirmation). File I/O (SAF streams) stays in the screen layer so
 * the VM remains plain-JVM testable.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val observeSession: ObserveSession,
    private val getAccount: GetAccount,
    private val updateAccount: UpdateAccount,
    private val logout: Logout,
    private val backupRepository: BackupRepository,
) : ViewModel() {

    sealed interface ImportResult {
        data class Success(val sessionsRestored: Int) : ImportResult
        data object Invalid : ImportResult
        data object Failed : ImportResult
    }

    data class UiState(
        val account: LocalAccount? = null,
        val showEditDialog: Boolean = false,
        val editDisplayName: String = "",
        val editUsername: String = "",
        val editErrorCodes: List<AuthErrorCode> = emptyList(),
        val isSaving: Boolean = false,
        val showAbout: Boolean = false,
        val isExporting: Boolean = false,
        // null = nothing to report; true/false = export result
        val exportDone: Boolean? = null,
        val showImportConfirm: Boolean = false,
        val isImporting: Boolean = false,
        val importResult: ImportResult? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Held between confirm and execution — deliberately not part of UiState. */
    private var pendingImportJson: String? = null

    init {
        viewModelScope.launch {
            observeSession().collect { userId ->
                val account = userId?.let { getAccount(it) }
                _state.update { it.copy(account = account) }
            }
        }
    }

    // ---------- identity ----------

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

    // ---------- backup (Season 2 / Phase 12) ----------

    fun onExportStarted() = _state.update { it.copy(isExporting = true) }

    /** Builds the backup payload; the screen writes it to the SAF uri. */
    suspend fun exportPayload(): String? = try {
        backupRepository.exportAll()
    } catch (e: Exception) {
        null
    }

    fun onExportFinished(success: Boolean) =
        _state.update { it.copy(isExporting = false, exportDone = success) }

    fun onExportCancelled() = _state.update { it.copy(isExporting = false) }

    fun onExportResultDismiss() = _state.update { it.copy(exportDone = null) }

    /** Screen read the picked file; ask for explicit confirmation. */
    fun onImportJson(text: String?) {
        if (text.isNullOrBlank()) {
            _state.update { it.copy(importResult = ImportResult.Invalid) }
            return
        }
        pendingImportJson = text
        _state.update { it.copy(showImportConfirm = true) }
    }

    fun onImportCancel() = _state.update { it.copy(showImportConfirm = false) }

    fun onImportConfirm() {
        val payload = pendingImportJson ?: return
        if (_state.value.isImporting) return
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true, showImportConfirm = false) }
            try {
                val restored = backupRepository.importAll(payload)
                _state.update { it.copy(isImporting = false, importResult = ImportResult.Success(restored)) }
            } catch (e: BackupFormatException) {
                _state.update { it.copy(isImporting = false, importResult = ImportResult.Invalid) }
            } catch (e: Exception) {
                _state.update { it.copy(isImporting = false, importResult = ImportResult.Failed) }
            }
        }
    }

    /**
     * After a successful restore the old session may point at a user that no
     * longer exists — sign out so the user logs in with a restored account.
     */
    fun onImportResultDismiss() {
        val wasSuccess = _state.value.importResult is ImportResult.Success
        _state.update { it.copy(importResult = null) }
        if (wasSuccess) {
            viewModelScope.launch { logout() }
        }
    }

    private fun codesFor(error: Throwable): List<AuthErrorCode> = when (error) {
        is AuthValidationException -> error.codes.ifEmpty { listOf(AuthErrorCode.UNEXPECTED) }
        is DuplicateUsernameException -> listOf(AuthErrorCode.DUPLICATE_USERNAME)
        else -> listOf(AuthErrorCode.UNEXPECTED)
    }
}
