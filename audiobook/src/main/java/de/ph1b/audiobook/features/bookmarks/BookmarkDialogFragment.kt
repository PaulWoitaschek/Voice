package de.ph1b.audiobook.features.bookmarks

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.Bookmark
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.find
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.BookmarkProvider
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayerController
import i
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

/**
 * Dialog for creating a bookmark
 *
 * @author Paul Woitaschek
 */
class BookmarkDialogFragment : DialogFragment(), BookmarkAdapter.OnOptionsMenuClickedListener {

  init {
    App.component.inject(this)
  }

  override fun onOptionsMenuClicked(bookmark: Bookmark, v: View) {
    val popup = PopupMenu(activity, v)
    popup.menuInflater.inflate(R.menu.bookmark_popup, popup.menu)
    popup.setOnMenuItemClickListener {
      val builder = MaterialDialog.Builder(activity)
      when (it.itemId) {
        R.id.edit -> {
          MaterialDialog.Builder(context)
            .title(R.string.bookmark_edit_title)
            .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
            .input(getString(R.string.bookmark_edit_hint), bookmark.title, false) { materialDialog, charSequence ->
              val newBookmark = Bookmark(bookmark.mediaFile, charSequence.toString(), bookmark.time)
              adapter.replace(bookmark, newBookmark)
              bookmarkProvider.deleteBookmark(bookmark.id)
              bookmarkProvider.addBookmark(newBookmark)
            }
            .positiveText(R.string.dialog_confirm).show()
          return@setOnMenuItemClickListener true
        }
        R.id.delete -> {
          builder.title(R.string.bookmark_delete_title)
            .content(bookmark.title)
            .positiveText(R.string.remove)
            .negativeText(R.string.dialog_cancel)
            .onPositive { materialDialog, dialogAction ->
              adapter.remove(bookmark)
              bookmarkProvider.deleteBookmark(bookmark.id)
            }
            .show()
          return@setOnMenuItemClickListener true
        }
        else -> return@setOnMenuItemClickListener false
      }
    }
    popup.show()
  }

  override fun onBookmarkClicked(bookmark: Bookmark) {
    val wasPlaying = playStateManager.playState.value == PlayStateManager.PlayState.PLAYING

    prefs.currentBookId.set(bookId())
    playerController.changePosition(bookmark.time, bookmark.mediaFile)

    if (wasPlaying) {
      playerController.play()
    }

    dialog.cancel()
  }

  @Inject lateinit var prefs: PrefsManager
  @Inject lateinit var repo: BookRepository
  @Inject lateinit var bookmarkProvider: BookmarkProvider
  @Inject lateinit var playStateManager: PlayStateManager
  @Inject lateinit var playerController: PlayerController
  private lateinit var book: Book
  private lateinit var bookmarkTitle: TextView
  private lateinit var adapter: BookmarkAdapter

  fun addClicked() {
    i { "Add bookmark clicked." }
    var title = bookmarkTitle.text.toString()
    if (title.isEmpty()) {
      title = book.currentChapter().name
    }

    bookmarkProvider.addBookmarkAtBookPosition(book, title)
    Toast.makeText(activity, R.string.bookmark_added, Toast.LENGTH_SHORT).show()
    bookmarkTitle.text = ""
    dismiss()
  }

  private fun bookId() = arguments.getLong(BOOK_ID)

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val inflater = activity.layoutInflater
    val view = inflater.inflate(R.layout.dialog_bookmark, null)
    bookmarkTitle = view.find(R.id.bookmarkTitle)

    book = repo.bookById(bookId())!!
    adapter = BookmarkAdapter(book.chapters, this, context)
    val recycler = view.find<RecyclerView>(R.id.recycler)
    recycler.adapter = adapter
    val layoutManager = LinearLayoutManager(activity)
    recycler.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
    recycler.layoutManager = layoutManager

    bookmarkProvider.bookmarks(book)
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe { adapter.addAll(it) }

    val add: View = view.find(R.id.add)
    add.setOnClickListener { addClicked() }
    val bookmarkTitle: TextView = view.find(R.id.bookmarkTitle)
    bookmarkTitle.setOnEditorActionListener { v1, actionId, event ->
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        addClicked() //same as clicking on the +
        return@setOnEditorActionListener true
      }
      false
    }

    return MaterialDialog.Builder(context)
      .customView(view, false)
      .title(R.string.bookmark)
      .negativeText(R.string.dialog_cancel)
      .build()
  }

  companion object {

    val TAG: String = BookmarkDialogFragment::class.java.simpleName
    private val BOOK_ID = "bookId"

    fun newInstance(bookId: Long): BookmarkDialogFragment {
      val bookmarkDialogFragment = BookmarkDialogFragment()
      val args = Bundle()
      args.putLong(BOOK_ID, bookId)
      bookmarkDialogFragment.arguments = args
      return bookmarkDialogFragment
    }
  }
}