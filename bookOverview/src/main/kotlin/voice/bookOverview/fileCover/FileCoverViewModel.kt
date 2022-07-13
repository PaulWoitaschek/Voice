package voice.bookOverview.fileCover

import com.squareup.anvil.annotations.ContributesMultibinding
import voice.bookOverview.bottomSheet.BottomSheetItem
import voice.bookOverview.bottomSheet.BottomSheetItemViewModel
import voice.bookOverview.di.BookOverviewScope
import voice.bookOverview.overview.BookOverviewNavigator
import voice.data.Book
import javax.inject.Inject

@BookOverviewScope
@ContributesMultibinding(BookOverviewScope::class)
class FileCoverViewModel
@Inject
constructor(
  private val navigator: BookOverviewNavigator
) : BottomSheetItemViewModel {

  override val menuOrder: Int
    get() = BottomSheetItem.FileCover.ordinal

  override suspend fun items(bookId: Book.Id): List<BottomSheetItem> {
    return listOf(BottomSheetItem.FileCover)
  }

  override suspend fun onItemClicked(bookId: Book.Id, item: BottomSheetItem): Boolean {
    if (item == BottomSheetItem.FileCover) {
      navigator.onFileCoverClick(bookId)
      return true
    }

    return false
  }
}
