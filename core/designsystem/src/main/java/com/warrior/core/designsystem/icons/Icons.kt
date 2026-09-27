package com.warrior.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private const val STROKE = 2f

private fun icon(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(stroke = SolidColor(Color.White), strokeLineWidth = STROKE) { block() }
    }.build()

/** Dashboard / home: 2x2 grid. */
val WarriorIconHome: ImageVector = icon("warrior_icon_home") {
    moveTo(4f, 4f)
    lineTo(10f, 4f)
    lineTo(10f, 10f)
    lineTo(4f, 10f)
    close()
    moveTo(14f, 4f)
    lineTo(20f, 4f)
    lineTo(20f, 10f)
    lineTo(14f, 10f)
    close()
    moveTo(4f, 14f)
    lineTo(10f, 14f)
    lineTo(10f, 20f)
    lineTo(4f, 20f)
    close()
    moveTo(14f, 14f)
    lineTo(20f, 14f)
    lineTo(20f, 20f)
    lineTo(14f, 20f)
    close()
}

/** History: calendar. */
val WarriorIconHistory: ImageVector = icon("warrior_icon_history") {
    moveTo(4f, 6f)
    lineTo(20f, 6f)
    lineTo(20f, 21f)
    lineTo(4f, 21f)
    close()
    moveTo(8f, 3f)
    lineTo(8f, 8f)
    moveTo(16f, 3f)
    lineTo(16f, 8f)
    moveTo(4f, 11f)
    lineTo(20f, 11f)
}

/** Progress: bar chart. */
val WarriorIconProgress: ImageVector = icon("warrior_icon_progress") {
    moveTo(7f, 20f)
    lineTo(7f, 12f)
    moveTo(12f, 20f)
    lineTo(12f, 5f)
    moveTo(17f, 20f)
    lineTo(17f, 15f)
    moveTo(4f, 20f)
    lineTo(20f, 20f)
}

/** Profile: person. */
val WarriorIconProfile: ImageVector = icon("warrior_icon_profile") {
    moveTo(12f, 4f)
    arcTo(4f, 4f, 0f, true, true, 12f, 12f)
    arcTo(4f, 4f, 0f, true, true, 12f, 4f)
    close()
    moveTo(4f, 21f)
    curveTo(5.5f, 16f, 8.5f, 14f, 12f, 14f)
    curveTo(15.5f, 14f, 18.5f, 16f, 20f, 21f)
}

/** Plus: "log workout" top-bar action (Phase 7, matches ui-preview). */
val WarriorIconPlus: ImageVector = icon("warrior_icon_plus") {
    moveTo(12f, 5f)
    lineTo(12f, 19f)
    moveTo(5f, 12f)
    lineTo(19f, 12f)
}
