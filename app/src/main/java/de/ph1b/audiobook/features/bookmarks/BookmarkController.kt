package de.ph1b.audiobook.features.bookmarks

import android.os.Bundle
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
import de.ph1b.audiobook.databinding.BookmarkBinding
import de.ph1b.audiobook.features.bookmarks.dialogs.AddBookmarkDialog
import de.ph1b.audiobook.features.bookmarks.dialogs.EditBookmarkDialog
import de.ph1b.audiobook.features.bookmarks.list.BookMarkHolder
import de.ph1b.audiobook.features.bookmarks.list.BookmarkAdapter
import de.ph1b.audiobook.features.bookmarks.list.BookmarkClickListener
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.conductor.context
import de.ph1b.audiobook.misc.getUUID
import de.ph1b.audiobook.misc.putUUID
import de.ph1b.audiobook.mvp.MvpController
import java.util.UUID

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

  constructor(bookId: UUID) : this(
    Bundle().apply {
      putUUID(NI_BOOK_ID, bookId)
    }
  )

  private val bookId = args.getUUID(NI_BOOK_ID)
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

  override fun onEditBookmark(id: UUID, title: String) {
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
