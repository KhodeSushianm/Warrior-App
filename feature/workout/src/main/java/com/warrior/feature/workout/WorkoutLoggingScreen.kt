package com.warrior.feature.workout

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warrior.core.common.time.DateFormats
import com.warrior.core.designsystem.components.WarriorBadge
import com.warrior.core.designsystem.components.WarriorButton
import com.warrior.core.designsystem.components.WarriorButtonVariant
import com.warrior.core.designsystem.components.WarriorCard
import com.warrior.core.designsystem.components.WarriorChip
import com.warrior.core.designsystem.components.WarriorTextField
import com.warrior.core.designsystem.components.WarriorTopBar
import com.warrior.core.designsystem.theme.Negative
import com.warrior.core.designsystem.theme.TextMuted
import com.warrior.domain.training.model.Feeling
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.WorkoutType
import com.warrior.domain.training.model.isRoundBased
import com.warrior.domain.training.model.label
import com.warrior.domain.training.validation.TrainingErrorCode

@Composable
fun WorkoutLoggingScreen(
    sessionId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutLoggingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isSaved || state.isDeleted) {
        onSaved()
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
    ) {
        WarriorTopBar(
            title = when (state.step) {
                WorkoutLoggingViewModel.Step.SESSION ->
                    stringResource(
                        if (state.sessionId == null) R.string.workout_title_new else R.string.workout_title_edit,
                    )
                WorkoutLoggingViewModel.Step.ACTIVITIES -> stringResource(R.string.workout_step_activities)
                WorkoutLoggingViewModel.Step.REVIEW -> stringResource(R.string.workout_step_review)
            },
            navigationIcon = {
                Text(
                    "←",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .clickable {
                            if (state.step == WorkoutLoggingViewModel.Step.SESSION) {
                                onBack()
                            } else {
                                viewModel.onPreviousStep()
                            }
                        }
                        .padding(4.dp),
                )
            },
            actions = {
                if (state.sessionId != null) {
                    WarriorBadge(text = stringResource(R.string.workout_badge_edit), highlight = true)
                }
            },
        )

        StepIndicator(state.step)
        Spacer(Modifier.height(12.dp))

        when (state.step) {
            WorkoutLoggingViewModel.Step.SESSION -> SessionStep(state, viewModel)
            WorkoutLoggingViewModel.Step.ACTIVITIES -> ActivitiesStep(state, viewModel)
            WorkoutLoggingViewModel.Step.REVIEW -> ReviewStep(state, viewModel)
        }

        if (state.errorCodes.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            state.errorCodes.forEach { code ->
                Text(stringResource(code.labelRes), color = Negative, style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(16.dp))
        when (state.step) {
            WorkoutLoggingViewModel.Step.SESSION ->
                WarriorButton(text = stringResource(R.string.workout_action_continue), onClick = viewModel::onNextStep)
            WorkoutLoggingViewModel.Step.ACTIVITIES -> {
                WarriorButton(
                    text = stringResource(R.string.workout_action_add_activity),
                    onClick = viewModel::onAddActivity,
                    variant = WarriorButtonVariant.GHOST,
                )
                Spacer(Modifier.height(10.dp))
                WarriorButton(
                    text = stringResource(R.string.workout_action_continue),
                    onClick = viewModel::onNextStep,
                    enabled = state.activities.isNotEmpty(),
                )
            }
            WorkoutLoggingViewModel.Step.REVIEW -> {
                WarriorButton(
                    text = stringResource(if (state.isSaving) R.string.workout_action_saving else R.string.workout_action_save),
                    onClick = viewModel::onSave,
                    enabled = !state.isSaving,
                )
                if (state.sessionId != null) {
                    Spacer(Modifier.height(10.dp))
                    WarriorButton(
                        text = stringResource(R.string.workout_action_delete_session),
                        onClick = { viewModel.onDeleteConfirmRequest(true) },
                        variant = WarriorButtonVariant.DANGER,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.onDeleteConfirmRequest(false) },
            title = { Text(stringResource(R.string.workout_delete_title)) },
            text = { Text(stringResource(R.string.workout_delete_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteSession) {
                    Text(stringResource(R.string.workout_delete_confirm), color = Negative)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDeleteConfirmRequest(false) }) {
                    Text(stringResource(R.string.workout_cancel))
                }
            },
        )
    }
}

/** Error code -> localized copy (Phase 9: no hardcoded UI strings). */
private val TrainingErrorCode.labelRes: Int
    @StringRes
    get() = when (this) {
        TrainingErrorCode.OVERALL_INTENSITY_RANGE -> R.string.workout_error_overall_intensity_range
        TrainingErrorCode.NO_ACTIVITIES -> R.string.workout_error_no_activities
        TrainingErrorCode.ACTIVITY_DURATION_POSITIVE -> R.string.workout_error_activity_duration
        TrainingErrorCode.ACTIVITY_INTENSITY_RANGE -> R.string.workout_error_activity_intensity
        TrainingErrorCode.ROUND_NUMBER_POSITIVE -> R.string.workout_error_round_number
        TrainingErrorCode.ROUND_DURATION_POSITIVE -> R.string.workout_error_round_duration
        TrainingErrorCode.ROUND_REST_NON_NEGATIVE -> R.string.workout_error_round_rest
        TrainingErrorCode.ROUND_INTENSITY_RANGE -> R.string.workout_error_round_intensity
        TrainingErrorCode.SESSION_NOT_FOUND -> R.string.workout_error_session_not_found
        TrainingErrorCode.NOT_SIGNED_IN -> R.string.workout_error_not_signed_in
        TrainingErrorCode.SAVE_FAILED -> R.string.workout_error_save_failed
        TrainingErrorCode.UNEXPECTED -> R.string.workout_error_unexpected
    }

@Composable
private fun StepIndicator(step: WorkoutLoggingViewModel.Step) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        WorkoutLoggingViewModel.Step.entries.forEach { entry ->
            val on = entry.ordinal <= step.ordinal
            androidx.compose.foundation.Canvas(
                Modifier
                    .weight(1f)
                    .height(4.dp),
            ) {
                drawRoundRect(
                    color = if (on) {
                        com.warrior.core.designsystem.theme.Accent
                    } else {
                        com.warrior.core.designsystem.theme.SurfaceVariant
                    },
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(99f),
                )
            }
        }
    }
}

@Composable
private fun SessionStep(state: WorkoutLoggingViewModel.UiState, viewModel: WorkoutLoggingViewModel) {
    WarriorCard {
        Text(stringResource(R.string.workout_label_date), style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(stringResource(R.string.workout_value_today), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.workout_overall_intensity, state.overallIntensity),
            style = MaterialTheme.typography.labelLarge,
            color = TextMuted,
            modifier = Modifier.padding(top = 12.dp),
        )
        Slider(
            value = state.overallIntensity.toFloat(),
            onValueChange = { viewModel.onIntensityChange(it.toInt()) },
            valueRange = 1f..10f,
            steps = 8,
        )
        Text(stringResource(R.string.workout_label_feeling), style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Feeling.entries.forEach { feeling ->
                WarriorChip(
                    label = feeling.label,
                    selected = feeling == state.feeling,
                    onClick = { viewModel.onFeelingChange(feeling) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        WarriorTextField(
            value = state.notes,
            onValueChange = viewModel::onNotesChange,
            label = stringResource(R.string.workout_field_notes),
        )
    }
}

@Composable
private fun ActivitiesStep(state: WorkoutLoggingViewModel.UiState, viewModel: WorkoutLoggingViewModel) {
    if (state.activities.isEmpty()) {
        WarriorCard {
            Text(stringResource(R.string.workout_empty_activities_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.workout_empty_activities_body),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        }
    }
    state.activities.forEachIndexed { index, activity ->
        WarriorCard {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                WorkoutType.entries.forEach { type ->
                    WarriorChip(
                        label = type.label,
                        selected = type == activity.type,
                        onClick = { viewModel.onActivityTypeChange(index, type) },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.workout_label_duration),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                    modifier = Modifier.weight(1f),
                )
                Stepper(
                    label = stringResource(R.string.workout_stepper_minutes, activity.duration.inWholeMinutes.toInt()),
                    onMinus = { viewModel.onActivityDurationChange(index, -5) },
                    onPlus = { viewModel.onActivityDurationChange(index, 5) },
                )
            }
            Text(
                stringResource(R.string.workout_activity_intensity, activity.intensity),
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted,
                modifier = Modifier.padding(top = 8.dp),
            )
            Slider(
                value = activity.intensity.toFloat(),
                onValueChange = { viewModel.onActivityIntensityChange(index, it.toInt()) },
                valueRange = 1f..10f,
                steps = 8,
            )
            Text(stringResource(R.string.workout_label_focus), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                FocusArea.entries.forEach { focus ->
                    WarriorChip(
                        label = focus.label,
                        selected = focus == activity.focusArea,
                        onClick = { viewModel.onActivityFocusChange(index, focus) },
                    )
                }
            }
            if (activity.type.isRoundBased) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.workout_rounds_count, activity.rounds.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextMuted,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { viewModel.onAddRound(index) }) {
                        Text(stringResource(R.string.workout_add_round))
                    }
                }
                activity.rounds.forEachIndexed { roundIndex, round ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            stringResource(R.string.workout_round_label, round.roundNumber),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.width(30.dp),
                        )
                        Stepper(
                            label = stringResource(R.string.workout_round_work, round.duration.inWholeMinutes.toInt()),
                            onMinus = { viewModel.onRoundWorkChange(index, roundIndex, -1) },
                            onPlus = { viewModel.onRoundWorkChange(index, roundIndex, 1) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Stepper(
                            label = stringResource(R.string.workout_round_rest, round.restDuration.inWholeMinutes.toInt()),
                            onMinus = { viewModel.onRoundRestChange(index, roundIndex, -1) },
                            onPlus = { viewModel.onRoundRestChange(index, roundIndex, 1) },
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "✕",
                            color = TextMuted,
                            modifier = Modifier.clickable { viewModel.onRemoveRound(index, roundIndex) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.workout_remove_activity),
                color = TextMuted,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clickable { viewModel.onRemoveActivity(index) },
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun ReviewStep(state: WorkoutLoggingViewModel.UiState, viewModel: WorkoutLoggingViewModel) {
    val totalMinutes = state.activities.sumOf { it.duration.inWholeMinutes }
    val totalRounds = state.activities.sumOf { it.rounds.size }
    WarriorCard {
        KeyValue(stringResource(R.string.workout_review_date), stringResource(R.string.workout_value_today))
        KeyValue(stringResource(R.string.workout_review_activities), state.activities.size.toString())
        KeyValue(stringResource(R.string.workout_review_total_duration), DateFormats.durationLabel(totalMinutes))
        KeyValue(stringResource(R.string.workout_review_rounds), totalRounds.toString())
        KeyValue(
            stringResource(R.string.workout_review_intensity),
            stringResource(R.string.workout_review_intensity_value, state.overallIntensity),
        )
        KeyValue(stringResource(R.string.workout_review_feeling), state.feeling.label)
    }
    Spacer(Modifier.height(10.dp))
    WarriorCard {
        state.activities.forEach { activity ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (activity.rounds.isNotEmpty()) {
                            stringResource(
                                R.string.workout_activity_title_with_rounds,
                                activity.type.label,
                                activity.rounds.size,
                            )
                        } else {
                            activity.type.label
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(
                            R.string.workout_review_activity_meta,
                            activity.focusArea.label,
                            activity.duration.inWholeMinutes.toInt(),
                            activity.intensity,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyValue(key: String, value: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text(key, style = MaterialTheme.typography.bodyMedium, color = TextMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Stepper(label: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onMinus) { Text("−") }
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(64.dp))
        TextButton(onClick = onPlus) { Text("+") }
    }
}
