package voice.features.bookOverview.editBookCategory

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.playback.PlayerController
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.features.bookOverview.bottomSheet.BottomSheetItemViewModel
import voice.features.bookOverview.di.BookActionsScope
import voice.features.bookOverview.overview.BookOverviewCategory
import voice.features.bookOverview.overview.category
import kotlin.time.Duration

@SingleIn(BookActionsScope::class)
@ContributesIntoSet(BookActionsScope::class)
class EditBookCategoryViewModel(
  private val repo: BookRepository,
  private val playerController: PlayerController,
  @CurrentBookStore
  private val currentBookStore: DataStore<BookId?>,
) : BottomSheetItemViewModel {

  // the menu shows every status in its status picker, so there are no items to list
  override suspend fun items(bookId: BookId): List<BottomSheetItem> = emptyList()

  override suspend fun onItemClick(
    bookId: BookId,
    item: BottomSheetItem,
  ) {
    val book = repo.get(bookId) ?: return

    val (currentChapter, positionInChapter) = when (item) {
      BottomSheetItem.BookCategoryMarkAsCurrent -> {
        book.chapters.first().id to 1L
      }
      BottomSheetItem.BookCategoryMarkAsNotStarted -> {
        book.chapters.first().id to 0L
      }
      BottomSheetItem.BookCategoryMarkAsCompleted -> {
        val lastChapter = book.chapters.last()
        lastChapter.id to lastChapter.duration
      }
      else -> return
    }

    repo.updateBook(book.id) {
      it.copy(
        currentChapter = currentChapter,
        positionInChapter = positionInChapter,
      )
    }
    // The player keeps the position of the book it has loaded and would write it back. It seeks in its current book,
    // so that has to be this one too.
    if (currentBookStore.data.first() == book.id && playerController.livePlaybackState(book.id) != null) {
      playerController.setPosition(positionInChapter, currentChapter)
      if (item == BottomSheetItem.BookCategoryMarkAsNotStarted) {
        // playing on would start the book right away
        playerController.pauseWithRewind(Duration.ZERO)
      }
    }
  }
}
