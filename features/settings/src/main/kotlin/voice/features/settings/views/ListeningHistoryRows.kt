@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.settings.views

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import voice.core.ui.icons.VoiceIcons
import voice.core.strings.R as StringsR

/**
 * The listening history behind the "Back to" pill and the History tab: on by default, kept on
 * the device, and clearable after a confirmation.
 */
@Composable
internal fun ListeningHistoryRows(
  enabled: Boolean,
  onToggle: () -> Unit,
  onClear: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var confirmClear by rememberSaveable { mutableStateOf(false) }
  Column(modifier) {
    IslandSwitchRow(
      title = stringResource(StringsR.string.settings_listening_history_title),
      checked = enabled,
      onCheckedChange = { onToggle() },
      leading = {
        ShapedIcon(
          icon = VoiceIcons.History,
          shape = MaterialShapes.Cookie6Sided,
          containerColor = MaterialTheme.colorScheme.secondary,
          contentColor = MaterialTheme.colorScheme.onSecondary,
        )
      },
      summary = { Text(stringResource(StringsR.string.settings_listening_history_summary)) },
    )
    IslandRow(
      title = stringResource(StringsR.string.settings_listening_history_clear),
      onClick = { confirmClear = true },
      leading = {
        ShapedIcon(
          icon = VoiceIcons.Delete,
          shape = MaterialShapes.Square,
          containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
          contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      },
      trailing = {},
    )
  }
  if (confirmClear) {
    ClearListeningHistoryDialog(
      onConfirm = {
        confirmClear = false
        onClear()
      },
      onDismiss = { confirmClear = false },
    )
  }
}

@Composable
private fun ClearListeningHistoryDialog(
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(StringsR.string.settings_listening_history_clear_title)) },
    text = { Text(stringResource(StringsR.string.settings_listening_history_clear_message)) },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text(stringResource(StringsR.string.settings_listening_history_clear_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(StringsR.string.common_dialog_cancel))
      }
    },
  )
}
