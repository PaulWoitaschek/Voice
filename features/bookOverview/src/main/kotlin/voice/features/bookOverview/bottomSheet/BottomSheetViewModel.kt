package voice.features.bookOverview.bottomSheet

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.features.bookOverview.di.BookOverviewScope
import voice.features.bookOverview.overview.category
import voice.features.bookOverview.overview.toItemViewState

@SingleIn(BookOverviewScope::class)
@Inject
class BottomSheetViewModel(
  private val viewModels: Set<@JvmSuppressWildcards BottomSheetItemViewModel>,
  private val repo: BookRepository,
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
      )
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
