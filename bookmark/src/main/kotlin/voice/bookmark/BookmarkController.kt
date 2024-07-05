package voice.bookmark

import android.os.Bundle
import androidx.compose.runtime.Composable
import com.squareup.anvil.annotations.ContributesTo
import voice.bookmark.dialogs.EditBookmarkDialog
import voice.common.AppScope
import voice.common.BookId
import voice.common.compose.ComposeController
import voice.common.rootComponentAs
import voice.data.Bookmark
import voice.data.getBookId
import voice.data.putBookId

private const val NI_BOOK_ID = "ni#bookId"

@Suppress("unused")
class BookmarkController(args: Bundle) :
  ComposeController(args),
  EditBookmarkDialog.Callback {

  constructor(bookId: BookId) : this(
    Bundle().apply {
      putBookId(NI_BOOK_ID, bookId)
    },
  )

  private val bookId = args.getBookId(NI_BOOK_ID)!!

  private val presenter = rootComponentAs<Component>()
    .bookmarkPresenterFactory
    .create(bookId)

  @Composable
  override fun Content() {
    val viewState = presenter.viewState()
    BookmarkScreen(
      viewState = viewState,
      onClose = { router.popController(this) },
      onAdd = {
        presenter.onAddClicked()
      },
      onDelete = presenter::deleteBookmark,
      onEdit = { bookmark, title ->
        showEditBookmarkDialog(bookmark, title)
      },
      onScrollConfirmed = presenter::onScrollConfirmed,
      onClick = presenter::selectBookmark,
      onNewBookmarkNameChosen = {
        presenter.addBookmark(it)
      },
      onCloseDialog = {
        presenter.closeDialog()
      },
    )
  }

  override fun onEditBookmark(
    id: Bookmark.Id,
    title: String,
  ) {
    presenter.editBookmark(id, title)
  }

  private fun showEditBookmarkDialog(
    bookmark: Bookmark.Id,
    title: String?,
  ) {
    EditBookmarkDialog(this, bookmark, title).showDialog(router)
  }

  @ContributesTo(AppScope::class)
  interface Component {
    val bookmarkPresenterFactory: BookmarkPresenter.Factory
  }
}
