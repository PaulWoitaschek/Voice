@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.views

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR
import voice.core.ui.VoiceTheme
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
    modifier =
      modifier
        .fillMaxWidth()
        // with many sources, the chips can be taller than the screen
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 12.dp)
        .padding(bottom = 24.dp),
  ) {
    Text(
      modifier = Modifier.padding(horizontal = 12.dp).semantics { heading() },
      text = stringResource(StringsR.string.library_filter_show),
      style = MaterialTheme.typography.labelLargeEmphasized,
    )
    Spacer(Modifier.height(8.dp))
    if (viewState.sources.isNotEmpty()) {
      FlowRow(
        modifier = Modifier.padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        viewState.sources.forEach { source ->
          SourceChip(source = source, onClick = { onSourceChange(source.source, !source.shown) })
        }
      }
    }
    if (showHiddenBooksSwitch) {
      FilterSwitch(
        icon = UiR.drawable.ic_visibility_off,
        title = stringResource(StringsR.string.library_filter_hidden_books),
        bookCount = viewState.hiddenBookCount,
        checked = viewState.showHiddenBooks,
        onCheckedChange = onShowHiddenBooksChange,
      )
    }
    if (showNotDownloadedBooksSwitch) {
      FilterSwitch(
        icon = UiR.drawable.ic_cloud,
        title = stringResource(StringsR.string.library_filter_not_downloaded),
        bookCount = viewState.notDownloadedBookCount,
        checked = viewState.showNotDownloadedBooks,
        onCheckedChange = onShowNotDownloadedBooksChange,
      )
    }
  }
}

@Composable
private fun FilterSwitch(
  @DrawableRes icon: Int,
  title: String,
  bookCount: Int,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .toggleable(
          value = checked,
          role = Role.Switch,
          onValueChange = onCheckedChange,
        )
        .heightIn(min = 64.dp)
        .padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(painterResource(icon), contentDescription = null)
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
    Switch(checked = checked, onCheckedChange = null)
  }
}

@Composable
private fun SourceChip(
  source: LibrarySourceViewState,
  onClick: () -> Unit,
) {
  FilterChip(
    selected = source.shown,
    onClick = onClick,
    leadingIcon = {
      Icon(
        painter =
          painterResource(
            when {
              source.shown -> UiR.drawable.ic_check
              source.source is LibrarySource.Folder -> UiR.drawable.ic_folder
              else -> UiR.drawable.ic_dns
            },
          ),
        contentDescription = null,
        modifier = Modifier.size(FilterChipDefaults.IconSize),
      )
    },
    label = { Text(source.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
  )
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
  SourcesPreview(names = listOf("Audiobooks", "Kids", "Radio plays"), hiddenBookCount = 12)
}

@Preview
@Composable
private fun LibraryFilterContentManySourcesPreview() {
  SourcesPreview(
    names =
      listOf(
        "Audiobooks",
        "Kids",
        "Radio plays",
        "Fantasy",
        "Science fiction",
        "Podcasts",
        "The complete works of Terry Pratchett, read by Stephen Briggs",
        "Crime",
        "Classics",
        "Language courses",
        "Biographies",
        "History",
        "Bedtime stories",
        "Downloads",
      ),
  )
}

@Composable
private fun SourcesPreview(
  names: List<String>,
  hiddenBookCount: Int = 0,
) {
  VoiceTheme {
    Surface {
      LibraryFilterContent(
        viewState =
          BookOverviewViewState.Loading.copy(
            hiddenBookCount = hiddenBookCount,
            sources =
              names.mapIndexed { index, name ->
                LibrarySourceViewState(
                  source =
                    if (index % 3 == 2) {
                      LibrarySource.ServerLibrary(name)
                    } else {
                      LibrarySource.Folder(name)
                    },
                  name = name,
                  shown = index % 4 != 1,
                )
              },
          ),
        showHiddenBooksSwitch = hiddenBookCount > 0,
        showNotDownloadedBooksSwitch = false,
        onShowHiddenBooksChange = {},
        onShowNotDownloadedBooksChange = {},
        onSourceChange = { _, _ -> },
      )
    }
  }
}
