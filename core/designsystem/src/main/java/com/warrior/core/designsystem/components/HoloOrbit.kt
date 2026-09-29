package com.warrior.core.designsystem.components

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Placement of one orbiting chip at a given rotation (pure math — unit-tested). */
data class OrbitPlacement(
    val angle: Float,
    val unitX: Float,
    val depth: Float,
    val depthFactor: Float,
    val scale: Float,
    val alpha: Float,
    val inFront: Boolean,
)

/**
 * Pseudo-3D orbit math for [HologramAthlete] (Phase 15). Chips sit on an
 * ellipse around the figure; `sin(angle)` is the depth used for z-order,
 * scale and alpha, so the ring reads as rotating *around* the boxer.
 * Pure and deterministic — no Compose dependencies.
 */
object HoloOrbit {

    const val MIN_SCALE = 0.80f
    const val MAX_SCALE = 1.00f
    const val MIN_ALPHA = 0.45f
    const val MAX_ALPHA = 1.00f

    private val TWO_PI = (2.0 * PI).toFloat()

    fun placement(index: Int, count: Int, rotation: Float): OrbitPlacement {
        require(count > 0) { "count must be > 0" }
        require(index in 0 until count) { "index out of range" }
        val angle = rotation + index * TWO_PI / count
        val depth = sin(angle)
        val depthFactor = (depth + 1f) / 2f
        return OrbitPlacement(
            angle = angle,
            unitX = cos(angle),
            depth = depth,
            depthFactor = depthFactor,
            scale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * depthFactor,
            alpha = MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * depthFactor,
            inFront = depth >= 0f,
        )
    }
}
