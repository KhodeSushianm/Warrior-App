package com.warrior.feature.workout

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                WorkoutLoggingViewModel.Step.SESSION -> if (state.sessionId == null) "New Session" else "Edit Session"
                WorkoutLoggingViewModel.Step.ACTIVITIES -> "Activities"
                WorkoutLoggingViewModel.Step.REVIEW -> "Review & Save"
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
                    WarriorBadge(text = "EDIT", highlight = true)
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

        if (state.errors.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            state.errors.forEach { message ->
                Text(message, color = Negative, style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(16.dp))
        when (state.step) {
            WorkoutLoggingViewModel.Step.SESSION ->
                WarriorButton(text = "Continue →", onClick = viewModel::onNextStep)
            WorkoutLoggingViewModel.Step.ACTIVITIES -> {
                WarriorButton(text = "+ Add Activity", onClick = viewModel::onAddActivity, variant = WarriorButtonVariant.GHOST)
                Spacer(Modifier.height(10.dp))
                WarriorButton(
                    text = "Continue →",
                    onClick = viewModel::onNextStep,
                    enabled = state.activities.isNotEmpty(),
                )
            }
            WorkoutLoggingViewModel.Step.REVIEW -> {
                WarriorButton(
                    text = if (state.isSaving) "Saving…" else "Save Session",
                    onClick = viewModel::onSave,
                    enabled = !state.isSaving,
                )
                if (state.sessionId != null) {
                    Spacer(Modifier.height(10.dp))
                    WarriorButton(
                        text = "Delete Session",
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
            title = { Text("Delete this session?") },
            text = { Text("Activities and rounds are deleted with it. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteSession) { Text("Delete", color = Negative) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDeleteConfirmRequest(false) }) { Text("Cancel") }
            },
        )
    }
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
                    color = if (on) com.warrior.core.designsystem.theme.Accent else com.warrior.core.designsystem.theme.SurfaceVariant,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(99f),
                )
            }
        }
    }
}

@Composable
private fun SessionStep(state: WorkoutLoggingViewModel.UiState, viewModel: WorkoutLoggingViewModel) {
    WarriorCard {
        Text("DATE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text("Today", style = MaterialTheme.typography.titleMedium)
        Text(
            "Overall intensity — ${state.overallIntensity}/10",
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
        Text("FEELING", style = MaterialTheme.typography.labelSmall, color = TextMuted)
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
        WarriorTextField(value = state.notes, onValueChange = viewModel::onNotesChange, label = "Notes")
    }
}

@Composable
private fun ActivitiesStep(state: WorkoutLoggingViewModel.UiState, viewModel: WorkoutLoggingViewModel) {
    if (state.activities.isEmpty()) {
        WarriorCard {
            Text("No activities yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("Add heavy bag, mitt work, sparring or cardio blocks.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
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
                Text("Duration", style = MaterialTheme.typography.labelLarge, color = TextMuted, modifier = Modifier.weight(1f))
                Stepper(
                    label = activity.duration.inWholeMinutes.toString() + " min",
                    onMinus = { viewModel.onActivityDurationChange(index, -5) },
                    onPlus = { viewModel.onActivityDurationChange(index, 5) },
                )
            }
            Text(
                "Intensity — ${activity.intensity}/10",
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
            Text("FOCUS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
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
                    Text("Rounds — ${activity.rounds.size}", style = MaterialTheme.typography.labelLarge, color = TextMuted, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.onAddRound(index) }) { Text("+ round") }
                }
                activity.rounds.forEachIndexed { roundIndex, round ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text("R${round.roundNumber}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(30.dp))
                        Stepper(
                            label = "${round.duration.inWholeMinutes}m work",
                            onMinus = { viewModel.onRoundWorkChange(index, roundIndex, -1) },
                            onPlus = { viewModel.onRoundWorkChange(index, roundIndex, 1) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Stepper(
                            label = "${round.restDuration.inWholeMinutes}m rest",
                            onMinus = { viewModel.onRoundRestChange(index, roundIndex, -1) },
                            onPlus = { viewModel.onRoundRestChange(index, roundIndex, 1) },
                        )
                        Spacer(Modifier.weight(1f))
                        Text("✕", color = TextMuted, modifier = Modifier.clickable { viewModel.onRemoveRound(index, roundIndex) })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "✕ remove activity",
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
        KeyValue("Date", "Today")
        KeyValue("Activities", state.activities.size.toString())
        KeyValue("Total duration", "${totalMinutes / 60}h ${totalMinutes % 60}m")
        KeyValue("Rounds", totalRounds.toString())
        KeyValue("Overall intensity", "${state.overallIntensity}/10")
        KeyValue("Feeling", state.feeling.label)
    }
    Spacer(Modifier.height(10.dp))
    WarriorCard {
        state.activities.forEach { activity ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        activity.type.label + if (activity.rounds.isNotEmpty()) " · ${activity.rounds.size} rounds" else "",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "${activity.focusArea.label} · ${activity.duration.inWholeMinutes}m · int ${activity.intensity}",
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
