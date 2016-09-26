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
package de.ph1b.audiobook.features.book_overview

import android.app.Dialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.widget.ImageView
import com.afollestad.materialdialogs.MaterialDialog
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.imagepicker.CropOverlay
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.persistence.BookChest
import de.ph1b.audiobook.uitools.*
import javax.inject.Inject
import com.squareup.picasso.Callback as PicassoCallback

/**
 * Simple dialog to edit the cover of a book.
 */
class EditCoverDialogFragment : DialogFragment() {

    @Inject internal lateinit var db: BookChest
    @Inject internal lateinit var bookChest: BookChest
    @Inject internal lateinit var imageHelper: ImageHelper

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        App.component().inject(this)

        val picasso = Picasso.with(context)

        // retrieve views
        val customView = activity.layoutInflater.inflate(R.layout.dialog_cover_edit, null)
        val cropOverlay = customView.findViewById(R.id.cropOverlay) as CropOverlay
        val coverImage = customView.findViewById(R.id.coverImage) as ImageView
        val loadingProgressBar = customView.findViewById(R.id.cover_replacement)

        // init values
        val bookId = arguments.getLong(NI_BOOK_ID)
        val uri = Uri.parse(arguments.getString(NI_COVER_URI))
        val book = bookChest.bookById(bookId)!!
        val coverReplacement = CoverReplacement(book.name, context)

        loadingProgressBar.visible = true
        coverImage.visible = false
        cropOverlay.selectionOn = false
        picasso.load(uri)
                .into(coverImage, object : PicassoCallback {
                    override fun onError() {
                        coverImage.setImageDrawable(coverReplacement)
                        coverImage.visible = true
                        cropOverlay.selectionOn = true
                        loadingProgressBar.visible = false
                    }

                    override fun onSuccess() {
                        coverImage.visible = true
                        cropOverlay.selectionOn = true
                        loadingProgressBar.visible = false
                    }
                })

        val positiveCallback = MaterialDialog.SingleButtonCallback { materialDialog, dialogAction ->
            cropOverlay.selectionOn = false
            val r = cropOverlay.selectedRect
            val useCoverReplacement: Boolean
            if (!r.isEmpty) {
                var cover = picasso.blocking { load(uri).get() }
                if (cover != null) {
                    cover = Bitmap.createBitmap(cover, r.left, r.top, r.width(), r.height())
                    imageHelper.saveCover(cover, book.coverFile())

                    picasso.invalidate(book.coverFile())
                    useCoverReplacement = false
                    cover.recycle()
                } else {
                    useCoverReplacement = true
                }
            } else {
                useCoverReplacement = true
            }

            //noinspection SynchronizeOnNonFinalField
            synchronized(db) {
                val dbBook = bookChest.bookById(bookId)?.copy(useCoverReplacement = useCoverReplacement)
                if (dbBook != null) {
                    db.updateBook(dbBook)
                }
            }
        }

        return MaterialDialog.Builder(context)
                .customView(customView, true)
                .title(R.string.use_cover)
                .positiveText(R.string.dialog_confirm)
                .onPositive(positiveCallback)
                .build()
    }

    companion object {
        val TAG = EditCoverDialogFragment::class.java.simpleName!!

        private val NI_COVER_URI = "coverPath"
        private val NI_BOOK_ID = "id"

        fun newInstance(book: Book, uri: Uri): EditCoverDialogFragment = EditCoverDialogFragment().apply {
            arguments = Bundle().apply {
                putString(NI_COVER_URI, uri.toString())
                putLong(NI_BOOK_ID, book.id)
            }
        }
    }
}