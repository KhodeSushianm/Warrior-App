package com.warrior.feature.workout

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warrior.core.common.time.DateFormats
import com.warrior.core.designsystem.components.WarriorButton
import com.warrior.core.designsystem.components.WarriorButtonVariant
import com.warrior.core.designsystem.components.WarriorCard
import com.warrior.core.designsystem.components.WarriorChip
import com.warrior.core.designsystem.components.WarriorSectionHeader
import com.warrior.core.designsystem.components.WarriorTopBar
import com.warrior.core.designsystem.theme.Accent
import com.warrior.core.designsystem.theme.Positive
import com.warrior.core.designsystem.theme.SurfaceVariant
import com.warrior.core.designsystem.theme.TextMuted
import com.warrior.core.designsystem.theme.TextPrimary
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.WorkoutType
import com.warrior.domain.training.model.label
import kotlinx.coroutines.delay

/**
 * Live round timer screen (Season 2 / Phase 14). Big glanceable countdown,
 * WORK/REST phase ring, audible + haptic phase cues, and one-tap save of the
 * trained session. The screen drives `viewModel.tick()`; all timing logic is
 * in the (fake-clock testable) ViewModel. Screen stays on while running.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveTimerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiveTimerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Keep the screen on while a workout is running.
    val activity = context as? Activity
    DisposableEffect(state.status) {
        val running = state.status == TimerStatus.RUNNING
        if (running) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Drive the state machine ~5 times a second while running.
    LaunchedEffect(state.status) {
        if (state.status == TimerStatus.RUNNING) {
            while (true) {
                viewModel.tick()
                delay(200)
            }
        }
    }

    // Phase cues: beep + vibration.
    val feedback = remember { TimerFeedback(context) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LiveTimerViewModel.TimerEvent.WorkStarted -> feedback.onWorkStart()
                is LiveTimerViewModel.TimerEvent.RestStarted -> feedback.onRestStart()
                LiveTimerViewModel.TimerEvent.Finished -> feedback.onFinished()
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { feedback.release() }
    }

    if (state.isSaved) {
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
            title = stringResource(R.string.timer_title),
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

        when (state.status) {
            TimerStatus.IDLE -> SetupPane(state, viewModel)
            TimerStatus.RUNNING, TimerStatus.PAUSED -> RunPane(state, viewModel)
            TimerStatus.FINISHED -> FinishPane(state, viewModel, onBack)
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ---------- panes ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupPane(state: LiveTimerViewModel.UiState, viewModel: LiveTimerViewModel) {
    WarriorCard {
        ConfigRow(
            label = stringResource(R.string.timer_config_work),
            value = stringResource(R.string.timer_seconds, state.config.workSeconds),
            onMinus = { viewModel.onWorkChange(-5) },
            onPlus = { viewModel.onWorkChange(5) },
        )
        ConfigRow(
            label = stringResource(R.string.timer_config_rest),
            value = stringResource(R.string.timer_seconds, state.config.restSeconds),
            onMinus = { viewModel.onRestChange(-5) },
            onPlus = { viewModel.onRestChange(5) },
        )
        ConfigRow(
            label = stringResource(R.string.timer_config_rounds),
            value = state.config.rounds.toString(),
            onMinus = { viewModel.onRoundsChange(-1) },
            onPlus = { viewModel.onRoundsChange(1) },
        )
    }

    Spacer(Modifier.height(16.dp))
    WarriorSectionHeader(stringResource(R.string.timer_type_header))
    Spacer(Modifier.height(8.dp))
    WarriorCard {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            WorkoutType.entries.forEach { type ->
                WarriorChip(
                    label = type.label,
                    selected = type == state.config.type,
                    onClick = { viewModel.onTypeChange(type) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        WarriorSectionHeader(stringResource(R.string.timer_focus_header))
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FocusArea.entries.forEach { focus ->
                WarriorChip(
                    label = focus.label,
                    selected = focus == state.config.focus,
                    onClick = { viewModel.onFocusChange(focus) },
                )
            }
        }
    }

    Spacer(Modifier.height(20.dp))
    WarriorButton(text = stringResource(R.string.timer_start), onClick = viewModel::onStart)
}

@Composable
private fun RunPane(state: LiveTimerViewModel.UiState, viewModel: LiveTimerViewModel) {
    val phaseColor = when (state.phase) {
        TimerPhase.WORK -> Accent
        TimerPhase.REST -> Positive
    }
    val phaseTotal = state.phaseTotalMillis.coerceAtLeast(1)
    val progress = 1f - (state.remainingMillis.toFloat() / phaseTotal).coerceIn(0f, 1f)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(
                R.string.timer_round_label,
                state.roundIndex + 1,
                state.config.rounds,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
        Spacer(Modifier.height(16.dp))

        // Progress ring + countdown.
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(240.dp)) {
                val stroke = 12.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = androidx.compose.ui.geometry.Offset(stroke / 2f, stroke / 2f)
                drawArc(
                    color = SurfaceVariant,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = phaseColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatClock(state.remainingMillis),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                )
                Text(
                    stringResource(
                        if (state.phase == TimerPhase.WORK) R.string.timer_work else R.string.timer_rest,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = phaseColor,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        // Round dots.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(state.config.rounds.coerceAtMost(20)) { index ->
                val done = index < state.roundIndex
                val current = index == state.roundIndex
                Box(
                    Modifier
                        .size(if (current) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                done -> phaseColor
                                current -> Accent
                                else -> SurfaceVariant
                            },
                        ),
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            WarriorButton(
                text = stringResource(R.string.timer_skip),
                onClick = viewModel::onSkipPhase,
                variant = WarriorButtonVariant.GHOST,
                modifier = Modifier.weight(1f),
            )
            if (state.status == TimerStatus.RUNNING) {
                WarriorButton(
                    text = stringResource(R.string.timer_pause),
                    onClick = viewModel::onPause,
                    modifier = Modifier.weight(1f),
                )
            } else {
                WarriorButton(
                    text = stringResource(R.string.timer_resume),
                    onClick = viewModel::onResume,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        WarriorButton(
            text = stringResource(R.string.timer_end),
            onClick = viewModel::onEndEarly,
            variant = WarriorButtonVariant.DANGER,
        )
    }
}

@Composable
private fun FinishPane(state: LiveTimerViewModel.UiState, viewModel: LiveTimerViewModel, onBack: () -> Unit) {
    val blockSeconds = state.completedRounds * state.config.workSeconds +
        (state.completedRounds - 1).coerceAtLeast(0) * state.config.restSeconds
    WarriorCard(
        brush = androidx.compose.ui.graphics.Brush.linearGradient(
            listOf(Accent.copy(alpha = 0.16f), Accent.copy(alpha = 0f)),
        ),
    ) {
        Text(stringResource(R.string.timer_finished_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.timer_summary_rounds, state.completedRounds),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
        Text(
            stringResource(
                R.string.timer_summary_time,
                DateFormats.durationLabel(blockSeconds.toLong() / 60),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
        Text(
            "${state.config.type.label} · ${state.config.focus.label}",
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
    if (state.saveFailed) {
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.timer_save_failed),
            color = com.warrior.core.designsystem.theme.Negative,
            style = MaterialTheme.typography.labelLarge,
        )
    }
    Spacer(Modifier.height(16.dp))
    if (state.completedRounds > 0) {
        WarriorButton(
            text = stringResource(if (state.isSaving) R.string.workout_action_saving else R.string.timer_save),
            onClick = viewModel::onSave,
            enabled = !state.isSaving,
        )
        Spacer(Modifier.height(10.dp))
    }
    WarriorButton(
        text = stringResource(R.string.timer_discard),
        onClick = onBack,
        variant = WarriorButtonVariant.GHOST,
    )
}

// ---------- helpers ----------

@Composable
private fun ConfigRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        RoundButton("−", onMinus)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(72.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        RoundButton("+", onPlus)
    }
}

@Composable
private fun RoundButton(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(SurfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
    }
}

private fun formatClock(millis: Long): String {
    val totalSeconds = (millis + 999) / 1000 // round up so it shows 3:00 at start, 0:00 at end
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/** Beeps + vibration for phase changes (STREAM_MUSIC needs no permission; VIBRATE is declared). */
private class TimerFeedback(context: Context) {

    private val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }.getOrNull()

    @Suppress("DEPRECATION")
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun onWorkStart() {
        tone(ToneGenerator.TONE_PROP_BEEP)
        vibrate(longArrayOf(0, 300))
    }

    fun onRestStart() {
        tone(ToneGenerator.TONE_PROP_ACK)
        vibrate(longArrayOf(0, 150, 120, 150))
    }

    fun onFinished() {
        tone(ToneGenerator.TONE_CDMA_CONFIRM)
        vibrate(longArrayOf(0, 250, 150, 250, 150, 500))
    }

    fun release() {
        runCatching { tone?.release() }
    }

    private fun tone(t: Int) {
        runCatching { tone?.startTone(t, 220) }
    }

    @Suppress("DEPRECATION")
    private fun vibrate(pattern: LongArray) {
        runCatching {
            val v = vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                v.vibrate(pattern, -1)
            }
        }
    }
}
