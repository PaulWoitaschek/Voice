package voice.features.bookOverview.editBookCategory

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import voice.core.data.BookId
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.features.bookOverview.bottomSheet.BottomSheetItemViewModel
import voice.features.bookOverview.bottomSheet.category
import voice.features.bookOverview.di.BookActionsScope

@SingleIn(BookActionsScope::class)
@ContributesIntoSet(BookActionsScope::class)
class EditBookCategoryViewModel(private val bookCategoryChanger: BookCategoryChanger) :
  BottomSheetItemViewModel {

  // the menu shows every status in its status picker, so there are no items to list
  override suspend fun items(bookId: BookId): List<BottomSheetItem> = emptyList()

  override suspend fun onItemClick(
    bookId: BookId,
    item: BottomSheetItem,
  ) {
    val category = item.category ?: return
    bookCategoryChanger.change(bookId, category)
  }
}
