package voice.bookOverview.bottomSheet

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
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
}
