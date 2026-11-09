@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.views

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.core.strings.R as StringsR
import voice.core.ui.VoiceTheme
import voice.core.ui.icons.VoiceIcons
import voice.features.bookOverview.overview.LibraryGrouping

@Composable
internal fun LibraryFilterSheet(
  grouping: LibraryGrouping,
  onGroupingChange: (LibraryGrouping) -> Unit,
  hiddenBookCount: Int,
  showHiddenBooks: Boolean,
  onShowHiddenBooksChange: (Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState =
      rememberBottomSheetState(
        initialValue = Hidden,
        enabledValues = setOf(Hidden, Expanded),
      ),
  ) {
    LibraryFilterContent(
      grouping = grouping,
      onGroupingChange = onGroupingChange,
      hiddenBookCount = hiddenBookCount,
      showHiddenBooks = showHiddenBooks,
      onShowHiddenBooksChange = onShowHiddenBooksChange,
    )
  }
}

@Composable
private fun LibraryFilterContent(
  grouping: LibraryGrouping,
  onGroupingChange: (LibraryGrouping) -> Unit,
  hiddenBookCount: Int,
  showHiddenBooks: Boolean,
  onShowHiddenBooksChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 24.dp),
  ) {
    SectionTitle(StringsR.string.library_filter_group_by)
    Spacer(Modifier.height(8.dp))
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).selectableGroup(),
      horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
      LibraryGrouping.entries.forEachIndexed { index, option ->
        ToggleButton(
          checked = grouping == option,
          onCheckedChange = { onGroupingChange(option) },
          modifier = Modifier.weight(1F).semantics { role = Role.RadioButton },
          shapes =
            if (index == 0) {
              ButtonGroupDefaults.connectedLeadingButtonShapes()
            } else {
              ButtonGroupDefaults.connectedTrailingButtonShapes()
            },
        ) {
          Text(
            text =
              stringResource(
                when (option) {
                  LibraryGrouping.Series -> StringsR.string.library_filter_group_by_series
                  LibraryGrouping.Author -> StringsR.string.library_filter_group_by_author
                },
              ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
    if (hiddenBookCount > 0 || showHiddenBooks) {
      Spacer(Modifier.height(24.dp))
      SectionTitle(StringsR.string.library_filter_show)
      Spacer(Modifier.height(8.dp))
      HiddenBooksRow(hiddenBookCount, showHiddenBooks, onShowHiddenBooksChange)
    }
  }
}

@Composable
private fun SectionTitle(@StringRes text: Int) {
  Text(
    modifier = Modifier.padding(horizontal = 12.dp).semantics { heading() },
    text = stringResource(text),
    style = MaterialTheme.typography.labelLargeEmphasized,
  )
}

@Composable
private fun HiddenBooksRow(
  hiddenBookCount: Int,
  showHiddenBooks: Boolean,
  onShowHiddenBooksChange: (Boolean) -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .toggleable(
          value = showHiddenBooks,
          role = Role.Switch,
          onValueChange = onShowHiddenBooksChange,
        )
        .heightIn(min = 64.dp)
        .padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(VoiceIcons.VisibilityOff, contentDescription = null)
    Spacer(Modifier.width(16.dp))
    Column(Modifier.weight(1F)) {
      Text(
        text = stringResource(StringsR.string.library_filter_hidden_books),
        style = MaterialTheme.typography.bodyLarge,
      )
      Text(
        text =
          pluralStringResource(
            StringsR.plurals.library_search_browse_book_count,
            hiddenBookCount,
            hiddenBookCount,
          ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(Modifier.width(12.dp))
    Switch(checked = showHiddenBooks, onCheckedChange = null)
  }
}

@Preview
@Composable
private fun LibraryFilterContentPreview() {
  VoiceTheme {
    Surface {
      LibraryFilterContent(
        grouping = LibraryGrouping.Series,
        onGroupingChange = {},
        hiddenBookCount = 12,
        showHiddenBooks = false,
        onShowHiddenBooksChange = {},
      )
    }
  }
}
