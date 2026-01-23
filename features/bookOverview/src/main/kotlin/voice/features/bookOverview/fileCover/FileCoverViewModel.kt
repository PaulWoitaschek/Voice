package voice.features.bookOverview.fileCover

import android.net.Uri
import dev.zacsweers.metro.ContributesIntoSet
import voice.core.data.BookId
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.features.bookOverview.bottomSheet.BottomSheetItemViewModel
import voice.features.bookOverview.di.BookOverviewScope
import voice.navigation.Destination
import voice.navigation.Navigator

@BookOverviewScope
@ContributesIntoSet(BookOverviewScope::class)
class FileCoverViewModel(private val navigator: Navigator) : BottomSheetItemViewModel {

  private var bookId: BookId? = null

  override suspend fun items(bookId: BookId): List<BottomSheetItem> {
    return listOf(BottomSheetItem.FileCover)
  }

  override suspend fun onItemClick(
    bookId: BookId,
    item: BottomSheetItem,
  ) {
    if (item == BottomSheetItem.FileCover) {
      this.bookId = bookId
    }
  }

  fun onImagePicked(uri: Uri) {
    val bookId = bookId ?: return
    navigator.goTo(Destination.EditCover(bookId, uri))
  }
}
