package de.ph1b.audiobook.features.bookmarks

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import de.ph1b.audiobook.Bookmark
import de.ph1b.audiobook.R
import de.ph1b.audiobook.misc.DialogController
import de.ph1b.audiobook.misc.argumentDelegate.LongArgumentDelegate
import de.ph1b.audiobook.misc.argumentDelegate.StringArgumentDelegate

/**
 * Dialog for confirming bookmark deletion.
 *
 * @author Paul Woitaschek
 */
class DeleteBookmarkDialog : DialogController() {

  private var bookId by LongArgumentDelegate()
  private var bookmarkTitle by StringArgumentDelegate()

  override fun onCreateDialog(savedViewState: Bundle?): Dialog = MaterialDialog.Builder(activity!!)
      .title(R.string.bookmark_delete_title)
      .content(bookmarkTitle)
      .positiveText(R.string.remove)
      .negativeText(R.string.dialog_cancel)
      .onPositive { _, _ ->
        val callback = targetController as Callback
        callback.onDeleteBookmarkConfirmed(bookId)
      }
      .build()

  interface Callback {
    fun onDeleteBookmarkConfirmed(id: Long)
  }

  companion object {

    operator fun <T> invoke(target: T, bookmark: Bookmark) where T : Controller, T : Callback = DeleteBookmarkDialog().apply {
      targetController = target
      bookId = bookmark.id
      bookmarkTitle = bookmark.title
    }
  }
}
