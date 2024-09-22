package voice.bookmark

import android.os.Bundle
import androidx.compose.runtime.Composable
import voice.common.BookId
import voice.common.compose.ComposeController
import voice.data.getBookId
import voice.data.putBookId

private const val NI_BOOK_ID = "ni#bookId"

@Suppress("unused")
class BookmarkController(args: Bundle) : ComposeController(args) {

  constructor(bookId: BookId) : this(
    Bundle().apply {
      putBookId(NI_BOOK_ID, bookId)
    },
  )

  private val bookId = args.getBookId(NI_BOOK_ID)!!

  @Composable
  override fun Content() {
    BookmarkScreen(bookId)
  }
}
