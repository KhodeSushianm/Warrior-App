package com.warrior.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warrior.core.common.time.DisplayCalendar
import com.warrior.domain.auth.AppPreferences
import com.warrior.domain.auth.usecase.ObserveSession
import com.warrior.domain.progress.model.ProgressSnapshot
import com.warrior.domain.progress.usecase.ObserveProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Progress screen (Phase 8): the charts render from this live derived flow
 * only — never from stored metrics (§13.4). Volume fractions are normalized
 * against the max week so the UI just maps them onto the bar chart.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProgressViewModel @Inject constructor(
    observeSession: ObserveSession,
    observeProgress: ObserveProgress,
    appPreferences: AppPreferences,
) : ViewModel() {

    data class UiState(
        val loaded: Boolean = false,
        val snapshot: ProgressSnapshot? = null,
        val calendar: DisplayCalendar = DisplayCalendar.GREGORIAN,
        val volumeFractions: List<Float> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(observeSession(), appPreferences.calendar) { userId, cal -> userId to cal }
                .flatMapLatest { (userId, cal) ->
                    if (userId == null) {
                        flowOf(null to cal)
                    } else {
                        observeProgress(userId).map { snapshot -> snapshot to cal }
                    }
                }
                .collect { (snapshot, cal) ->
                    _state.update {
                        it.copy(
                            loaded = true,
                            snapshot = snapshot,
                            calendar = if (cal == AppPreferences.CALENDAR_JALALI) {
                                DisplayCalendar.JALALI
                            } else {
                                DisplayCalendar.GREGORIAN
                            },
                            volumeFractions = snapshot?.let(::volumeFractions) ?: emptyList(),
                        )
                    }
                }
        }
    }

    private fun volumeFractions(snapshot: ProgressSnapshot): List<Float> {
        val series = snapshot.volumeSeries
        val max = series.maxOfOrNull { it.trainingMinutes } ?: 0L
        if (max <= 0L) return List(series.size) { 0f }
        return series.map { it.trainingMinutes.toFloat() / max.toFloat() }
    }
}
