package voice.features.bookOverview.bottomSheet

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import voice.core.ui.icons.VoiceIcons
import voice.features.bookOverview.overview.BookOverviewCategory
import voice.features.bookOverview.overview.BookOverviewItemViewState
import voice.core.strings.R as StringsR

internal data class EditBookBottomSheetState(
  val book: BookOverviewItemViewState?,
  val category: BookOverviewCategory?,
  val items: List<BottomSheetItem>,
  /** Where the book comes from, set once books can come from a server as well. */
  val source: BookSource? = null,
) {

  companion object {
    val Empty = EditBookBottomSheetState(book = null, category = null, items = emptyList())
  }
}

internal sealed interface BookSource {
  data object Device : BookSource

  data class Server(
    val name: String,
    val downloaded: Boolean,
  ) : BookSource
}

enum class BottomSheetItem(
  @StringRes val titleRes: Int,
  val icon: ImageVector,
) {
  Title(StringsR.string.book_edit_rename, VoiceIcons.Title),
  InternetCover(StringsR.string.book_edit_cover_internet, VoiceIcons.ImageSearch),
  FileCover(StringsR.string.book_edit_cover_file, VoiceIcons.Image),
  Download(StringsR.string.book_download_action, VoiceIcons.Download),
  RemoveDownload(StringsR.string.book_download_remove, VoiceIcons.Delete),
  DeleteBook(StringsR.string.book_delete_bottom_sheet_title, VoiceIcons.Delete),
  BookCategoryMarkAsNotStarted(StringsR.string.book_category_action_mark_not_started, VoiceIcons.HourglassEmpty),
  BookCategoryMarkAsCurrent(StringsR.string.book_category_action_mark_current, VoiceIcons.NotStarted),
  BookCategoryMarkAsCompleted(StringsR.string.book_category_action_mark_completed, VoiceIcons.Done),
}
