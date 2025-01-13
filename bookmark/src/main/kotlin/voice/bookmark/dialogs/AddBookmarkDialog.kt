package voice.bookmark.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import voice.strings.R as StringsR

@Composable
internal fun AddBookmarkDialog(
  onDismissRequest: () -> Unit,
  onBookmarkNameChoose: (String) -> Unit,
) {
  var bookmarkName by remember { mutableStateOf("") }
  val focusRequester = remember { FocusRequester() }
  AlertDialog(
    onDismissRequest = onDismissRequest,
    title = { Text(text = stringResource(StringsR.string.bookmark)) },
    text = {
      Column {
        OutlinedTextField(
          value = bookmarkName,
          onValueChange = { bookmarkName = it },
          label = { Text(stringResource(StringsR.string.bookmark_edit_hint)) },
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .focusRequester(focusRequester),
          keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            autoCorrectEnabled = true,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              onBookmarkNameChoose(bookmarkName)
            },
          ),
          singleLine = true,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onBookmarkNameChoose(bookmarkName)
          onDismissRequest()
        },
      ) {
        Text(stringResource(StringsR.string.dialog_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismissRequest) {
        Text(stringResource(StringsR.string.dialog_cancel))
      }
    },
  )

  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }
}
