package voice.bookOverview.editTitle

import voice.data.BookId

internal data class EditBookTitleState(
  val title: String,
  val bookId: BookId,
) {

  val confirmButtonEnabled: Boolean = title.trim().isNotEmpty()
}
