@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.core.strings.R as StringsR
import voice.core.ui.VoiceTheme
import voice.core.ui.icons.VoiceIcons
import voice.features.bookOverview.overview.BookOverviewViewState
import voice.features.bookOverview.overview.LibrarySource
import voice.features.bookOverview.overview.LibrarySourceViewState

@Composable
internal fun LibraryFilterSheet(
  viewState: BookOverviewViewState,
  onShowHiddenBooksChange: (Boolean) -> Unit,
  onShowNotDownloadedBooksChange: (Boolean) -> Unit,
  onSourceChange: (LibrarySource, Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  // the rows stay while the sheet is open, so a switch doesn't vanish under the finger
  val showHiddenBooksSwitch = remember { viewState.showHiddenBooksSwitch }
  val showNotDownloadedBooksSwitch = remember { viewState.showNotDownloadedBooksSwitch }
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState =
      rememberBottomSheetState(
        initialValue = Hidden,
        enabledValues = setOf(Hidden, Expanded),
      ),
  ) {
    LibraryFilterContent(
      viewState = viewState,
      showHiddenBooksSwitch = showHiddenBooksSwitch,
      showNotDownloadedBooksSwitch = showNotDownloadedBooksSwitch,
      onShowHiddenBooksChange = onShowHiddenBooksChange,
      onShowNotDownloadedBooksChange = onShowNotDownloadedBooksChange,
      onSourceChange = onSourceChange,
    )
  }
}

@Composable
private fun LibraryFilterContent(
  viewState: BookOverviewViewState,
  showHiddenBooksSwitch: Boolean,
  showNotDownloadedBooksSwitch: Boolean,
  onShowHiddenBooksChange: (Boolean) -> Unit,
  onShowNotDownloadedBooksChange: (Boolean) -> Unit,
  onSourceChange: (LibrarySource, Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 24.dp),
  ) {
    Text(
      modifier = Modifier.padding(horizontal = 12.dp).semantics { heading() },
      text = stringResource(StringsR.string.library_filter_show),
      style = MaterialTheme.typography.labelLargeEmphasized,
    )
    Spacer(Modifier.height(8.dp))
    if (showHiddenBooksSwitch) {
      FilterSwitch(
        icon = VoiceIcons.VisibilityOff,
        title = stringResource(StringsR.string.library_filter_hidden_books),
        bookCount = viewState.hiddenBookCount,
        checked = viewState.showHiddenBooks,
        onCheckedChange = onShowHiddenBooksChange,
      )
    }
    if (showNotDownloadedBooksSwitch) {
      FilterSwitch(
        icon = VoiceIcons.Cloud,
        title = stringResource(StringsR.string.library_filter_not_downloaded),
        bookCount = viewState.notDownloadedBookCount,
        checked = viewState.showNotDownloadedBooks,
        onCheckedChange = onShowNotDownloadedBooksChange,
      )
    }
    val shownSources = viewState.sources.count { it.shown }
    viewState.sources.forEach { source ->
      FilterSwitch(
        icon =
          when (source.source) {
            is LibrarySource.Folder -> VoiceIcons.Folder
            is LibrarySource.ServerLibrary -> VoiceIcons.Dns
          },
        title = source.name,
        bookCount = source.bookCount,
        checked = source.shown,
        // a library without any source shown would look empty
        enabled = !source.shown || shownSources > 1,
        onCheckedChange = { onSourceChange(source.source, it) },
      )
    }
  }
}

@Composable
private fun FilterSwitch(
  icon: ImageVector,
  title: String,
  bookCount: Int,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  enabled: Boolean = true,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .toggleable(
          value = checked,
          enabled = enabled,
          role = Role.Switch,
          onValueChange = onCheckedChange,
        )
        .heightIn(min = 64.dp)
        .padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(icon, contentDescription = null)
    Spacer(Modifier.width(16.dp))
    Column(Modifier.weight(1F)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
      )
      Text(
        text =
          pluralStringResource(
            StringsR.plurals.library_search_browse_book_count,
            bookCount,
            bookCount,
          ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(Modifier.width(12.dp))
    Switch(checked = checked, onCheckedChange = null, enabled = enabled)
  }
}

@Preview
@Composable
private fun LibraryFilterContentPreview() {
  VoiceTheme {
    Surface {
      LibraryFilterContent(
        viewState =
          BookOverviewViewState.Loading.copy(hiddenBookCount = 12, notDownloadedBookCount = 30),
        showHiddenBooksSwitch = true,
        showNotDownloadedBooksSwitch = true,
        onShowHiddenBooksChange = {},
        onShowNotDownloadedBooksChange = {},
        onSourceChange = { _, _ -> },
      )
    }
  }
}

@Preview
@Composable
private fun LibraryFilterContentSourcesPreview() {
  VoiceTheme {
    Surface {
      LibraryFilterContent(
        viewState =
          BookOverviewViewState.Loading.copy(
            sources =
              listOf(
                LibrarySourceViewState(
                  source = LibrarySource.Folder("content://audiobooks"),
                  name = "Audiobooks",
                  bookCount = 214,
                  shown = true,
                ),
                LibrarySourceViewState(
                  source = LibrarySource.Folder("content://kids"),
                  name = "Kids",
                  bookCount = 38,
                  shown = false,
                ),
                LibrarySourceViewState(
                  source = LibrarySource.ServerLibrary("radio-plays"),
                  name = "Radio plays",
                  bookCount = 1207,
                  shown = true,
                ),
              ),
          ),
        showHiddenBooksSwitch = false,
        showNotDownloadedBooksSwitch = false,
        onShowHiddenBooksChange = {},
        onShowNotDownloadedBooksChange = {},
        onSourceChange = { _, _ -> },
      )
    }
  }
}
