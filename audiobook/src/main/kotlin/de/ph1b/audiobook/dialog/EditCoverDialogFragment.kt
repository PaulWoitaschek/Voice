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
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.google.common.base.Preconditions
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.persistence.BookChest
import de.ph1b.audiobook.uitools.CoverDownloader
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.uitools.DraggableBoxImageView
import de.ph1b.audiobook.uitools.ImageHelper
import de.ph1b.audiobook.utils.BookVendor
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * Simple dialog to edit the cover of a book.
 */
class EditCoverDialogFragment : DialogFragment() {

    @Inject internal lateinit var db: BookChest
    @Inject internal lateinit var bookVendor: BookVendor
    @Inject internal lateinit var coverDownloader: CoverDownloader
    @Inject internal lateinit var imageHelper: ImageHelper

    private val imageURLS = ArrayList<String>(20)
    private lateinit var coverImageView: DraggableBoxImageView
    private lateinit var loadingProgressBar: View
    private lateinit var previousCover: View
    private lateinit var nextCover: View
    private var addCoverAsync: AddCoverAsync? = null
    /**
     * The position in the [.imageURLS] or -1 if it is the [.coverReplacement].
     */
    private var coverPosition = -1
    private var googleCount = 0
    private lateinit var book: Book
    private var coverReplacement: CoverReplacement? = null
    private lateinit var picasso: Picasso
    private var isOnline: Boolean = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        App.component().inject(this)

        // retrieve views
        val customView = activity.layoutInflater.inflate(R.layout.dialog_cover_edit, null)
        coverImageView = customView.findViewById(R.id.edit_book) as DraggableBoxImageView
        loadingProgressBar = customView.findViewById(R.id.cover_replacement)
        previousCover = customView.findViewById(R.id.previous_cover)
        nextCover = customView.findViewById(R.id.next_cover)

        previousCover.setOnClickListener {
            if (addCoverAsync != null && !addCoverAsync!!.isCancelled) {
                addCoverAsync!!.cancel(true)
            }
            coverPosition--
            loadCoverPosition()
            setNextPreviousEnabledDisabled()
        }
        nextCover.setOnClickListener {
            if (coverPosition < imageURLS.size - 1) {
                coverPosition++
                loadCoverPosition()
            } else {
                genCoverFromInternet(book.name)
            }
            setNextPreviousEnabledDisabled()
        }

        // init values
        val bookId = arguments.getLong(NI_BOOK)
        book = bookVendor.byId(bookId)!!
        coverReplacement = CoverReplacement(book.name, context)
        isOnline = imageHelper.isOnline
        if (savedInstanceState == null) {
            coverPosition = -1
        } else {
            imageURLS.clear()
            //noinspection ConstantConditions
            imageURLS.addAll(savedInstanceState.getStringArrayList(SI_COVER_URLS))
            coverPosition = savedInstanceState.getInt(SI_COVER_POSITION)
        }
        loadCoverPosition()
        setNextPreviousEnabledDisabled()

        val positiveCallback = MaterialDialog.SingleButtonCallback { materialDialog, dialogAction ->
            Timber.d("edit book positive clicked. CoverPosition=%s", coverPosition)
            if (addCoverAsync != null && !addCoverAsync!!.isCancelled) {
                addCoverAsync!!.cancel(true)
            }

            val r = coverImageView.selectedRect
            val useCoverReplacement: Boolean
            if (coverPosition > -1 && !r.isEmpty) {
                var cover = imageHelper.picassoGetBlocking(imageURLS[coverPosition])
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

            val callback = targetFragment as OnEditBookFinished
            callback.onEditBookFinished(bookId)
        }
        val negativeCallback = MaterialDialog.SingleButtonCallback { materialDialog, dialogAction ->
            if (addCoverAsync != null && !addCoverAsync!!.isCancelled) {
                addCoverAsync!!.cancel(true)
            }
        }

        return MaterialDialog.Builder(context)
                .customView(customView, true)
                .title(R.string.edit_book_cover)
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .onPositive(positiveCallback)
                .onNegative(negativeCallback)
                .build()
    }

    /**
     * Loads the current cover and sets progress replacement visibility accordingly.
     */
    private fun loadCoverPosition() {
        if (coverPosition == -1) {
            loadingProgressBar.visibility = View.GONE
            coverImageView.visibility = View.VISIBLE
            coverImageView.setImageDrawable(coverReplacement)
        } else {
            loadingProgressBar.visibility = View.VISIBLE
            coverImageView.visibility = View.GONE
            picasso.load(imageURLS[coverPosition]).into(coverImageView, object : Callback {
                override fun onSuccess() {
                    loadingProgressBar.visibility = View.GONE
                    coverImageView.visibility = View.VISIBLE
                }

                override fun onError() {

                }
            })
        }
    }

    /**
     * Initiates a search on a cover from the internet and shows it if successful

     * @param searchString the name to search the cover by
     */
    private fun genCoverFromInternet(searchString: String) {
        //cancels task if running
        if (addCoverAsync != null) {
            if (!addCoverAsync!!.isCancelled) {
                addCoverAsync!!.cancel(true)
            }
        }
        addCoverAsync = AddCoverAsync(searchString)
        googleCount++
        addCoverAsync!!.execute()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        picasso = Picasso.with(context)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(SI_COVER_POSITION, coverPosition)
        outState.putStringArrayList(SI_COVER_URLS, ArrayList(imageURLS))
    }

    /**
     * Sets the next and previous buttons (that navigate within covers) visible / invisible,
     * accordingly to the current position.
     */
    private fun setNextPreviousEnabledDisabled() {
        if (coverPosition > -1) {
            previousCover.visibility = View.VISIBLE
        } else {
            previousCover.visibility = View.INVISIBLE
        }

        if (isOnline || (coverPosition + 1 < imageURLS.size)) {
            nextCover.visibility = View.VISIBLE
        } else {
            nextCover.visibility = View.INVISIBLE
        }
    }


    interface OnEditBookFinished {
        fun onEditBookFinished(bookId: Long)
    }

    private inner class AddCoverAsync(private val searchString: String) : AsyncTask<Void, Void, String>() {

        override fun doInBackground(vararg voids: Void): String? {
            return coverDownloader.fetchCover(searchString, googleCount)
        }

        override fun onPreExecute() {
            nextCover.visibility = View.INVISIBLE
            if (!imageURLS.isEmpty()) {
                previousCover.visibility = View.VISIBLE
            }
            loadingProgressBar.visibility = View.VISIBLE
            coverImageView.visibility = View.GONE
        }

        override fun onPostExecute(bitmapUrl: String?) {
            if (isAdded) {
                if (bitmapUrl != null) {
                    imageURLS.add(bitmapUrl)
                    coverPosition = imageURLS.size - 1
                }
                loadCoverPosition()
                setNextPreviousEnabledDisabled()
            }
        }
    }

    companion object {
        val TAG = EditCoverDialogFragment::class.java.simpleName
        private val SI_COVER_POSITION = "siCoverPosition"
        private val SI_COVER_URLS = "siCoverUrls"
        private val NI_BOOK = "niBook"

        fun <T> newInstance(target: T, book: Book): EditCoverDialogFragment where T : Fragment, T : OnEditBookFinished {
            Preconditions.checkNotNull(target)
            Preconditions.checkNotNull(book)

            val bundle = Bundle()
            bundle.putLong(NI_BOOK, book.id)

            val editCoverDialogFragment = EditCoverDialogFragment()
            editCoverDialogFragment.setTargetFragment(target, 42)
            editCoverDialogFragment.arguments = bundle
            return editCoverDialogFragment
        }
    }
}
