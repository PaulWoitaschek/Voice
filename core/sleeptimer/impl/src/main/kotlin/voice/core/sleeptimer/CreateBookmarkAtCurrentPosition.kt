package voice.core.sleeptimer

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import voice.core.data.BookId
import voice.core.data.repo.BookmarkRepo
import voice.core.data.store.CurrentBookStore
import voice.core.playback.CurrentBookResolver

@Inject
class CreateBookmarkAtCurrentPosition(
  private val bookmarkRepo: BookmarkRepo,
  private val currentBookResolver: CurrentBookResolver,
  @CurrentBookStore
  private val currentBookStore: DataStore<BookId?>,
) {

  suspend fun create() {
    val currentBookId = currentBookStore.data.first() ?: return
    val currentBook = currentBookResolver.book(currentBookId) ?: return
    bookmarkRepo.addBookmarkAtBookPosition(
      book = currentBook,
      title = null,
      setBySleepTimer = true,
    )
  }
}
