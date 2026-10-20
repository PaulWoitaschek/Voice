package voice.features.bookOverview.overview

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import voice.core.data.BookId

@Immutable
data class BookOverviewViewState(
  val books: Map<BookOverviewCategory, Map<BookId, State<BookOverviewItemViewState>>>,
  val currentBookId: BookId?,
  val layoutMode: BookOverviewLayoutMode,
  val playButtonState: PlayButtonState?,
  val showEmptyLibrary: Boolean,
  val showSearchIcon: Boolean,
  val isLoading: Boolean,
  val showStoragePermissionBugCard: Boolean,
  val showSurveyCard: Boolean,
  val showFolderAccessBadge: Boolean,
  val showFolderPickerIcon: Boolean,
  val dialog: Dialog?,
) {

  companion object {
    val Loading = BookOverviewViewState(
      books = mapOf(),
      currentBookId = null,
      layoutMode = BookOverviewLayoutMode.List,
      playButtonState = null,
      showEmptyLibrary = false,
      showSearchIcon = false,
      isLoading = true,
      showStoragePermissionBugCard = false,
      showSurveyCard = false,
      showFolderAccessBadge = false,
      showFolderPickerIcon = true,
      dialog = null,
    )
  }

  enum class PlayButtonState {
    Playing,
    Paused,
  }

  enum class Dialog {
    FolderPickerMovedToSettings,
  }
}
