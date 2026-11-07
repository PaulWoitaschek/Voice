package voice.features.bookOverview.author

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesTo
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.SerializationException
import voice.core.audiobookshelf.ServerDownloads
import voice.core.audiobookshelf.ServerLibrary
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.data.Book
import voice.core.data.BookCategory
import voice.core.data.BookId
import voice.core.data.category
import voice.core.data.repo.BookRepository
import voice.core.logging.api.Logger
import voice.features.bookOverview.overview.LibraryEntry
import voice.features.bookOverview.overview.badge
import voice.features.bookOverview.overview.libraryEntries
import voice.features.bookOverview.overview.mostCommon
import voice.features.bookOverview.overview.nameKey
import voice.features.bookOverview.overview.toItemViewState
import voice.features.bookOverview.overview.toStackViewState
import voice.navigation.Destination
import voice.navigation.Navigator

@AssistedInject
class AuthorViewModel(
  private val repo: BookRepository,
  private val navigator: Navigator,
  private val serverLibrary: ServerLibrary,
  private val serverDownloads: ServerDownloads,
  private val authorInfoLookup: AuthorInfoLookup,
  dispatcherProvider: DispatcherProvider,
  @Assisted private val name: String,
) {

  private val scope = MainScope(dispatcherProvider)

  private val info: StateFlow<AuthorInfo?> = flow {
    emit(lookUpInfo())
  }
    .stateIn(scope, SharingStarted.Lazily, null)

  @Composable
  internal fun viewState(): AuthorViewState? {
    val books = remember { repo.flow() }.collectAsState(initial = null).value ?: return null
    val unavailableBooks = remember {
      serverLibrary.unavailableBooks
    }
      .collectAsState(initial = emptySet())
      .value
    val downloadStates = remember {
      serverDownloads.states
    }
      .collectAsState(initial = emptyMap())
      .value
    val authorInfo = info.collectAsState().value
    val authorBooks = remember(books) { books.byAuthor(name) }
    LaunchedEffect(authorBooks.isEmpty()) {
      if (authorBooks.isEmpty()) {
        navigator.remove(Destination.Author(name))
      }
    }
    if (authorBooks.isEmpty()) return null
    // in progress first, then the ones left to listen to, like in the library
    val entries =
      remember(authorBooks) { libraryEntries(authorBooks).values.flatten() }
        .map { entry ->
          when (entry) {
            is LibraryEntry.Single ->
              AuthorViewState.Entry.Book(
                // the header names the author already
                book =
                  entry.book
                    .toItemViewState(
                      unavailable = entry.book.id in unavailableBooks,
                      download = downloadStates[entry.book.id].badge(),
                    )
                    .copy(author = null),
                finished = entry.book.category == BookCategory.FINISHED,
              )
            is LibraryEntry.Stack ->
              AuthorViewState.Entry.Series(entry.series.toStackViewState().copy(author = null))
          }
        }
    return AuthorViewState(
      name = authorBooks.mapNotNull { it.author?.trim() }.mostCommon() ?: name,
      bookCount = authorBooks.size,
      finishedCount = authorBooks.count { it.category == BookCategory.FINISHED },
      info = authorInfo,
      entries = entries,
    )
  }

  private suspend fun lookUpInfo(): AuthorInfo? {
    return try {
      authorInfoLookup.info(name, Locale.getDefault())
    } catch (e: IOException) {
      Logger.w(e, "Could not look up the author")
      null
    } catch (e: SerializationException) {
      Logger.w(e, "Could not read the author info")
      null
    }
  }

  internal fun onBookClick(id: BookId) {
    navigator.goTo(Destination.Playback(id))
  }

  internal fun onBookLongClick(id: BookId) {
    navigator.goTo(Destination.BookActions(id))
  }

  internal fun onSeriesClick(name: String) {
    navigator.goTo(Destination.Series(name))
  }

  internal fun onSeriesLongClick(name: String) {
    navigator.goTo(Destination.SeriesActions(name))
  }

  internal fun onWikipediaClick(url: String) {
    navigator.goTo(Destination.Website(url))
  }

  internal fun onCloseClick() {
    navigator.goBack()
  }

  @AssistedFactory
  interface Factory {
    fun create(name: String): AuthorViewModel

    @ContributesTo(AppScope::class)
    interface Provider {
      val authorViewModelFactory: Factory
    }
  }
}

private fun List<Book>.byAuthor(name: String): List<Book> {
  val key = nameKey(name) ?: return emptyList()
  return filter { book -> book.author?.let(::nameKey) == key }
}
