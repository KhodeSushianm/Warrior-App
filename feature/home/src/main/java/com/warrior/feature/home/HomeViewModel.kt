package com.warrior.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warrior.core.common.time.DisplayCalendar
import com.warrior.domain.auth.AppPreferences
import com.warrior.domain.auth.usecase.ObserveSession
import com.warrior.domain.progress.model.HomeProgress
import com.warrior.domain.progress.usecase.ObserveHomeProgress
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
 * Home dashboard (Phase 7): live-derived weekly stats, deltas vs last week,
 * streak, personal records and recent sessions — all from one consistent
 * [HomeProgress] snapshot per Room emission. Nothing is stored (§13.4).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeSession: ObserveSession,
    observeHomeProgress: ObserveHomeProgress,
    appPreferences: AppPreferences,
) : ViewModel() {

    data class UiState(
        val loaded: Boolean = false,
        val progress: HomeProgress? = null,
        val calendar: DisplayCalendar = DisplayCalendar.GREGORIAN,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(observeSession(), appPreferences.calendar) { userId, cal -> userId to cal }
                .flatMapLatest { (userId, cal) ->
                    val jalali = cal == AppPreferences.CALENDAR_JALALI
                    if (userId == null) {
                        flowOf(null to cal)
                    } else {
                        observeHomeProgress(userId, jalali).map { progress -> progress to cal }
                    }
                }
                .collect { (progress, cal) ->
                    _state.update {
                        it.copy(
                            loaded = true,
                            progress = progress,
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
}
