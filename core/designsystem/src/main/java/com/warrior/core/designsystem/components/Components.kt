package com.warrior.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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

@Composable
fun WarriorCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Surface,
        border = BorderStroke(1.dp, Outline),
    ) {
        Column(Modifier.padding(16.dp), content = content)
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
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
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
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f),
        )
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
            color = if (selected) TextPrimary else TextMuted,
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
        delta > 0 -> "+$delta$unit"
        else -> "$delta$unit"
    }
    val color = when {
        delta > 0 -> Positive
        delta < 0 -> Negative
        else -> TextMuted
    }
    Text(text, color = color, style = MaterialTheme.typography.labelSmall, modifier = modifier)
}
