package voice.core.sleeptimer

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.repo.BookmarkRepo
import voice.core.data.store.CurrentBookStore

@Inject
class CreateBookmarkAtCurrentPosition(
  private val bookmarkRepo: BookmarkRepo,
  private val bookRepository: BookRepository,
  @CurrentBookStore
  private val currentBookStore: DataStore<BookId?>,
) {

  suspend fun create() {
    val currentBookId = currentBookStore.data.first() ?: return
    val currentBook = bookRepository.get(currentBookId) ?: return
    bookmarkRepo.addBookmarkAtBookPosition(
      book = currentBook,
      title = null,
      setBySleepTimer = true,
    )
  }
}
