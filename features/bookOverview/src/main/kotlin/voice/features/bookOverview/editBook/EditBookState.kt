package voice.features.bookOverview.editBook

import voice.core.data.BookId

internal data class EditBookState(
  val title: String,
  val author: String,
  val bookId: BookId,
  val cover: String?,
  val initialTitle: String = title,
  val initialAuthor: String = author,
) {

  val confirmButtonEnabled: Boolean = title.trim().isNotEmpty()
}
