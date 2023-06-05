package voice.bookOverview.views.topbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import voice.bookOverview.views.BookFolderIcon
import voice.bookOverview.views.MigrateIcon
import voice.bookOverview.views.SettingsIcon

@Composable
internal fun ColumnScope.TopBarTrailingIcon(
  searchActive: Boolean,
  showMigrateIcon: Boolean,
  showMigrateHint: Boolean,
  showAddBookHint: Boolean,
  onBookMigrationClick: () -> Unit,
  onBoomMigrationHelperConfirmClick: () -> Unit,
  onBookFolderClick: () -> Unit,
  onSettingsClick: () -> Unit,
) {
  AnimatedVisibility(
    visible = !searchActive,
    enter = fadeIn(),
    exit = fadeOut(),
  ) {
    Row {
      if (showMigrateIcon) {
        MigrateIcon(
          onClick = onBookMigrationClick,
          withHint = showMigrateHint,
          onHintClick = onBoomMigrationHelperConfirmClick,
        )
      }
      BookFolderIcon(withHint = showAddBookHint, onClick = onBookFolderClick)

      SettingsIcon(onSettingsClick)
    }
  }
}
