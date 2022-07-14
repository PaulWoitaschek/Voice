package voice.bookOverview.bottomSheet

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.bookOverview.di.BookOverviewScope
import voice.data.Book
import javax.inject.Inject

@BookOverviewScope
class BottomSheetViewModel
@Inject constructor(
  private val viewModels: Set<@JvmSuppressWildcards BottomSheetItemViewModel>
) {

  private val _state: MutableState<EditBookBottomSheetState> = mutableStateOf(EditBookBottomSheetState(emptyList()))
  internal val state: State<EditBookBottomSheetState> get() = _state
  private val scope = MainScope()

  internal fun bookSelected(bookId: Book.Id) {
    scope.launch {
      val items = viewModels.flatMap { it.items(bookId) }
        .toSet()
        .sorted()
      _state.value = EditBookBottomSheetState(items)
    }
  }

  internal fun onItemClick(bookId: Book.Id, item: BottomSheetItem) {
    viewModels.forEach {
      it.onItemClicked(bookId, item)
    }
  }
}
