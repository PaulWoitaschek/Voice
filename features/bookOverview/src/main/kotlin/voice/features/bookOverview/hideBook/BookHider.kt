package voice.features.bookOverview.hideBook

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.features.bookOverview.overview.inSeries
import voice.features.bookOverview.overview.series

@SingleIn(AppScope::class)
@Inject
class BookHider(private val repo: BookRepository) {

  // no replay, so only a library that's on screen while the books leave it offers the undo
  internal val hidden: SharedFlow<HiddenBooks>
    field = MutableSharedFlow<HiddenBooks>(extraBufferCapacity = 1)

  internal suspend fun hide(id: BookId) {
    val book = repo.get(id) ?: return
    repo.updateCustomMetadata(id) { it.copy(hidden = true) }
    hidden.emit(HiddenBooks(listOf(id), book.name, series = false))
  }

  internal suspend fun hideSeries(name: String) {
    val series = repo.all().inSeries(name).series().singleOrNull() ?: return
    // the undo leaves the books that were hidden before hidden
    val ids = series.books.filterNot { it.custom.hidden }.map { it.id }
    if (ids.isEmpty()) return
    ids.forEach { id -> repo.updateCustomMetadata(id) { it.copy(hidden = true) } }
    hidden.emit(HiddenBooks(ids, series.name, series = true))
  }

  internal suspend fun showSeries(name: String) {
    show(repo.all().inSeries(name).map { it.id })
  }

  internal suspend fun show(ids: List<BookId>) {
    ids.forEach { id -> repo.updateCustomMetadata(id) { it.copy(hidden = false) } }
  }
}

internal data class HiddenBooks(
  val ids: List<BookId>,
  val name: String,
  val series: Boolean,
)
