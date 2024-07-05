package voice.app.features.bookmarks

import android.os.Bundle
import androidx.compose.runtime.Composable
import voice.app.features.bookmarks.dialogs.AddBookmarkDialog
import voice.app.features.bookmarks.dialogs.EditBookmarkDialog
import voice.app.injection.appComponent
import voice.common.BookId
import voice.common.compose.ComposeController
import voice.data.Bookmark
import voice.data.getBookId
import voice.data.putBookId

private const val NI_BOOK_ID = "ni#bookId"

@Suppress("unused")
class BookmarkController(args: Bundle) :
  ComposeController(args),
  AddBookmarkDialog.Callback,
  EditBookmarkDialog.Callback {

  constructor(bookId: BookId) : this(
    Bundle().apply {
      putBookId(NI_BOOK_ID, bookId)
    },
  )

  private val bookId = args.getBookId(NI_BOOK_ID)!!

  private val presenter = appComponent.bookmarkPresenterFactory.create(bookId)

  @Composable
  override fun Content() {
    val viewState = presenter.viewState()
    BookmarkScreen(
      viewState = viewState,
      onClose = { router.popController(this) },
      onAdd = {
        showAddBookmarkDialog()
      },
      onDelete = presenter::deleteBookmark,
      onEdit = { bookmark, title ->
        showEditBookmarkDialog(bookmark, title)
      },
      onScrollConfirmed = presenter::onScrollConfirmed,
      onClick = presenter::selectBookmark,
    )
  }

  override fun onEditBookmark(
    id: Bookmark.Id,
    title: String,
  ) {
    presenter.editBookmark(id, title)
  }

  override fun onBookmarkNameChosen(name: String) {
    presenter.addBookmark(name)
  }

  private fun showEditBookmarkDialog(
    bookmark: Bookmark.Id,
    title: String?,
  ) {
    EditBookmarkDialog(this, bookmark, title).showDialog(router)
  }

  private fun showAddBookmarkDialog() {
    AddBookmarkDialog(this).showDialog(router)
  }
}
