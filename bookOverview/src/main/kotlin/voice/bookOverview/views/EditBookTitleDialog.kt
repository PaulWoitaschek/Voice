package voice.bookOverview.views

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import voice.bookOverview.R
import voice.bookOverview.editTitle.EditBookTitleState

@Composable
internal fun EditBookTitleDialog(
  onDismissEditTitleClick: () -> Unit,
  onConfirmEditTitle: () -> Unit,
  viewState: EditBookTitleState,
  onUpdateEditTitle: (String) -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismissEditTitleClick,
    title = {
      Text(text = stringResource(R.string.edit_book_title))
    },
    confirmButton = {
      Button(
        onClick = onConfirmEditTitle,
        enabled = viewState.confirmButtonEnabled
      ) {
        Text(stringResource(id = R.string.dialog_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismissEditTitleClick) {
        Text(stringResource(id = R.string.dialog_cancel))
      }
    },
    text = {
      TextField(
        value = viewState.title,
        onValueChange = onUpdateEditTitle,
        label = {
          Text(stringResource(R.string.change_book_name))
        }
      )
    }
  )
}
