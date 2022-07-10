package voice.bookOverview.editTitle

import voice.data.Book

internal data class EditBookTitleState(
  val title: String,
  val bookId: Book.Id
)
