package com.warrior.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warrior.core.common.time.DateFormats
import com.warrior.domain.auth.usecase.ObserveSession
import com.warrior.domain.progress.model.HomeProgress
import com.warrior.domain.progress.usecase.ObserveHomeProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
) : ViewModel() {

    data class UiState(
        val loaded: Boolean = false,
        val progress: HomeProgress? = null,
        val weekRangeLabel: String = "",
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession()
                .flatMapLatest { userId ->
                    if (userId == null) flowOf(null) else observeHomeProgress(userId)
                }
                .collect { progress ->
                    _state.update {
                        it.copy(
                            loaded = true,
                            progress = progress,
                            weekRangeLabel = progress?.let { p ->
                                DateFormats.weekRange(p.thisWeek.weekStart, p.thisWeek.weekEndExclusive)
                            } ?: "",
                        )
                    }
                }
        }
    }
}
