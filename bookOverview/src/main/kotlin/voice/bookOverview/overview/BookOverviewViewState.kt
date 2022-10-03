package voice.bookOverview.overview

import androidx.compose.runtime.Immutable

sealed interface BookOverviewViewState {

  val playButtonState: PlayButtonState?
  val showAddBookHint: Boolean
  val showMigrateHint: Boolean
  val showMigrateIcon: Boolean
  val showSearchIcon: Boolean

  object Loading : BookOverviewViewState {
    override val playButtonState: PlayButtonState? = null
    override val showAddBookHint: Boolean = false
    override val showMigrateHint: Boolean = false
    override val showMigrateIcon: Boolean = false
    override val showSearchIcon: Boolean = false
  }

  @Immutable
  data class Content(
    val books: Map<BookOverviewCategory, List<BookOverviewItemViewState>>,
    val layoutMode: BookOverviewLayoutMode,
    override val playButtonState: PlayButtonState?,
    override val showAddBookHint: Boolean,
    override val showMigrateHint: Boolean,
    override val showMigrateIcon: Boolean,
    override val showSearchIcon: Boolean,
  ) : BookOverviewViewState

  enum class PlayButtonState {
    Playing, Paused
  }
}
