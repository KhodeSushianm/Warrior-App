package com.warrior.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import com.warrior.core.designsystem.components.WarriorButton
import com.warrior.core.designsystem.components.WarriorButtonVariant
import com.warrior.core.designsystem.components.WarriorTextField
import com.warrior.core.designsystem.theme.Negative
import com.warrior.core.designsystem.theme.TextMuted

@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRegister = state.mode == AuthViewModel.Mode.REGISTER

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))
        Text("WARRIOR", style = MaterialTheme.typography.displayLarge)
        Text(
            "LOG · TRACK · ANALYZE · IMPROVE",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
        Spacer(Modifier.height(32.dp))

        if (isRegister) {
            WarriorTextField(
                value = state.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                label = "Display name",
            )
            Spacer(Modifier.height(10.dp))
        }
        WarriorTextField(
            value = state.username,
            onValueChange = viewModel::onUsernameChange,
            label = "Username",
        )
        Spacer(Modifier.height(10.dp))
        WarriorTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Password",
            isPassword = true,
        )

        if (state.errors.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.errors.forEach { message ->
                    Text(message, color = Negative, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        WarriorButton(
            text = if (isRegister) "Create Account" else "Log In",
            onClick = viewModel::onSubmit,
            enabled = !state.isSubmitting,
        )
        Spacer(Modifier.height(10.dp))
        WarriorButton(
            text = if (isRegister) "Back to Login" else "Create Account",
            onClick = viewModel::onToggleMode,
            variant = WarriorButtonVariant.GHOST,
            enabled = !state.isSubmitting,
        )

        Spacer(Modifier.height(24.dp))
        Text(
            "Local-only account — your data never leaves this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
    }
}
