package voice.features.bookOverview.listeningStats

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import voice.core.data.BookId
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.features.bookOverview.bottomSheet.BottomSheetItemViewModel
import voice.features.bookOverview.di.BookOverviewScope
import voice.navigation.Destination
import voice.navigation.Navigator

// The library long-press menu only routes to the stats page and does no data queries here.
@SingleIn(BookOverviewScope::class)
@ContributesIntoSet(BookOverviewScope::class)
class ListeningStatsBottomSheetItemViewModel(private val navigator: Navigator) : BottomSheetItemViewModel {

  // Every book can show local stats, so the entry does not depend on any book state.
  override suspend fun items(bookId: BookId): List<BottomSheetItem> {
    return listOf(BottomSheetItem.ListeningStats)
  }

  // The click only navigates to the per-book stats page; queries are left to that screen.
  override suspend fun onItemClick(
    bookId: BookId,
    item: BottomSheetItem,
  ) {
    if (item == BottomSheetItem.ListeningStats) {
      navigator.goTo(Destination.ListeningStats(bookId))
    }
  }
}
