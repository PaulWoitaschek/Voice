package de.ph1b.audiobook.features.bookOverview

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.DialogController
import de.ph1b.audiobook.misc.getUUID
import de.ph1b.audiobook.misc.putUUID
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject

/**
 * Simple dialog for changing the name of a book
 */

private const val NI_PRESET_NAME = "niPresetName"
private const val NI_BOOK_ID = "niBookId"

class EditBookTitleDialogController(args: Bundle) : DialogController(args) {

  constructor(book: Book) : this(Bundle().apply {
    putString(NI_PRESET_NAME, book.name)
    putUUID(NI_BOOK_ID, book.id)
  })

  @Inject
  lateinit var repo: BookRepository

  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    App.component.inject(this)

    val presetName = args.getString(NI_PRESET_NAME)
    val bookId = args.getUUID(NI_BOOK_ID)

    return MaterialDialog.Builder(activity!!)
      .title(R.string.edit_book_title)
      .inputType(
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
      )
      .input(
        activity!!.getString(R.string.bookmark_edit_hint), presetName,
        false
      ) { _, charSequence ->
        val newText = charSequence.toString()
        if (newText != presetName) {
          repo.bookById(bookId)?.updateMetaData {
            copy(name = newText)
          }?.let {
            GlobalScope.launch {
              repo.updateBook(it)
            }
          }
        }
      }
      .positiveText(R.string.dialog_confirm)
      .build()
  }
}
