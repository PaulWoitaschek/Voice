@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.folderPicker.selectType

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import voice.core.ui.icons.VoiceIcons
import voice.features.folderPicker.FolderTypeIcon
import voice.core.strings.R as StringsR

/**
 * The ways Voice can find books in a folder, named after the folder itself and each with the books
 * it would lead to. The option matching the folder best is marked.
 */
@Composable
internal fun FolderModeSheet(
  viewState: SelectFolderTypeViewState,
  onModeSelect: (FolderMode) -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberBottomSheetState(
      initialValue = Hidden,
      enabledValues = setOf(Hidden, Expanded),
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp)
        .padding(bottom = 32.dp),
    ) {
      Text(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp),
        text = stringResource(StringsR.string.folder_mode_sheet_title, viewState.folderName),
        style = MaterialTheme.typography.headlineSmallEmphasized,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(24.dp))
      Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        viewState.options.forEach { option ->
          FolderModeOption(
            option = option,
            folderName = viewState.folderName,
            selected = option.mode == viewState.selectedMode,
            guessed = option.mode == viewState.guessedMode,
            onClick = { onModeSelect(option.mode) },
          )
        }
      }
    }
  }
}

@Composable
private fun FolderModeOption(
  option: SelectFolderTypeViewState.Option,
  folderName: String,
  selected: Boolean,
  guessed: Boolean,
  onClick: () -> Unit,
) {
  val colorScheme = MaterialTheme.colorScheme
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    shape = RoundedCornerShape(24.dp),
    color = if (selected) colorScheme.secondaryContainer else colorScheme.surfaceContainerHigh,
    contentColor = if (selected) colorScheme.onSecondaryContainer else colorScheme.onSurface,
    border = if (selected) BorderStroke(2.dp, colorScheme.primary) else null,
  ) {
    Row(
      modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(horizontal = 12.dp))
      Column(Modifier.weight(1F)) {
        if (guessed) {
          GuessBadge()
          Spacer(Modifier.height(6.dp))
        }
        Text(
          text = option.mode.optionTitle(folderName),
          style = MaterialTheme.typography.titleMediumEmphasized,
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = option.summary(),
          style = MaterialTheme.typography.bodyMedium,
          color = colorScheme.onSurfaceVariant,
          maxLines = 2,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          FolderTypeIcon(folderType = option.mode.toFolderType(), modifier = Modifier.size(18.dp))
          Spacer(Modifier.size(6.dp))
          Text(
            text = pluralStringResource(StringsR.plurals.folder_mode_book_count, option.books.size, option.books.size),
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}

@Composable
private fun GuessBadge() {
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.tertiary,
    contentColor = MaterialTheme.colorScheme.onTertiary,
  ) {
    Row(
      modifier = Modifier.padding(start = 8.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(imageVector = VoiceIcons.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
      Spacer(Modifier.size(4.dp))
      Text(text = stringResource(StringsR.string.folder_mode_guess), style = MaterialTheme.typography.labelMedium)
    }
  }
}

@Composable
private fun SelectFolderTypeViewState.Option.summary(): String {
  return when (mode) {
    FolderMode.SingleBook -> stringResource(StringsR.string.folder_mode_single_summary)
    FolderMode.Audiobooks,
    FolderMode.Authors,
    -> {
      val examples = books.take(3).joinToString(separator = ", ") { it.name }
      if (books.size > 3) "$examples, …" else examples
    }
  }
}

@Composable
internal fun FolderMode.optionTitle(folderName: String): String {
  val res = when (this) {
    FolderMode.SingleBook -> StringsR.string.folder_mode_single_option
    FolderMode.Audiobooks -> StringsR.string.folder_mode_root_option
    FolderMode.Authors -> StringsR.string.folder_mode_author_option
  }
  return stringResource(res, folderName)
}
