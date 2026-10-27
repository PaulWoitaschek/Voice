package voice.features.bookOverview.editBook

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.core.data.BookId
import voice.core.data.isRemote
import voice.core.data.repo.BookRepository
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.features.bookOverview.bottomSheet.BottomSheetItemViewModel
import voice.features.bookOverview.di.BookActionsScope

@SingleIn(BookActionsScope::class)
@ContributesIntoSet(BookActionsScope::class)
class EditBookViewModel(private val repo: BookRepository) : BottomSheetItemViewModel {

  private val scope = MainScope()

  private val _state = mutableStateOf<EditBookState?>(null)
  internal val state: State<EditBookState?> get() = _state

  override suspend fun items(bookId: BookId): List<BottomSheetItem> {
    // server books take their names from the server
    if (bookId.isRemote) return emptyList()
    return listOf(BottomSheetItem.Edit)
  }

  override suspend fun onItemClick(
    bookId: BookId,
    item: BottomSheetItem,
  ) {
    if (item != BottomSheetItem.Edit) return
    val book = repo.get(bookId) ?: return
    _state.value = EditBookState(
      title = book.content.name,
      author = book.content.author.orEmpty(),
      bookId = bookId,
      cover = book.content.coverUrl,
    )
  }

  internal fun onDismiss() {
    _state.value = null
  }

  internal fun onTitleChange(title: String) {
    _state.value = _state.value?.copy(title = title)
  }

  internal fun onAuthorChange(author: String) {
    _state.value = _state.value?.copy(author = author)
  }

  internal fun onConfirm() {
    val state = _state.value
    if (state != null) {
      check(state.confirmButtonEnabled)
      scope.launch {
        // only what the listener changed, so the other name keeps following the files
        repo.updateBook(state.bookId) { content ->
          var edited = content
          if (state.title != state.initialTitle) edited = edited.withCustomName(state.title.trim())
          if (state.author != state.initialAuthor) edited = edited.withCustomAuthor(state.author.trim().ifEmpty { null })
          edited
        }
      }
    }
    _state.value = null
  }
}
