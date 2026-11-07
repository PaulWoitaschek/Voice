package voice.features.bookOverview.editBookCategory

import dev.zacsweers.metro.Inject
import java.time.Instant
import kotlin.time.Duration
import kotlinx.coroutines.flow.first
import voice.core.data.BookCategory
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.playback.LastPlaybackSpeedStore
import voice.core.playback.PlayerController
import voice.core.playback.withDefaultSpeed

@Inject
class BookCategoryChanger(
  private val repo: BookRepository,
  private val playerController: PlayerController,
  private val currentBookStore: CurrentBookStore,
  private val lastPlaybackSpeedStore: LastPlaybackSpeedStore,
) {

  internal suspend fun change(
    bookId: BookId,
    category: BookCategory,
  ) {
    val book = repo.get(bookId) ?: return

    val (currentChapter, positionInChapter) =
      when (category) {
        BookCategory.CURRENT -> book.chapters.first().id to 1L
        BookCategory.NOT_STARTED -> book.chapters.first().id to 0L
        BookCategory.FINISHED -> {
          val lastChapter = book.chapters.last()
          lastChapter.id to lastChapter.duration
        }
      }

    val speed = book.withDefaultSpeed { lastPlaybackSpeedStore.data.first() }.content.playbackSpeed
    repo.updateBook(book.id) {
      it.copy(
        currentChapter = currentChapter,
        positionInChapter = positionInChapter,
        // newer than the place other devices wrote, so their place doesn't undo this
        lastPlayedAt = Instant.now(),
        playbackSpeed = speed,
      )
    }
    // The player keeps the position of the book it has loaded and would write it back. It seeks in
    // its current book,
    // so that has to be this one too.
    if (
      currentBookStore.data.first() == book.id &&
        playerController.livePlaybackState(book.id) != null
    ) {
      playerController.setPosition(positionInChapter, currentChapter)
      if (category == BookCategory.NOT_STARTED) {
        // playing on would start the book right away
        playerController.pauseWithRewind(Duration.ZERO)
      }
    }
  }
}
