package voice.features.bookmark

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import voice.core.data.BookId
import voice.core.data.Bookmark
import voice.core.data.repo.BookRepository
import voice.core.data.repo.BookmarkRepo
import kotlin.time.Duration.Companion.minutes

@SingleIn(AppScope::class)
@Inject
class AutoSaveBookmarkController(
  private val bookRepository: BookRepository,
  private val bookmarkRepo: BookmarkRepo,
) {

  private val scope = MainScope()
  private var job: Job? = null

  private val _activeBookId = MutableStateFlow<BookId?>(null)
  val activeBookId: StateFlow<BookId?> = _activeBookId

  private val _added = MutableSharedFlow<Bookmark>(extraBufferCapacity = 8)
  val added: SharedFlow<Bookmark> = _added

  fun toggle(bookId: BookId) {
    if (_activeBookId.value == bookId) {
      job?.cancel()
      job = null
      _activeBookId.value = null
      return
    }
    job?.cancel()
    _activeBookId.value = bookId
    job = scope.launch {
      while (isActive) {
        val book = bookRepository.get(bookId) ?: break
        val bookmark = bookmarkRepo.addBookmarkAtBookPosition(
          book = book,
          title = "auto save",
          setBySleepTimer = false,
        )
        _added.emit(bookmark)
        delay(5.minutes)
      }
    }
  }
}
