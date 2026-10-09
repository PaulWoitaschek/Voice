package voice.features.bookOverview.fileCover

import android.net.Uri
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
class FileCoverViewModel(private val navigator: Navigator) : BottomSheetItemViewModel {

  override suspend fun items(bookId: BookId): List<BottomSheetItem> {
    return listOf(BottomSheetItem.FileCover)
  }

  // the menu launches the picker itself, as only the UI can
  override suspend fun onItemClick(
    bookId: BookId,
    item: BottomSheetItem,
  ) {
  }

  fun onImagePicked(
    bookId: BookId,
    uri: Uri,
  ) {
    navigator.goTo(Destination.EditCover(bookId, uri))
  }
}
