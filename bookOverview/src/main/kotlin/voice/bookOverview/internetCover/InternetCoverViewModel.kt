package voice.bookOverview.internetCover

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import voice.bookOverview.bottomSheet.BottomSheetItem
import voice.bookOverview.bottomSheet.BottomSheetItemViewModel
import voice.bookOverview.di.BookOverviewScope
import voice.common.BookId
import voice.common.navigation.Destination
import voice.common.navigation.Navigator
import javax.inject.Inject

@BookOverviewScope
@ContributesBinding(BookOverviewScope::class)
class InternetCoverViewModel
@Inject
constructor(private val navigator: Navigator) : BottomSheetItemViewModel {

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
