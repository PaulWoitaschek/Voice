package voice.app.features.bookOverview

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import voice.app.R
import voice.app.injection.appComponent
import voice.common.conductor.DialogController
import voice.data.Book
import voice.data.getBookId
import voice.data.putBookId
import voice.data.repo.BookRepository
import javax.inject.Inject

private const val NI_BOOK_ID = "niBookId"

class EditBookTitleDialogController(args: Bundle) : DialogController(args) {

  constructor(bookId: Book.Id) : this(
    Bundle().apply {
      putBookId(NI_BOOK_ID, bookId)
    }
  )

  @Inject
  lateinit var repo: BookRepository

  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    appComponent.inject(this)

    val bookId = args.getBookId(NI_BOOK_ID)!!
    val presetName = runBlocking {
      repo.flow(bookId).first()?.content?.name ?: ""
    }

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
          lifecycleScope.launch {
            repo.updateBook(bookId) {
              it.copy(name = newText)
            }
          }
        }
        positiveButton(R.string.dialog_confirm)
      }
    }
  }
}
