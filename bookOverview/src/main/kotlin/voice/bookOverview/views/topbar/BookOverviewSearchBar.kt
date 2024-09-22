package voice.bookOverview.views.topbar

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import voice.bookOverview.search.BookSearchContent
import voice.bookOverview.search.BookSearchViewState
import voice.common.BookId

@Composable
internal fun ColumnScope.BookOverviewSearchBar(
  horizontalPadding: Dp,
  onQueryChange: (String) -> Unit,
  onActiveChange: (Boolean) -> Unit,
  onBookMigrationClick: () -> Unit,
  onBoomMigrationHelperConfirmClick: () -> Unit,
  onBookFolderClick: () -> Unit,
  onSettingsClick: () -> Unit,
  onSearchBookClick: (BookId) -> Unit,
  searchActive: Boolean,
  showMigrateIcon: Boolean,
  showMigrateHint: Boolean,
  showAddBookHint: Boolean,
  searchViewState: BookSearchViewState,
) {
  SearchBar(
    inputField = {
      SearchBarDefaults.InputField(
        query = if (searchActive) {
          searchViewState.query
        } else {
          ""
        },
        onQueryChange = onQueryChange,
        onSearch = onQueryChange,
        expanded = searchActive,
        onExpandedChange = onActiveChange,
        leadingIcon = {
          TopBarLeadingIcon(
            searchActive = searchActive,
            onActiveChange = onActiveChange,
          )
        },
        trailingIcon = {
          TopBarTrailingIcon(
            searchActive = searchActive,
            showMigrateIcon = showMigrateIcon,
            showMigrateHint = showMigrateHint,
            showAddBookHint = showAddBookHint,
            onBookMigrationClick = onBookMigrationClick,
            onBoomMigrationHelperConfirmClick = onBoomMigrationHelperConfirmClick,
            onBookFolderClick = onBookFolderClick,
            onSettingsClick = onSettingsClick,
          )
        },
      )
    },
    expanded = searchActive,
    onExpandedChange = onActiveChange,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = horizontalPadding),
    content = {
      BookSearchContent(
        viewState = searchViewState,
        contentPadding = PaddingValues(),
        onQueryChange = onQueryChange,
        onBookClick = onSearchBookClick,
      )
    },
  )
}
