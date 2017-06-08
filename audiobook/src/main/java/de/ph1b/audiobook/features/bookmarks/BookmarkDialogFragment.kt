package de.ph1b.audiobook.features.bookmarks

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.Bookmark
import de.ph1b.audiobook.R
import de.ph1b.audiobook.databinding.DialogBookmarkBinding
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.value
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
class BookmarkDialogFragment : DialogFragment(), BookMarkClickListener {

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
              .input(getString(R.string.bookmark_edit_hint), bookmark.title, false) { _, charSequence ->
                val newBookmark = Bookmark(bookmark.mediaFile, charSequence.toString(), bookmark.time)
                bookmarkProvider.deleteBookmark(bookmark.id)
                bookmarkProvider.addBookmark(newBookmark)
                updateAdapterContents()
              }
              .positiveText(R.string.dialog_confirm).show()
          return@setOnMenuItemClickListener true
        }
        R.id.delete -> {
          builder.title(R.string.bookmark_delete_title)
              .content(bookmark.title)
              .positiveText(R.string.remove)
              .negativeText(R.string.dialog_cancel)
              .onPositive { _, _ ->
                bookmarkProvider.deleteBookmark(bookmark.id)
                updateAdapterContents()
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
    val wasPlaying = playStateManager.playState == PlayStateManager.PlayState.PLAYING

    prefs.currentBookId.value = bookId()
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
  private lateinit var adapter: BookmarkAdapter
  private lateinit var binding: DialogBookmarkBinding

  fun addClicked() {
    i { "Add bookmark clicked." }
    var title = binding.bookmarkTitle.text.toString()
    if (title.isEmpty()) {
      title = book.currentChapter().name
    }

    bookmarkProvider.addBookmarkAtBookPosition(book, title)
    Toast.makeText(activity, R.string.bookmark_added, Toast.LENGTH_SHORT).show()
    binding.bookmarkTitle.setText("")
    dismiss()
  }

  private fun bookId() = arguments.getLong(BOOK_ID)

  @SuppressLint("InflateParams")
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val inflater = activity.layoutInflater
    binding = DialogBookmarkBinding.inflate(inflater)


    book = repo.bookById(bookId())!!
    adapter = BookmarkAdapter(book.chapters, this)
    binding.recycler.adapter = adapter
    val layoutManager = LinearLayoutManager(activity)
    binding.recycler.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
    binding.recycler.layoutManager = layoutManager

    updateAdapterContents()

    binding.add.setOnClickListener { addClicked() }
    binding.bookmarkTitle.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        addClicked() //same as clicking on the +
        return@setOnEditorActionListener true
      }
      false
    }

    return MaterialDialog.Builder(context)
        .customView(binding.root, false)
        .title(R.string.bookmark)
        .negativeText(R.string.dialog_cancel)
        .build()
  }

  private fun updateAdapterContents() {
    bookmarkProvider.bookmarks(book)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { adapter.newData(it) }
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
