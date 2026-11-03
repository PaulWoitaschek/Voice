package voice.core.playback.session

import androidx.media3.common.MediaItem
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import dev.zacsweers.metro.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import voice.core.data.repo.BookRepository

// Android Auto keeps the lists it loaded until it's told they changed.
@Inject
class LibraryChangeNotifier(
  private val bookRepository: BookRepository,
  private val mediaItemProvider: MediaItemProvider,
) {

  suspend fun notifyChanges(session: MediaLibrarySession) {
    var notified: Map<String, List<MediaItem>>? = null
    bookRepository.flow().conflate().collect { books ->
      val lists = mediaItemProvider.libraryLists(books)
      notified?.let { notified ->
        lists
          .filter { (parentId, children) -> notified[parentId] != children }
          .forEach { (parentId, children) ->
            session.notifyChildrenChanged(parentId, children.size, null)
          }
      }
      notified = lists
      // While playing, the position is saved several times a second
      delay(10.seconds)
    }
  }
}
