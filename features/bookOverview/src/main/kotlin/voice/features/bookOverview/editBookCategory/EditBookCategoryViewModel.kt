package voice.features.bookOverview.editBookCategory

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.features.bookOverview.bottomSheet.BottomSheetItemViewModel
import voice.features.bookOverview.di.BookOverviewScope
import voice.features.bookOverview.overview.BookOverviewCategory
import voice.features.bookOverview.overview.category
import java.time.Instant

@BookOverviewScope
@ContributesIntoSet(BookOverviewScope::class)
@Inject
class EditBookCategoryViewModel(private val repo: BookRepository) : BottomSheetItemViewModel {

  override suspend fun items(bookId: BookId): List<BottomSheetItem> {
    val book = repo.get(bookId) ?: return emptyList()
    return when (book.category) {
      BookOverviewCategory.CURRENT -> listOf(
        BottomSheetItem.BookCategoryMarkAsNotStarted,
        BottomSheetItem.BookCategoryMarkAsCompleted,
      )
      BookOverviewCategory.NOT_STARTED -> listOf(
        BottomSheetItem.BookCategoryMarkAsCurrent,
        BottomSheetItem.BookCategoryMarkAsCompleted,
      )
      BookOverviewCategory.FINISHED -> listOf(
        BottomSheetItem.BookCategoryMarkAsCurrent,
        BottomSheetItem.BookCategoryMarkAsNotStarted,
      )
    }
  }

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
        lastPlayedAt = Instant.now(),
      )
    }
  }
}
