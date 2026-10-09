package voice.features.bookOverview.internetCover

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import voice.core.data.BookId
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.features.bookOverview.bottomSheet.BottomSheetItemViewModel
import voice.features.bookOverview.di.BookActionsScope
import voice.navigation.Destination
import voice.navigation.Navigator

@SingleIn(BookActionsScope::class)
@ContributesIntoSet(BookActionsScope::class)
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
