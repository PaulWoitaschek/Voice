package voice.features.bookOverview.series

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesTo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.playback.ExperimentalPlaybackPersistenceFeatureFlag
import voice.core.playback.LivePlaybackState
import voice.core.playback.PlayerController
import voice.core.playback.overlay
import voice.core.playback.playstate.PlayStateManager
import voice.features.bookOverview.overview.BookOverviewCategory
import voice.features.bookOverview.overview.CurrentBook
import voice.features.bookOverview.overview.category
import voice.features.bookOverview.overview.series
import voice.features.bookOverview.overview.seriesKey
import voice.features.bookOverview.overview.seriesName
import voice.features.bookOverview.overview.seriesPart
import voice.features.bookOverview.overview.toItemViewState
import voice.navigation.Destination
import voice.navigation.Navigator

@AssistedInject
class SeriesViewModel(
  private val repo: BookRepository,
  private val navigator: Navigator,
  private val playerController: PlayerController,
  private val currentBookStore: CurrentBookStore,
  private val playStateManager: PlayStateManager,
  private val experimentalPlaybackPersistenceFeatureFlag: ExperimentalPlaybackPersistenceFeatureFlag,
  dispatcherProvider: DispatcherProvider,
  @Assisted
  private val name: String,
) {

  private val scope = MainScope(dispatcherProvider)
  private val key = seriesKey(name)

  @Composable
  internal fun viewState(): SeriesViewState? {
    val books = remember { repo.flow() }.collectAsState(initial = null).value ?: return null
    // the button would say Continue for a moment before it says Pause
    val currentBookId = (remember { currentBookStore.data.map(::CurrentBook) }.collectAsState(initial = null).value ?: return null).id
    val playing = remember { playStateManager.playStateFlow }.collectAsState().value == PlayStateManager.PlayState.Playing
    val livePlaybackState = livePlaybackState(currentBookId)
    val series = remember(books, livePlaybackState) {
      books
        .filter { book -> book.seriesName?.let(::seriesKey) == key }
        .map { book -> livePlaybackState?.let(book::overlay) ?: book }
        .series()
        .singleOrNull()
    }
    LaunchedEffect(series == null) {
      if (series == null) {
        navigator.remove(Destination.Series(name))
      }
    }
    if (series == null) return null
    val playingBook = series.books.find { it.id == currentBookId }?.takeIf { playing }
    val action = (playingBook ?: series.inProgress ?: series.firstUnfinished)?.let { book ->
      SeriesViewState.Action(
        bookId = book.id,
        part = book.seriesPart,
        continues = book.category == BookOverviewCategory.CURRENT,
        playing = book == playingBook,
      )
    }
    return SeriesViewState(
      key = series.key,
      name = series.name,
      author = series.author,
      covers = series.covers,
      bookCount = series.books.size,
      finishedCount = series.finishedCount,
      action = action,
      books = series.books.map { book ->
        SeriesViewState.Book(
          // the header names the author already
          book = book.toItemViewState().let { if (it.author == series.author) it.copy(author = null) else it },
          part = book.seriesPart,
          finished = book.category == BookOverviewCategory.FINISHED,
          current = book.id == currentBookId,
          playing = book == playingBook,
        )
      },
    )
  }

  internal fun onActionClick(id: BookId) {
    scope.launch {
      if (currentBookStore.data.first() == id) {
        playerController.playPause()
      } else {
        playerController.pauseIfCurrentBookDifferentFrom(id)
        currentBookStore.updateData { id }
        playerController.play()
      }
    }
  }

  // with the experimental persistence, the stored position of the playing book lags behind
  @Composable
  private fun livePlaybackState(currentBookId: BookId?): LivePlaybackState? {
    val experimentalPlaybackPersistence = remember { experimentalPlaybackPersistenceFeatureFlag.get() }
    if (!experimentalPlaybackPersistence || currentBookId == null) return null
    return remember(currentBookId) { playerController.livePlaybackStateFlow(currentBookId) }
      .collectAsState(initial = null).value
  }

  internal fun onBookClick(id: BookId) {
    navigator.goTo(Destination.Playback(id))
  }

  internal fun onBookLongClick(id: BookId) {
    navigator.goTo(Destination.BookActions(id))
  }

  internal fun onCloseClick() {
    navigator.goBack()
  }

  @AssistedFactory
  interface Factory {
    fun create(name: String): SeriesViewModel

    @ContributesTo(AppScope::class)
    interface Provider {
      val seriesViewModelFactory: Factory
    }
  }
}
