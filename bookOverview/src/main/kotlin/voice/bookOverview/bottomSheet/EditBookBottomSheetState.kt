package voice.bookOverview.bottomSheet

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
import voice.bookOverview.R

internal data class EditBookBottomSheetState(
  val items: List<BottomSheetItem>
)

enum class BottomSheetItem(
  @StringRes val titleRes: Int,
  val icon: ImageVector
) {
  Title(R.string.change_book_name, Icons.Outlined.Title),
  InternetCover(R.string.download_book_cover, Icons.Outlined.Download),
  FileCover(R.string.pick_book_cover, Icons.Outlined.Image),
  DeleteBook(R.string.delete_book_bottom_sheet_title, Icons.Outlined.Delete),
  BookCategoryMarkAsNotStarted(R.string.mark_as_not_started, Icons.Outlined.HourglassEmpty),
  BookCategoryMarkAsCurrent(R.string.mark_as_current, Icons.Outlined.NotStarted),
  BookCategoryMarkAsCompleted(R.string.mark_as_completed, Icons.Outlined.Done),
}
