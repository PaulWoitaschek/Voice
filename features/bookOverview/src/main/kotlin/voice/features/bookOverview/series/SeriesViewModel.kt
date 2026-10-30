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
import kotlinx.coroutines.launch
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.playback.PlayerController
import voice.features.bookOverview.overview.BookOverviewCategory
import voice.features.bookOverview.overview.category
import voice.features.bookOverview.overview.part
import voice.features.bookOverview.overview.series
import voice.features.bookOverview.overview.seriesKey
import voice.features.bookOverview.overview.seriesName
import voice.features.bookOverview.overview.toItemViewState
import voice.navigation.Destination
import voice.navigation.Navigator

@AssistedInject
class SeriesViewModel(
  private val repo: BookRepository,
  private val navigator: Navigator,
  private val playerController: PlayerController,
  private val currentBookStore: CurrentBookStore,
  dispatcherProvider: DispatcherProvider,
  @Assisted
  private val name: String,
) {

  private val scope = MainScope(dispatcherProvider)
  private val key = seriesKey(name)

  @Composable
  internal fun viewState(): SeriesViewState? {
    val books = remember { repo.flow() }.collectAsState(initial = null).value ?: return null
    val series = remember(books) {
      books.filter { book -> book.seriesName?.let(::seriesKey) == key }.series().singleOrNull()
    }
    LaunchedEffect(series == null) {
      if (series == null) {
        navigator.remove(Destination.Series(name))
      }
    }
    if (series == null) return null
    val action = series.inProgress?.let { SeriesViewState.Action(it.id, it.part, continues = true) }
      ?: series.firstUnfinished?.let { SeriesViewState.Action(it.id, it.part, continues = false) }
    return SeriesViewState(
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
          part = book.part,
          finished = book.category == BookOverviewCategory.FINISHED,
        )
      },
    )
  }

  internal fun onActionClick(id: BookId) {
    scope.launch {
      if (currentBookStore.data.first() != id) {
        playerController.pauseIfCurrentBookDifferentFrom(id)
        currentBookStore.updateData { id }
      }
      playerController.play()
      navigator.goTo(Destination.Playback(id))
    }
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
