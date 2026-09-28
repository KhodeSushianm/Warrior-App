package com.warrior.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.warrior.core.designsystem.theme.Accent
import com.warrior.core.designsystem.theme.AccentSoft
import com.warrior.core.designsystem.theme.Negative
import com.warrior.core.designsystem.theme.Outline
import com.warrior.core.designsystem.theme.Positive
import com.warrior.core.designsystem.theme.Surface
import com.warrior.core.designsystem.theme.SurfaceVariant
import com.warrior.core.designsystem.theme.TextMuted
import com.warrior.core.designsystem.theme.TextPrimary

/**
 * App card. [contentPadding] lets dense cards (mini stats) breathe less;
 * [brush] layers a subtle glow over the Surface base (Phase 11 polish —
 * used by the Home hero card).
 */
@Composable
fun WarriorCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 16.dp,
    brush: Brush? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    if (brush == null) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = Surface,
            border = BorderStroke(1.dp, Outline),
        ) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Box(
            modifier
                .clip(shape)
                .background(Surface)
                .background(brush)
                .border(BorderStroke(1.dp, Outline), shape),
        ) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

enum class WarriorButtonVariant { PRIMARY, GHOST, DANGER }

@Composable
fun WarriorButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: WarriorButtonVariant = WarriorButtonVariant.PRIMARY,
    enabled: Boolean = true,
) {
    if (variant == WarriorButtonVariant.PRIMARY) {
        // Phase 11 polish: gradient CTA (Accent -> deep red), ripple bounded by shape.
        val shape = RoundedCornerShape(16.dp)
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(shape)
                .background(
                    if (enabled) {
                        Brush.linearGradient(listOf(Accent, Color(0xFFD92E2E)))
                    } else {
                        Brush.linearGradient(listOf(Outline, Outline))
                    },
                )
                .clickable(onClick = onClick, enabled = enabled),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) Color.White else TextMuted,
            )
        }
        return
    }
    val colors = when (variant) {
        WarriorButtonVariant.PRIMARY -> ButtonDefaults.buttonColors(
            containerColor = Accent,
            contentColor = Color.White,
            disabledContainerColor = Outline,
            disabledContentColor = TextMuted,
        )
        WarriorButtonVariant.GHOST -> ButtonDefaults.buttonColors(
            containerColor = SurfaceVariant,
            contentColor = TextPrimary,
            disabledContainerColor = SurfaceVariant,
            disabledContentColor = TextMuted,
        )
        WarriorButtonVariant.DANGER -> ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Accent,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = TextMuted,
        )
    }
    val border = when (variant) {
        WarriorButtonVariant.DANGER -> BorderStroke(1.dp, Accent)
        else -> null
    }
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = colors,
        border = border,
        enabled = enabled,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun WarriorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Accent,
            unfocusedBorderColor = Outline,
            focusedLabelColor = Accent,
            unfocusedLabelColor = TextMuted,
            cursorColor = Accent,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
        ),
    )
}

@Composable
fun WarriorTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    titleContent: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        navigationIcon?.invoke()
        if (titleContent != null) {
            Box(Modifier.weight(1f)) { titleContent() }
        } else {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
        }
        actions()
    }
}

@Composable
fun WarriorChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        onClick = onClick,
        modifier = modifier,
        color = if (selected) AccentSoft else SurfaceVariant,
        border = BorderStroke(1.dp, if (selected) Accent else Outline),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Accent else TextMuted,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
fun WarriorBadge(
    text: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (highlight) AccentSoft else SurfaceVariant,
        border = BorderStroke(1.dp, if (highlight) Accent else Outline),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlight) Accent else TextMuted,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

/** Renders a week-over-week delta like +35m / ±0 with semantic coloring. */
@Composable
fun DeltaText(
    delta: Int,
    unit: String = "",
    modifier: Modifier = Modifier,
) {
    val text = when {
        delta == 0 -> "±0"
        delta > 0 -> "▲ $delta$unit"
        else -> "▼ ${-delta}$unit"
    }
    val color = when {
        delta > 0 -> Positive
        delta < 0 -> Negative
        else -> TextMuted
    }
    Text(text, color = color, style = MaterialTheme.typography.labelSmall, modifier = modifier)
}
