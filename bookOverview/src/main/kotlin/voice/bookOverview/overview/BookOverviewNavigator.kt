package voice.bookOverview.overview

import androidx.compose.runtime.Stable
import voice.data.Book

@Stable
interface BookOverviewNavigator {
  fun onSettingsClick()
  fun onBookMigrationClick()
  fun toFolderOverview()
  fun toBook(id: Book.Id)
  fun onCoverFromInternetClick(id: Book.Id)
  fun onFileCoverClick(id: Book.Id)
}
