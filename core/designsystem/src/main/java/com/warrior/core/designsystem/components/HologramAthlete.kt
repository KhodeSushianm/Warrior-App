package com.warrior.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warrior.core.designsystem.theme.Accent
import com.warrior.core.designsystem.theme.TextMuted
import kotlin.math.PI
import kotlin.math.sin

/** One data chip orbiting the hologram. */
data class HoloStat(
    val label: String,
    val value: String,
    val anchor: HoloAnchor = HoloAnchor.WAIST,
)

/** Body point (as image-space fractions) a chip's leader line points at. */
enum class HoloAnchor(val fx: Float, val fy: Float) {
    HEAD(0.52f, 0.11f),
    CHEST(0.50f, 0.28f),
    ARMS(0.56f, 0.24f),
    WAIST(0.48f, 0.45f),
    LEGS(0.52f, 0.72f),
}

// Glass/hologram palette (icy tones + brand red).
private val Ice = Color(0xFFBFE9FF)
private val IceBright = Color(0xFFEAF6FF)
private val ChipBg = Color(0xFF1B1B24)

/**
 * The WARRIOR digital athlete (Phase 15, revised per owner: a **realistic
 * muscular holographic human**, not a drawn stick figure). The figure is a
 * pre-rendered glass-body render (feature asset, alpha-extracted offline)
 * composited live with: breathing scale/alpha, a clipped scan sweep +
 * scanlines, a rotating red-arc pedestal, ambient vignette — and the body
 * stats orbiting on a pseudo-3D ellipse ([HoloOrbit]): depth drives z-order
 * (chips behind render *under* the figure), scale and alpha, with thin
 * leader lines to each metric's body anchor.
 *
 * Domain-agnostic: the feature layer maps BodyMetric -> [HoloStat] and
 * supplies the figure [Painter].
 */
@Composable
fun HologramAthlete(
    stats: List<HoloStat>,
    figure: Painter,
    modifier: Modifier = Modifier.height(420.dp),
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 9.sp,
        letterSpacing = 0.6.sp,
    )
    val valueStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp)

    val chipLayouts = remember(stats, labelStyle, valueStyle) {
        stats.map { stat ->
            ChipLayout(
                label = textMeasurer.measure(stat.label, style = labelStyle),
                value = textMeasurer.measure(stat.value, style = valueStyle),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "hologram")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing)),
        label = "holoRotation",
    )
    val scan01 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3_800, easing = LinearEasing)),
        label = "holoScan",
    )
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(tween(4_200, easing = LinearEasing)),
        label = "holoBreath",
    )

    Canvas(
        modifier = modifier.fillMaxWidth(),
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // ----- figure rect: fill the height, centered -----
        val intrinsic = figure.intrinsicSize
        val imgAspect = if (intrinsic.width > 0f && intrinsic.height > 0f) {
            intrinsic.width / intrinsic.height
        } else {
            1f
        }
        val figH = h * 0.96f
        val figW = figH * imgAspect
        val figLeft = cx - figW / 2f
        val figTop = h * 0.01f
        val feetY = figTop + figH * 0.97f

        val breathScale = 1f + 0.010f * sin(breath)
        val breathAlpha = 0.90f + 0.10f * (0.5f + 0.5f * sin(breath))

        val anchors = HoloAnchor.entries.associateWith { anchor ->
            Offset(figLeft + anchor.fx * figW, figTop + anchor.fy * figH)
        }

        // ----- ambient vignette -----
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Ice.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(cx, figTop + figH * 0.45f),
                radius = figH * 0.80f,
            ),
            radius = figH * 0.80f,
            center = Offset(cx, figTop + figH * 0.45f),
        )

        val placements = stats.indices.map { HoloOrbit.placement(it, stats.size, rotation) }

        // ----- back-half chips (render UNDER the figure) -----
        drawChipLayer(stats, chipLayouts, placements, front = false, w = w, h = h, cx = cx, anchors = anchors)

        // ----- pedestal under the feet -----
        val pedRx = figW * 0.34f
        val pedRy = pedRx * 0.26f
        val pedCenter = Offset(cx, feetY)
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Ice.copy(alpha = 0.26f), Color.Transparent),
                center = pedCenter,
                radius = pedRx,
            ),
            topLeft = Offset(cx - pedRx, pedCenter.y - pedRy),
            size = Size(pedRx * 2, pedRy * 2),
        )
        drawOval(
            color = Ice.copy(alpha = 0.30f),
            topLeft = Offset(cx - pedRx, pedCenter.y - pedRy),
            size = Size(pedRx * 2, pedRy * 2),
            style = Stroke(width = 1.3.dp.toPx()),
        )
        val rotDeg = rotation * 180f / PI.toFloat()
        repeat(3) { k ->
            drawArc(
                color = Accent.copy(alpha = 0.85f),
                startAngle = rotDeg + k * 120f,
                sweepAngle = 44f,
                useCenter = false,
                topLeft = Offset(cx - pedRx * 1.14f, pedCenter.y - pedRy * 1.14f),
                size = Size(pedRx * 2.28f, pedRy * 2.28f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        // ----- the holographic human (breathing) -----
        // Breathing: gentle scale around the feet + luminance pulse.
        scale(breathScale, pivot = Offset(cx, feetY)) {
            translate(figLeft, figTop) {
                with(figure) {
                    draw(Size(figW, figH), alpha = breathAlpha)
                }
            }
        }

        // ----- scanlines + moving sweep clipped to the figure viewport -----
        clipRect(left = figLeft, top = figTop, right = figLeft + figW, bottom = figTop + figH) {
            val gap = 6.dp.toPx()
            var y = figTop
            while (y < figTop + figH) {
                drawLine(
                    color = IceBright.copy(alpha = 0.05f),
                    start = Offset(figLeft, y),
                    end = Offset(figLeft + figW, y),
                    strokeWidth = 1.dp.toPx(),
                )
                y += gap
            }
            val sweepH = 26.dp.toPx()
            val sweepY = figTop + scan01 * figH
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Ice.copy(alpha = 0.22f), Color.Transparent),
                    startY = sweepY - sweepH / 2f,
                    endY = sweepY + sweepH / 2f,
                ),
                topLeft = Offset(figLeft, sweepY - sweepH / 2f),
                size = Size(figW, sweepH),
            )
        }

        // ----- front-half chips (render OVER the figure) -----
        drawChipLayer(stats, chipLayouts, placements, front = true, w = w, h = h, cx = cx, anchors = anchors)
    }
}

