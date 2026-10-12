package voice.core.playback

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.store.LastPlaybackSpeedStore
import java.time.Instant

/**
 * A book that was never played starts at the speed the listener last played at, so they don't have to set their
 * speed again for every book.
 */
@Inject
class NewBookSpeed(
  @LastPlaybackSpeedStore
  private val lastPlaybackSpeedStore: DataStore<Float>,
  private val bookRepository: BookRepository,
) {

  suspend fun applyTo(bookId: BookId) {
    val speed = lastPlaybackSpeedStore.data.first()
    bookRepository.updateBook(bookId) { content ->
      // a speed picked for this book before it was played stays
      if (content.lastPlayedAt == Instant.EPOCH && content.playbackSpeed == 1F) {
        content.copy(playbackSpeed = speed)
      } else {
        content
      }
    }
  }
}
