package voice.bookOverview

import androidx.compose.runtime.Immutable
import voice.common.compose.ImmutableFile
import voice.data.Book

sealed interface BookOverviewViewState {

  val layoutIcon: Content.LayoutIcon?
  val playButtonState: PlayButtonState?
  val showAddBookHint: Boolean
  val showMigrateHint: Boolean
  val showMigrateIcon: Boolean

  object Loading : BookOverviewViewState {
    override val playButtonState: PlayButtonState? = null
    override val layoutIcon: Content.LayoutIcon? = null
    override val showAddBookHint: Boolean = false
    override val showMigrateHint: Boolean = false
    override val showMigrateIcon: Boolean = false
  }

  @Immutable
  data class Content(
    val books: Map<BookOverviewCategory, List<BookViewState>>,
    val layoutMode: LayoutMode,
    override val layoutIcon: LayoutIcon?,
    override val playButtonState: PlayButtonState?,
    override val showAddBookHint: Boolean,
    override val showMigrateHint: Boolean,
    override val showMigrateIcon: Boolean,
    val editBookTitleState: EditBookTitleState?,
  ) : BookOverviewViewState {

    data class EditBookTitleState(
      val title: String,
      val bookId: Book.Id
    )

    @Immutable
    data class BookViewState(
      val name: String,
      val author: String?,
      val cover: ImmutableFile?,
      val progress: Float,
      val id: Book.Id,
      val remainingTime: String,
    )

    enum class LayoutMode {
      List, Grid
    }

    enum class LayoutIcon {
      List, Grid
    }
  }

  enum class PlayButtonState {
    Playing, Paused
  }
}
