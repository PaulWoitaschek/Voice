package voice.features.bookOverview.editBook

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.core.audiobookshelf.ServerBookEdit
import voice.core.audiobookshelf.ServerEdit
import voice.core.audiobookshelf.ServerLibrary
import voice.core.common.comparator.NaturalOrderComparator
import voice.core.data.BookId
import voice.core.data.isRemote
import voice.core.data.repo.BookRepository
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.features.bookOverview.bottomSheet.BottomSheetItemViewModel
import voice.features.bookOverview.di.BookActionsScope

@SingleIn(BookActionsScope::class)
@ContributesIntoSet(BookActionsScope::class)
class EditBookViewModel(
  private val repo: BookRepository,
  private val editSeriesFeatureFlag: EditSeriesFeatureFlag,
  private val serverLibrary: ServerLibrary,
) : BottomSheetItemViewModel {

  private val scope = MainScope()

  private val _state = mutableStateOf<EditBookState?>(null)
  internal val state: State<EditBookState?> get() = _state

  override suspend fun items(bookId: BookId): List<BottomSheetItem> {
    // edits of server books are saved on the server, which is new and waits for the flag
    if (bookId.isRemote && !editSeriesFeatureFlag.get()) return emptyList()
    return listOf(BottomSheetItem.Edit)
  }

  override suspend fun onItemClick(
    bookId: BookId,
    item: BottomSheetItem,
  ) {
    if (item != BottomSheetItem.Edit) return
    val book = repo.get(bookId) ?: return
    val showSeries = editSeriesFeatureFlag.get()
    _state.value = EditBookState(
      title = book.name,
      author = book.author.orEmpty(),
      bookId = bookId,
      cover = book.coverUrl,
      series = book.series.orEmpty(),
      part = book.part.orEmpty(),
      showSeries = showSeries,
      remote = bookId.isRemote,
      allSeries = if (showSeries) seriesOfOtherBooks(bookId) else emptyList(),
    )
  }

  private suspend fun seriesOfOtherBooks(bookId: BookId): List<SeriesSuggestion> {
    return repo.all()
      .filter { it.id != bookId }
      .mapNotNull { book -> book.series?.trim()?.takeIf { it.isNotEmpty() } }
      .groupBy { it.lowercase() }
      .map { (_, names) ->
        SeriesSuggestion(
          name = names.groupingBy { it }.eachCount().maxBy { it.value }.key,
          bookCount = names.size,
        )
      }
      .sortedWith(compareBy(NaturalOrderComparator.stringComparator) { it.name.lowercase() })
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

  internal fun onSeriesChange(series: String) {
    _state.value = _state.value?.copy(series = series)
  }

  internal fun onPartChange(part: String) {
    _state.value = _state.value?.copy(part = part)
  }

  internal fun onConfirm() {
    val state = _state.value ?: return
    check(state.confirmButtonEnabled)
    if (state.remote) {
      saveOnServer(state)
      return
    }
    scope.launch {
      // only what the listener changed, so the other name keeps following the files
      repo.updateCustomMetadata(state.bookId) { custom ->
        var edited = custom
        if (state.title != state.initialTitle) edited = edited.copy(name = state.title.trim())
        if (state.author != state.initialAuthor) edited = edited.copy(author = state.author.trim().ifEmpty { null })
        if (state.series != state.initialSeries) edited = edited.copy(series = state.series.trim().ifEmpty { null })
        if (state.part != state.initialPart) edited = edited.copy(part = state.part.trim().ifEmpty { null })
        edited
      }
    }
    _state.value = null
  }

  private fun saveOnServer(state: EditBookState) {
    if (state.saving) return
    val seriesChanged = state.series != state.initialSeries || state.part != state.initialPart
    // only what the listener changed, so the rest stays as the server has it
    val edit = ServerBookEdit(
      title = state.title.takeIf { it != state.initialTitle },
      author = state.author.takeIf { it != state.initialAuthor },
      series = state.series.takeIf { seriesChanged },
      part = state.part.takeIf { seriesChanged },
    )
    if (edit == ServerBookEdit()) {
      _state.value = state.copy(serverResult = ServerEdit.Saved)
      return
    }
    _state.value = state.copy(saving = true, serverResult = null)
    scope.launch {
      val result = serverLibrary.updateBook(state.bookId, edit)
      _state.value = _state.value?.copy(saving = false, serverResult = result)
    }
  }
}
