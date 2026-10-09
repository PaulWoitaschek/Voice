package voice.features.bookOverview.download

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first
import voice.core.audiobookshelf.download.AudiobookshelfDownloads
import voice.core.audiobookshelf.download.BookDownloadState
import voice.core.data.BookId
import voice.core.data.isRemote
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.features.bookOverview.bottomSheet.BottomSheetItemViewModel
import voice.features.bookOverview.di.BookOverviewScope

@SingleIn(BookOverviewScope::class)
@ContributesIntoSet(BookOverviewScope::class)
class DownloadBookViewModel(private val downloads: AudiobookshelfDownloads) : BottomSheetItemViewModel {

  override suspend fun items(bookId: BookId): List<BottomSheetItem> {
    if (!bookId.isRemote) return emptyList()
    return when (downloads.state(bookId).first()) {
      BookDownloadState.NotDownloaded, BookDownloadState.Failed -> listOf(BottomSheetItem.Download)
      is BookDownloadState.Downloading, BookDownloadState.Downloaded -> listOf(BottomSheetItem.RemoveDownload)
    }
  }

  override suspend fun onItemClick(
    bookId: BookId,
    item: BottomSheetItem,
  ) {
    when (item) {
      BottomSheetItem.Download -> downloads.download(bookId)
      BottomSheetItem.RemoveDownload -> downloads.remove(bookId)
      else -> Unit
    }
  }
}
