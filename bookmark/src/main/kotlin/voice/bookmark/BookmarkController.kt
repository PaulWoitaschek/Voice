package voice.bookmark

import android.os.Bundle
import androidx.compose.runtime.Composable
import com.squareup.anvil.annotations.ContributesTo
import voice.common.AppScope
import voice.common.BookId
import voice.common.compose.ComposeController
import voice.common.rootComponentAs
import voice.data.getBookId
import voice.data.putBookId

private const val NI_BOOK_ID = "ni#bookId"

@Suppress("unused")
class BookmarkController(args: Bundle) :
  ComposeController(args) {

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
      onAdd = presenter::onAddClicked,
      onDelete = presenter::deleteBookmark,
      onEdit = presenter::onEditClicked,
      onScrollConfirmed = presenter::onScrollConfirmed,
      onClick = presenter::selectBookmark,
      onNewBookmarkNameChosen = presenter::addBookmark,
      onCloseDialog = presenter::closeDialog,
      onEditBookmark = presenter::editBookmark,
    )
  }

  @ContributesTo(AppScope::class)
  interface Component {
    val bookmarkPresenterFactory: BookmarkPresenter.Factory
  }
}
