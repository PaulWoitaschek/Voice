package de.ph1b.audiobook.features.bookmarks.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.bluelinelabs.conductor.Controller
import de.ph1b.audiobook.R
import de.ph1b.audiobook.common.conductor.DialogController
import de.ph1b.audiobook.data.Bookmark
import de.ph1b.audiobook.data.getBookmarkId
import de.ph1b.audiobook.data.putBookmarkId

/**
 * Dialog for changing the bookmark title.
 */
class EditBookmarkDialog(args: Bundle) : DialogController(args) {

  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    val bookmarkTitle = args.getString(NI_BOOKMARK_TITLE)
    val bookmarkId = args.getBookmarkId(NI_BOOKMARK_ID)!!

    val dialog = MaterialDialog(activity!!).apply {
      title(R.string.bookmark_edit_title)
      val inputType = InputType.TYPE_CLASS_TEXT or
        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
        InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or
        InputType.TYPE_TEXT_FLAG_MULTI_LINE
      @Suppress("CheckResult")
      input(
        hintRes = R.string.bookmark_edit_hint,
        prefill = bookmarkTitle,
        allowEmpty = false,
        inputType = inputType
      ) { _, charSequence ->
        val callback = targetController as Callback
        val newTitle = charSequence.toString()
        callback.onEditBookmark(bookmarkId, newTitle)
      }
      positiveButton(R.string.dialog_confirm)
    }
    val editText = dialog.getInputField()
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
    fun onEditBookmark(id: Bookmark.Id, title: String)
  }

  companion object {

    private const val NI_BOOKMARK_ID = "ni#bookMarkId"
    private const val NI_BOOKMARK_TITLE = "ni#bookmarkTitle"

    operator fun <T> invoke(
      target: T,
      bookmark: Bookmark
    ): EditBookmarkDialog where T : Controller, T : Callback {
      val args = Bundle().apply {
        putBookmarkId(NI_BOOKMARK_ID, bookmark.id)
        putString(NI_BOOKMARK_TITLE, bookmark.title)
      }
      return EditBookmarkDialog(args).apply {
        targetController = target
      }
    }
  }
}
