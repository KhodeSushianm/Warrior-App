package com.warrior.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warrior.core.common.time.TimeUtils
import com.warrior.domain.auth.usecase.ObserveSession
import com.warrior.domain.progress.time.TimeProvider
import com.warrior.domain.training.TimerPreferences
import com.warrior.domain.training.model.Feeling
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.Round
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.model.WorkoutActivity
import com.warrior.domain.training.model.WorkoutType
import com.warrior.domain.training.usecase.CreateTrainingSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

enum class TimerPhase { WORK, REST }

enum class TimerStatus { IDLE, RUNNING, PAUSED, FINISHED }

/**
 * Live round timer (Season 2 / Phase 14) — a tick-driven state machine.
 *
 * The SCREEN drives `tick()` from a ~200ms loop; all timing math lives here
 * against the injected [TimeProvider], which makes the whole flow testable
 * with a fake clock (no real delays in tests). Anchor-based timing means the
 * countdown never drifts: remaining = anchorRemaining − (now − anchor).
 *
 * On finish, a full session aggregate is derived from what was actually
 * trained (rounds started) and can be saved through the normal transactional
 * CreateTrainingSession path.
 */
@HiltViewModel
class LiveTimerViewModel @Inject constructor(
    private val observeSession: ObserveSession,
    private val createTrainingSession: CreateTrainingSession,
    private val timerPreferences: TimerPreferences,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    data class Config(
        val workSeconds: Int = 180,
        val restSeconds: Int = 60,
        val rounds: Int = 6,
        val type: WorkoutType = WorkoutType.HEAVY_BAG,
        val focus: FocusArea = FocusArea.POWER,
    )

    data class UiState(
        val loaded: Boolean = false,
        val status: TimerStatus = TimerStatus.IDLE,
        val config: Config = Config(),
        val phase: TimerPhase = TimerPhase.WORK,
        // 0-based; rounds started = roundIndex + 1 while running
        val roundIndex: Int = 0,
        val remainingMillis: Long = 0,
        val phaseTotalMillis: Long = 0,
        // set when the timer finishes
        val completedRounds: Int = 0,
        val isSaving: Boolean = false,
        val isSaved: Boolean = false,
        val saveFailed: Boolean = false,
    )

    sealed interface TimerEvent {
        data class WorkStarted(val round: Int) : TimerEvent
        data class RestStarted(val round: Int) : TimerEvent
        data object Finished : TimerEvent
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TimerEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<TimerEvent> = _events.asSharedFlow()

    // Timing anchors (not observable state — derived on every tick).
    private var anchorMillis: Long = 0
    private var anchorRemainingMillis: Long = 0

    init {
        viewModelScope.launch {
            val defaults = timerPreferences.defaults.first()
            _state.update {
                it.copy(
                    loaded = true,
                    config = it.config.copy(
                        workSeconds = defaults.workSeconds,
                        restSeconds = defaults.restSeconds,
                        rounds = defaults.rounds,
                    ),
                )
            }
        }
    }

    // ---------- configuration (IDLE only) ----------

    fun onWorkChange(deltaSeconds: Int) = _state.update {
        it.copy(config = it.config.copy(workSeconds = (it.config.workSeconds + deltaSeconds).coerceIn(5, 3600)))
    }

    fun onRestChange(deltaSeconds: Int) = _state.update {
        it.copy(config = it.config.copy(restSeconds = (it.config.restSeconds + deltaSeconds).coerceIn(0, 600)))
    }

    fun onRoundsChange(delta: Int) = _state.update {
        it.copy(config = it.config.copy(rounds = (it.config.rounds + delta).coerceIn(1, 99)))
    }

    fun onTypeChange(type: WorkoutType) = _state.update { it.copy(config = it.config.copy(type = type)) }

    fun onFocusChange(focus: FocusArea) = _state.update { it.copy(config = it.config.copy(focus = focus)) }

    // ---------- run control ----------

    fun onStart() {
        val snapshot = _state.value
        if (snapshot.status != TimerStatus.IDLE) return
        viewModelScope.launch { timerPreferences.setDefaults(snapshot.config.toDefaults()) }
        val now = timeProvider.nowMillis()
        anchorMillis = now
        anchorRemainingMillis = snapshot.config.workSeconds * 1000L
        _state.update {
            it.copy(
                status = TimerStatus.RUNNING,
                phase = TimerPhase.WORK,
                roundIndex = 0,
                remainingMillis = anchorRemainingMillis,
                phaseTotalMillis = anchorRemainingMillis,
                completedRounds = 0,
                isSaved = false,
                saveFailed = false,
            )
        }
        _events.tryEmit(TimerEvent.WorkStarted(1))
    }

    fun onPause() {
        if (_state.value.status != TimerStatus.RUNNING) return
        tick() // freeze the exact remaining time
        _state.update { it.copy(status = TimerStatus.PAUSED) }
    }

    fun onResume() {
        if (_state.value.status != TimerStatus.PAUSED) return
        anchorMillis = timeProvider.nowMillis()
        anchorRemainingMillis = _state.value.remainingMillis
        _state.update { it.copy(status = TimerStatus.RUNNING) }
    }

    fun onSkipPhase() {
        if (_state.value.status != TimerStatus.RUNNING && _state.value.status != TimerStatus.PAUSED) return
        anchorRemainingMillis = 0
        anchorMillis = timeProvider.nowMillis()
        advance()
    }

    fun onEndEarly() {
        val snapshot = _state.value
        if (snapshot.status != TimerStatus.RUNNING && snapshot.status != TimerStatus.PAUSED) return
        if (snapshot.status == TimerStatus.RUNNING) tick()
        val started = _state.value.roundIndex + 1
        _state.update {
            it.copy(status = TimerStatus.FINISHED, completedRounds = started.coerceAtMost(it.config.rounds), remainingMillis = 0)
        }
        _events.tryEmit(TimerEvent.Finished)
    }

    /** Called by the screen every ~200ms while RUNNING. */
    fun tick() {
        if (_state.value.status != TimerStatus.RUNNING) return
        val now = timeProvider.nowMillis()
        // No clamp here: a negative remainder is the overshoot that the next
        // phase must consume (drift-free transitions).
        anchorRemainingMillis -= now - anchorMillis
        anchorMillis = now
        advance()
    }

    /** Applies pending phase/round transitions (may cascade after long jumps). */
    private fun advance() {
        var state = _state.value
        var remaining = anchorRemainingMillis
        var guard = 0
        while (remaining <= 0 && guard++ < 512) {
            when {
                state.phase == TimerPhase.WORK -> {
                    if (state.roundIndex == state.config.rounds - 1) {
                        // Last round's work is done: the workout ends here —
                        // no trailing rest phase.
                        state = finishRoundOrAdvance(state) ?: break
                        remaining = 0
                        break
                    }
                    if (state.config.restSeconds > 0) {
                        state = state.copy(phase = TimerPhase.REST)
                        remaining += state.config.restSeconds * 1000L
                        _events.tryEmit(TimerEvent.RestStarted(state.roundIndex + 1))
                    } else {
                        state = finishRoundOrAdvance(state) ?: break
                        if (state.status == TimerStatus.FINISHED) {
                            remaining = 0
                            break
                        }
                        remaining += state.config.workSeconds * 1000L
                        _events.tryEmit(TimerEvent.WorkStarted(state.roundIndex + 1))
                    }
                }
                else -> { // REST finished -> next round
                    state = finishRoundOrAdvance(state) ?: break
                    if (state.status == TimerStatus.FINISHED) {
                        remaining = 0
                        break
                    }
                    remaining += state.config.workSeconds * 1000L
                    _events.tryEmit(TimerEvent.WorkStarted(state.roundIndex + 1))
                }
            }
            state = state.copy(
                remainingMillis = remaining,
                phaseTotalMillis = if (state.phase == TimerPhase.WORK) {
                    state.config.workSeconds * 1000L
                } else {
                    state.config.restSeconds * 1000L
                },
            )
        }
        anchorRemainingMillis = remaining.coerceAtLeast(0)
        _state.update { current ->
            state.copy(
                remainingMillis = anchorRemainingMillis,
                loaded = current.loaded,
                config = current.config,
                isSaving = current.isSaving,
                isSaved = current.isSaved,
                saveFailed = current.saveFailed,
            )
        }
    }

    /** Returns the next-round state, or a FINISHED state, or null when already finished. */
    private fun finishRoundOrAdvance(state: UiState): UiState? {
        val nextRound = state.roundIndex + 1
        return if (nextRound >= state.config.rounds) {
            state.copy(
                status = TimerStatus.FINISHED,
                completedRounds = state.config.rounds,
                remainingMillis = 0,
            ).also {
                _events.tryEmit(TimerEvent.Finished)
            }
        } else {
            state.copy(roundIndex = nextRound, phase = TimerPhase.WORK)
        }
    }

    // ---------- save ----------

    fun onSave() {
        val snapshot = _state.value
        if (snapshot.status != TimerStatus.FINISHED || snapshot.isSaving || snapshot.isSaved) return
        val draft = buildDraft(snapshot) ?: run {
            _state.update { it.copy(saveFailed = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, saveFailed = false) }
            val userId = observeSession().first()
            if (userId == null) {
                _state.update { it.copy(isSaving = false, saveFailed = true) }
                return@launch
            }
            createTrainingSession(userId, draft)
                .onSuccess { _state.update { it.copy(isSaving = false, isSaved = true) } }
                .onFailure { _state.update { it.copy(isSaving = false, saveFailed = true) } }
        }
    }

    private fun buildDraft(snapshot: UiState): TrainingSession? {
        val roundsDone = snapshot.completedRounds
        if (roundsDone <= 0) return null
        val cfg = snapshot.config
        val rounds = (1..roundsDone).map { n ->
            Round(
                roundNumber = n,
                duration = cfg.workSeconds.seconds,
                restDuration = cfg.restSeconds.seconds,
                intensity = DEFAULT_INTENSITY,
            )
        }
        val blockSeconds = roundsDone * cfg.workSeconds + (roundsDone - 1) * cfg.restSeconds
        return TrainingSession(
            // scoped by the use case
            userId = 0,
            date = TimeUtils.localDayMidnightUtcMillis(timeProvider.nowMillis()),
            overallIntensity = DEFAULT_INTENSITY,
            overallFeeling = Feeling.GOOD,
            activities = listOf(
                WorkoutActivity(
                    type = cfg.type,
                    duration = blockSeconds.seconds,
                    intensity = DEFAULT_INTENSITY,
                    focusArea = cfg.focus,
                    rounds = rounds,
                ),
            ),
        )
    }

    private fun Config.toDefaults() =
        com.warrior.domain.training.TimerDefaults(workSeconds, restSeconds, rounds)

    companion object {
        const val DEFAULT_INTENSITY = 7
    }
}
