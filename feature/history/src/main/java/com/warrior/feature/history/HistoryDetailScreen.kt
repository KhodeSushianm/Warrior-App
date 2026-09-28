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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
            title = stringResource(R.string.history_detail_title),
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
                WarriorCard { Text(stringResource(R.string.history_detail_not_found), style = MaterialTheme.typography.titleMedium) }
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
                                stringResource(
                                    R.string.history_detail_activity_meta,
                                    activity.focusArea.label,
                                    DateFormats.durationLabel(activity.duration.inWholeMinutes),
                                    activity.intensity,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
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
                                stringResource(
                                    R.string.history_detail_round_line,
                                    round.roundNumber,
                                    round.duration.inWholeMinutes.toInt(),
                                    round.restDuration.inWholeMinutes.toInt(),
                                    round.intensity,
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = TextMuted,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            WarriorButton(
                text = stringResource(R.string.history_action_edit),
                onClick = { onEdit(session.id) },
                variant = WarriorButtonVariant.GHOST,
            )
            Spacer(Modifier.height(10.dp))
            WarriorButton(
                text = stringResource(R.string.history_action_delete),
                onClick = { viewModel.onDeleteConfirmRequest(true) },
                variant = WarriorButtonVariant.DANGER,
            )
        }
        Spacer(Modifier.height(8.dp))
    }

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.onDeleteConfirmRequest(false) },
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { Text(stringResource(R.string.history_delete_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmDelete) {
                    Text(stringResource(R.string.history_delete_confirm), color = Negative)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDeleteConfirmRequest(false) }) {
                    Text(stringResource(R.string.history_cancel))
                }
            },
        )
    }
}

@Composable
private fun HeaderCard(session: TrainingSession) {
    WarriorCard {
        Text(
            DateFormats.dayHeader(
                session.date,
                todayLabel = stringResource(R.string.history_day_today),
                yesterdayLabel = stringResource(R.string.history_day_yesterday),
            ),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(
                R.string.history_detail_header_meta,
                DateFormats.durationLabel(session.totalDuration.inWholeMinutes),
                pluralStringResource(R.plurals.history_rounds_count, session.totalRounds, session.totalRounds),
                session.overallIntensity,
                session.overallFeeling.label,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
        session.notes?.let { note ->
            Spacer(Modifier.height(8.dp))
            Text(note, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
