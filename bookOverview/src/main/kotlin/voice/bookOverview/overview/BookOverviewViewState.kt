package voice.bookOverview.overview

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

@Immutable
data class BookOverviewViewState(
  val books: ImmutableMap<BookOverviewCategory, List<BookOverviewItemViewState>>,
  val layoutMode: BookOverviewLayoutMode,
  val playButtonState: PlayButtonState?,
  val showAddBookHint: Boolean,
  val showMigrateHint: Boolean,
  val showMigrateIcon: Boolean,
  val showSearchIcon: Boolean,
  val isLoading: Boolean,
) {

  companion object {
    val Loading = BookOverviewViewState(
      books = persistentMapOf(),
      layoutMode = BookOverviewLayoutMode.List,
      playButtonState = null,
      showAddBookHint = false,
      showMigrateHint = false,
      showMigrateIcon = false,
      showSearchIcon = false,
      isLoading = true,
    )
  }

  enum class PlayButtonState {
    Playing, Paused
  }
}
