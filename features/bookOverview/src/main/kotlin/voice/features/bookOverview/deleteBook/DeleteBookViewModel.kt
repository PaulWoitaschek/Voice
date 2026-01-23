package voice.features.bookOverview.deleteBook

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.documentfile.provider.DocumentFile
import dev.zacsweers.metro.ContributesIntoSet
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.core.data.BookId
import voice.core.logging.api.Logger
import voice.core.scanner.MediaScanTrigger
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.features.bookOverview.bottomSheet.BottomSheetItemViewModel
import voice.features.bookOverview.di.BookOverviewScope

@BookOverviewScope
@ContributesIntoSet(BookOverviewScope::class)
class DeleteBookViewModel(
  private val application: Application,
  private val mediaScanTrigger: MediaScanTrigger,
) : BottomSheetItemViewModel {

  private val scope = MainScope()

  private val _state = mutableStateOf<DeleteBookViewState?>(null)
  internal val state: State<DeleteBookViewState?> get() = _state

  override suspend fun items(bookId: BookId): List<BottomSheetItem> {
    return listOf(BottomSheetItem.DeleteBook)
  }

  override suspend fun onItemClick(
    bookId: BookId,
    item: BottomSheetItem,
  ) {
    if (item != BottomSheetItem.DeleteBook) return

    _state.value = DeleteBookViewState(
      id = bookId,
      deleteCheckBoxChecked = false,
      fileToDelete = bookId.toUri().pathSegments
        .let { segments ->
          val result = segments.lastOrNull()?.removePrefix("primary:")
          if (result.isNullOrEmpty()) {
            Logger.w("Could not determine path for $segments")
            segments.joinToString(separator = "\"")
          } else {
            result
          }
        },
    )
  }

  internal fun onDismiss() {
    _state.value = null
  }

  internal fun onDeleteCheckBoxCheck(checked: Boolean) {
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
  val id: BookId,
  val deleteCheckBoxChecked: Boolean,
  val fileToDelete: String,
) {

  val confirmButtonEnabled = deleteCheckBoxChecked
}
