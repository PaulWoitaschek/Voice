package voice.bookOverview.deleteBook

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import com.squareup.anvil.annotations.ContributesMultibinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.app.scanner.MediaScanTrigger
import voice.bookOverview.bottomSheet.BottomSheetItem
import voice.bookOverview.bottomSheet.BottomSheetItemViewModel
import voice.bookOverview.di.BookOverviewScope
import voice.data.Book
import javax.inject.Inject

@BookOverviewScope
@ContributesMultibinding(
  scope = BookOverviewScope::class,
  boundType = BottomSheetItemViewModel::class
)
class DeleteBookViewModel
@Inject
constructor(
  private val application: Application,
  private val mediaScanTrigger: MediaScanTrigger,
) : ViewModel(), BottomSheetItemViewModel {

  private val scope = MainScope()

  private val _state = mutableStateOf<DeleteBookViewState?>(null)
  internal val state: State<DeleteBookViewState?> get() = _state

  override suspend fun items(bookId: Book.Id): List<BottomSheetItem> {
    return listOf(BottomSheetItem.DeleteBook)
  }

  override suspend fun onItemClicked(bookId: Book.Id, item: BottomSheetItem) {
    if (item != BottomSheetItem.DeleteBook) return
    _state.value = DeleteBookViewState(
      id = bookId,
      deleteCheckBoxChecked = false,
    )
  }

  internal fun onDismiss() {
    _state.value = null
  }

  internal fun onDeleteCheckBoxChecked(checked: Boolean) {
    _state.value = _state.value?.copy(deleteCheckBoxChecked = checked)
  }

  internal fun onConfirmDeletion() {
    val state = _state.value
    if (state != null) {
      check(state.confirmButtonEnabled)
      scope.launch {
        val uri = state.id.toUri()
        val documentFile = DocumentFile.fromSingleUri(application, uri)
        scope.launch {
          documentFile?.delete()
          mediaScanTrigger.scan(restartIfScanning = true)
        }
      }
    }
    _state.value = null
  }
}

data class DeleteBookViewState(
  val id: Book.Id,
  val deleteCheckBoxChecked: Boolean,
) {

  val confirmButtonEnabled = deleteCheckBoxChecked
}
