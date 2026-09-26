package com.warrior.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warrior.core.designsystem.components.WarriorBadge
import com.warrior.core.designsystem.components.WarriorButton
import com.warrior.core.designsystem.components.WarriorButtonVariant
import com.warrior.core.designsystem.components.WarriorCard
import com.warrior.core.designsystem.components.WarriorTopBar
import com.warrior.core.designsystem.theme.Accent
import com.warrior.core.designsystem.theme.AccentSoft
import com.warrior.core.designsystem.theme.TextMuted

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val account = state.account

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
    ) {
        WarriorTopBar(title = "Profile")
        WarriorCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(54.dp)
                        .background(AccentSoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        (account?.displayName ?: "?").take(1).uppercase(),
                        color = Accent,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(account?.displayName ?: "—", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "@${account?.username ?: "—"} · local account",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        WarriorButton(
            text = "Log Out",
            onClick = viewModel::onLogout,
            variant = WarriorButtonVariant.DANGER,
        )
        Spacer(Modifier.height(12.dp))
        WarriorBadge(text = "PHASE 9 · EDIT PROFILE")
    }
}
