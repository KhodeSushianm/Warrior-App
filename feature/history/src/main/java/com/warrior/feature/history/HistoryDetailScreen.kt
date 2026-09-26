package com.warrior.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warrior.core.common.time.DateFormats
import com.warrior.core.designsystem.components.WarriorBadge
import com.warrior.core.designsystem.components.WarriorButton
import com.warrior.core.designsystem.components.WarriorButtonVariant
import com.warrior.core.designsystem.components.WarriorCard
import com.warrior.core.designsystem.components.WarriorTopBar
import com.warrior.core.designsystem.theme.Negative
import com.warrior.core.designsystem.theme.TextMuted
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.model.label

@Composable
fun HistoryDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isDeleted) {
        onBack()
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
    ) {
        WarriorTopBar(
            title = "Session Detail",
            navigationIcon = {
                Text(
                    "←",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(4.dp),
                )
            },
        )

        val session = state.session
        if (session == null) {
            if (state.loaded) {
                WarriorCard { Text("Session not found", style = MaterialTheme.typography.titleMedium) }
            }
        } else {
            HeaderCard(session)
            Spacer(Modifier.height(12.dp))
            session.activities.forEach { activity ->
                WarriorCard {
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text(activity.type.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${activity.focusArea.label} · ${DateFormats.durationLabel(activity.duration.inWholeMinutes)} · int ${activity.intensity}/10",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                            )
                        }
                        WarriorBadge(text = activity.type.label)
                    }
                    activity.notes?.let { note ->
                        Spacer(Modifier.height(8.dp))
                        Text(note, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    }
                    if (activity.rounds.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        activity.rounds.forEach { round ->
                            Text(
                                "R${round.roundNumber}  ${round.duration.inWholeMinutes}:00 work / ${round.restDuration.inWholeMinutes}:00 rest · int ${round.intensity}",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextMuted,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            WarriorButton(text = "Edit Session", onClick = { onEdit(session.id) }, variant = WarriorButtonVariant.GHOST)
            Spacer(Modifier.height(10.dp))
            WarriorButton(
                text = "Delete Session",
                onClick = { viewModel.onDeleteConfirmRequest(true) },
                variant = WarriorButtonVariant.DANGER,
            )
        }
        Spacer(Modifier.height(8.dp))
    }

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.onDeleteConfirmRequest(false) },
            title = { Text("Delete this session?") },
            text = { Text("Activities and rounds are deleted with it. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmDelete) { Text("Delete", color = Negative) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDeleteConfirmRequest(false) }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun HeaderCard(session: TrainingSession) {
    WarriorCard {
        Text(DateFormats.dayHeader(session.date), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "${DateFormats.durationLabel(session.totalDuration.inWholeMinutes)} · ${session.totalRounds} rounds · " +
                "int ${session.overallIntensity}/10 · felt ${session.overallFeeling.name.lowercase()}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
        session.notes?.let { note ->
            Spacer(Modifier.height(8.dp))
            Text(note, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
