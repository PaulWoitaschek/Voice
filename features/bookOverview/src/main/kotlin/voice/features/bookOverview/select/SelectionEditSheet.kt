@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.select

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import voice.core.data.BookId
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR
import voice.core.ui.ShapedIcon
import voice.core.ui.VoiceTheme
import voice.features.bookOverview.editBook.NameSuggestion
import voice.features.bookOverview.editBook.NameSuggestions
import voice.features.bookOverview.editBook.matching

@Composable
internal fun SelectionEditSheet(
  viewState: SelectionEditViewState,
  onSave: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState =
    rememberBottomSheetState(
      initialValue = Hidden,
      enabledValues = setOf(Hidden, Expanded),
    )
  val scope = rememberCoroutineScope()
  // the first choice wins, so taps while the sheet slides away can't change it
  var closing by remember { mutableStateOf(false) }
  val hideThen: (() -> Unit) -> Unit = { action ->
    if (!closing) {
      closing = true
      scope.launch {
        try {
          sheetState.hide()
        } finally {
          action()
        }
      }
    }
  }
  val dismiss by rememberUpdatedState(onDismiss)
  LaunchedEffect(viewState.saved) {
    if (viewState.saved) hideThen(dismiss)
  }
  ModalBottomSheet(
    onDismissRequest = { if (!closing) onDismiss() },
    sheetState = sheetState,
    // a save on the server can't be called back, so the sheet waits until all books are saved
    sheetGesturesEnabled = !viewState.saving,
    properties =
      ModalBottomSheetProperties(
        shouldDismissOnBackPress = !viewState.saving,
        shouldDismissOnClickOutside = !viewState.saving,
      ),
  ) {
    SelectionEditContent(
      viewState = viewState,
      readOnly = closing || viewState.saving,
      onSave = onSave,
      onCancel = { hideThen(onDismiss) },
    )
  }
}

@Composable
private fun SelectionEditContent(
  viewState: SelectionEditViewState,
  readOnly: Boolean,
  onSave: (String) -> Unit,
  onCancel: () -> Unit,
) {
  // the whole name starts selected, so typing replaces it
  var value by
    remember(viewState.field) {
      mutableStateOf(
        TextFieldValue(viewState.value, selection = TextRange(0, viewState.value.length)),
      )
    }
  val canSave = value.text.isNotBlank() && !readOnly
  val label =
    when (viewState.field) {
      BookField.Series -> StringsR.string.book_edit_series_label
      BookField.Author -> StringsR.string.book_edit_author_label
    }
  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()
        .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      ShapedIcon(
        icon =
          when (viewState.field) {
            BookField.Series -> UiR.drawable.ic_collections_bookmark
            BookField.Author -> UiR.drawable.ic_person
          },
        shape = MaterialShapes.Pentagon,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        size = 56.dp,
      )
      Spacer(Modifier.width(16.dp))
      Column {
        Text(
          text = stringResource(label),
          style = MaterialTheme.typography.headlineSmallEmphasized,
        )
        Text(
          text =
            pluralStringResource(
              StringsR.plurals.library_search_browse_book_count,
              viewState.ids.size,
              viewState.ids.size,
            ),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    Spacer(Modifier.height(28.dp))
    OutlinedTextField(
      value = value,
      onValueChange = { value = it },
      modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
      label = { Text(stringResource(label)) },
      singleLine = true,
      readOnly = readOnly,
      shape = MaterialTheme.shapes.large,
      keyboardOptions =
        KeyboardOptions(
          capitalization = KeyboardCapitalization.Words,
          imeAction = ImeAction.Done,
        ),
      keyboardActions = KeyboardActions(onDone = { if (canSave) onSave(value.text) }),
    )
    // the name all books share needs no suggestion
    val suggestions =
      if (readOnly || value.text == viewState.value) emptyList()
      else viewState.suggestions.matching(value.text)
    if (suggestions.isNotEmpty()) {
      Spacer(Modifier.height(8.dp))
      NameSuggestions(
        suggestions = suggestions,
        onClick = { name -> value = TextFieldValue(name, selection = TextRange(name.length)) },
      )
    }
    Spacer(Modifier.height(24.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      Spacer(Modifier.weight(1F))
      TextButton(onClick = onCancel, enabled = !viewState.saving) {
        Text(stringResource(StringsR.string.common_dialog_cancel))
      }
      Spacer(Modifier.width(8.dp))
      Button(
        onClick = { onSave(value.text) },
        enabled = canSave,
        shapes = ButtonDefaults.shapes(),
      ) {
        if (viewState.saving) {
          LoadingIndicator(
            Modifier.size(ButtonDefaults.IconSize),
            color = LocalContentColor.current,
          )
        } else {
          Icon(
            painterResource(UiR.drawable.ic_check),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
          )
        }
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(stringResource(StringsR.string.common_action_save))
      }
    }
  }
}

@Composable
@Preview
private fun SelectionEditPreview() {
  VoiceTheme {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
      SelectionEditContent(
        viewState =
          SelectionEditViewState(
            field = BookField.Series,
            ids = List(4) { BookId("$it") }.toSet(),
            value = "The Signal Saga",
            suggestions =
              listOf(NameSuggestion("The Signal Saga", 3), NameSuggestion("Signal Lost", 1)),
          ),
        readOnly = false,
        onSave = {},
        onCancel = {},
      )
    }
  }
}
