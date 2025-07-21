package voice.bookOverview.editTitle

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.bookOverview.bottomSheet.BottomSheetItem
import voice.bookOverview.bottomSheet.BottomSheetItemViewModel
import voice.bookOverview.di.BookOverviewScope
import voice.common.BookId
import voice.data.repo.BookRepository

@BookOverviewScope
@ContributesIntoSet(
  scope = BookOverviewScope::class,
  binding = binding<BottomSheetItemViewModel>(),
)
@Inject
class EditBookTitleViewModel(private val repo: BookRepository) : BottomSheetItemViewModel {

  private val scope = MainScope()

  private val _state = mutableStateOf<EditBookTitleState?>(null)
  internal val state: State<EditBookTitleState?> get() = _state

  override suspend fun items(bookId: BookId): List<BottomSheetItem> {
    return listOf(BottomSheetItem.Title)
  }

  override suspend fun onItemClick(
    bookId: BookId,
    item: BottomSheetItem,
  ) {
    if (item != BottomSheetItem.Title) return
    val book = repo.get(bookId) ?: return
    _state.value = EditBookTitleState(
      title = book.content.name,
      bookId = bookId,
    )
  }

  internal fun onDismissEditTitle() {
    _state.value = null
  }

  internal fun onUpdateEditTitle(title: String) {
    _state.value = _state.value?.copy(title = title)
  }

  internal fun onConfirmEditTitle() {
    val state = _state.value
    if (state != null) {
      check(state.confirmButtonEnabled)
      scope.launch {
        repo.updateBook(state.bookId) {
          it.copy(name = state.title.trim())
        }
      }
    }
    _state.value = null
  }
}
