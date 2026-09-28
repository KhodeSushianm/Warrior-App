package com.warrior.feature.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warrior.core.common.time.DateFormats
import com.warrior.core.designsystem.components.WarriorButton
import com.warrior.core.designsystem.components.WarriorCard
import com.warrior.core.designsystem.components.WarriorEmptyState
import com.warrior.core.designsystem.components.WarriorLoadingBox
import com.warrior.core.designsystem.components.WarriorSectionHeader
import com.warrior.core.designsystem.components.WarriorTextField
import com.warrior.core.designsystem.components.WarriorTopBar
import com.warrior.core.designsystem.components.WeightTrendChart
import com.warrior.core.designsystem.theme.Accent
import com.warrior.core.designsystem.theme.Negative
import com.warrior.core.designsystem.theme.Positive
import com.warrior.core.designsystem.theme.TextMuted
import com.warrior.domain.progress.model.BodyErrorCode
import com.warrior.domain.progress.model.BodyMetric
import java.util.Locale

/**
 * Athlete body screen (Season 2 / Phase 12): current weight hero with trend,
 * key measurements, history and the add-measurement dialog. The 3D glass-boxer
 * visualization of this data arrives in Phase 18 on the same screen family.
 */
@Composable
fun BodyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BodyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
    ) {
        WarriorTopBar(
            title = stringResource(R.string.body_title),
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

        when {
            !state.loaded -> WarriorLoadingBox()
            state.metrics.isEmpty() -> {
                WarriorCard {
                    WarriorEmptyState(
                        title = stringResource(R.string.body_empty_title),
                        body = stringResource(R.string.body_empty_body),
                        actionLabel = stringResource(R.string.body_add),
                        onAction = viewModel::onAddOpen,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            else -> BodyContent(state, viewModel)
        }
        Spacer(Modifier.height(8.dp))
    }

    if (state.showDialog) {
        AddMetricDialog(state, viewModel)
    }

    if (state.pendingDelete != null) {
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismiss,
            title = { Text(stringResource(R.string.body_delete_title)) },
            text = { Text(stringResource(R.string.body_delete_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteConfirm) {
                    Text(stringResource(R.string.body_delete_confirm), color = Negative)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteDismiss) {
                    Text(stringResource(R.string.body_cancel))
                }
            },
        )
    }
}

@Composable
private fun BodyContent(state: BodyViewModel.UiState, viewModel: BodyViewModel) {
    val latest = state.latest ?: return

    // Hero: current weight + change vs previous measurement.
    WarriorCard(
        brush = androidx.compose.ui.graphics.Brush.linearGradient(
            listOf(Accent.copy(alpha = 0.14f), Accent.copy(alpha = 0f)),
        ),
    ) {
        Text(
            stringResource(R.string.body_latest_weight),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                formatKg(latest.weightKg),
                style = MaterialTheme.typography.displayLarge,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.body_unit_kg),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            state.previousWeightKg?.let { previous ->
                val delta = latest.weightKg - previous
                Text(
                    (if (delta > 0f) "+" else if (delta < 0f) "−" else "±") +
                        String.format(Locale.ENGLISH, "%.1f", kotlin.math.abs(delta)) +
                        " " + stringResource(R.string.body_unit_kg),
                    color = if (delta > 0f) Negative else if (delta < 0f) Positive else TextMuted,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Text(
            DateFormats.dayHeader(
                latest.date,
                todayLabel = stringResource(R.string.body_day_today),
                yesterdayLabel = stringResource(R.string.body_day_yesterday),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
    }

    if (state.trendPoints.size >= 2) {
        Spacer(Modifier.height(16.dp))
        WarriorSectionHeader(stringResource(R.string.body_trend_title))
        Spacer(Modifier.height(8.dp))
        WarriorCard {
            WeightTrendChart(points = state.trendPoints)
        }
    }

    // Key measurements (latest non-null values across history).
    Spacer(Modifier.height(16.dp))
    WarriorSectionHeader(stringResource(R.string.body_measurements_title))
    Spacer(Modifier.height(8.dp))
    WarriorCard {
        MeasurementRow(
            stringResource(R.string.body_height),
            latestNonNull(state.metrics) { it.heightCm }?.let { String.format(Locale.ENGLISH, "%.0f cm", it) } ?: "—",
        )
        MeasurementRow(
            stringResource(R.string.body_reach),
            latestNonNull(state.metrics) { it.reachCm }?.let { String.format(Locale.ENGLISH, "%.0f cm", it) } ?: "—",
        )
        MeasurementRow(
            stringResource(R.string.body_bodyfat),
            latestNonNull(state.metrics) { it.bodyFatPercent }?.let { String.format(Locale.ENGLISH, "%.1f%%", it) } ?: "—",
        )
        MeasurementRow(
            stringResource(R.string.body_hr),
            latestNonNull(state.metrics) { it.restingHeartRate }?.let { "$it bpm" } ?: "—",
        )
    }

    // History.
    Spacer(Modifier.height(16.dp))
    WarriorSectionHeader(stringResource(R.string.body_history_title))
    Spacer(Modifier.height(8.dp))
    WarriorCard {
        state.metrics.forEachIndexed { index, metric ->
            HistoryRow(
                metric = metric,
                onDelete = { viewModel.onDeleteRequest(metric.id) },
            )
            if (index != state.metrics.lastIndex) {
                androidx.compose.material3.HorizontalDivider(
                    color = com.warrior.core.designsystem.theme.Outline,
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    WarriorButton(text = stringResource(R.string.body_add), onClick = viewModel::onAddOpen)
}

@Composable
private fun HistoryRow(metric: BodyMetric, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                DateFormats.dayHeader(
                    metric.date,
                    todayLabel = stringResource(R.string.body_day_today),
                    yesterdayLabel = stringResource(R.string.body_day_yesterday),
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                buildString {
                    append(formatKg(metric.weightKg))
                    append(" ")
                    metric.bodyFatPercent?.let { append("· ").append(String.format(Locale.ENGLISH, "%.1f%%", it)).append(" ") }
                    metric.restingHeartRate?.let { append("· ").append(it).append(" bpm") }
                }.trim(),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
            )
        }
        Text(
            "✕",
            color = TextMuted,
            modifier = Modifier
                .clickable(onClick = onDelete)
                .padding(8.dp),
        )
    }
}

@Composable
private fun MeasurementRow(label: String, value: String) {
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

@Composable
private fun AddMetricDialog(state: BodyViewModel.UiState, viewModel: BodyViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::onDialogCancel,
        title = { Text(stringResource(R.string.body_dialog_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                WarriorTextField(
                    value = state.weightText,
                    onValueChange = viewModel::onWeightChange,
                    label = stringResource(R.string.body_field_weight),
                    keyboardType = KeyboardType.Decimal,
                )
                Spacer(Modifier.height(8.dp))
                WarriorTextField(
                    value = state.heightText,
                    onValueChange = viewModel::onHeightChange,
                    label = stringResource(R.string.body_field_height),
                    keyboardType = KeyboardType.Decimal,
                )
                Spacer(Modifier.height(8.dp))
                WarriorTextField(
                    value = state.reachText,
                    onValueChange = viewModel::onReachChange,
                    label = stringResource(R.string.body_field_reach),
                    keyboardType = KeyboardType.Decimal,
                )
                Spacer(Modifier.height(8.dp))
                WarriorTextField(
                    value = state.bodyFatText,
                    onValueChange = viewModel::onBodyFatChange,
                    label = stringResource(R.string.body_field_bodyfat),
                    keyboardType = KeyboardType.Decimal,
                )
                Spacer(Modifier.height(8.dp))
                WarriorTextField(
                    value = state.heartRateText,
                    onValueChange = viewModel::onHeartRateChange,
                    label = stringResource(R.string.body_field_hr),
                    keyboardType = KeyboardType.Number,
                )
                if (state.errorCodes.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    state.errorCodes.forEach { code ->
                        Text(
                            stringResource(code.labelRes),
                            color = Negative,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::onSave) {
                Text(stringResource(R.string.body_save), color = Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::onDialogCancel) {
                Text(stringResource(R.string.body_cancel))
            }
        },
    )
}

/** Error code -> localized copy (i18n-ready, like auth/workout). */
private val BodyErrorCode.labelRes: Int
    @StringRes
    get() = when (this) {
        BodyErrorCode.WEIGHT_RANGE -> R.string.body_error_weight
        BodyErrorCode.HEIGHT_RANGE -> R.string.body_error_height
        BodyErrorCode.REACH_RANGE -> R.string.body_error_reach
        BodyErrorCode.BODY_FAT_RANGE -> R.string.body_error_bodyfat
        BodyErrorCode.HEART_RATE_RANGE -> R.string.body_error_hr
        BodyErrorCode.INVALID_DATE -> R.string.body_error_date
        BodyErrorCode.UNEXPECTED -> R.string.body_error_unexpected
    }

private fun formatKg(value: Float): String = String.format(Locale.ENGLISH, "%.1f", value)

private fun <T> latestNonNull(metrics: List<BodyMetric>, selector: (BodyMetric) -> T?): T? {
    metrics.forEach { metric -> selector(metric)?.let { return it } }
    return null
}
