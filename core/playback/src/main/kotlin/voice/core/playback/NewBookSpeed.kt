package voice.core.playback

import dev.zacsweers.metro.Inject
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.repo.BookContentRepo
import voice.core.data.repo.BookRepository
import java.time.Instant

/**
 * A book that was never played starts at the speed of the book played last, so listeners don't have to set their
 * speed again for every book.
 */
@Inject
class NewBookSpeed(
  private val contentRepo: BookContentRepo,
  private val bookRepository: BookRepository,
) {

  suspend fun applyTo(bookId: BookId) {
    // includes inactive books, so a finished book whose files were deleted still counts
    val lastPlayed = contentRepo.all()
      .filter { it.id != bookId && it.wasPlayed }
      .maxByOrNull { it.lastPlayedAt }
      ?: return
    bookRepository.updateBook(bookId) { content ->
      // a speed picked for this book before it was played stays
      if (!content.wasPlayed && content.playbackSpeed == 1F) {
        content.copy(playbackSpeed = lastPlayed.playbackSpeed)
      } else {
        content
      }
    }
  }
}

private val BookContent.wasPlayed: Boolean
  get() = lastPlayedAt > Instant.EPOCH