private class ChipLayout(
    val label: androidx.compose.ui.text.TextLayoutResult,
    val value: androidx.compose.ui.text.TextLayoutResult,
)

private fun DrawScope.drawChipLayer(
    stats: List<HoloStat>,
    layouts: List<ChipLayout>,
    placements: List<OrbitPlacement>,
    front: Boolean,
    w: Float,
    h: Float,
    cx: Float,
    anchors: Map<HoloAnchor, Offset>,
) {
    if (stats.isEmpty()) return
    val orbitY = h * 0.40f
    val rx = w * 0.40f
    val ry = h * 0.26f
    val padH = 7.dp.toPx()
    val padV = 5.dp.toPx()
    stats.forEachIndexed { index, stat ->
        val p = placements[index]
        if (p.inFront != front) return@forEachIndexed
        val layout = layouts[index]
        val chipW = maxOf(layout.label.size.width, layout.value.size.width) + padH * 2
        val chipH = layout.label.size.height + layout.value.size.height + padV * 2
        val rawX = cx + rx * p.unitX
        val chipCx = rawX.coerceIn(chipW / 2f + 2.dp.toPx(), w - chipW / 2f - 2.dp.toPx())
        val chipCy = orbitY + ry * p.depth
        val alpha = p.alpha

        anchors[stat.anchor]?.let { anchor ->
            drawLine(
                color = Ice.copy(alpha = 0.10f + 0.22f * p.depthFactor),
                start = Offset(chipCx, chipCy),
                end = anchor,
                strokeWidth = 1.dp.toPx(),
            )
        }

        scale(p.scale, pivot = Offset(chipCx, chipCy)) {
            val left = chipCx - chipW / 2f
            val top = chipCy - chipH / 2f
            drawRoundRect(
                color = ChipBg.copy(alpha = (0.55f + 0.35f * p.depthFactor) * alpha),
                topLeft = Offset(left, top),
                size = Size(chipW, chipH),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
            )
            drawRoundRect(
                color = Ice.copy(alpha = (0.18f + 0.30f * p.depthFactor) * alpha),
                topLeft = Offset(left, top),
                size = Size(chipW, chipH),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                style = Stroke(width = 1.dp.toPx()),
            )
            drawText(
                textLayoutResult = layout.label,
                color = TextMuted.copy(alpha = alpha),
                topLeft = Offset(left + (chipW - layout.label.size.width) / 2f, top + padV),
            )
            drawText(
                textLayoutResult = layout.value,
                color = IceBright.copy(alpha = alpha),
                topLeft = Offset(
                    left + (chipW - layout.value.size.width) / 2f,
                    top + padV + layout.label.size.height,
                ),
            )
        }
    }
}
