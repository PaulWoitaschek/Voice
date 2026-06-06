package voice.features.bookOverview.bottomSheet

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.NotStarted
import androidx.compose.material.icons.outlined.Title
import androidx.compose.ui.graphics.vector.ImageVector
import voice.core.strings.R as StringsR

internal data class EditBookBottomSheetState(val items: List<BottomSheetItem>)

enum class BottomSheetItem(
  @StringRes val titleRes: Int,
  val icon: ImageVector,
) {
  Title(StringsR.string.book_edit_name_label, Icons.Outlined.Title),
  InternetCover(StringsR.string.book_edit_cover_internet, Icons.Outlined.Download),
  FileCover(StringsR.string.book_edit_cover_file, Icons.Outlined.Image),
  DeleteBook(StringsR.string.book_delete_bottom_sheet_title, Icons.Outlined.Delete),
  BookCategoryMarkAsNotStarted(StringsR.string.book_category_action_mark_not_started, Icons.Outlined.HourglassEmpty),
  BookCategoryMarkAsCurrent(StringsR.string.book_category_action_mark_current, Icons.Outlined.NotStarted),
  BookCategoryMarkAsCompleted(StringsR.string.book_category_action_mark_completed, Icons.Outlined.Done),
}
