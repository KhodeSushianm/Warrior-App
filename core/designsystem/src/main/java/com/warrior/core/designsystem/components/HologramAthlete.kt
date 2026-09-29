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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
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

/** Body point a chip's leader line points at. */
enum class HoloAnchor { HEAD, CHEST, ARMS, WAIST, LEGS }

// Glass/hologram palette (self-contained: icy tones + brand red).
private val Ice = Color(0xFFBFE9FF)
private val IceBright = Color(0xFFEAF6FF)
private val ChipBg = Color(0xFF1B1B24)

/**
 * The WARRIOR digital athlete (Phase 15, owner spec): a stylized **glass
 * boxer** rendered entirely with Compose Canvas — translucent icy body with
 * rim-light strokes and bloom, red brand gloves and shorts, animated scan
 * sweep + breathing pulse over a rotating dashed pedestal — with the body
 * stats orbiting it on a pseudo-3D ellipse: depth drives z-order (behind /
 * in front of the figure), scale and alpha ([HoloOrbit]).
 *
 * Domain-agnostic: the feature layer maps BodyMetric -> [HoloStat].
 */
@Composable
fun HologramAthlete(
    stats: List<HoloStat>,
    modifier: Modifier = Modifier.height(400.dp),
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 9.sp,
        letterSpacing = 0.6.sp,
    )
    val valueStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp)

    // Pre-measure chip texts (cheap recomputes only when stats change).
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
    val breath01 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(tween(4_200, easing = LinearEasing)),
        label = "holoBreath",
    )

    Canvas(modifier = modifier.fillMaxWidth()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // ----- figure metrics -----
        val figH = h * 0.66f
        val feetY = h * 0.80f
        val headR = figH * 0.080f
        val headCY = feetY - figH + headR
        val shoulderY = headCY + headR + figH * 0.045f
        val shoulderW = figH * 0.31f
        val waistY = shoulderY + figH * 0.29f
        val waistW = figH * 0.185f
        val hipsY = waistY + figH * 0.075f
        val limbW = figH * 0.052f
        val gloveR = figH * 0.050f

        val breathScale = 1f + 0.012f * sin(breath01)
        val gloveBob = figH * 0.008f * sin(breath01 * 1.7f)

        // ----- ambient vignette -----
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Ice.copy(alpha = 0.07f), Color.Transparent),
                center = Offset(cx, feetY - figH * 0.45f),
                radius = figH * 0.95f,
            ),
            radius = figH * 0.95f,
            center = Offset(cx, feetY - figH * 0.45f),
        )

        // ----- back-half chips (behind the figure) -----
        val placements = stats.indices.map { HoloOrbit.placement(it, stats.size, rotation) }
        drawChipLayer(
            stats = stats,
            layouts = chipLayouts,
            placements = placements,
            front = false,
            w = w,
            h = h,
            cx = cx,
            anchors = anchors(
                cx = cx,
                headCY = headCY,
                shoulderY = shoulderY,
                shoulderW = shoulderW,
                waistY = waistY,
                hipsY = hipsY,
                feetY = feetY,
                figH = figH,
            ),
            labelColor = TextMuted,
        )

        // ----- pedestal -----
        val pedRx = figH * 0.32f
        val pedRy = figH * 0.080f
        val pedCenter = Offset(cx, feetY + figH * 0.015f)
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Ice.copy(alpha = 0.22f), Color.Transparent),
                center = pedCenter,
                radius = pedRx,
            ),
            topLeft = Offset(cx - pedRx, pedCenter.y - pedRy),
            size = Size(pedRx * 2, pedRy * 2),
        )
        drawOval(
            color = Ice.copy(alpha = 0.32f),
            topLeft = Offset(cx - pedRx, pedCenter.y - pedRy),
            size = Size(pedRx * 2, pedRy * 2),
            style = Stroke(width = 1.4.dp.toPx()),
        )
        val rotDeg = (rotation * 180f / PI.toFloat())
        repeat(3) { k ->
            drawArc(
                color = Accent.copy(alpha = 0.85f),
                startAngle = rotDeg + k * 120f,
                sweepAngle = 44f,
                useCenter = false,
                topLeft = Offset(cx - pedRx * 1.16f, pedCenter.y - pedRy * 1.16f),
                size = Size(pedRx * 2.32f, pedRy * 2.32f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        // ----- the glass boxer -----
        scale(breathScale, pivot = Offset(cx, feetY)) {
            drawBoxer(
                cx = cx,
                figH = figH,
                feetY = feetY,
                headR = headR,
                headCY = headCY,
                shoulderY = shoulderY,
                shoulderW = shoulderW,
                waistY = waistY,
                waistW = waistW,
                hipsY = hipsY,
                limbW = limbW,
                gloveR = gloveR,
                gloveBob = gloveBob,
                scan01 = scan01,
            )
        }

        // ----- front-half chips (in front of the figure) -----
        drawChipLayer(
            stats = stats,
            layouts = chipLayouts,
            placements = placements,
            front = true,
            w = w,
            h = h,
            cx = cx,
            anchors = anchors(
                cx = cx,
                headCY = headCY,
                shoulderY = shoulderY,
                shoulderW = shoulderW,
                waistY = waistY,
                hipsY = hipsY,
                feetY = feetY,
                figH = figH,
            ),
            labelColor = TextMuted,
        )
    }
}

private class ChipLayout(
    val label: androidx.compose.ui.text.TextLayoutResult,
    val value: androidx.compose.ui.text.TextLayoutResult,
)

private fun anchors(
    cx: Float,
    headCY: Float,
    shoulderY: Float,
    shoulderW: Float,
    waistY: Float,
    hipsY: Float,
    feetY: Float,
    figH: Float,
): Map<HoloAnchor, Offset> = mapOf(
    HoloAnchor.HEAD to Offset(cx, headCY),
    HoloAnchor.CHEST to Offset(cx, shoulderY + figH * 0.06f),
    HoloAnchor.ARMS to Offset(cx - shoulderW * 0.34f, headCY + figH * 0.075f),
    HoloAnchor.WAIST to Offset(cx, waistY),
    HoloAnchor.LEGS to Offset(cx + figH * 0.09f, (hipsY + feetY) / 2f),
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
    labelColor: Color,
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

        // leader line to the body anchor
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
                color = labelColor.copy(alpha = alpha),
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

@Suppress("LongParameterList") // DrawScope geometry helper — all params are figure metrics
private fun DrawScope.drawBoxer(
    cx: Float,
    figH: Float,
    feetY: Float,
    headR: Float,
    headCY: Float,
    shoulderY: Float,
    shoulderW: Float,
    waistY: Float,
    waistW: Float,
    hipsY: Float,
    limbW: Float,
    gloveR: Float,
    gloveBob: Float,
    scan01: Float,
) {
    val glassFill = Brush.verticalGradient(
        colors = listOf(Ice.copy(alpha = 0.16f), Ice.copy(alpha = 0.04f)),
        startY = headCY - headR,
        endY = feetY,
    )
    val rimStroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val bloomStroke = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

    // --- silhouette paths (also reused for the scanline clip) ---
    val torso = Path().apply {
        moveTo(cx - shoulderW / 2f, shoulderY)
        quadraticTo(cx - shoulderW * 0.60f, shoulderY + figH * 0.11f, cx - waistW / 2f, waistY)
        lineTo(cx + waistW / 2f, waistY)
        quadraticTo(cx + shoulderW * 0.60f, shoulderY + figH * 0.11f, cx + shoulderW / 2f, shoulderY)
        quadraticTo(cx, shoulderY - figH * 0.022f, cx - shoulderW / 2f, shoulderY)
        close()
    }
    val shorts = Path().apply {
        moveTo(cx - waistW / 2f - figH * 0.012f, waistY + figH * 0.008f)
        lineTo(cx + waistW / 2f + figH * 0.012f, waistY + figH * 0.008f)
        lineTo(cx + waistW * 0.72f, hipsY + figH * 0.035f)
        lineTo(cx + figH * 0.016f, hipsY + figH * 0.012f)
        lineTo(cx - figH * 0.016f, hipsY + figH * 0.012f)
        lineTo(cx - waistW * 0.72f, hipsY + figH * 0.035f)
        close()
    }
    val headRect = Rect(cx - headR, headCY - headR, cx + headR, headCY + headR)

    // limbs as polyline strokes (guard stance; feet apart)
    val leftArm = Path().apply {
        moveTo(cx - shoulderW * 0.46f, shoulderY + figH * 0.015f)
        lineTo(cx - shoulderW * 0.66f, shoulderY + figH * 0.135f) // elbow out-down
        lineTo(cx - shoulderW * 0.30f, headCY + headR * 1.05f + gloveBob) // glove up near chin
    }
    val rightArm = Path().apply {
        moveTo(cx + shoulderW * 0.46f, shoulderY + figH * 0.015f)
        lineTo(cx + shoulderW * 0.66f, shoulderY + figH * 0.135f)
        lineTo(cx + shoulderW * 0.30f, headCY + headR * 1.05f - gloveBob)
    }
    val leftLeg = Path().apply {
        moveTo(cx - waistW * 0.30f, hipsY)
        lineTo(cx - figH * 0.070f, hipsY + figH * 0.155f) // knee
        lineTo(cx - figH * 0.080f, feetY - figH * 0.012f) // ankle
    }
    val rightLeg = Path().apply {
        moveTo(cx + waistW * 0.30f, hipsY)
        lineTo(cx + figH * 0.085f, hipsY + figH * 0.155f)
        lineTo(cx + figH * 0.100f, feetY - figH * 0.012f)
    }

    // --- bloom pass (wide, soft) ---
    listOf(leftArm, rightArm, leftLeg, rightLeg).forEach { limb ->
        drawPath(limb, color = Ice.copy(alpha = 0.10f), style = bloomStroke)
    }
    drawPath(torso, color = Ice.copy(alpha = 0.10f), style = bloomStroke)
    drawCircle(color = Ice.copy(alpha = 0.10f), radius = headR * 1.5f, center = Offset(cx, headCY))

    // --- glass fills ---
    drawPath(torso, brush = glassFill)
    drawCircle(brush = glassFill, radius = headR, center = Offset(cx, headCY))

    // --- rim strokes ---
    listOf(leftArm, rightArm, leftLeg, rightLeg).forEach { limb ->
        drawPath(limb, color = Ice.copy(alpha = 0.80f), style = rimStroke)
    }
    drawPath(torso, color = Ice.copy(alpha = 0.85f), style = rimStroke)
    drawCircle(color = Ice.copy(alpha = 0.85f), radius = headR, center = Offset(cx, headCY), style = rimStroke)

    // neck + digital anatomy hints (pec/abs lines)
    drawLine(
        color = Ice.copy(alpha = 0.8f),
        start = Offset(cx, headCY + headR * 0.9f),
        end = Offset(cx, shoulderY + figH * 0.01f),
        strokeWidth = limbW * 0.8f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Ice.copy(alpha = 0.22f),
        start = Offset(cx - shoulderW * 0.26f, shoulderY + figH * 0.075f),
        end = Offset(cx + shoulderW * 0.26f, shoulderY + figH * 0.075f),
        strokeWidth = 1.4.dp.toPx(),
    )
    drawLine(
        color = Ice.copy(alpha = 0.16f),
        start = Offset(cx, shoulderY + figH * 0.09f),
        end = Offset(cx, waistY - figH * 0.02f),
        strokeWidth = 1.4.dp.toPx(),
    )
    // visor (face-less digital look)
    drawLine(
        color = IceBright.copy(alpha = 0.9f),
        start = Offset(cx - headR * 0.55f, headCY),
        end = Offset(cx + headR * 0.55f, headCY),
        strokeWidth = figH * 0.016f,
        cap = StrokeCap.Round,
    )

    // --- shorts (brand red) ---
    drawPath(shorts, color = Accent.copy(alpha = 0.50f))
    drawPath(shorts, color = Accent.copy(alpha = 0.85f), style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round))

    // --- gloves (brand red, with highlight) ---
    listOf(
        Offset(cx - shoulderW * 0.30f, headCY + headR * 1.05f + gloveBob),
        Offset(cx + shoulderW * 0.30f, headCY + headR * 1.05f - gloveBob),
    ).forEach { glove ->
        drawCircle(color = Accent.copy(alpha = 0.30f), radius = gloveR * 1.55f, center = glove)
        drawCircle(color = Accent, radius = gloveR, center = glove)
        drawCircle(
            color = Color.White.copy(alpha = 0.55f),
            radius = gloveR * 0.30f,
            center = Offset(glove.x - gloveR * 0.32f, glove.y - gloveR * 0.36f),
        )
    }

    // --- feet capsules ---
    listOf(cx - figH * 0.080f to -1f, cx + figH * 0.100f to 1f).forEach { (ankleX, dir) ->
        drawLine(
            color = Ice.copy(alpha = 0.8f),
            start = Offset(ankleX, feetY - figH * 0.010f),
            end = Offset(ankleX + dir * figH * 0.035f, feetY - figH * 0.010f),
            strokeWidth = limbW * 0.72f,
            cap = StrokeCap.Round,
        )
    }

    // --- scanlines + moving sweep, clipped to the silhouette ---
    val silhouette = Path().apply {
        addPath(torso)
        addPath(shorts)
        addOval(headRect)
        addRect(Rect(cx - figH * 0.070f - limbW / 2, hipsY, cx - figH * 0.080f + limbW / 2, feetY))
        addRect(Rect(cx + figH * 0.085f - limbW / 2, hipsY, cx + figH * 0.100f + limbW / 2, feetY))
        addRect(Rect(cx - shoulderW * 0.66f - limbW / 2, shoulderY, cx - shoulderW * 0.20f, waistY))
        addRect(Rect(cx + shoulderW * 0.20f, shoulderY, cx + shoulderW * 0.66f + limbW / 2, waistY))
    }
    clipPath(silhouette) {
        val lineGap = 5.dp.toPx()
        var y = headCY - headR
        while (y < feetY) {
            drawLine(
                color = IceBright.copy(alpha = 0.06f),
                start = Offset(cx - shoulderW, y),
                end = Offset(cx + shoulderW, y),
                strokeWidth = 1.dp.toPx(),
            )
            y += lineGap
        }
        val sweepY = (headCY - headR) + scan01 * (feetY - headCY + headR)
        val sweepH = 22.dp.toPx()
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Ice.copy(alpha = 0.28f), Color.Transparent),
                startY = sweepY - sweepH / 2f,
                endY = sweepY + sweepH / 2f,
            ),
            topLeft = Offset(cx - shoulderW, sweepY - sweepH / 2f),
            size = Size(shoulderW * 2, sweepH),
        )
    }
}
