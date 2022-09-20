package voice.bookOverview.deleteBook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.bookOverview.R

@Composable
internal fun DeleteBookDialog(
  viewState: DeleteBookViewState,
  onDismiss: () -> Unit,
  onConfirmDeletion: () -> Unit,
  onDeleteCheckBoxChecked: (Boolean) -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(stringResource(R.string.delete_book_dialog_title))
    },
    confirmButton = {
      Button(
        onClick = onConfirmDeletion,
        enabled = viewState.deleteCheckBoxChecked,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.errorContainer,
          contentColor = MaterialTheme.colorScheme.error,
        ),
      ) {
        Text(stringResource(id = R.string.delete))
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
      ) {
        Text(stringResource(id = R.string.dialog_cancel))
      }
    },
    text = {
      Column {
        Text(stringResource(id = R.string.delete_book_dialog_content))

        Spacer(modifier = Modifier.heightIn(8.dp))
        Text(viewState.fileToDelete, style = MaterialTheme.typography.bodyLarge)

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clickable {
              onDeleteCheckBoxChecked(!viewState.deleteCheckBoxChecked)
            },
        ) {
          Checkbox(
            checked = viewState.deleteCheckBoxChecked,
            onCheckedChange = onDeleteCheckBoxChecked,
          )
          Text(stringResource(id = R.string.delete_book_dialog_deletion_confirmation))
        }
      }
    },
  )
}
