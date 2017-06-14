package de.ph1b.audiobook.features.bookmarks

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import de.ph1b.audiobook.Bookmark
import de.ph1b.audiobook.R
import de.ph1b.audiobook.misc.DialogController

/**
 * Dialog for confirming all bookmarks deletion.
 *
 * @author Paul Woitaschek, Jim Miller
 */
class DeleteAllBookmarksDialog : DialogController() {

  override fun onCreateDialog(savedViewState: Bundle?): Dialog = MaterialDialog.Builder(activity!!)
      .title(R.string.bookmark_delete_all_title)
      .content(R.string.bookmark_delete_all)
      .positiveText(R.string.remove_all)
      .negativeText(R.string.dialog_cancel)
      .onPositive { _, _ ->
        val callback = targetController as Callback
        callback.onDeleteAllBookmarksConfirmed()
      }
      .build()

  interface Callback {
    fun onDeleteAllBookmarksConfirmed()
  }

  companion object {

    operator fun <T> invoke(target: T) where T : Controller, T : Callback = DeleteAllBookmarksDialog().apply {
      targetController = target
    }
  }
}
