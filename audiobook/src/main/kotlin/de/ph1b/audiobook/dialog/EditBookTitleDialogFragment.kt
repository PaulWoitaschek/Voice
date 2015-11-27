package de.ph1b.audiobook.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.text.InputType

import com.afollestad.materialdialogs.MaterialDialog

import de.ph1b.audiobook.R
import de.ph1b.audiobook.model.Book

/**
 * Simple dialog for changing the name of a book

 * @author Paul Woitaschek
 */
class EditBookTitleDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val presetName = arguments.getString(NI_PRESET_NAME)
        val bookId = arguments.getLong(NI_BOOK_ID)

        return MaterialDialog.Builder(activity)
                .title(R.string.edit_book_title)
                .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
                .input(getString(R.string.bookmark_edit_hint), presetName, false) { materialDialog, charSequence ->
                    val newText = charSequence.toString()
                    if (newText != presetName) {
                        val callback = targetFragment as OnTextChanged
                        callback.onTitleChanged(newText, bookId)
                    }
                }
                .positiveText(R.string.dialog_confirm)
                .build()
    }

    interface OnTextChanged {
        fun onTitleChanged(newTitle: String, bookId: Long)
    }

    companion object {

        val TAG = EditBookTitleDialogFragment::class.java.simpleName
        private val NI_PRESET_NAME = "niPresetName"
        private val NI_BOOK_ID = "niBookId"

        fun <T> newInstance(target: T, book: Book): EditBookTitleDialogFragment where T : Fragment, T : OnTextChanged {

            val args = Bundle()
            args.putString(NI_PRESET_NAME, book.name)
            args.putLong(NI_BOOK_ID, book.id)

            val dialog = EditBookTitleDialogFragment()
            dialog.setTargetFragment(target, 42)
            dialog.arguments = args
            return dialog
        }
    }
}
