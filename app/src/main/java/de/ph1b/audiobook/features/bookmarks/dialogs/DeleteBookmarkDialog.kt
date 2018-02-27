package de.ph1b.audiobook.features.bookmarks.dialogs

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Bookmark
import de.ph1b.audiobook.misc.DialogController

/**
 * Dialog for confirming bookmark deletion.
 */
class DeleteBookmarkDialog : DialogController() {

  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    val bookId = args.getLong(NI_BOOK_ID)
    val bookmarkTitle = args.getString(NI_BOOKMARK_TITLE)
    return MaterialDialog.Builder(activity!!)
      .title(R.string.bookmark_delete_title)
      .content(bookmarkTitle)
      .positiveText(R.string.remove)
      .negativeText(R.string.dialog_cancel)
      .onPositive { _, _ ->
        val callback = targetController as Callback
        callback.onDeleteBookmarkConfirmed(bookId)
      }
      .build()
  }

  interface Callback {
    fun onDeleteBookmarkConfirmed(id: Long)
  }

  companion object {

    private const val NI_BOOK_ID = "ni#bookId"
    private const val NI_BOOKMARK_TITLE = "ni#bookmarkTitle"

    operator fun <T> invoke(target: T, bookmark: Bookmark) where T : Controller, T : Callback =
      DeleteBookmarkDialog().apply {
        targetController = target
        args.putLong(NI_BOOK_ID, bookmark.id)
        args.putString(NI_BOOKMARK_TITLE, bookmark.title)
      }
  }
}
