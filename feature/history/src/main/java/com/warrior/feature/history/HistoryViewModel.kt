package com.warrior.feature.history

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    getTrainingHistory: GetTrainingHistory,
    private val observeSession: ObserveSession,
    private val deleteTrainingSession: DeleteTrainingSession,
) : ViewModel() {

    data class DayGroup(
        val key: Long,
        val sessions: List<TrainingSession>,
    )

    data class UiState(
        val groups: List<DayGroup> = emptyList(),
        val loaded: Boolean = false,
        val pendingDelete: Long? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession()
                .flatMapLatest { userId ->
                    if (userId == null) flowOf(emptyList()) else getTrainingHistory(userId)
                }
                .collect { sessions ->
                    _state.update {
                        it.copy(
                            groups = sessions
                                .groupBy { session -> session.date }
                                .toSortedMap(compareByDescending { key -> key })
                                .map { (date, list) ->
                                    DayGroup(
                                        key = date,
                                        sessions = list.sortedByDescending { it.createdAt },
                                    )
                                },
                            loaded = true,
                        )
                    }
                }
        }
    }

    fun onDeleteRequest(sessionId: Long) = _state.update { it.copy(pendingDelete = sessionId) }

    fun onDismissDelete() = _state.update { it.copy(pendingDelete = null) }

    fun onConfirmDelete() {
        val sessionId = _state.value.pendingDelete ?: return
        viewModelScope.launch {
            val userId = observeSession().first()
            if (userId != null) deleteTrainingSession(userId, sessionId)
            _state.update { it.copy(pendingDelete = null) }
        }
    }
}
