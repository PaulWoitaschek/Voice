package voice.bookOverview.internetCover

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import voice.bookOverview.bottomSheet.BottomSheetItem
import voice.bookOverview.bottomSheet.BottomSheetItemViewModel
import voice.bookOverview.di.BookOverviewScope
import voice.common.BookId
import voice.common.navigation.Destination
import voice.common.navigation.Navigator

@BookOverviewScope
@ContributesIntoSet(BookOverviewScope::class)
@Inject
class InternetCoverViewModel(private val navigator: Navigator) : BottomSheetItemViewModel {

  override suspend fun items(bookId: BookId): List<BottomSheetItem> {
    return listOf(BottomSheetItem.InternetCover)
  }

  override suspend fun onItemClick(
    bookId: BookId,
    item: BottomSheetItem,
  ) {
    if (item == BottomSheetItem.InternetCover) {
      navigator.goTo(Destination.CoverFromInternet(bookId))
    }
  }
}
