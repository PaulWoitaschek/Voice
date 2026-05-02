package voice.core.playback

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.repo.BookmarkRepo
import voice.core.data.store.CurrentBookStore

@SingleIn(AppScope::class)
@Inject
class AudiologRecorder(
  @CurrentBookStore
  private val currentBookStore: DataStore<BookId?>,
  private val bookRepository: BookRepository,
  private val bookmarkRepo: BookmarkRepo,
) {

  var pausedDueToSleeping: Boolean = false

  suspend fun record(reason: String) {
    val bookId = currentBookStore.data.first() ?: return
    val book = bookRepository.get(bookId) ?: return
    bookmarkRepo.addBookmarkAtBookPosition(
      book = book,
      title = reason,
      setBySleepTimer = false,
      setByAudiolog = true,
    )
  }
}
