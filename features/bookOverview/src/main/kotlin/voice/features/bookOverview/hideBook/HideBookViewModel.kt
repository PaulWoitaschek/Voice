package voice.features.bookOverview.hideBook

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.features.bookOverview.bottomSheet.BottomSheetItemViewModel
import voice.features.bookOverview.di.BookActionsScope

@SingleIn(BookActionsScope::class)
@ContributesIntoSet(BookActionsScope::class)
class HideBookViewModel(
  private val repo: BookRepository,
  private val bookHider: BookHider,
) : BottomSheetItemViewModel {

  override suspend fun items(bookId: BookId): List<BottomSheetItem> {
    val book = repo.get(bookId) ?: return emptyList()
    return listOf(if (book.custom.hidden) BottomSheetItem.Show else BottomSheetItem.Hide)
  }

  override suspend fun onItemClick(
    bookId: BookId,
    item: BottomSheetItem,
  ) {
    when (item) {
      BottomSheetItem.Hide -> bookHider.hide(bookId)
      BottomSheetItem.Show -> bookHider.show(bookId)
      else -> {}
    }
  }
}
