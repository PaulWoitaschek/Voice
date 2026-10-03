package voice.features.bookOverview.editSeries

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.features.bookOverview.bottomSheet.BottomSheetItemViewModel
import voice.features.bookOverview.di.BookOverviewScope

@SingleIn(BookOverviewScope::class)
@ContributesIntoSet(BookOverviewScope::class)
class EditBookSeriesViewModel(private val repo: BookRepository) : BottomSheetItemViewModel {

  private val scope = MainScope()

  private val _state = mutableStateOf<EditBookSeriesState?>(null)
  internal val state: State<EditBookSeriesState?> get() = _state

  override suspend fun items(bookId: BookId): List<BottomSheetItem> {
    return listOf(BottomSheetItem.BookSeries)
  }

  override suspend fun onItemClick(
    bookId: BookId,
    item: BottomSheetItem,
  ) {
    if (item != BottomSheetItem.BookSeries) return
    val book = repo.get(bookId) ?: return
    
    // Find all series for this author
    val author = book.content.author
    val suggestedSeries = if (author != null) {
        repo.all()
            .filter { it.content.author == author && !it.content.series.isNullOrBlank() }
            .mapNotNull { it.content.series }
            .distinct()
            .sorted()
    } else {
        emptyList()
    }

    _state.value = EditBookSeriesState(
      bookId = bookId,
      author = author,
      currentSeries = book.content.series.orEmpty(),
      currentPart = book.content.part.orEmpty(),
      suggestedSeries = suggestedSeries,
    )
  }

  internal fun onDismiss() {
    _state.value = null
  }

  internal fun onUpdateSeries(series: String) {
    _state.value = _state.value?.copy(currentSeries = series)
  }

  internal fun onUpdatePart(part: String) {
    _state.value = _state.value?.copy(currentPart = part)
  }

  internal fun onConfirm() {
    val state = _state.value
    if (state != null) {
      scope.launch {
        repo.updateBook(state.bookId) {
          val seriesToSave = state.currentSeries.trim().takeIf { it.isNotEmpty() }
          val partToSave = state.currentPart.trim().takeIf { it.isNotEmpty() }
          it.copy(
            series = seriesToSave,
            part = partToSave,
          )
        }
      }
    }
    _state.value = null
  }
}
