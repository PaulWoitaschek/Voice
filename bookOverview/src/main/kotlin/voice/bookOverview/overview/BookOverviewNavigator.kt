package voice.bookOverview.overview

import androidx.compose.runtime.Stable
import voice.common.BookId

@Stable
interface BookOverviewNavigator {
  fun onSettingsClick()
  fun onBookMigrationClick()
  fun toFolderOverview()
  fun toBook(id: BookId)
  fun onCoverFromInternetClick(id: BookId)
  fun onFileCoverClick(id: BookId)
}
