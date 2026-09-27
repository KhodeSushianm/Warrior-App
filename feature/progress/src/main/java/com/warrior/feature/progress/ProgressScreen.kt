package com.warrior.feature.progress

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warrior.core.common.time.DateFormats
import com.warrior.core.designsystem.components.ChartBar
import com.warrior.core.designsystem.components.DeltaText
import com.warrior.core.designsystem.components.DistributionBarRow
import com.warrior.core.designsystem.components.VolumeBarChart
import com.warrior.core.designsystem.components.WarriorCard
import com.warrior.core.designsystem.components.WarriorTopBar
import com.warrior.core.designsystem.theme.Accent
import com.warrior.core.designsystem.theme.Cardio
import com.warrior.core.designsystem.theme.HeavyBag
import com.warrior.core.designsystem.theme.MittWork
import com.warrior.core.designsystem.theme.Sparring
import com.warrior.core.designsystem.theme.TextMuted
import com.warrior.domain.progress.model.ProgressSnapshot
import com.warrior.domain.training.model.WorkoutType
import com.warrior.domain.training.model.label
import java.util.Locale

/**
 * Progress screen (Phase 8), matching the approved ui-preview:
 * week-over-week comparison card, 8-week training-volume bars, all-time
 * workout distribution (%) and top-4 focus distribution (time). Charts are
 * custom Compose Canvas components from :core:designsystem.
 */
@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
    ) {
        WarriorTopBar(title = "Progress")

        val snapshot = state.snapshot
        when {
            snapshot != null -> ProgressContent(snapshot, state.weekRangeLabel, state.volumeFractions)
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
private fun ProgressContent(
    snapshot: ProgressSnapshot,
    weekRangeLabel: String,
    volumeFractions: List<Float>,
) {
    val thisWeek = snapshot.thisWeek
    val lastWeek = snapshot.lastWeek
    val hasData = snapshot.volumeSeries.any { it.trainingMinutes > 0 }

    WarriorCard {
        Text(
            "${weekRangeLabel.uppercase(Locale.ENGLISH)}  vs  PREVIOUS WEEK",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
        Spacer(Modifier.height(10.dp))
        CompareRow(
            label = "Sessions",
            value = if (thisWeek.isEmpty) "—" else thisWeek.sessionCount.toString(),
            delta = thisWeek.sessionCount - lastWeek.sessionCount,
        )
        CompareRow(
            label = "Training time",
            value = if (thisWeek.isEmpty) "—" else DateFormats.durationLabel(thisWeek.trainingMinutes),
            delta = (thisWeek.trainingMinutes - lastWeek.trainingMinutes).toInt(),
            deltaUnit = "m",
        )
        CompareRow(
            label = "Rounds",
            value = if (thisWeek.isEmpty) "—" else thisWeek.totalRounds.toString(),
            delta = thisWeek.totalRounds - lastWeek.totalRounds,
        )
        CompareRow(
            label = "Avg intensity",
            value = if (thisWeek.isEmpty) "—" else thisWeek.averageIntensity.toString(),
            delta = thisWeek.averageIntensity - lastWeek.averageIntensity,
        )
        CompareRow(
            label = "Training days",
            value = if (thisWeek.isEmpty) "—" else thisWeek.trainingDays.toString(),
            delta = thisWeek.trainingDays - lastWeek.trainingDays,
        )
    }

    if (!hasData) {
        Spacer(Modifier.height(16.dp))
        WarriorCard {
            Text("No training data yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Log workouts and your volume trends and distributions will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        }
        Spacer(Modifier.height(8.dp))
        return
    }

    Spacer(Modifier.height(16.dp))
    SectionHeader("TRAINING VOLUME · LAST ${volumeFractions.size} WEEKS")
    Spacer(Modifier.height(8.dp))
    WarriorCard {
        VolumeBarChart(
            bars = volumeFractions.mapIndexed { index, fraction ->
                ChartBar(fraction = fraction, highlighted = index == volumeFractions.lastIndex)
            },
            labels = volumeFractions.indices.map { index ->
                if (index == volumeFractions.lastIndex) "NOW" else "W-${volumeFractions.lastIndex - index}"
            },
        )
    }

    Spacer(Modifier.height(16.dp))
    SectionHeader("WORKOUT DISTRIBUTION")
    Spacer(Modifier.height(8.dp))
    WarriorCard {
        val total = snapshot.workoutDistribution.values.sum()
        if (total <= 0L) {
            EmptyDistributionText()
        } else {
            snapshot.workoutDistribution.forEach { (type, minutes) ->
                val percent = Math.round(minutes * 100.0 / total).toInt()
                DistributionBarRow(
                    name = type.label,
                    valueLabel = "$percent%",
                    fraction = minutes.toFloat() / total.toFloat(),
                    color = typeColor(type),
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    SectionHeader("FOCUS DISTRIBUTION")
    Spacer(Modifier.height(8.dp))
    WarriorCard {
        val top = snapshot.topFocusAreas
        val maxMinutes = top.maxOfOrNull { it.minutes } ?: 0L
        if (top.isEmpty() || maxMinutes <= 0L) {
            EmptyDistributionText()
        } else {
            top.forEach { slice ->
                DistributionBarRow(
                    name = slice.focus.label,
                    valueLabel = DateFormats.durationLabel(slice.minutes),
                    fraction = slice.minutes.toFloat() / maxMinutes.toFloat(),
                    color = TextMuted,
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = TextMuted)
}

@Composable
private fun CompareRow(label: String, value: String, delta: Int, deltaUnit: String = "") {
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
        Spacer(Modifier.padding(5.dp))
        DeltaText(delta = delta, unit = deltaUnit)
    }
}

@Composable
private fun EmptyDistributionText() {
    Text(
        "No activities recorded yet.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextMuted,
    )
}

private fun typeColor(type: WorkoutType): Color = when (type) {
    WorkoutType.HEAVY_BAG -> HeavyBag
    WorkoutType.MITT_WORK -> MittWork
    WorkoutType.SPARRING -> Sparring
    WorkoutType.CARDIO -> Cardio
}
