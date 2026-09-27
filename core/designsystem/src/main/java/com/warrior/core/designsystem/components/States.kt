package com.warrior.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.warrior.core.designsystem.theme.Accent
import com.warrior.core.designsystem.theme.TextMuted

/**
 * Shared screen-state building blocks (Phase 9 polish): every feature uses the
 * same section header, empty state and loading treatment for a consistent UX.
 */

/** Small muted uppercase-style section label used above cards. */
@Composable
fun WarriorSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = modifier)
}

/**
 * Empty state inside a card: title, explanatory body and an optional CTA.
 * Copy comes from the calling feature's string resources (i18n-ready).
 */
@Composable
fun WarriorEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            WarriorButton(text = actionLabel, onClick = onAction)
        }
    }
}

/** Centered progress indicator for first-load states. */
@Composable
fun WarriorLoadingBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 120.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Accent)
    }
}
