package voice.features.bookOverview.internetCover

import dev.zacsweers.metro.ContributesIntoSet
import voice.core.data.BookId
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.features.bookOverview.bottomSheet.BottomSheetItemViewModel
import voice.features.bookOverview.di.BookOverviewScope
import voice.navigation.Destination
import voice.navigation.Navigator

@BookOverviewScope
@ContributesIntoSet(BookOverviewScope::class)
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
