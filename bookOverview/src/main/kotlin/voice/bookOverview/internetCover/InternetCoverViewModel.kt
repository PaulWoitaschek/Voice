package voice.bookOverview.internetCover

import com.squareup.anvil.annotations.ContributesMultibinding
import voice.bookOverview.bottomSheet.BottomSheetItem
import voice.bookOverview.bottomSheet.BottomSheetItemViewModel
import voice.bookOverview.di.BookOverviewScope
import voice.bookOverview.overview.BookOverviewNavigator
import voice.data.Book
import javax.inject.Inject

@BookOverviewScope
@ContributesMultibinding(BookOverviewScope::class)
class InternetCoverViewModel
@Inject
constructor(
  private val navigator: BookOverviewNavigator
) : BottomSheetItemViewModel {

  override suspend fun items(bookId: Book.Id): List<BottomSheetItem> {
    return listOf(BottomSheetItem.InternetCover)
  }

  override fun onItemClicked(bookId: Book.Id, item: BottomSheetItem) {
    if (item == BottomSheetItem.InternetCover) {
      navigator.onCoverFromInternetClick(bookId)
    }
  }
}
