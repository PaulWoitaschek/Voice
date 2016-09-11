package de.ph1b.audiobook.features.book_overview

import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.BottomSheetDialogFragment
import android.widget.TextView
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.bookmarks.BookmarkDialogFragment
import de.ph1b.audiobook.features.imagepicker.ImagePickerController
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.*
import de.ph1b.audiobook.persistence.BookChest
import e
import javax.inject.Inject

/**
 * Bottom sheet dialog fragment that will be displayed when a book edit was requested
 *
 * @author Paul Woitaschek
 */
class EditBookBottomSheet : BottomSheetDialogFragment() {

    @Inject lateinit var bookChest: BookChest

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        App.component().inject(this)

        val dialog = BottomSheetDialog(context, R.style.BottomSheetStyle)

        // if there is no book, skip here
        val book = bookChest.bookById(bookId())
        if (book == null) {
            e { "book is null. Return early" }
            return dialog
        }

        @SuppressWarnings("InflateParams")
        val view = context.layoutInflater().inflate(R.layout.book_more_bottom_sheet, null, false)
        val title = view.findViewById(R.id.title) as TextView
        val cover = view.findViewById(R.id.cover) as TextView
        val bookmark = view.findViewById(R.id.bookmark) as TextView
        dialog.setContentView(view)

        title.setOnClickListener {
            EditBookTitleDialogFragment.newInstance(book)
                    .show(fragmentManager, EditBookTitleDialogFragment.TAG)
            dismiss()
        }
        cover.setOnClickListener {
            val intent = ImagePickerController.newIntent(context, book.id)
            startActivity(intent)
            dismiss()
        }
        bookmark.setOnClickListener {
            BookmarkDialogFragment.newInstance(book.id)
                    .show(fragmentManager, BookShelfController.TAG)
            dismiss()
        }

        tintLeftDrawable(title)
        tintLeftDrawable(cover)
        tintLeftDrawable(bookmark)

        return dialog
    }

    private fun tintLeftDrawable(textView: TextView) {
        val left = textView.leftCompoundDrawable()!!
        val tinted = left.tinted(context.color(R.color.icon_color))
        textView.setCompoundDrawables(tinted, textView.topCompoundDrawable(), textView.rightCompoundDrawable(), textView.bottomCompoundDrawable())
    }

    private fun bookId() = arguments.getLong(NI_BOOK)

    companion object {
        private const val NI_BOOK = "niBook"
        fun newInstance(book: Book) = EditBookBottomSheet().apply {
            arguments = Bundle().apply {
                putLong(NI_BOOK, book.id)
            }
        }
    }
}