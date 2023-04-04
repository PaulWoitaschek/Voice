package voice.bookOverview.views

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.collections.immutable.persistentMapOf
import voice.bookOverview.overview.BookOverviewLayoutMode
import voice.bookOverview.overview.BookOverviewViewState
import voice.common.R
import voice.common.compose.VoiceTheme

@Composable
internal fun BookOverviewTopAppBar(
  scrollBehavior: TopAppBarScrollBehavior,
  viewState: BookOverviewViewState,
  onSearchClick: () -> Unit,
  onBookMigrationClick: () -> Unit,
  onBoomMigrationHelperConfirmClick: () -> Unit,
  onBookFolderClick: () -> Unit,
  onSettingsClick: () -> Unit,
) {
  TopAppBar(
    title = {
      Text(text = stringResource(id = R.string.app_name))
    },
    scrollBehavior = scrollBehavior,
    actions = {
      if (viewState.showSearchIcon) {
        IconButton(onClick = onSearchClick) {
          Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
        }
      }
      if (viewState.showMigrateIcon) {
        MigrateIcon(
          onClick = onBookMigrationClick,
          withHint = viewState.showMigrateHint,
          onHintClick = onBoomMigrationHelperConfirmClick,
        )
      }
      BookFolderIcon(withHint = viewState.showAddBookHint, onClick = onBookFolderClick)

      SettingsIcon(onSettingsClick)
    },
  )
}

@Composable
@Preview
private fun BookOverviewTopAppBarPreview() {
  VoiceTheme {
    BookOverviewTopAppBar(
      scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
      viewState = BookOverviewViewState(
        books = persistentMapOf(),
        layoutMode = BookOverviewLayoutMode.List,
        playButtonState = BookOverviewViewState.PlayButtonState.Paused,
        showAddBookHint = true,
        showMigrateHint = true,
        showMigrateIcon = true,
        showSearchIcon = true,
        isLoading = true,
      ),
      onSearchClick = {},
      onBookMigrationClick = {},
      onBoomMigrationHelperConfirmClick = {},
      onBookFolderClick = {},
      onSettingsClick = {},
    )
  }
}
