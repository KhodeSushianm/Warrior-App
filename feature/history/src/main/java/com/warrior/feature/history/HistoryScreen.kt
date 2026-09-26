package com.warrior.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warrior.core.common.time.DateFormats
import com.warrior.core.designsystem.components.WarriorBadge
import com.warrior.core.designsystem.components.WarriorButton
import com.warrior.core.designsystem.components.WarriorCard
import com.warrior.core.designsystem.components.WarriorTopBar
import com.warrior.core.designsystem.theme.Cardio
import com.warrior.core.designsystem.theme.HeavyBag
import com.warrior.core.designsystem.theme.MittWork
import com.warrior.core.designsystem.theme.Sparring
import com.warrior.core.designsystem.theme.TextMuted
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.model.WorkoutType
import com.warrior.domain.training.model.label

@Composable
fun HistoryScreen(
    onOpenSession: (Long) -> Unit,
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        WarriorTopBar(title = "History")
        if (state.loaded && state.groups.isEmpty()) {
            WarriorCard {
                Text("No sessions yet", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Logged workouts group here by day, using Saturday → Friday weeks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
                Spacer(Modifier.height(12.dp))
                WarriorButton(text = "+ Log Workout", onClick = onStartWorkout)
            }
        } else {
            LazyColumn {
                state.groups.forEach { group ->
                    item(key = "header-${group.key}") {
                        Text(
                            group.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 14.dp, bottom = 6.dp, start = 2.dp),
                        )
                    }
                    item(key = "card-${group.key}") {
                        WarriorCard {
                            group.sessions.forEachIndexed { index, session ->
                                SessionRow(
                                    session = session,
                                    showDivider = index != group.sessions.lastIndex,
                                    onClick = { onOpenSession(session.id) },
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun SessionRow(session: TrainingSession, showDivider: Boolean, onClick: () -> Unit) {
    val first = session.activities.firstOrNull()
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(typeColor(first?.type), RoundedCornerShape(3.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(rowTitle(session), style = MaterialTheme.typography.titleMedium)
                Text(
                    "${DateFormats.short(session.date)} · " +
                        DateFormats.durationLabel(session.totalDuration.inWholeMinutes) +
                        " · felt ${session.overallFeeling.name.lowercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
            WarriorBadge(text = "INT ${session.overallIntensity}", highlight = true)
        }
        if (showDivider) {
            androidx.compose.material3.HorizontalDivider(color = com.warrior.core.designsystem.theme.Outline)
        }
    }
}

private fun rowTitle(session: TrainingSession): String {
    val first = session.activities.firstOrNull() ?: return "Session"
    val rounds = session.totalRounds
    val extra = session.activities.size - 1
    return buildString {
        append(first.type.label)
        if (rounds > 0) append(" · $rounds rounds")
        if (extra > 0) append(" +$extra more")
    }
}

private fun typeColor(type: WorkoutType?) = when (type) {
    WorkoutType.HEAVY_BAG -> HeavyBag
    WorkoutType.MITT_WORK -> MittWork
    WorkoutType.SPARRING -> Sparring
    WorkoutType.CARDIO -> Cardio
    null -> TextMuted
}
