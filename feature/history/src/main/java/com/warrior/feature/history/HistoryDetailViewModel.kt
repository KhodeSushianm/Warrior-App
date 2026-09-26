package com.warrior.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warrior.domain.auth.usecase.ObserveSession
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.usecase.DeleteTrainingSession
import com.warrior.domain.training.usecase.GetTrainingHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Live detail: derives from the history flow so edits/deletes elsewhere
 * refresh this screen automatically.
 */
@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    getTrainingHistory: GetTrainingHistory,
    private val observeSession: ObserveSession,
    private val deleteTrainingSession: DeleteTrainingSession,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    data class UiState(
        val session: TrainingSession? = null,
        val loaded: Boolean = false,
        val showDeleteConfirm: Boolean = false,
        val isDeleted: Boolean = false,
    )

    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession()
                .flatMapLatest { userId ->
                    if (userId == null) {
                        flowOf<TrainingSession?>(null)
                    } else {
                        getTrainingHistory(userId)
                            .map { sessions -> sessions.firstOrNull { it.id == sessionId } }
                    }
                }
                .collect { session ->
                    _state.update { it.copy(session = session, loaded = true) }
                }
        }
    }

    fun onDeleteConfirmRequest(show: Boolean) = _state.update { it.copy(showDeleteConfirm = show) }

    fun onConfirmDelete() {
        viewModelScope.launch {
            val userId = observeSession().first()
            if (userId != null) deleteTrainingSession(userId, sessionId)
            _state.update { it.copy(showDeleteConfirm = false, isDeleted = true) }
        }
    }
}
