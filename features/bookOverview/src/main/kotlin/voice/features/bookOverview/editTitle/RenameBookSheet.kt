@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.editTitle

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import voice.core.ui.ShapedIcon
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.rememberCoverThumbnailRequest
import voice.features.bookOverview.views.CoverShape
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR

@Composable
internal fun RenameBookSheet(
  viewState: EditBookTitleState,
  onTitleChange: (String) -> Unit,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberBottomSheetState(
    initialValue = Hidden,
    enabledValues = setOf(Hidden, Expanded),
  )
  val scope = rememberCoroutineScope()
  val hideThen: (() -> Unit) -> Unit = { action ->
    scope.launch {
      try {
        sheetState.hide()
      } finally {
        action()
      }
    }
  }
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    // the whole name starts selected, so typing replaces it and a tap places the cursor
    var value by remember {
      mutableStateOf(TextFieldValue(viewState.title, selection = TextRange(0, viewState.title.length)))
    }
    val focusRequester = remember { FocusRequester() }
    var focused by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
      if (!focused) {
        focusRequester.requestFocus()
        focused = true
      }
    }
    Column(
      modifier = Modifier
        .fillMaxWidth()
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
            icon = VoiceIcons.Title,
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
          text = stringResource(StringsR.string.book_edit_rename),
          style = MaterialTheme.typography.headlineSmallEmphasized,
        )
      }
      Spacer(Modifier.height(28.dp))
      OutlinedTextField(
        value = value,
        onValueChange = {
          value = it
          onTitleChange(it.text)
        },
        modifier = Modifier
          .fillMaxWidth()
          .focusRequester(focusRequester),
        label = { Text(stringResource(StringsR.string.book_edit_name_label)) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.Sentences,
          imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
          onDone = {
            if (viewState.confirmButtonEnabled) hideThen(onConfirm)
          },
        ),
      )
      Spacer(Modifier.height(24.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.weight(1F))
        TextButton(onClick = { hideThen(onDismiss) }) {
          Text(stringResource(StringsR.string.common_dialog_cancel))
        }
        Spacer(Modifier.width(8.dp))
        Button(
          onClick = { hideThen(onConfirm) },
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
}
