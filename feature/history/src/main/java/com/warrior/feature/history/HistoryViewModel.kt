package com.warrior.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warrior.core.common.time.DisplayCalendar
import com.warrior.domain.auth.AppPreferences
import com.warrior.domain.auth.usecase.ObserveSession
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.usecase.DeleteTrainingSession
import com.warrior.domain.training.usecase.GetTrainingHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    getTrainingHistory: GetTrainingHistory,
    private val observeSession: ObserveSession,
    private val deleteTrainingSession: DeleteTrainingSession,
    appPreferences: AppPreferences,
) : ViewModel() {

    data class DayGroup(
        val key: Long,
        val sessions: List<TrainingSession>,
    )

    data class UiState(
        val groups: List<DayGroup> = emptyList(),
        val loaded: Boolean = false,
        val pendingDelete: Long? = null,
        val calendar: DisplayCalendar = DisplayCalendar.GREGORIAN,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(observeSession(), appPreferences.calendar) { userId, cal -> userId to cal }
                .flatMapLatest { (userId, cal) ->
                    if (userId == null) {
                        flowOf(emptyList<TrainingSession>() to cal)
                    } else {
                        getTrainingHistory(userId).map { sessions -> sessions to cal }
                    }
                }
                .collect { (sessions, cal) ->
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
                            calendar = if (cal == AppPreferences.CALENDAR_JALALI) {
                                DisplayCalendar.JALALI
                            } else {
                                DisplayCalendar.GREGORIAN
                            },
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
