package voice.features.bookOverview.hideBook

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import voice.core.data.BookId
import voice.core.data.repo.BookRepository

@SingleIn(AppScope::class)
@Inject
class BookHider(private val repo: BookRepository) {

  // no replay, so only a library that's on screen while the book leaves it offers the undo
  internal val hidden: SharedFlow<HiddenBook>
    field = MutableSharedFlow<HiddenBook>(extraBufferCapacity = 1)

  internal suspend fun hide(id: BookId) {
    val book = repo.get(id) ?: return
    repo.updateCustomMetadata(id) { it.copy(hidden = true) }
    hidden.emit(HiddenBook(id, book.name))
  }

  internal suspend fun show(id: BookId) {
    repo.updateCustomMetadata(id) { it.copy(hidden = false) }
  }
}

internal data class HiddenBook(
  val id: BookId,
  val name: String,
)
