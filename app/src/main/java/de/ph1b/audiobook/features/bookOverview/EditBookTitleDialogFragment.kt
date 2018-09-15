package de.ph1b.audiobook.features.bookOverview

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.misc.getUUID
import de.ph1b.audiobook.misc.putUUID
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.launch
import org.koin.android.ext.android.inject

/**
 * Simple dialog for changing the name of a book
 */
class EditBookTitleDialogFragment : DialogFragment() {

  private val repo: BookRepository by inject()

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val presetName = arguments!!.getString(NI_PRESET_NAME)
    val bookId = arguments!!.getUUID(NI_BOOK_ID)

    return MaterialDialog.Builder(activity!!)
      .title(R.string.edit_book_title)
      .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
      .input(getString(R.string.bookmark_edit_hint), presetName, false) { _, charSequence ->
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

  companion object {

    val TAG: String = EditBookTitleDialogFragment::class.java.simpleName
    private const val NI_PRESET_NAME = "niPresetName"
    private const val NI_BOOK_ID = "niBookId"

    fun newInstance(book: Book): EditBookTitleDialogFragment {
      val args = Bundle()
      args.putString(NI_PRESET_NAME, book.name)
      args.putUUID(NI_BOOK_ID, book.id)

      val dialog = EditBookTitleDialogFragment()
      dialog.arguments = args
      return dialog
    }
  }
}
