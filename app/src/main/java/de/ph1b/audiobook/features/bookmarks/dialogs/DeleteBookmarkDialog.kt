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
class DeleteBookmarkDialog(args: Bundle) : DialogController(args) {

  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    val bookId = args.getLong(NI_BOOK_ID)
    val bookmarkTitle = args.getString(NI_BOOKMARK_TITLE)!!
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

    operator fun <T> invoke(
      target: T,
      bookmark: Bookmark
    ): DeleteBookmarkDialog where T : Controller, T : Callback {
      val args = Bundle().apply {
        putLong(NI_BOOK_ID, bookmark.id)
        putString(NI_BOOKMARK_TITLE, bookmark.title)
      }
      return DeleteBookmarkDialog(args).apply {
        targetController = target
      }
    }
  }
}
