package de.ph1b.audiobook.features.bookmarks

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import de.ph1b.audiobook.R
import de.ph1b.audiobook.misc.DialogController

/**
 * Dialog for chosing a title for a new bookmark.
 *
 * @author Paul Woitaschek
 */
class AddBookmarkDialog : DialogController() {

  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    val dialog = MaterialDialog.Builder(activity!!)
        .title(R.string.bookmark)
        .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
        .input(activity!!.getString(R.string.bookmark_edit_hint), null, true) { _, charSequence ->
          val title = charSequence.toString()
          val callback = targetController as Callback
          callback.onBookmarkNameChosen(title)
        }
        .positiveText(R.string.dialog_confirm)
        .build()
    val editText = dialog.inputEditText!!
    editText.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        val title = editText.text.toString()
        val callback = targetController as Callback
        callback.onBookmarkNameChosen(title)
        dismissDialog()
        true
      } else false
    }
    return dialog
  }

  interface Callback {
    fun onBookmarkNameChosen(name: String)
  }

  companion object {
    operator fun <T> invoke(target: T) where T : Controller, T : Callback = AddBookmarkDialog().apply {
      targetController = target
    }
  }
}
