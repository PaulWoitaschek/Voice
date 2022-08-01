package voice.app.features.bookmarks

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import voice.app.R
import voice.app.databinding.BookmarkBinding
import voice.app.features.bookmarks.dialogs.AddBookmarkDialog
import voice.app.features.bookmarks.dialogs.EditBookmarkDialog
import voice.app.features.bookmarks.list.BookMarkHolder
import voice.app.features.bookmarks.list.BookmarkAdapter
import voice.app.features.bookmarks.list.BookmarkClickListener
import voice.app.injection.appComponent
import voice.app.misc.conductor.context
import voice.app.mvp.MvpController
import voice.common.BookId
import voice.data.Bookmark
import voice.data.Chapter
import voice.data.getBookId
import voice.data.putBookId

/**
 * Dialog for creating a bookmark
 */
private const val NI_BOOK_ID = "ni#bookId"

class BookmarkController(args: Bundle) :
  MvpController<BookmarkView, BookmarkPresenter, BookmarkBinding>(BookmarkBinding::inflate, args),
  BookmarkView,
  BookmarkClickListener,
  AddBookmarkDialog.Callback,
  EditBookmarkDialog.Callback {

  constructor(bookId: BookId) : this(
    Bundle().apply {
      putBookId(NI_BOOK_ID, bookId)
    },
  )

  private val bookId = args.getBookId(NI_BOOK_ID)!!
  private val adapter = BookmarkAdapter(this)

  override fun createPresenter() = appComponent.bookmarkPresenter.apply {
    bookId = this@BookmarkController.bookId
  }

  override fun render(bookmarks: List<Bookmark>, chapters: List<Chapter>) {
    adapter.newData(bookmarks, chapters)
  }

  override fun showBookmarkAdded(bookmark: Bookmark) {
    val index = adapter.indexOf(bookmark)
    binding.recycler.smoothScrollToPosition(index)
    Snackbar.make(view!!, R.string.bookmark_added, Snackbar.LENGTH_SHORT)
      .show()
  }

  override fun onBookmarkClicked(bookmark: Bookmark) {
    presenter.selectBookmark(bookmark.id)
    router.popController(this)
  }

  override fun onEditBookmark(id: Bookmark.Id, title: String) {
    presenter.editBookmark(id, title)
  }

  override fun onBookmarkNameChosen(name: String) {
    presenter.addBookmark(name)
  }

  override fun finish() {
    router.popController(this)
  }

  override fun BookmarkBinding.onBindingCreated() {
    setupToolbar()
    setupList()

    addBookmarkFab.setOnClickListener {
      showAddBookmarkDialog()
    }
  }

  override fun onDestroyView() {
    binding.recycler.adapter = null
  }

  private fun BookmarkBinding.setupToolbar() {
    toolbar.setNavigationIcon(R.drawable.close)
    toolbar.setNavigationOnClickListener {
      router.popController(this@BookmarkController)
    }
  }

  private fun BookmarkBinding.setupList() {
    val layoutManager = LinearLayoutManager(context)
    recycler.layoutManager = layoutManager
    recycler.adapter = adapter
    val itemAnimator = recycler.itemAnimator as DefaultItemAnimator
    itemAnimator.supportsChangeAnimations = false

    val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
      override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder,
      ): Boolean {
        return false
      }

      override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val boundBookmark = (viewHolder as BookMarkHolder).boundBookmark
        boundBookmark?.let { presenter.deleteBookmark(it.id) }
      }
    }
    ItemTouchHelper(swipeCallback).attachToRecyclerView(recycler)
  }

  override fun onOptionsMenuClicked(bookmark: Bookmark, v: View) {
    val popup = PopupMenu(context, v)
    popup.menuInflater.inflate(R.menu.bookmark_popup, popup.menu)
    popup.setOnMenuItemClickListener {
      when (it.itemId) {
        R.id.edit -> {
          showEditBookmarkDialog(bookmark)
          true
        }
        R.id.delete -> {
          presenter.deleteBookmark(bookmark.id)
          true
        }
        else -> false
      }
    }
    popup.show()
  }

  private fun showEditBookmarkDialog(bookmark: Bookmark) {
    EditBookmarkDialog(this, bookmark).showDialog(router)
  }

  private fun showAddBookmarkDialog() {
    AddBookmarkDialog(this).showDialog(router)
  }
}
