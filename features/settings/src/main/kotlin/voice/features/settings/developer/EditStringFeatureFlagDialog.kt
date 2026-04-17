package voice.features.settings.developer

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
internal fun EditStringFeatureFlagDialog(
  key: String,
  initialValue: String,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var value by remember(initialValue) { mutableStateOf(initialValue) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(key)
    },
    text = {
      OutlinedTextField(
        value = value,
        onValueChange = {
          value = it
        },
        singleLine = true,
      )
    },
    confirmButton = {
      TextButton(
        onClick = {
          onConfirm(value)
        },
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}
