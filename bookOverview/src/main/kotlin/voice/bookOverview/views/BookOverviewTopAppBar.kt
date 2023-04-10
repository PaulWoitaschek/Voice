package voice.bookOverview.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentMapOf
import voice.bookOverview.overview.BookOverviewLayoutMode
import voice.bookOverview.overview.BookOverviewViewState
import voice.bookOverview.search.BookSearchContent
import voice.bookOverview.search.BookSearchViewState
import voice.common.BookId
import voice.common.compose.VoiceTheme
import voice.strings.R as StringsR

@Composable
internal fun BookOverviewTopAppBar(
  viewState: BookOverviewViewState,
  onBookMigrationClick: () -> Unit,
  onBoomMigrationHelperConfirmClick: () -> Unit,
  onBookFolderClick: () -> Unit,
  onSettingsClick: () -> Unit,
  onActiveChange: (Boolean) -> Unit,
  onQueryChange: (String) -> Unit,
  onSearchBookClick: (BookId) -> Unit,
) {
  val horizontalPadding by animateDpAsState(targetValue = if (viewState.searchActive) 0.dp else 16.dp)
  SearchBar(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = horizontalPadding),
    query = if (viewState.searchActive) {
      viewState.searchViewState.query
    } else {
      ""
    },
    onQueryChange = onQueryChange,
    onSearch = onQueryChange,
    active = viewState.searchActive,
    onActiveChange = onActiveChange,
    leadingIcon = {
      AnimatedVisibility(
        visible = viewState.searchActive,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        IconButton(onClick = { onActiveChange(false) }) {
          Icon(
            imageVector = Icons.Outlined.ArrowBack,
            contentDescription = stringResource(id = StringsR.string.close),
          )
        }
      }
      AnimatedVisibility(
        visible = !viewState.searchActive,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = stringResource(id = StringsR.string.search_hint),
          )
        }
      }
    },
    trailingIcon = {
      AnimatedVisibility(
        visible = !viewState.searchActive,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        Row {
          if (viewState.showMigrateIcon) {
            MigrateIcon(
              onClick = onBookMigrationClick,
              withHint = viewState.showMigrateHint,
              onHintClick = onBoomMigrationHelperConfirmClick,
            )
          }
          BookFolderIcon(withHint = viewState.showAddBookHint, onClick = onBookFolderClick)

          SettingsIcon(onSettingsClick)
        }
      }
    },
  ) {
    BookSearchContent(
      viewState = viewState.searchViewState,
      contentPadding = PaddingValues(),
      onQueryChange = onQueryChange,
      onBookClick = onSearchBookClick,
    )
  }
}

@Composable
@Preview
private fun BookOverviewTopAppBarPreview() {
  VoiceTheme {
    BookOverviewTopAppBar(
      viewState = BookOverviewViewState(
        books = persistentMapOf(),
        layoutMode = BookOverviewLayoutMode.List,
        playButtonState = BookOverviewViewState.PlayButtonState.Paused,
        showAddBookHint = true,
        showMigrateHint = true,
        showMigrateIcon = true,
        showSearchIcon = true,
        isLoading = true,
        searchActive = true,
        searchViewState = BookSearchViewState.EmptySearch(
          suggestedAuthors = listOf(),
          recentQueries = listOf(),
          query = "",
        ),
      ),
      onBookMigrationClick = {},
      onBoomMigrationHelperConfirmClick = {},
      onBookFolderClick = {},
      onSettingsClick = {},
      onActiveChange = {},
      onQueryChange = {},
      onSearchBookClick = {},
    )
  }
}
