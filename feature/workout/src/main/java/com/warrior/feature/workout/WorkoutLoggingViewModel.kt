package com.warrior.feature.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warrior.core.common.time.TimeUtils
import com.warrior.domain.auth.usecase.ObserveSession
import com.warrior.domain.training.model.Feeling
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.Round
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.model.WorkoutActivity
import com.warrior.domain.training.model.WorkoutType
import com.warrior.domain.training.model.isRoundBased
import com.warrior.domain.training.usecase.CreateTrainingSession
import com.warrior.domain.training.usecase.DeleteTrainingSession
import com.warrior.domain.training.usecase.GetTrainingSession
import com.warrior.domain.training.usecase.UpdateTrainingSession
import com.warrior.domain.training.validation.ValidationException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Three-step logging flow (Session info → Activities → Review).
 * The draft aggregate lives in UiState; persistence happens once, through the
 * single transactional repository entry point (Architecture Rule 7).
 */
@HiltViewModel
class WorkoutLoggingViewModel @Inject constructor(
    private val createTrainingSession: CreateTrainingSession,
    private val updateTrainingSession: UpdateTrainingSession,
    private val deleteTrainingSession: DeleteTrainingSession,
    private val getTrainingSession: GetTrainingSession,
    private val observeSession: ObserveSession,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    enum class Step { SESSION, ACTIVITIES, REVIEW }

    data class UiState(
        val sessionId: Long? = null,
        val step: Step = Step.SESSION,
        val date: Long = TimeUtils.todayLocalMidnightUtcMillis(),
        val overallIntensity: Int = 7,
        val feeling: Feeling = Feeling.GOOD,
        val notes: String = "",
        val activities: List<WorkoutActivity> = emptyList(),
        val isSaving: Boolean = false,
        val errors: List<String> = emptyList(),
        val isSaved: Boolean = false,
        val isDeleted: Boolean = false,
        val showDeleteConfirm: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        val sessionId = savedStateHandle.get<Long?>("sessionId")
        if (sessionId != null) loadExisting(sessionId)
    }

    private fun loadExisting(sessionId: Long) {
        viewModelScope.launch {
            val userId = observeSession().first() ?: return@launch
            val session = getTrainingSession(userId, sessionId) ?: return@launch
            _state.update {
                it.copy(
                    sessionId = session.id,
                    date = session.date,
                    overallIntensity = session.overallIntensity,
                    feeling = session.overallFeeling,
                    notes = session.notes.orEmpty(),
                    activities = session.activities,
                )
            }
        }
    }

    fun onIntensityChange(value: Int) = _state.update { it.copy(overallIntensity = value, errors = emptyList()) }

    fun onFeelingChange(value: Feeling) = _state.update { it.copy(feeling = value, errors = emptyList()) }

    fun onNotesChange(value: String) = _state.update { it.copy(notes = value, errors = emptyList()) }

    fun onNextStep() = _state.update {
        it.copy(step = if (it.step == Step.SESSION) Step.ACTIVITIES else Step.REVIEW, errors = emptyList())
    }

    fun onPreviousStep() = _state.update {
        it.copy(step = if (it.step == Step.REVIEW) Step.ACTIVITIES else Step.SESSION, errors = emptyList())
    }

    fun onAddActivity() = _state.update {
        it.copy(
            activities = it.activities + WorkoutActivity(
                type = WorkoutType.HEAVY_BAG,
                duration = 15.minutes,
                intensity = 7,
                focusArea = FocusArea.POWER,
                rounds = defaultRounds(),
            ),
            errors = emptyList(),
        )
    }

    fun onRemoveActivity(index: Int) = _state.update {
        it.copy(activities = it.activities.filterIndexed { i, _ -> i != index }, errors = emptyList())
    }

    fun onActivityTypeChange(index: Int, type: WorkoutType) = updateActivity(index) { activity ->
        val rounds = when {
            type.isRoundBased && activity.rounds.isEmpty() -> defaultRounds()
            !type.isRoundBased -> emptyList()
            else -> activity.rounds
        }
        activity.copy(type = type, rounds = rounds)
    }

    fun onActivityDurationChange(index: Int, deltaMinutes: Long) = updateActivity(index) { activity ->
        val next = (activity.duration + deltaMinutes.minutes).coerceAtLeast(5.minutes)
        activity.copy(duration = next)
    }

    fun onActivityIntensityChange(index: Int, value: Int) = updateActivity(index) { it.copy(intensity = value) }

    fun onActivityFocusChange(index: Int, focus: FocusArea) = updateActivity(index) { it.copy(focusArea = focus) }

    fun onAddRound(index: Int) = updateActivity(index) { activity ->
        activity.copy(
            rounds = renumber(
                activity.rounds + Round(
                    roundNumber = activity.rounds.size + 1,
                    duration = 3.minutes,
                    restDuration = 1.minutes,
                    intensity = activity.intensity,
                ),
            ),
        )
    }

    fun onRemoveRound(index: Int, roundIndex: Int) = updateActivity(index) { activity ->
        renumber(activity.rounds.filterIndexed { i, _ -> i != roundIndex })
            .let { activity.copy(rounds = it) }
    }

    fun onRoundWorkChange(index: Int, roundIndex: Int, deltaMinutes: Long) = updateRound(index, roundIndex) { round ->
        round.copy(duration = (round.duration + deltaMinutes.minutes).coerceAtLeast(1.minutes))
    }

    fun onRoundRestChange(index: Int, roundIndex: Int, deltaMinutes: Long) = updateRound(index, roundIndex) { round ->
        round.copy(restDuration = (round.restDuration + deltaMinutes.minutes).coerceAtLeast(Duration.ZERO))
    }

    fun onDeleteConfirmRequest(show: Boolean) = _state.update { it.copy(showDeleteConfirm = show) }

    fun onDeleteSession() {
        val sessionId = _state.value.sessionId ?: return
        viewModelScope.launch {
            val userId = observeSession().first() ?: return@launch
            deleteTrainingSession(userId, sessionId).onSuccess {
                _state.update { it.copy(showDeleteConfirm = false, isDeleted = true) }
            }
        }
    }

    fun onSave() {
        if (_state.value.isSaving) return
        viewModelScope.launch {
            val userId = observeSession().first()
            if (userId == null) {
                _state.update { it.copy(errors = listOf("not signed in")) }
                return@launch
            }
            _state.update { it.copy(isSaving = true, errors = emptyList()) }
            val draft = buildDraft()
            val result = if (draft.id == 0L) {
                createTrainingSession(userId, draft)
            } else {
                updateTrainingSession(userId, draft)
            }
            result
                .onSuccess { _state.update { it.copy(isSaving = false, isSaved = true) } }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errors = if (error is ValidationException) error.messages else listOf(error.message ?: "save failed"),
                        )
                    }
                }
        }
    }

    private fun buildDraft(): TrainingSession {
        val current = _state.value
        return TrainingSession(
            id = current.sessionId ?: 0,
            // use cases re-scope to the signed-in user
            userId = 0,
            date = current.date,
            overallIntensity = current.overallIntensity,
            overallFeeling = current.feeling,
            notes = current.notes.takeIf { it.isNotBlank() },
            activities = current.activities,
        )
    }

    private fun defaultRounds(): List<Round> = listOf(
        Round(roundNumber = 1, duration = 3.minutes, restDuration = 1.minutes, intensity = 7),
        Round(roundNumber = 2, duration = 3.minutes, restDuration = 1.minutes, intensity = 7),
        Round(roundNumber = 3, duration = 3.minutes, restDuration = 1.minutes, intensity = 7),
        Round(roundNumber = 4, duration = 3.minutes, restDuration = 1.minutes, intensity = 7),
    )

    private fun renumber(rounds: List<Round>): List<Round> =
        rounds.mapIndexed { index, round -> round.copy(roundNumber = index + 1) }

    private fun updateActivity(index: Int, transform: (WorkoutActivity) -> WorkoutActivity) =
        _state.update {
            it.copy(
                activities = it.activities.mapIndexed { i, activity ->
                    if (i == index) transform(activity) else activity
                },
                errors = emptyList(),
            )
        }

    private fun updateRound(index: Int, roundIndex: Int, transform: (Round) -> Round) =
        updateActivity(index) { activity ->
            activity.copy(
                rounds = activity.rounds.mapIndexed { i, round ->
                    if (i == roundIndex) transform(round) else round
                },
            )
        }
}
