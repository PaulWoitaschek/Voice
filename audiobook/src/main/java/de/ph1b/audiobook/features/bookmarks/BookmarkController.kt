package de.ph1b.audiobook.features.bookmarks

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.PopupMenu
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.Bookmark
import de.ph1b.audiobook.R
import de.ph1b.audiobook.databinding.BookmarkBinding
import de.ph1b.audiobook.features.BaseController
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.argumentDelegate.LongArgumentDelegate
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.BookmarkProvider
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayerController
import javax.inject.Inject

/**
 * Dialog for creating a bookmark
 *
 * @author Paul Woitaschek
 */
class BookmarkController(args: Bundle) : BaseController<BookmarkBinding>(args), BookmarkClickListener, AddBookmarkDialog.Callback, DeleteBookmarkDialog.Callback, EditBookmarkDialog.Callback {

  private var bookId by LongArgumentDelegate()
  private val bookmarks = ArrayList<Bookmark>()

  @Inject lateinit var prefs: PrefsManager
  @Inject lateinit var repo: BookRepository
  @Inject lateinit var bookmarkProvider: BookmarkProvider
  @Inject lateinit var playStateManager: PlayStateManager
  @Inject lateinit var playerController: PlayerController

  private lateinit var book: Book
  private lateinit var adapter: BookmarkAdapter
  override val layoutRes = R.layout.bookmark

  init {
    App.component.inject(this)
  }

  override fun onBindingCreated(binding: BookmarkBinding) {
    book = repo.bookById(bookId)!!
    this.bookmarks.clear()
    this.bookmarks.addAll(bookmarkProvider.bookmarks(book))

    setupToolbar()
    setupList()

    binding.addBookmarkFab.setOnClickListener {
      showAddBookmarkDialog()
    }
  }

  private fun setupToolbar() {
    binding.toolbar.setTitle(R.string.bookmark)
    binding.toolbar.setNavigationIcon(R.drawable.close)
    binding.toolbar.setNavigationOnClickListener {
      router.popController(this)
    }
  }

  private fun setupList() {
    adapter = BookmarkAdapter(book.chapters, this)
    binding.recycler.adapter = adapter
    val layoutManager = LinearLayoutManager(activity)
    binding.recycler.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
    binding.recycler.layoutManager = layoutManager
    adapter.newData(bookmarks)
  }

  override fun onOptionsMenuClicked(bookmark: Bookmark, v: View) {
    val popup = PopupMenu(activity, v)
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

  override fun onDeleteBookmarkConfirmed(id: Long) {
    bookmarkProvider.deleteBookmark(id)
    bookmarks.removeIf { it.id == id }
    adapter.newData(bookmarks)
  }

  override fun onBookmarkClicked(bookmark: Bookmark) {
    val wasPlaying = playStateManager.playState == PlayStateManager.PlayState.PLAYING

    prefs.currentBookId.value = bookId
    playerController.changePosition(bookmark.time, bookmark.mediaFile)

    if (wasPlaying) {
      playerController.play()
    }

    router.popController(this)
  }

  override fun onEditBookmark(id: Long, title: String) {
    bookmarks.find { it.id == id }?.let {
      val withNewTitle = it.copy(
          title = title,
          id = Bookmark.ID_UNKNOWN
      )
      bookmarkProvider.deleteBookmark(it.id)
      val newBookmark = bookmarkProvider.addBookmark(withNewTitle)
      val index = bookmarks.indexOfFirst { it.id == id }
      bookmarks[index] = newBookmark
      adapter.newData(bookmarks)
    }
  }

  override fun onBookmarkNameChosen(name: String) {
    val title = if (name.isEmpty()) book.currentChapter().name else name
    val addedBookmark = bookmarkProvider.addBookmarkAtBookPosition(book, title)
    bookmarks.add(addedBookmark)
    adapter.newData(bookmarks)
    val index = adapter.indexOf(addedBookmark)
    binding.recycler.smoothScrollToPosition(index)
    Snackbar.make(binding.root, R.string.bookmark_added, Snackbar.LENGTH_SHORT)
        .show()
  }

  companion object {

    fun newInstance(bookId: Long) = BookmarkController(Bundle()).apply {
      this.bookId = bookId
    }
  }
}
