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
import voice.core.data.category
import voice.core.data.repo.BookRepository
import voice.features.bookOverview.overview.AuthorEntryViewState
import voice.features.bookOverview.overview.AuthorGroup
import voice.features.bookOverview.overview.authorKey
import voice.features.bookOverview.overview.libraryEntries
import voice.features.bookOverview.overview.toEntryViewState
import voice.features.bookOverview.select.BookSelectionRequests
import voice.navigation.Destination
import voice.navigation.Navigator

@AssistedInject
class AuthorActionsViewModel(
  private val repo: BookRepository,
  private val bookSelectionRequests: BookSelectionRequests,
  private val navigator: Navigator,
  @Assisted private val destination: Destination.AuthorActions,
) {

  @Composable
  internal fun state(): AuthorEntryViewState? {
    val books = remember { repo.flow() }.collectAsState(initial = null).value ?: return null
    val author =
      remember(books) {
        val ids = destination.bookIds.toSet()
        val authorBooks = books.filter { it.id in ids }
        if (authorBooks.isEmpty()) return@remember null
        AuthorGroup(
            key = authorKey(destination.name),
            name = destination.name,
            entries = libraryEntries(authorBooks).values.flatten(),
          )
          .toEntryViewState(authorBooks.first().category)
      }
    // with no books there is no menu to show, and an invisible one would swallow the next back
    // press
    LaunchedEffect(author == null) {
      if (author == null) onClose()
    }
    return author
  }

  internal fun onSelect() {
    bookSelectionRequests.select(destination.bookIds)
  }

  internal fun onClose() {
    navigator.remove(destination)
  }

  @AssistedFactory
  interface Factory {
    fun create(destination: Destination.AuthorActions): AuthorActionsViewModel

    @ContributesTo(AppScope::class)
    interface Provider {
      val authorActionsViewModelFactory: Factory
    }
  }
}
