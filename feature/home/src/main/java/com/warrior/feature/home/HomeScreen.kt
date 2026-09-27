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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warrior.core.common.time.DateFormats
import com.warrior.core.designsystem.components.DeltaText
import com.warrior.core.designsystem.components.HeatmapMonth
import com.warrior.core.designsystem.components.TrainingHeatmapGrid
import com.warrior.core.designsystem.components.WarriorButton
import com.warrior.core.designsystem.components.WarriorCard
import com.warrior.core.designsystem.components.WarriorEmptyState
import com.warrior.core.designsystem.components.WarriorLoadingBox
import com.warrior.core.designsystem.components.WarriorSectionHeader
import com.warrior.core.designsystem.components.WarriorTopBar
import com.warrior.core.designsystem.icons.WarriorIconPlus
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
            title = stringResource(R.string.home_wordmark),
            actions = {
                IconButton(onClick = onStartWorkout) {
                    Icon(
                        WarriorIconPlus,
                        contentDescription = stringResource(R.string.home_log_workout_cd),
                        tint = TextPrimary,
                    )
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
            !state.loaded -> WarriorLoadingBox()
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
    WarriorSectionHeader(stringResource(R.string.home_heatmap_title))
    Spacer(Modifier.height(8.dp))
    WarriorCard {
        TrainingHeatmapGrid(
            months = progress.heatmap.months.map { month ->
                HeatmapMonth(
                    label = DateFormats.monthLabel(month.year, month.month),
                    leadingBlanks = month.firstDayColumnOffset,
                    daysInMonth = month.daysInMonth,
                    trainedDays = month.trainedDays,
                    todayDay = month.todayDay,
                )
            },
        )
    }

    Spacer(Modifier.height(16.dp))
    WarriorSectionHeader(stringResource(R.string.home_recent_title))
    Spacer(Modifier.height(8.dp))
    RecentSessionsCard(progress.recentSessions, onOpenSession)

    Spacer(Modifier.height(16.dp))
    WarriorSectionHeader(stringResource(R.string.home_records_title))
    Spacer(Modifier.height(8.dp))
    PersonalRecordsCard(progress)

    Spacer(Modifier.height(16.dp))
    WarriorButton(text = stringResource(R.string.home_cta_log_workout), onClick = onStartWorkout)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ThisWeekCard(progress: HomeProgress, weekRangeLabel: String) {
    val thisWeek = progress.thisWeek
    val lastWeek = progress.lastWeek
    WarriorCard {
        Text(
            if (weekRangeLabel.isEmpty()) {
                stringResource(R.string.home_this_week)
            } else {
                stringResource(R.string.home_this_week_range, weekRangeLabel)
            },
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
                stringResource(R.string.home_training_time),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 8.dp),
            )
            DeltaText(
                delta = (thisWeek.trainingMinutes - lastWeek.trainingMinutes).toInt(),
                unit = stringResource(R.string.home_delta_unit_minutes),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat(
                value = thisWeek.sessionCount.toString(),
                label = stringResource(R.string.home_stat_sessions),
                delta = thisWeek.sessionCount - lastWeek.sessionCount,
            )
            MiniStat(
                value = thisWeek.totalRounds.toString(),
                label = stringResource(R.string.home_stat_rounds),
                delta = thisWeek.totalRounds - lastWeek.totalRounds,
            )
            MiniStat(
                value = if (thisWeek.isEmpty) "—" else thisWeek.averageIntensity.toString(),
                label = stringResource(R.string.home_stat_avg_intensity),
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
            WarriorEmptyState(
                title = stringResource(R.string.home_recent_empty_title),
                body = stringResource(R.string.home_recent_empty_body),
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
                    stringResource(
                        R.string.home_session_meta,
                        DateFormats.short(session.date),
                        DateFormats.durationLabel(session.totalDuration.inWholeMinutes),
                        session.overallIntensity,
                    ),
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
            label = stringResource(R.string.home_record_longest),
            value = if (records.isEmpty) "—" else DateFormats.durationLabel(records.longestSessionMinutes),
        )
        RecordRow(
            label = stringResource(R.string.home_record_most_rounds),
            value = if (records.isEmpty) "—" else records.mostRoundsInSession.toString(),
        )
        RecordRow(
            label = stringResource(R.string.home_record_streak),
            value = if (progress.streakWeeks > 0) {
                pluralStringResource(R.plurals.home_streak_weeks, progress.streakWeeks, progress.streakWeeks)
            } else {
                "—"
            },
        )
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

private fun typeColor(type: WorkoutType?) = when (type) {
    WorkoutType.HEAVY_BAG -> HeavyBag
    WorkoutType.MITT_WORK -> MittWork
    WorkoutType.SPARRING -> Sparring
    WorkoutType.CARDIO -> Cardio
    null -> TextMuted
}
