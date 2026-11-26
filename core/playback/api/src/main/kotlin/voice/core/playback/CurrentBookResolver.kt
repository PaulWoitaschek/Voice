package voice.core.playback

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.repo.BookRepository

@SingleIn(AppScope::class)
@Inject
class CurrentBookResolver(
  private val bookRepository: BookRepository,
  private val playerController: PlayerController,
) {

  suspend fun book(bookId: BookId): Book? {
    val book = bookRepository.get(bookId) ?: return null
    return playerController.livePlaybackState(bookId)?.let(book::overlay) ?: book
  }
}
