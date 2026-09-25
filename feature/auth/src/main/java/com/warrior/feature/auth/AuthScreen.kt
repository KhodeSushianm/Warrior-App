package com.warrior.feature.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.warrior.core.designsystem.components.WarriorBadge
import com.warrior.core.designsystem.components.WarriorButton
import com.warrior.core.designsystem.components.WarriorButtonVariant
import com.warrior.core.designsystem.components.WarriorTextField
import com.warrior.core.designsystem.theme.TextMuted

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(72.dp))
        Text("WARRIOR", style = MaterialTheme.typography.displayLarge)
        Text(
            "LOG · TRACK · ANALYZE · IMPROVE",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
        Spacer(Modifier.height(40.dp))
        WarriorTextField(value = username, onValueChange = { username = it }, label = "Username")
        Spacer(Modifier.height(10.dp))
        WarriorTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            isPassword = true,
        )
        Spacer(Modifier.height(20.dp))
        WarriorButton(
            text = "Log In",
            onClick = onAuthenticated,
            enabled = username.isNotBlank() && password.isNotBlank(),
        )
        Spacer(Modifier.height(10.dp))
        WarriorButton(
            text = "Create Account",
            onClick = onAuthenticated,
            variant = WarriorButtonVariant.GHOST,
        )
        Spacer(Modifier.height(28.dp))
        Text(
            "Local-only account — your data never leaves this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
        Spacer(Modifier.height(12.dp))
        WarriorBadge(text = "PHASE 4 · REAL PBKDF2 AUTH")
    }
}
