package de.ph1b.audiobook.features.bookmarks

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Bookmark
import de.ph1b.audiobook.data.Chapter
import de.ph1b.audiobook.features.bookmarks.dialogs.AddBookmarkDialog
import de.ph1b.audiobook.features.bookmarks.dialogs.DeleteBookmarkDialog
import de.ph1b.audiobook.features.bookmarks.dialogs.EditBookmarkDialog
import de.ph1b.audiobook.features.bookmarks.list.BookMarkHolder
import de.ph1b.audiobook.features.bookmarks.list.BookmarkAdapter
import de.ph1b.audiobook.features.bookmarks.list.BookmarkClickListener
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.getUUID
import de.ph1b.audiobook.misc.putUUID
import de.ph1b.audiobook.misc.tint
import de.ph1b.audiobook.mvp.MvpController
import de.ph1b.audiobook.uitools.VerticalDividerItemDecoration
import kotlinx.android.synthetic.main.bookmark.*
import java.util.UUID

/**
 * Dialog for creating a bookmark
 */
private const val NI_BOOK_ID = "ni#bookId"

class BookmarkController(args: Bundle) :
  MvpController<BookmarkView, BookmarkPresenter>(args), BookmarkView,
  BookmarkClickListener, AddBookmarkDialog.Callback, DeleteBookmarkDialog.Callback,
  EditBookmarkDialog.Callback {

  constructor(bookId: UUID) : this(Bundle().apply {
    putUUID(NI_BOOK_ID, bookId)
  })

  private val bookId = args.getUUID(NI_BOOK_ID)
  private val adapter = BookmarkAdapter(this)

  override val layoutRes = R.layout.bookmark
  override fun createPresenter() = appComponent.bookmarkPresenter.apply {
    bookId = this@BookmarkController.bookId
  }

  override fun render(bookmarks: List<Bookmark>, chapters: List<Chapter>) {
    adapter.newData(bookmarks, chapters)
  }

  override fun showBookmarkAdded(bookmark: Bookmark) {
    val index = adapter.indexOf(bookmark)
    recycler.smoothScrollToPosition(index)
    Snackbar.make(view!!, R.string.bookmark_added, Snackbar.LENGTH_SHORT)
      .show()
  }

  override fun onDeleteBookmarkConfirmed(id: Long) {
    presenter.deleteBookmark(id)
  }

  override fun onBookmarkClicked(bookmark: Bookmark) {
    presenter.selectBookmark(bookmark.id)
    router.popController(this)
  }

  override fun onEditBookmark(id: Long, title: String) {
    presenter.editBookmark(id, title)
  }

  override fun onBookmarkNameChosen(name: String) {
    presenter.addBookmark(name)
  }

  override fun finish() {
    router.popController(this)
  }

  override fun onViewCreated() {
    setupToolbar()
    setupList()

    addBookmarkFab.setOnClickListener {
      showAddBookmarkDialog()
    }
  }

  override fun onDestroyView() {
    recycler.adapter = null
  }

  private fun setupToolbar() {
    toolbar.setTitle(R.string.bookmark)
    toolbar.setNavigationIcon(R.drawable.close)
    toolbar.setNavigationOnClickListener {
      router.popController(this)
    }
    toolbar.tint()
  }

  private fun setupList() {
    val layoutManager = LinearLayoutManager(activity)
    recycler.addItemDecoration(VerticalDividerItemDecoration(activity))
    recycler.layoutManager = layoutManager
    recycler.adapter = adapter
    val itemAnimator = recycler.itemAnimator as DefaultItemAnimator
    itemAnimator.supportsChangeAnimations = false

    val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
      override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
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
    val themedContext = ContextThemeWrapper(activity, R.style.PopupMenuStyle)
    val popup = PopupMenu(themedContext, v)
    popup.menuInflater.inflate(R.menu.bookmark_popup, popup.menu)
    popup.setOnMenuItemClickListener {
      when (it.itemId) {
        R.id.edit -> {
          showEditBookmarkDialog(bookmark)
          true
        }
        R.id.delete -> {
          showDeleteBookmarkDialog(bookmark)
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

  private fun showDeleteBookmarkDialog(bookmark: Bookmark) {
    DeleteBookmarkDialog(this, bookmark).showDialog(router)
  }
}
