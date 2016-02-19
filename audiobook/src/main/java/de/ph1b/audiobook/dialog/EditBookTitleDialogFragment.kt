/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.text.InputType
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.persistence.BookChest
import rx.schedulers.Schedulers
import javax.inject.Inject

/**
 * Simple dialog for changing the name of a book

 * @author Paul Woitaschek
 */
class EditBookTitleDialogFragment : DialogFragment() {

    @Inject internal lateinit var bookChest: BookChest

    init {
        App.component().inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val presetName = arguments.getString(NI_PRESET_NAME)
        val bookId = arguments.getLong(NI_BOOK_ID)

        return MaterialDialog.Builder(activity)
                .title(R.string.edit_book_title)
                .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
                .input(getString(R.string.bookmark_edit_hint), presetName, false) { materialDialog, charSequence ->
                    val newText = charSequence.toString()
                    if (newText != presetName) {
                        bookChest.activeBooks
                                .filter { it.id == bookId } // find book
                                .subscribeOn(Schedulers.io()) // dont block
                                .subscribe {
                                    val updatedBook = it.copy(name = newText) // update title
                                    bookChest.updateBook(updatedBook) // update book
                                }
                    }
                }
                .positiveText(R.string.dialog_confirm)
                .build()
    }

    companion object {

        val TAG = EditBookTitleDialogFragment::class.java.simpleName
        private val NI_PRESET_NAME = "niPresetName"
        private val NI_BOOK_ID = "niBookId"

        fun newInstance(book: Book): EditBookTitleDialogFragment {

            val args = Bundle()
            args.putString(NI_PRESET_NAME, book.name)
            args.putLong(NI_BOOK_ID, book.id)

            val dialog = EditBookTitleDialogFragment()
            dialog.arguments = args
            return dialog
        }
    }
}
