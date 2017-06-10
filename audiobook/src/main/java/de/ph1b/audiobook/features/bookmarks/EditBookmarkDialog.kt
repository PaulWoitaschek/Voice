package de.ph1b.audiobook.features.bookmarks

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import de.ph1b.audiobook.Bookmark
import de.ph1b.audiobook.R
import de.ph1b.audiobook.misc.DialogController
import de.ph1b.audiobook.misc.argumentDelegate.LongArgumentDelegate
import de.ph1b.audiobook.misc.argumentDelegate.StringArgumentDelegate

/**
 * Dialog for changing the bookmark title.
 *
 * @author Paul Woitaschek
 */
class EditBookmarkDialog : DialogController() {

  private var bookmarkTitle by StringArgumentDelegate()
  private var bookmarkId by LongArgumentDelegate()

  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    val dialog = MaterialDialog.Builder(activity!!)
        .title(R.string.bookmark_edit_title)
        .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
        .input(activity!!.getString(R.string.bookmark_edit_hint), bookmarkTitle, false) { _, charSequence ->
          val callback = targetController as Callback
          val newTitle = charSequence.toString()
          callback.onEditBookmark(bookmarkId, newTitle)
        }
        .positiveText(R.string.dialog_confirm)
        .build()
    val editText = dialog.inputEditText!!
    editText.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        val callback = targetController as Callback
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

    operator fun <T> invoke(target: T, bookmark: Bookmark) where T : Controller, T : Callback = EditBookmarkDialog().apply {
      targetController = target
      bookmarkTitle = bookmark.title
      bookmarkId = bookmark.id
    }
  }
}
