package voice.bookOverview.bottomSheet

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import voice.bookOverview.editTitle.EditBookTitleViewModel
import voice.bookOverview.overview.BookOverviewNavigator
import voice.data.Book
import javax.inject.Inject

class BottomSheetViewModel
@Inject constructor(
  private val editBookTitleViewModel: EditBookTitleViewModel,
  private val stateHolder: BottomSheetSelectionStateHolder,
  private val navigator: BookOverviewNavigator,
) {

  private val _state: MutableState<EditBookBottomSheetState> = mutableStateOf(EditBookBottomSheetState(emptyList()))
  internal val state: State<EditBookBottomSheetState> get() = _state

  internal fun bookSelected(book: Book.Id) {
    stateHolder.selectedBook = book
    _state.value = EditBookBottomSheetState(BottomSheetItem.values().toList())
  }

  internal fun onItemClick(item: BottomSheetItem) {
    when (item) {
      BottomSheetItem.Title -> {
        editBookTitleViewModel.onEditBookTitleClick()
      }
      BottomSheetItem.InternetCover -> {
        stateHolder.selectedBook?.let(navigator::onCoverFromInternetClick)
      }
      BottomSheetItem.FileCover -> {
        stateHolder.selectedBook?.let(navigator::onFileCoverClick)
      }
    }
  }
}
