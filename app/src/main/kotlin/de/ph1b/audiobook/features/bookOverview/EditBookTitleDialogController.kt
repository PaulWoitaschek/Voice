package de.ph1b.audiobook.features.bookOverview

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import de.ph1b.audiobook.R
import de.ph1b.audiobook.common.conductor.DialogController
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.getUUID
import de.ph1b.audiobook.misc.putUUID
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val NI_PRESET_NAME = "niPresetName"
private const val NI_BOOK_ID = "niBookId"

class EditBookTitleDialogController(args: Bundle) : DialogController(args) {

  constructor(book: Book) : this(
    Bundle().apply {
      putString(NI_PRESET_NAME, book.name)
      putUUID(NI_BOOK_ID, book.id)
    }
  )

  @Inject
  lateinit var repo: BookRepository

  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    appComponent.inject(this)

    val presetName = args.getString(NI_PRESET_NAME)
    val bookId = args.getUUID(NI_BOOK_ID)

    return MaterialDialog(activity!!).apply {
      title(R.string.edit_book_title)
      val inputType = InputType.TYPE_CLASS_TEXT or
        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
      @Suppress("CheckResult")
      input(
        inputType = inputType,
        hintRes = R.string.change_book_name,
        prefill = presetName
      ) { _, text ->
        val newText = text.toString()
        if (newText != presetName) {
          GlobalScope.launch {
            repo.updateBookName(bookId, newText)
          }
        }
        positiveButton(R.string.dialog_confirm)
      }
    }
  }
}
