package com.warrior.feature.auth

import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warrior.core.designsystem.components.WarriorButton
import com.warrior.core.designsystem.components.WarriorButtonVariant
import com.warrior.core.designsystem.components.WarriorTextField
import com.warrior.core.designsystem.theme.Accent
import com.warrior.core.designsystem.theme.Negative
import com.warrior.core.designsystem.theme.TextMuted
import com.warrior.core.designsystem.theme.TextPrimary
import com.warrior.domain.auth.validation.AuthErrorCode

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
        run {
            val wordmark = stringResource(R.string.auth_wordmark)
            val splitAt = (wordmark.length + 1) / 2
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = TextPrimary)) { append(wordmark.take(splitAt)) }
                    withStyle(SpanStyle(color = Accent)) { append(wordmark.drop(splitAt)) }
                },
                style = MaterialTheme.typography.displayLarge,
            )
        }
        Text(
            stringResource(R.string.auth_tagline),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
        Spacer(Modifier.height(32.dp))

        if (isRegister) {
            WarriorTextField(
                value = state.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                label = stringResource(R.string.auth_field_display_name),
            )
            Spacer(Modifier.height(10.dp))
        }
        WarriorTextField(
            value = state.username,
            onValueChange = viewModel::onUsernameChange,
            label = stringResource(R.string.auth_field_username),
        )
        Spacer(Modifier.height(10.dp))
        WarriorTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = stringResource(R.string.auth_field_password),
            isPassword = true,
        )

        if (state.errorCodes.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.errorCodes.forEach { code ->
                    Text(
                        stringResource(code.labelRes),
                        color = Negative,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        WarriorButton(
            text = stringResource(if (isRegister) R.string.auth_action_register else R.string.auth_action_login),
            onClick = viewModel::onSubmit,
            enabled = !state.isSubmitting,
        )
        Spacer(Modifier.height(10.dp))
        WarriorButton(
            text = stringResource(
                if (isRegister) R.string.auth_action_back_to_login else R.string.auth_action_register,
            ),
            onClick = viewModel::onToggleMode,
            variant = WarriorButtonVariant.GHOST,
            enabled = !state.isSubmitting,
        )

        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.auth_local_only_note),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
    }
}

/** Error code -> localized copy (Phase 9: no hardcoded UI strings). */
private val AuthErrorCode.labelRes: Int
    @StringRes
    get() = when (this) {
        AuthErrorCode.USERNAME_FORMAT -> R.string.auth_error_username_format
        AuthErrorCode.DISPLAY_NAME_INVALID -> R.string.auth_error_display_name_invalid
        AuthErrorCode.PASSWORD_TOO_SHORT -> R.string.auth_error_password_too_short
        AuthErrorCode.USERNAME_BLANK -> R.string.auth_error_username_blank
        AuthErrorCode.PASSWORD_EMPTY -> R.string.auth_error_password_empty
        AuthErrorCode.DUPLICATE_USERNAME -> R.string.auth_error_duplicate_username
        AuthErrorCode.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
        AuthErrorCode.UNEXPECTED -> R.string.auth_error_unexpected
    }
