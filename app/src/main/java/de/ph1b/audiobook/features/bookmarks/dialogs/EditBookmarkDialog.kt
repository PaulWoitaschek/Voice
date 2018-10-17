package de.ph1b.audiobook.features.bookmarks.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Bookmark
import de.ph1b.audiobook.misc.DialogController

/**
 * Dialog for changing the bookmark title.
 */
class EditBookmarkDialog(args: Bundle) : DialogController(args) {

  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    val bookmarkTitle = args.getString(NI_BOOKMARK_TITLE)
    val bookmarkId = args.getLong(NI_BOOK_ID)

    val dialog = MaterialDialog.Builder(activity!!)
      .title(de.ph1b.audiobook.R.string.bookmark_edit_title)
      .inputType(
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
      )
      .input(
        activity!!.getString(R.string.bookmark_edit_hint),
        bookmarkTitle,
        false
      ) { _, charSequence ->
        val callback = targetController as EditBookmarkDialog.Callback
        val newTitle = charSequence.toString()
        callback.onEditBookmark(bookmarkId, newTitle)
      }
      .positiveText(de.ph1b.audiobook.R.string.dialog_confirm)
      .build()
    val editText = dialog.inputEditText!!
    editText.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        val callback = targetController as EditBookmarkDialog.Callback
        val newTitle = editText.text.toString()
        callback.onEditBookmark(bookmarkId, newTitle)
        dismissDialog()
        true
      } else false
    }
    return dialog
  }

  interface Callback {
    fun onEditBookmark(id: Long, title: String)
  }

  companion object {

    private const val NI_BOOK_ID = "ni#bookId"
    private const val NI_BOOKMARK_TITLE = "ni#bookmarkTitle"

    operator fun <T> invoke(
      target: T,
      bookmark: Bookmark
    ): EditBookmarkDialog where T : Controller, T : EditBookmarkDialog.Callback {
      val args = Bundle().apply {
        putLong(NI_BOOK_ID, bookmark.id)
        putString(NI_BOOKMARK_TITLE, bookmark.title)
      }
      return EditBookmarkDialog(args).apply {
        targetController = target
      }
    }
  }
}
