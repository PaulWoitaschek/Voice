package voice.bookOverview.bottomSheet

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import voice.bookOverview.di.BookOverviewScope
import voice.common.BookId
import javax.inject.Inject

@BookOverviewScope
class BottomSheetViewModel
@Inject constructor(
  private val viewModels: Set<@JvmSuppressWildcards BottomSheetItemViewModel>
) : ViewModel() {

  private val _state: MutableState<EditBookBottomSheetState> = mutableStateOf(EditBookBottomSheetState(emptyList()))
  internal val state: State<EditBookBottomSheetState> get() = _state
  private var bookId: BookId? = null

  internal fun bookSelected(bookId: BookId) {
    this.bookId = bookId
    viewModelScope.launch {
      val items = viewModels.flatMap { it.items(bookId) }
        .toSet()
        .sorted()
      _state.value = EditBookBottomSheetState(items)
    }
  }

  internal fun onItemClick(item: BottomSheetItem) {
    val bookId = bookId ?: return
    viewModelScope.launch {
      viewModels.forEach {
        it.onItemClicked(bookId, item)
      }
    }
  }
}
