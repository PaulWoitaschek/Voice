@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.editBook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import voice.core.data.BookId
import voice.core.ui.ShapedIcon
import voice.core.ui.VoiceTheme
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.rememberCoverThumbnailRequest
import voice.features.bookOverview.views.CoverShape
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR

@Composable
internal fun EditBookSheet(
  viewState: EditBookState,
  onTitleChange: (String) -> Unit,
  onAuthorChange: (String) -> Unit,
  onSeriesChange: (String) -> Unit,
  onPartChange: (String) -> Unit,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberBottomSheetState(
    initialValue = Hidden,
    enabledValues = setOf(Hidden, Expanded),
  )
  val scope = rememberCoroutineScope()
  // the first choice wins, so taps while the sheet slides away can't change or undo it
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
  ModalBottomSheet(
    onDismissRequest = { if (!closing) onDismiss() },
    sheetState = sheetState,
  ) {
    EditBookContent(
      viewState = viewState,
      readOnly = closing,
      onTitleChange = onTitleChange,
      onAuthorChange = onAuthorChange,
      onSeriesChange = onSeriesChange,
      onPartChange = onPartChange,
      onSave = { hideThen(onConfirm) },
      onCancel = { hideThen(onDismiss) },
    )
  }
}

@Composable
private fun EditBookContent(
  viewState: EditBookState,
  readOnly: Boolean,
  onTitleChange: (String) -> Unit,
  onAuthorChange: (String) -> Unit,
  onSeriesChange: (String) -> Unit,
  onPartChange: (String) -> Unit,
  onSave: () -> Unit,
  onCancel: () -> Unit,
) {
  // the whole title starts selected, so typing replaces it
  var title by remember {
    mutableStateOf(TextFieldValue(viewState.title, selection = TextRange(0, viewState.title.length)))
  }
  var author by remember {
    mutableStateOf(TextFieldValue(viewState.author, selection = TextRange(viewState.author.length)))
  }
  var series by remember {
    mutableStateOf(TextFieldValue(viewState.series, selection = TextRange(viewState.series.length)))
  }
  var part by remember {
    mutableStateOf(TextFieldValue(viewState.part, selection = TextRange(viewState.part.length)))
  }
  val onDone = KeyboardActions(
    onDone = {
      if (viewState.confirmButtonEnabled) onSave()
    },
  )
  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      // in landscape, the keyboard leaves room for one field at a time
      .verticalScroll(rememberScrollState())
      .navigationBarsPadding()
      .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box {
        AsyncImage(
          modifier = Modifier
            .size(64.dp)
            .clip(CoverShape),
          model = rememberCoverThumbnailRequest(viewState.cover),
          placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
          error = painterResource(id = UiR.drawable.album_art),
          contentScale = ContentScale.Crop,
          contentDescription = null,
        )
        ShapedIcon(
          icon = VoiceIcons.Edit,
          shape = MaterialShapes.Pentagon,
          containerColor = MaterialTheme.colorScheme.secondary,
          contentColor = MaterialTheme.colorScheme.onSecondary,
          size = 32.dp,
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset(x = 10.dp, y = 10.dp),
        )
      }
      Spacer(Modifier.width(24.dp))
      Text(
        text = stringResource(StringsR.string.common_action_edit),
        style = MaterialTheme.typography.headlineSmallEmphasized,
      )
    }
    Spacer(Modifier.height(28.dp))
    OutlinedTextField(
      value = title,
      onValueChange = {
        title = it
        onTitleChange(it.text)
      },
      modifier = Modifier
        .fillMaxWidth()
        .focusRequester(focusRequester),
      label = { Text(stringResource(StringsR.string.book_edit_title_label)) },
      singleLine = true,
      readOnly = readOnly,
      shape = RoundedCornerShape(16.dp),
      keyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences,
        imeAction = ImeAction.Next,
      ),
    )
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
      value = author,
      onValueChange = {
        author = it
        onAuthorChange(it.text)
      },
      modifier = Modifier.fillMaxWidth(),
      label = { Text(stringResource(StringsR.string.book_edit_author_label)) },
      singleLine = true,
      readOnly = readOnly,
      shape = RoundedCornerShape(16.dp),
      keyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Words,
        imeAction = if (viewState.showSeries) ImeAction.Next else ImeAction.Done,
      ),
      keyboardActions = onDone,
    )
    if (viewState.showSeries) {
      // grows with the text, so its label stays on one line at large font sizes
      val partField = @Composable {
        OutlinedTextField(
          value = part,
          onValueChange = {
            part = it
            onPartChange(it.text)
          },
          modifier = Modifier.width(96.dp * LocalDensity.current.fontScale),
          label = { Text(stringResource(StringsR.string.book_edit_part_label)) },
          singleLine = true,
          readOnly = readOnly,
          shape = RoundedCornerShape(16.dp),
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
          ),
          keyboardActions = onDone,
        )
      }
      // at large font sizes, the part goes below the series, so the series stays wide enough to read
      val partBelow = LocalDensity.current.fontScale >= 1.5F
      Spacer(Modifier.height(16.dp))
      Row {
        OutlinedTextField(
          value = series,
          onValueChange = {
            series = it
            onSeriesChange(it.text)
          },
          modifier = Modifier.weight(1F),
          label = { Text(stringResource(StringsR.string.book_edit_series_label)) },
          singleLine = true,
          readOnly = readOnly,
          shape = RoundedCornerShape(16.dp),
          keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next,
          ),
        )
        if (!partBelow) {
          Spacer(Modifier.width(12.dp))
          partField()
        }
      }
      if (partBelow) {
        Spacer(Modifier.height(16.dp))
        partField()
      }
    }
    Spacer(Modifier.height(24.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      Spacer(Modifier.weight(1F))
      TextButton(onClick = onCancel) {
        Text(stringResource(StringsR.string.common_dialog_cancel))
      }
      Spacer(Modifier.width(8.dp))
      Button(
        onClick = onSave,
        enabled = viewState.confirmButtonEnabled,
        shapes = ButtonDefaults.shapes(),
      ) {
        Icon(VoiceIcons.Check, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(stringResource(StringsR.string.common_action_save))
      }
    }
  }
}

@Composable
@Preview
private fun EditBookPreview() {
  VoiceTheme {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
      EditBookContent(
        viewState = EditBookState(
          title = "Interference",
          author = "Sue Burke",
          bookId = BookId("preview"),
          cover = null,
          series = "The Signal Saga",
          part = "2",
          showSeries = true,
        ),
        readOnly = false,
        onTitleChange = {},
        onAuthorChange = {},
        onSeriesChange = {},
        onPartChange = {},
        onSave = {},
        onCancel = {},
      )
    }
  }
}
