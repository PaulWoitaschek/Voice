@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.views

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import voice.features.bookOverview.overview.BookOverviewViewState
import voice.features.bookOverview.overview.LibraryGrouping
import voice.features.bookOverview.overview.LibrarySource
import voice.features.bookOverview.overview.LibrarySourceViewState

@Composable
internal fun LibraryFilterSheet(
  viewState: BookOverviewViewState,
  onGroupingChange: (LibraryGrouping) -> Unit,
  onShowHiddenBooksChange: (Boolean) -> Unit,
  onShowNotDownloadedBooksChange: (Boolean) -> Unit,
  onSourceChange: (LibrarySource, Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  // the chips stay while the sheet is open, so one doesn't vanish under the finger
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
      onGroupingChange = onGroupingChange,
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
  onGroupingChange: (LibraryGrouping) -> Unit,
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
    SectionTitle(StringsR.string.library_filter_group_by)
    Spacer(Modifier.height(8.dp))
    FlowRow(
      modifier = Modifier.padding(horizontal = 12.dp).selectableGroup(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      LibraryGrouping.entries.forEach { option ->
        LibraryChip(
          selected = viewState.grouping == option,
          onClick = { onGroupingChange(option) },
          icon =
            when (option) {
              LibraryGrouping.Series -> VoiceIcons.CollectionsBookmark
              LibraryGrouping.Author -> VoiceIcons.Person
            },
          label =
            stringResource(
              when (option) {
                LibraryGrouping.Series -> StringsR.string.library_filter_group_by_series
                LibraryGrouping.Author -> StringsR.string.library_filter_group_by_author
              },
            ),
          modifier = Modifier.semantics { role = Role.RadioButton },
        )
      }
    }
    if (showHiddenBooksSwitch || showNotDownloadedBooksSwitch || viewState.sources.isNotEmpty()) {
      Spacer(Modifier.height(24.dp))
      SectionTitle(StringsR.string.library_filter_show)
      Spacer(Modifier.height(8.dp))
      FlowRow(
        modifier = Modifier.padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        viewState.sources.forEach { source ->
          LibraryChip(
            selected = source.shown,
            onClick = { onSourceChange(source.source, !source.shown) },
            icon =
              when (source.source) {
                is LibrarySource.Folder -> VoiceIcons.Folder
                is LibrarySource.ServerLibrary -> VoiceIcons.Dns
              },
            label = source.name,
          )
        }
        if (showHiddenBooksSwitch) {
          LibraryChip(
            selected = viewState.showHiddenBooks,
            onClick = { onShowHiddenBooksChange(!viewState.showHiddenBooks) },
            icon = VoiceIcons.VisibilityOff,
            label =
              stringResource(StringsR.string.library_filter_hidden_books) +
                " · ${viewState.hiddenBookCount}",
          )
        }
        if (showNotDownloadedBooksSwitch) {
          LibraryChip(
            selected = viewState.showNotDownloadedBooks,
            onClick = { onShowNotDownloadedBooksChange(!viewState.showNotDownloadedBooks) },
            icon = VoiceIcons.Cloud,
            label =
              stringResource(StringsR.string.library_filter_not_downloaded) +
                " · ${viewState.notDownloadedBookCount}",
          )
        }
      }
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
private fun LibraryChip(
  selected: Boolean,
  onClick: () -> Unit,
  icon: ImageVector,
  label: String,
  modifier: Modifier = Modifier,
) {
  FilterChip(
    selected = selected,
    onClick = onClick,
    modifier = modifier,
    leadingIcon = {
      Icon(
        imageVector = if (selected) VoiceIcons.Check else icon,
        contentDescription = null,
        modifier = Modifier.size(FilterChipDefaults.IconSize),
      )
    },
    label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
        onGroupingChange = {},
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
        onGroupingChange = {},
        showHiddenBooksSwitch = hiddenBookCount > 0,
        showNotDownloadedBooksSwitch = false,
        onShowHiddenBooksChange = {},
        onShowNotDownloadedBooksChange = {},
        onSourceChange = { _, _ -> },
      )
    }
  }
}
