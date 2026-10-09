package voice.features.bookOverview.bottomSheet

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import voice.core.ui.icons.VoiceIcons
import voice.features.bookOverview.overview.BookOverviewCategory
import voice.features.bookOverview.overview.BookOverviewItemViewState
import voice.core.strings.R as StringsR

internal data class EditBookBottomSheetState(
  val book: BookOverviewItemViewState,
  val category: BookOverviewCategory,
  val items: List<BottomSheetItem>,
)

enum class BottomSheetItem(
  @StringRes val titleRes: Int,
  val icon: ImageVector,
) {
  Title(StringsR.string.book_edit_rename, VoiceIcons.Title),
  InternetCover(StringsR.string.book_edit_cover_internet, VoiceIcons.ImageSearch),
  FileCover(StringsR.string.book_edit_cover_file, VoiceIcons.Image),
  DeleteBook(StringsR.string.book_delete_bottom_sheet_title, VoiceIcons.Delete),
  BookCategoryMarkAsNotStarted(StringsR.string.book_category_action_mark_not_started, VoiceIcons.HourglassEmpty),
  BookCategoryMarkAsCurrent(StringsR.string.book_category_action_mark_current, VoiceIcons.NotStarted),
  BookCategoryMarkAsCompleted(StringsR.string.book_category_action_mark_completed, VoiceIcons.Done),
}

internal fun BookOverviewCategory.markAsItem(): BottomSheetItem = when (this) {
  BookOverviewCategory.NOT_STARTED -> BottomSheetItem.BookCategoryMarkAsNotStarted
  BookOverviewCategory.CURRENT -> BottomSheetItem.BookCategoryMarkAsCurrent
  BookOverviewCategory.FINISHED -> BottomSheetItem.BookCategoryMarkAsCompleted
}
