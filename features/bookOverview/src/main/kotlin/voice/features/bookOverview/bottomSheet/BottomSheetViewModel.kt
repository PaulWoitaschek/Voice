package voice.features.bookOverview.bottomSheet

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import voice.core.audiobookshelf.ServerLibrary
import voice.core.data.BookId
import voice.core.data.isRemote
import voice.core.data.repo.BookRepository
import voice.features.bookOverview.di.BookOverviewScope
import voice.features.bookOverview.overview.category
import voice.features.bookOverview.overview.toItemViewState

@SingleIn(BookOverviewScope::class)
@Inject
class BottomSheetViewModel(
  private val viewModels: Set<@JvmSuppressWildcards BottomSheetItemViewModel>,
  private val repo: BookRepository,
  private val serverLibrary: ServerLibrary,
) {

  private val scope = MainScope()
  private var loadJob: Job? = null

  internal val state: State<EditBookBottomSheetState>
    field = mutableStateOf(EditBookBottomSheetState.Empty)

  var bookId: BookId? = null
    private set

  internal fun bookSelected(bookId: BookId) {
    this.bookId = bookId
    // the previous book must not flash up while this one loads
    state.value = EditBookBottomSheetState.Empty
    loadJob?.cancel()
    loadJob = scope.launch {
      val book = repo.get(bookId)
      val items = viewModels.flatMap { it.items(bookId) }
        .toSet()
        .sorted()
      state.value = EditBookBottomSheetState(
        book = book?.toItemViewState(),
        category = book?.category,
        items = items,
        source = source(bookId),
      )
    }
  }

  // with books only on the device there is nothing to tell apart
  private suspend fun source(bookId: BookId): BookSource? {
    val serverName = serverLibrary.serverName.first() ?: return null
    return if (bookId.isRemote) {
      BookSource.Server(name = serverName, downloaded = bookId in serverLibrary.downloadedBooks.first())
    } else {
      BookSource.Device
    }
  }

  internal fun onItemClick(item: BottomSheetItem) {
    val bookId = bookId ?: return
    scope.launch {
      viewModels.forEach {
        it.onItemClick(bookId, item)
      }
    }
  }
}
