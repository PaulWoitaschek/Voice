package voice.features.bookOverview.overview

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import voice.core.data.BookId

@Immutable
data class BookOverviewViewState(
  val sections: Map<BookOverviewCategory, List<LibraryItem>>,
  val hero: State<BookOverviewItemViewState>?,
  val inProgressCount: Int,
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
  val hiddenBookCount: Int = 0,
  val showHiddenBooks: Boolean = false,
) {

  companion object {
    val Loading =
      BookOverviewViewState(
        sections = mapOf(),
        hero = null,
        inProgressCount = 0,
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

@Immutable
sealed interface LibraryItem {

  val key: String

  @Immutable
  data class Book(
    val id: BookId,
    val state: State<BookOverviewItemViewState>,
  ) : LibraryItem {
    override val key: String
      get() = id.value
  }

  @Immutable
  data class Series(val series: SeriesStackViewState) : LibraryItem {
    // a book id is a URI or starts with abs:, so this never clashes with one
    override val key: String
      get() = "series:${series.key}"
  }
}

@Immutable
data class SeriesStackViewState(
  val key: String,
  val name: String,
  val author: String?,
  val covers: List<String?>,
  val bookCount: Int,
  val finishedCount: Int,
  val progress: List<Float>,
  val category: BookOverviewCategory,
)

internal fun Series.toStackViewState() =
  SeriesStackViewState(
    key = key,
    name = name,
    author = author,
    covers = covers,
    bookCount = books.size,
    finishedCount = finishedCount,
    progress = progress,
    category = category,
  )
