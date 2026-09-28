package com.warrior.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.warrior.core.designsystem.theme.Accent
import com.warrior.core.designsystem.theme.Outline
import com.warrior.core.designsystem.theme.SurfaceVariant
import com.warrior.core.designsystem.theme.TextMuted
import com.warrior.core.designsystem.theme.TextPrimary
import kotlin.math.ceil

/**
 * Custom Compose Canvas charts (Phase 8) — no external chart libraries,
 * matching the approved ui-preview visual language. All components take plain
 * presentation data; feature layers map domain models into them.
 */

/** One bar of the training-volume chart. [fraction] is 0..1 of the max week. */
data class ChartBar(
    val fraction: Float,
    val highlighted: Boolean = false,
)

/** One calendar-month block of the training-days heatmap. */
data class HeatmapMonth(
    val label: String,
    val leadingBlanks: Int,
    val daysInMonth: Int,
    val trainedDays: Set<Int>,
    val todayDay: Int?,
)

/**
 * Vertical bar chart: rounded-top bars (8dp/4dp radii like the reference),
 * the highlighted bar in Accent, the rest in SurfaceVariant. Labels render
 * below with the exact same geometry (equal weights + 8dp gaps) so columns
 * always line up with their bars.
 */
@Composable
fun VolumeBarChart(
    bars: List<ChartBar>,
    labels: List<String>,
    modifier: Modifier = Modifier,
) {
    val idleColor = SurfaceVariant
    val activeColor = Accent
    // Phase 9 polish: bars grow/shrink smoothly when the derived flow re-emits.
    val animatedFractions = bars.map { bar ->
        animateFloatAsState(
            targetValue = bar.fraction.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 450),
            label = "volumeBar",
        ).value
    }
    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            if (bars.isEmpty()) return@Canvas
            val gap = 8.dp.toPx()
            val barWidth = (size.width - gap * (bars.size - 1)) / bars.size
            val minBarHeight = 4.dp.toPx()
            bars.forEachIndexed { index, bar ->
                val fraction = animatedFractions.getOrElse(index) { bar.fraction.coerceIn(0f, 1f) }
                if (fraction <= 0f) return@forEachIndexed
                val barHeight = maxOf(size.height * fraction, minBarHeight)
                val left = index * (barWidth + gap)
                drawPath(
                    path = barPath(
                        left = left,
                        top = size.height - barHeight,
                        right = left + barWidth,
                        bottom = size.height,
                        topRadius = 8.dp.toPx(),
                        bottomRadius = 4.dp.toPx(),
                    ),
                    color = if (bar.highlighted) activeColor else idleColor,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            labels.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Dot heatmap of training days: N calendar months side by side, each a
 * 7-column dot grid whose columns run Saturday→Friday (the app's central week
 * rule). Untrained = Outline, trained = TextPrimary, today = Accent.
 */
@Composable
fun TrainingHeatmapGrid(
    months: List<HeatmapMonth>,
    modifier: Modifier = Modifier,
) {
    if (months.isEmpty()) return
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val idleColor = Outline
    val trainedColor = TextPrimary
    val todayColor = Accent
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val monthGap = 12.dp
        val cellSize = ((maxWidth - monthGap * (months.size - 1)) / months.size / 7).coerceAtMost(16.dp)
        val rowCount = months.maxOf { ceil((it.leadingBlanks + it.daysInMonth) / 7f).toInt() }
        val labelHeight = 18.dp
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(labelHeight + cellSize * rowCount),
        ) {
            val cell = cellSize.toPx()
            val monthWidth = cell * 7
            val gap = monthGap.toPx()
            val gridTop = labelHeight.toPx()
            val totalWidth = monthWidth * months.size + gap * (months.size - 1)
            var blockLeft = (size.width - totalWidth) / 2f
            val dotRadius = (cell * 0.26f).coerceAtMost(4.dp.toPx())
            months.forEach { month ->
                val labelLayout = textMeasurer.measure(
                    text = month.label,
                    style = labelStyle.copy(color = TextMuted, fontWeight = FontWeight.Bold),
                )
                drawText(
                    textLayoutResult = labelLayout,
                    topLeft = Offset(blockLeft + (monthWidth - labelLayout.size.width) / 2f, 0f),
                )
                for (cellIndex in 0 until month.leadingBlanks + month.daysInMonth) {
                    val day = cellIndex - month.leadingBlanks + 1
                    if (day < 1) continue
                    val center = Offset(
                        x = blockLeft + (cellIndex % 7 + 0.5f) * cell,
                        y = gridTop + (cellIndex / 7 + 0.5f) * cell,
                    )
                    val color = when {
                        day == month.todayDay -> todayColor
                        day in month.trainedDays -> trainedColor
                        else -> idleColor
                    }
                    drawCircle(color = color, radius = dotRadius, center = center)
                }
                blockLeft += monthWidth + gap
            }
        }
    }
}

/**
 * Horizontal distribution row: fixed-width name, pill track (SurfaceVariant)
 * with a proportional fill, and a right-aligned value — the ".hbar" of the
 * reference prototype.
 */
@Composable
fun DistributionBarRow(
    name: String,
    valueLabel: String,
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val trackColor = SurfaceVariant
    // Phase 9 polish: fills ease into their target width on data changes.
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450),
        label = "distributionFill",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 9.dp),
    ) {
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(100.dp),
        )
        Spacer(Modifier.width(10.dp))
        Canvas(
            Modifier
                .weight(1f)
                .height(8.dp),
        ) {
            val corner = CornerRadius(size.height / 2f, size.height / 2f)
            drawRoundRect(color = trackColor, cornerRadius = corner)
            val fillWidth = size.width * animatedFraction
            if (fillWidth > 0f) {
                drawRoundRect(
                    color = color,
                    size = Size(fillWidth, size.height),
                    cornerRadius = corner,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            valueLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(44.dp),
        )
    }
}

/** Bar outline with independently rounded top/bottom corners (Path-based). */
private fun barPath(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    topRadius: Float,
    bottomRadius: Float,
): Path {
    val width = right - left
    val height = bottom - top
    val rt = minOf(topRadius, height / 2f, width / 2f)
    val rb = minOf(bottomRadius, height / 2f, width / 2f)
    return Path().apply {
        moveTo(left, bottom - rb)
        arcTo(Rect(left, bottom - 2 * rb, left + 2 * rb, bottom), 180f, -90f, false)
        lineTo(right - rb, bottom)
        arcTo(Rect(right - 2 * rb, bottom - 2 * rb, right, bottom), 90f, -90f, false)
        lineTo(right, top + rt)
        arcTo(Rect(right - 2 * rt, top, right, top + 2 * rt), 0f, -90f, false)
        lineTo(left + rt, top)
        arcTo(Rect(left, top, left + 2 * rt, top + 2 * rt), 270f, -90f, false)
        close()
    }
}
