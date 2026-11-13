package voice.features.bookOverview.bottomSheet

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import voice.core.audiobookshelf.download.BookDownloadState
import voice.core.data.BookCategory
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR
import voice.features.bookOverview.overview.BookOverviewItemViewState

internal data class EditBookBottomSheetState(
  val book: BookOverviewItemViewState,
  val category: BookCategory,
  val items: List<BottomSheetItem>,
  /** Where the book comes from, set once books can come from a server as well. */
  val source: BookSource? = null,
  /** How far the download of a server book got, null for a book on the device. */
  val download: BookDownloadState? = null,
  /** The size of the files of a server book, or 0 when the server didn't tell. */
  val bookSize: Long = 0,
  /** Whether a download asks for notifications first, so its progress shows. */
  val askForNotifications: Boolean = false,
)

internal sealed interface BookSource {
  data object Device : BookSource

  data class Server(
    val name: String,
    val download: Download,
  ) : BookSource

  enum class Download {
    None,
    Done,
  }
}

enum class BottomSheetItem(
  @StringRes val titleRes: Int,
  @DrawableRes val icon: Int,
) {
  Edit(StringsR.string.common_action_edit, UiR.drawable.ic_edit),
  InternetCover(StringsR.string.book_edit_cover_internet, UiR.drawable.ic_image_search),
  FileCover(StringsR.string.book_edit_cover_file, UiR.drawable.ic_image),
  Hide(StringsR.string.book_hide, UiR.drawable.ic_visibility_off),
  Show(StringsR.string.book_show, UiR.drawable.ic_visibility),
  Select(StringsR.string.book_select, UiR.drawable.ic_check_circle),
  Download(StringsR.string.book_download_action, UiR.drawable.ic_download),
  StopDownload(StringsR.string.book_download_stop, UiR.drawable.ic_close),
  RetryDownload(StringsR.string.book_download_retry, UiR.drawable.ic_replay),
  RemoveDownload(StringsR.string.book_download_remove, UiR.drawable.ic_delete),
  DeleteBook(StringsR.string.book_delete_bottom_sheet_title, UiR.drawable.ic_delete),
  BookCategoryMarkAsNotStarted(
    StringsR.string.book_category_action_mark_not_started,
    UiR.drawable.ic_hourglass_empty,
  ),
  BookCategoryMarkAsCurrent(
    StringsR.string.book_category_action_mark_current,
    UiR.drawable.ic_not_started,
  ),
  BookCategoryMarkAsCompleted(
    StringsR.string.book_category_action_mark_completed,
    UiR.drawable.ic_done,
  ),
}

internal fun BookCategory.markAsItem(): BottomSheetItem =
  when (this) {
    BookCategory.NOT_STARTED -> BottomSheetItem.BookCategoryMarkAsNotStarted
    BookCategory.CURRENT -> BottomSheetItem.BookCategoryMarkAsCurrent
    BookCategory.FINISHED -> BottomSheetItem.BookCategoryMarkAsCompleted
  }

internal val BottomSheetItem.category: BookCategory?
  get() =
    when (this) {
      BottomSheetItem.BookCategoryMarkAsNotStarted -> BookCategory.NOT_STARTED
      BottomSheetItem.BookCategoryMarkAsCurrent -> BookCategory.CURRENT
      BottomSheetItem.BookCategoryMarkAsCompleted -> BookCategory.FINISHED
      else -> null
    }
