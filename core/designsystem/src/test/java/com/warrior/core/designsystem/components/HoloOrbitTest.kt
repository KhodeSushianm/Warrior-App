package com.warrior.core.designsystem.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Phase 15 — pseudo-3D orbit math behind [HologramAthlete]. Every value is
 * hand-derivable from sin/cos of the placement angle.
 */
class HoloOrbitTest {

    private val eps = 1e-4f

    @Test
    fun angleSpacing_isEvenAroundTheCircle() {
        val count = 6
        val placements = (0 until count).map { HoloOrbit.placement(it, count, rotation = 0f) }
        val step = (2f * PI.toFloat()) / count
        for (i in 1 until count) {
            assertEquals(step, placements[i].angle - placements[i - 1].angle, eps)
        }
    }

    @Test
    fun depthFrontAndBack_driveScaleAlphaAndZOrder() {
        // angle = PI/2 -> sin = +1 -> front, max scale/alpha.
        val front = HoloOrbit.placement(0, 1, rotation = PI.toFloat() / 2f)
        assertTrue(front.inFront)
        assertEquals(1f, front.depthFactor, eps)
        assertEquals(HoloOrbit.MAX_SCALE, front.scale, eps)
        assertEquals(HoloOrbit.MAX_ALPHA, front.alpha, eps)

        // angle = 3PI/2 -> sin = -1 -> back, min scale/alpha.
        val back = HoloOrbit.placement(0, 1, rotation = 3f * PI.toFloat() / 2f)
        assertFalse(back.inFront)
        assertEquals(0f, back.depthFactor, eps)
        assertEquals(HoloOrbit.MIN_SCALE, back.scale, eps)
        assertEquals(HoloOrbit.MIN_ALPHA, back.alpha, eps)
    }

    @Test
    fun unitX_followsCosine_sidePositionsAtOrbitExtremes() {
        val right = HoloOrbit.placement(0, 1, rotation = 0f)
        assertEquals(1f, right.unitX, eps)
        assertEquals(0f, right.depth, eps)
        val left = HoloOrbit.placement(0, 1, rotation = PI.toFloat())
        assertEquals(-1f, left.unitX, eps)
    }

    @Test
    fun scaleAndAlpha_stayInBounds_forAnyRotation() {
        var rotation = 0f
        while (rotation < 2f * PI.toFloat()) {
            val p = HoloOrbit.placement(3, 7, rotation)
            assertTrue(p.scale in HoloOrbit.MIN_SCALE..HoloOrbit.MAX_SCALE)
            assertTrue(p.alpha in HoloOrbit.MIN_ALPHA..HoloOrbit.MAX_ALPHA)
            assertTrue(abs(p.depth) <= 1f)
            assertEquals(cos(p.angle), p.unitX, eps)
            assertEquals(sin(p.angle), p.depth, eps)
            rotation += 0.37f
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroCount_isRejected() {
        HoloOrbit.placement(0, 0, 0f)
    }
}
