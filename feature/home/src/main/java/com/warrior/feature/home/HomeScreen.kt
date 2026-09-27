package com.warrior.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.warrior.core.designsystem.components.DeltaText
import com.warrior.core.designsystem.components.WarriorButton
import com.warrior.core.designsystem.components.WarriorCard
import com.warrior.core.designsystem.components.WarriorTopBar
import com.warrior.core.designsystem.icons.WarriorIconPlus
import com.warrior.core.designsystem.theme.Accent
import com.warrior.core.designsystem.theme.Cardio
import com.warrior.core.designsystem.theme.HeavyBag
import com.warrior.core.designsystem.theme.MittWork
import com.warrior.core.designsystem.theme.Outline
import com.warrior.core.designsystem.theme.Sparring
import com.warrior.core.designsystem.theme.TextMuted
import com.warrior.core.designsystem.theme.TextPrimary
import com.warrior.domain.progress.model.HomeProgress
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.model.WorkoutType
import com.warrior.domain.training.model.rowTitle

/**
 * Home dashboard (Phase 7) — live-derived numbers only, per ui-preview:
 * THIS WEEK card (training time + deltas vs last week), recent sessions,
 * personal records (longest / most rounds / streak) and the log CTA.
 * The heatmap card belongs to Phase 8 (Charts).
 */
@Composable
fun HomeScreen(
    onStartWorkout: () -> Unit,
    onOpenSession: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
    ) {
        WarriorTopBar(
            title = "WARRIOR",
            actions = {
                IconButton(onClick = onStartWorkout) {
                    Icon(WarriorIconPlus, contentDescription = "Log workout", tint = TextPrimary)
                }
            },
        )

        val progress = state.progress
        when {
            progress != null -> HomeContent(
                progress = progress,
                weekRangeLabel = state.weekRangeLabel,
                onStartWorkout = onStartWorkout,
                onOpenSession = onOpenSession,
            )
            !state.loaded -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 120.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Accent)
            }
            // Loaded with a null snapshot = signed out; root nav swaps to Auth.
        }
    }
}

@Composable
private fun HomeContent(
    progress: HomeProgress,
    weekRangeLabel: String,
    onStartWorkout: () -> Unit,
    onOpenSession: (Long) -> Unit,
) {
    ThisWeekCard(progress, weekRangeLabel)

    Spacer(Modifier.height(16.dp))
    SectionHeader("RECENT SESSIONS")
    Spacer(Modifier.height(8.dp))
    RecentSessionsCard(progress.recentSessions, onOpenSession)

    Spacer(Modifier.height(16.dp))
    SectionHeader("PERSONAL RECORDS")
    Spacer(Modifier.height(8.dp))
    PersonalRecordsCard(progress)

    Spacer(Modifier.height(16.dp))
    WarriorButton(text = "+ Log Workout", onClick = onStartWorkout)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = TextMuted)
}

@Composable
private fun ThisWeekCard(progress: HomeProgress, weekRangeLabel: String) {
    val thisWeek = progress.thisWeek
    val lastWeek = progress.lastWeek
    WarriorCard {
        Text(
            if (weekRangeLabel.isEmpty()) "THIS WEEK" else "THIS WEEK · $weekRangeLabel",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (thisWeek.isEmpty) "—" else DateFormats.durationLabel(thisWeek.trainingMinutes),
                style = MaterialTheme.typography.displayLarge,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "training time",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 8.dp),
            )
            DeltaText(
                delta = (thisWeek.trainingMinutes - lastWeek.trainingMinutes).toInt(),
                unit = "m",
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat(
                value = thisWeek.sessionCount.toString(),
                label = "SESSIONS",
                delta = thisWeek.sessionCount - lastWeek.sessionCount,
            )
            MiniStat(
                value = thisWeek.totalRounds.toString(),
                label = "ROUNDS",
                delta = thisWeek.totalRounds - lastWeek.totalRounds,
            )
            MiniStat(
                value = if (thisWeek.isEmpty) "—" else thisWeek.averageIntensity.toString(),
                label = "AVG INTENSITY",
                delta = thisWeek.averageIntensity - lastWeek.averageIntensity,
            )
        }
    }
}

@Composable
private fun RowScope.MiniStat(value: String, label: String, delta: Int) {
    WarriorCard(modifier = Modifier.weight(1f)) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        DeltaText(delta = delta)
    }
}

@Composable
private fun RecentSessionsCard(sessions: List<TrainingSession>, onOpenSession: (Long) -> Unit) {
    WarriorCard {
        if (sessions.isEmpty()) {
            Text("No sessions yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Log your first workout and your latest sessions will show up here.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        } else {
            sessions.forEachIndexed { index, session ->
                RecentRow(
                    session = session,
                    showDivider = index != sessions.lastIndex,
                    onClick = { onOpenSession(session.id) },
                )
            }
        }
    }
}

@Composable
private fun RecentRow(session: TrainingSession, showDivider: Boolean, onClick: () -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp),
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(typeColor(session.activities.firstOrNull()?.type), RoundedCornerShape(3.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(session.rowTitle, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${DateFormats.short(session.date)} · " +
                        DateFormats.durationLabel(session.totalDuration.inWholeMinutes) +
                        " · int ${session.overallIntensity}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(color = Outline)
        }
    }
}

@Composable
private fun PersonalRecordsCard(progress: HomeProgress) {
    val records = progress.records
    WarriorCard {
        RecordRow(
            label = "Longest session",
            value = if (records.isEmpty) "—" else DateFormats.durationLabel(records.longestSessionMinutes),
        )
        RecordRow(
            label = "Most rounds in one session",
            value = if (records.isEmpty) "—" else records.mostRoundsInSession.toString(),
        )
        RecordRow(label = "Current streak", value = streakLabel(progress.streakWeeks))
    }
}

@Composable
private fun RecordRow(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

private fun streakLabel(weeks: Int): String = when {
    weeks <= 0 -> "—"
    weeks == 1 -> "1 week"
    else -> "$weeks weeks"
}

private fun typeColor(type: WorkoutType?) = when (type) {
    WorkoutType.HEAVY_BAG -> HeavyBag
    WorkoutType.MITT_WORK -> MittWork
    WorkoutType.SPARRING -> Sparring
    WorkoutType.CARDIO -> Cardio
    null -> TextMuted
}
