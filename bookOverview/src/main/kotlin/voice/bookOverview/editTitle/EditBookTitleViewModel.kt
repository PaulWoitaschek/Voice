package voice.bookOverview.editTitle

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.bookOverview.bottomSheet.BottomSheetSelectionStateHolder
import voice.bookOverview.di.BookOverviewScope
import voice.data.repo.BookRepository
import javax.inject.Inject

interface EditBookTitleListener {
  fun onDismissEditTitle()
  fun onUpdateEditTitle(title: String)
  fun onConfirmEditTitle()
}

@BookOverviewScope
class EditBookTitleViewModel
@Inject
constructor(
  private val repo: BookRepository,
  private val stateHolder: BottomSheetSelectionStateHolder,
) : EditBookTitleListener {

  private val scope = MainScope()

  private val _state = mutableStateOf<EditBookTitleState?>(null)
  internal val state: State<EditBookTitleState?> get() = _state

  fun onEditBookTitleClick() {
    val id = stateHolder.selectedBook ?: return
    scope.launch {
      val book = repo.get(id) ?: return@launch
      _state.value = EditBookTitleState(
        title = book.content.name,
        bookId = id,
      )
    }
  }

  override fun onDismissEditTitle() {
    _state.value = null
  }

  override fun onUpdateEditTitle(title: String) {
    _state.value = _state.value?.copy(title = title)
  }

  override fun onConfirmEditTitle() {
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
