package voice.folderPicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

internal enum class FileTypeSelection {
  File,
  Folder
}

@Composable
internal fun FileTypeSelectionDialog(
  onDismiss: () -> Unit,
  onSelected: (FileTypeSelection) -> Unit,
) {
  var selectedFileType: FileTypeSelection? by remember {
    mutableStateOf(null)
  }
  AlertDialog(
    onDismissRequest = {
      onDismiss()
    },
    icon = {
      Icon(
        imageVector = Icons.Outlined.Book,
        contentDescription = stringResource(id = R.string.folder_type_dialog_title),
      )
    },
    confirmButton = {
      ConfirmButton(
        enabled = selectedFileType != null,
        onConfirm = {
          onSelected(selectedFileType!!)
          onDismiss()
        },
      )
    },
    dismissButton = {
      DismissButton(onDismiss)
    },
    title = {
      Text(text = stringResource(id = R.string.folder_type_dialog_title))
    },
    text = {
      Column {
        FileTypeSelection.values().forEach { fileType ->
          FileTypeRow(
            fileType = fileType,
            selected = fileType == selectedFileType,
            onSelected = {
              selectedFileType = fileType
            },
          )
        }
      }
    },
  )
}

@Composable
private fun ConfirmButton(enabled: Boolean, onConfirm: () -> Unit) {
  Button(
    enabled = enabled,
    onClick = onConfirm,
  ) {
    Text(text = stringResource(id = R.string.add))
  }
}

@Composable
private fun DismissButton(onDismiss: () -> Unit) {
  TextButton(
    onClick = {
      onDismiss()
    },
  ) {
    Text(text = stringResource(id = R.string.dialog_cancel))
  }
}

@Composable
private fun FileTypeRow(
  fileType: FileTypeSelection,
  selected: Boolean,
  onSelected: () -> Unit,
) {
  Row(
    modifier = Modifier.clickable(
      onClick = onSelected,
      role = Role.RadioButton,
    ),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    RadioButton(
      onClick = onSelected,
      selected = selected,
    )
    val fileTypeSelectionText = fileType.name
    Text(
      modifier = Modifier.weight(1F),
      text = fileTypeSelectionText,
      style = MaterialTheme.typography.bodyLarge,
    )
    Icon(
      modifier = Modifier.padding(end = 16.dp, start = 8.dp),
      imageVector = when (fileType) {
        FileTypeSelection.File -> Icons.Outlined.AudioFile
        FileTypeSelection.Folder -> Icons.Outlined.Folder
      },
      contentDescription = fileTypeSelectionText,
    )
  }
}
