package voice.bookOverview.fileCover

import com.squareup.anvil.annotations.ContributesMultibinding
import voice.bookOverview.bottomSheet.BottomSheetItem
import voice.bookOverview.bottomSheet.BottomSheetItemViewModel
import voice.bookOverview.di.BookOverviewScope
import voice.common.BookId
import voice.common.navigation.Navigator
import voice.common.navigation.Screen
import javax.inject.Inject

@BookOverviewScope
@ContributesMultibinding(BookOverviewScope::class)
class FileCoverViewModel
@Inject
constructor(
  private val navigator: Navigator
) : BottomSheetItemViewModel {

  override suspend fun items(bookId: BookId): List<BottomSheetItem> {
    return listOf(BottomSheetItem.FileCover)
  }

  override suspend fun onItemClicked(bookId: BookId, item: BottomSheetItem) {
    if (item == BottomSheetItem.FileCover) {
      navigator.toScreen(Screen.CoverFromFiles(bookId))
    }
  }
}
