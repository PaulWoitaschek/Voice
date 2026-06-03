package voice.features.bookOverview.bottomSheet

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.core.data.BookId
import voice.features.bookOverview.di.BookOverviewScope

@SingleIn(BookOverviewScope::class)
@Inject
class BottomSheetViewModel(private val viewModels: Set<@JvmSuppressWildcards BottomSheetItemViewModel>) {

  private val scope = MainScope()

  internal val state: State<EditBookBottomSheetState>
    field = mutableStateOf(EditBookBottomSheetState(emptyList()))

  var bookId: BookId? = null
    private set

  internal fun bookSelected(bookId: BookId) {
    this.bookId = bookId
    scope.launch {
      val items = viewModels.flatMap { it.items(bookId) }
        .toSet()
        .sorted()
      state.value = EditBookBottomSheetState(items)
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
