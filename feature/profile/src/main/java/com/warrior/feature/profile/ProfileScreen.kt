package com.warrior.feature.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warrior.core.designsystem.components.WarriorBadge
import com.warrior.core.designsystem.components.WarriorButton
import com.warrior.core.designsystem.components.WarriorButtonVariant
import com.warrior.core.designsystem.components.WarriorCard
import com.warrior.core.designsystem.components.WarriorTextField
import com.warrior.core.designsystem.components.WarriorTopBar
import com.warrior.core.designsystem.theme.Accent
import com.warrior.core.designsystem.theme.AccentSoft
import com.warrior.core.designsystem.theme.Negative
import com.warrior.core.designsystem.theme.TextMuted
import com.warrior.core.designsystem.theme.TextPrimary
import com.warrior.domain.auth.validation.AuthErrorCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Profile screen (Phase 9 + Season 2 Phase 12): identity card, action rows
 * (Athlete body / Edit / Export / Import / About), the local-storage privacy
 * note and the danger Log Out button. Backup files are exchanged through the
 * Storage Access Framework — no storage permission, fully offline.
 */
@Composable
fun ProfileScreen(
    version: String,
    onOpenBody: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val account = state.account
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // SAF: create the export document, then write the payload the VM builds.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) {
            viewModel.onExportCancelled()
        } else {
            scope.launch {
                val payload = viewModel.exportPayload()
                val written = payload != null && withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(payload.toByteArray(Charsets.UTF_8))
                        } ?: error("no stream")
                    }.isSuccess
                }
                viewModel.onExportFinished(written)
            }
        }
    }

    // SAF: pick a backup file; its text goes to the VM for validation/confirm.
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> pendingImportUri = uri }
    LaunchedEffect(pendingImportUri) {
        val uri = pendingImportUri ?: return@LaunchedEffect
        pendingImportUri = null
        val text = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
        }
        viewModel.onImportJson(text)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
    ) {
        WarriorTopBar(title = stringResource(R.string.profile_title))

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
                        stringResource(R.string.profile_local_account, account?.username ?: "—"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        WarriorCard(modifier = Modifier.fillMaxWidth()) {
            ActionRow(text = stringResource(R.string.profile_row_body), onClick = onOpenBody)
            ActionRow(text = stringResource(R.string.profile_row_edit), onClick = viewModel::onEditOpen)
            ActionRow(
                text = stringResource(R.string.profile_row_export),
                onClick = {
                    if (!state.isExporting) {
                        viewModel.onExportStarted()
                        val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
                        exportLauncher.launch("warrior-backup-$stamp.json")
                    }
                },
            )
            ActionRow(
                text = stringResource(R.string.profile_row_import),
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
            )
            ActionRow(
                text = stringResource(R.string.profile_row_about, version),
                badge = stringResource(R.string.profile_badge_offline),
                onClick = viewModel::onAboutOpen,
            )
        }

        Spacer(Modifier.height(12.dp))
        WarriorCard {
            Text(
                stringResource(R.string.profile_privacy_body),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
            )
        }

        Spacer(Modifier.height(16.dp))
        WarriorButton(
            text = stringResource(R.string.profile_logout),
            onClick = viewModel::onLogout,
            variant = WarriorButtonVariant.DANGER,
        )
        Spacer(Modifier.height(8.dp))
    }

    if (state.showEditDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onEditCancel,
            title = { Text(stringResource(R.string.profile_edit_title)) },
            text = {
                Column {
                    WarriorTextField(
                        value = state.editDisplayName,
                        onValueChange = viewModel::onEditDisplayNameChange,
                        label = stringResource(R.string.profile_field_display_name),
                    )
                    Spacer(Modifier.height(10.dp))
                    WarriorTextField(
                        value = state.editUsername,
                        onValueChange = viewModel::onEditUsernameChange,
                        label = stringResource(R.string.profile_field_username),
                    )
                    if (state.editErrorCodes.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        state.editErrorCodes.forEach { code ->
                            Text(
                                stringResource(code.labelRes),
                                color = Negative,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::onSaveProfile, enabled = !state.isSaving) {
                    Text(stringResource(R.string.profile_action_save), color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onEditCancel) {
                    Text(stringResource(R.string.profile_action_cancel))
                }
            },
        )
    }

    if (state.showAbout) {
        AlertDialog(
            onDismissRequest = viewModel::onAboutClose,
            title = { Text(stringResource(R.string.profile_about_title)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.profile_about_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.profile_about_version, version),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::onAboutClose) {
                    Text(stringResource(R.string.profile_action_cancel))
                }
            },
        )
    }

    // ---------- backup dialogs ----------

    if (state.showImportConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::onImportCancel,
            title = { Text(stringResource(R.string.profile_import_confirm_title)) },
            text = { Text(stringResource(R.string.profile_import_confirm_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::onImportConfirm) {
                    Text(stringResource(R.string.profile_import_confirm_action), color = Negative)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onImportCancel) {
                    Text(stringResource(R.string.profile_action_cancel))
                }
            },
        )
    }

    state.exportDone?.let { success ->
        AlertDialog(
            onDismissRequest = viewModel::onExportResultDismiss,
            title = {
                Text(
                    stringResource(
                        if (success) R.string.profile_export_success_title else R.string.profile_export_failed_title,
                    ),
                )
            },
            text = {
                Text(
                    stringResource(
                        if (success) R.string.profile_export_success_body else R.string.profile_export_failed_body,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onExportResultDismiss) {
                    Text(stringResource(R.string.profile_action_ok))
                }
            },
        )
    }

    state.importResult?.let { result ->
        AlertDialog(
            onDismissRequest = viewModel::onImportResultDismiss,
            title = {
                Text(
                    stringResource(
                        when (result) {
                            is ProfileViewModel.ImportResult.Success -> R.string.profile_import_success_title
                            ProfileViewModel.ImportResult.Invalid -> R.string.profile_import_invalid_title
                            ProfileViewModel.ImportResult.Failed -> R.string.profile_import_failed_title
                        },
                    ),
                )
            },
            text = {
                Text(
                    when (result) {
                        is ProfileViewModel.ImportResult.Success ->
                            stringResource(R.string.profile_import_success_body, result.sessionsRestored)
                        ProfileViewModel.ImportResult.Invalid ->
                            stringResource(R.string.profile_import_invalid_body)
                        ProfileViewModel.ImportResult.Failed ->
                            stringResource(R.string.profile_import_failed_body)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onImportResultDismiss) {
                    Text(stringResource(R.string.profile_action_ok), color = Accent)
                }
            },
        )
    }
}

@Composable
private fun ActionRow(text: String, badge: String? = null, onClick: (() -> Unit)?) {
    val enabled = onClick != null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick!!) else Modifier)
            .padding(vertical = 12.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) TextPrimary else TextMuted,
            modifier = Modifier.weight(1f),
        )
        if (badge != null) {
            WarriorBadge(text = badge)
        }
    }
}

/** Error code -> localized copy (Phase 9: no hardcoded UI strings). */
private val AuthErrorCode.labelRes: Int
    @StringRes
    get() = when (this) {
        AuthErrorCode.USERNAME_FORMAT -> R.string.profile_error_username_format
        AuthErrorCode.DISPLAY_NAME_INVALID -> R.string.profile_error_display_name_invalid
        AuthErrorCode.DUPLICATE_USERNAME -> R.string.profile_error_duplicate_username
        else -> R.string.profile_error_unexpected
    }
