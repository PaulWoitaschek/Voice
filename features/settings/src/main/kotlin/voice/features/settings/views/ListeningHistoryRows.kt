package voice.features.settings.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import voice.core.strings.R
import voice.core.ui.icons.VoiceIcons

/**
 * The listening history behind the "Back to" pill and the History tab: on by default, kept on
 * the device, and clearable.
 */
@Composable
internal fun ListeningHistoryRows(
  enabled: Boolean,
  onToggle: () -> Unit,
  onClearClick: () -> Unit,
) {
  Column {
    ListItem(
      modifier = Modifier.clickable(onClick = onToggle),
      leadingContent = {
        Icon(imageVector = VoiceIcons.History, contentDescription = null)
      },
      supportingContent = {
        Text(stringResource(R.string.settings_listening_history_summary))
      },
      trailingContent = {
        Switch(checked = enabled, onCheckedChange = { onToggle() })
      },
    ) {
      Text(stringResource(R.string.settings_listening_history_title))
    }
    ListItem(
      modifier = Modifier.clickable(onClick = onClearClick),
      leadingContent = {
        Icon(imageVector = VoiceIcons.Delete, contentDescription = null)
      },
    ) {
      Text(stringResource(R.string.settings_listening_history_clear))
    }
  }
}

@Composable
internal fun ClearListeningHistoryDialog(
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.settings_listening_history_clear_title)) },
    text = { Text(stringResource(R.string.settings_listening_history_clear_message)) },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text(stringResource(R.string.settings_listening_history_clear_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.common_dialog_cancel))
      }
    },
  )
}
