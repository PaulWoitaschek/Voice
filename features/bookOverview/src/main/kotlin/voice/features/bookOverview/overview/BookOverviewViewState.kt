package voice.features.bookOverview.overview

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import voice.core.data.BookId
import voice.features.bookOverview.search.BookSearchViewState

@Immutable
sealed interface BookOverviewItem {
  val id: String

  data class SingleBook(val state: State<BookOverviewItemViewState>) : BookOverviewItem {
    override val id: String
      get() = state.value.id.value
  }

  data class AuthorGroup(
    val author: String,
    val category: BookOverviewCategory,
    val books: List<State<BookOverviewItemViewState>>,
    val isExpanded: Boolean,
  ) : BookOverviewItem {
    override val id: String
      get() = "author_${category.name}_$author"
  }
}

@Immutable
data class BookOverviewViewState(
  val books: Map<BookOverviewCategory, List<BookOverviewItem>>,
  val layoutMode: BookOverviewLayoutMode,
  val playButtonState: PlayButtonState?,
  val showAddBookHint: Boolean,
  val showSearchIcon: Boolean,
  val isLoading: Boolean,
  val searchActive: Boolean,
  val searchViewState: BookSearchViewState,
  val showStoragePermissionBugCard: Boolean,
  val showFolderPickerIcon: Boolean,
  val dialog: Dialog?,
) {

  companion object {
    val Loading = BookOverviewViewState(
      books = mapOf(),
      layoutMode = BookOverviewLayoutMode.List,
      playButtonState = null,
      showAddBookHint = false,
      showSearchIcon = false,
      isLoading = true,
      searchActive = false,
      searchViewState = BookSearchViewState.EmptySearch(
        suggestedAuthors = emptyList(),
        recentQueries = emptyList(),
        query = "",
      ),
      showStoragePermissionBugCard = false,
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
