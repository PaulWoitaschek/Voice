package voice.bookOverview.editBookCategory

import androidx.lifecycle.ViewModel
import com.squareup.anvil.annotations.ContributesMultibinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.bookOverview.bottomSheet.BottomSheetItem
import voice.bookOverview.bottomSheet.BottomSheetItemViewModel
import voice.bookOverview.di.BookOverviewScope
import voice.bookOverview.overview.BookOverviewCategory
import voice.bookOverview.overview.category
import voice.data.Book
import voice.data.repo.BookRepository
import java.time.Instant
import javax.inject.Inject

@BookOverviewScope
@ContributesMultibinding(
  scope = BookOverviewScope::class,
  boundType = BottomSheetItemViewModel::class
)
class EditBookCategoryViewModel
@Inject
constructor(
  private val repo: BookRepository,
) : ViewModel(), BottomSheetItemViewModel {

  private val scope = MainScope()

  private val menuItems = listOf(
    BottomSheetItem.BookCategoryMarkAsCurrent,
    BottomSheetItem.BookCategoryMarkAsNotStarted,
    BottomSheetItem.BookCategoryMarkAsCompleted,
  )

  override suspend fun items(bookId: Book.Id): List<BottomSheetItem> {
    val book = repo.get(bookId) ?: return emptyList()
    val bookOverviewCategory = book.category

    return menuItems.filter { bottomSheetItem ->
      when (bottomSheetItem) {
        BottomSheetItem.BookCategoryMarkAsCurrent -> (bookOverviewCategory != BookOverviewCategory.CURRENT)
        BottomSheetItem.BookCategoryMarkAsNotStarted -> (bookOverviewCategory != BookOverviewCategory.NOT_STARTED)
        BottomSheetItem.BookCategoryMarkAsCompleted -> (bookOverviewCategory != BookOverviewCategory.FINISHED)
        else -> false
      }
    }
  }

  override fun onItemClicked(bookId: Book.Id, item: BottomSheetItem) {
    if (!menuItems.contains(item)) return

    scope.launch {
      val book = repo.get(bookId) ?: return@launch

      val (currentChapter, positionInChapter) = when (item) {
        BottomSheetItem.BookCategoryMarkAsCurrent -> {
          Pair(book.chapters.first().id, 1L)
        }
        BottomSheetItem.BookCategoryMarkAsNotStarted -> {
          Pair(book.chapters.first().id, 0L)
        }
        BottomSheetItem.BookCategoryMarkAsCompleted -> {
          val lastChapter = book.chapters.last()
          Pair(lastChapter.id, lastChapter.duration)
        }
        else -> return@launch
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
}
