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
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.dialog

import android.app.Dialog
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.persistence.BookChest
import de.ph1b.audiobook.uitools.*
import de.ph1b.audiobook.utils.BookVendor
import javax.inject.Inject
import com.squareup.picasso.Callback as PicassoCallback

/**
 * Simple dialog to edit the cover of a book.
 */
class EditCoverDialogFragment : DialogFragment() {

    init {
        App.component().inject(this)
    }

    @Inject internal lateinit var db: BookChest
    @Inject internal lateinit var bookVendor: BookVendor
    @Inject internal lateinit var imageHelper: ImageHelper

    private val callback by lazy { targetFragment as Callback }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val picasso = Picasso.with(context)

        // retrieve views
        val customView = activity.layoutInflater.inflate(R.layout.dialog_cover_edit, null)
        val coverImageView = customView.findViewById(R.id.edit_book) as DraggableBoxImageView
        val loadingProgressBar = customView.findViewById(R.id.cover_replacement)

        // init values
        val bookId = arguments.getLong(NI_BOOK_ID)
        val url = arguments.getString(NI_COVER_URL)
        val book = bookVendor.byId(bookId)!!
        val coverReplacement = CoverReplacement(book.name, context)

        loadingProgressBar.setVisible()
        coverImageView.setInvisible()
        picasso.load(url)
                .into(coverImageView, object : PicassoCallback {
                    override fun onError() {
                        coverImageView.setImageDrawable(coverReplacement)
                        coverImageView.setVisible()
                        loadingProgressBar.setInvisible()
                    }

                    override fun onSuccess() {
                        coverImageView.setVisible()
                        loadingProgressBar.setInvisible()
                    }
                })

        val positiveCallback = MaterialDialog.SingleButtonCallback { materialDialog, dialogAction ->

            val r = coverImageView.selectedRect
            val useCoverReplacement: Boolean
            if (!r.isEmpty) {
                var cover = imageHelper.picassoGetBlocking(url)
                if (cover != null) {
                    cover = Bitmap.createBitmap(cover, r.left, r.top, r.width(), r.height())
                    imageHelper.saveCover(cover, book.coverFile())

                    picasso.invalidate(book.coverFile())
                    useCoverReplacement = false
                } else {
                    useCoverReplacement = true
                }
            } else {
                useCoverReplacement = true
            }

            //noinspection SynchronizeOnNonFinalField
            synchronized (db) {
                var dbBook = bookVendor.byId(bookId)?.copy(useCoverReplacement = useCoverReplacement)
                if (dbBook != null) {
                    db.updateBook(dbBook)
                }
            }

            callback.editBookFinished()
        }

        return MaterialDialog.Builder(context)
                .customView(customView, true)
                .title(R.string.cover)
                .positiveText(R.string.dialog_confirm)
                .onPositive(positiveCallback)
                .build()
    }

    interface Callback {
        fun editBookFinished()
    }

    companion object {
        val TAG = EditCoverDialogFragment::class.java.simpleName

        private val NI_COVER_URL = "coverUrl"
        private val NI_BOOK_ID = "id"

        fun <T> newInstance(target: T, book: Book, url: String) where T : Fragment, T : Callback = EditCoverDialogFragment().apply {
            arguments = Bundle().apply {
                putString(NI_COVER_URL, url)
                putLong(NI_BOOK_ID, book.id)
            }
            this.setTargetFragment(target, 42)
        }
    }
}
