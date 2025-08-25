package voice.features.bookOverview.editTitle

import voice.core.data.BookId

internal data class EditBookTitleState(
  val title: String,
  val bookId: BookId,
) {

  val confirmButtonEnabled: Boolean = title.trim().isNotEmpty()
}
