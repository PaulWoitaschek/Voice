package de.ph1b.audiobook.features.book_overview

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.widget.ImageView
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.imagepicker.CropOverlay
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.uitools.CropTransformation
import de.ph1b.audiobook.uitools.ImageHelper
import de.ph1b.audiobook.uitools.SimpleTarget
import de.ph1b.audiobook.uitools.visible
import javax.inject.Inject
import com.squareup.picasso.Callback as PicassoCallback

/**
 * Simple dialog to edit the cover of a book.
 */
class EditCoverDialogFragment : DialogFragment() {

  @Inject internal lateinit var repo: BookRepository
  @Inject internal lateinit var imageHelper: ImageHelper
  private fun callback() = activity as Callback

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
    val book = repo.bookById(bookId)!!

    loadingProgressBar.visible = true
    cropOverlay.selectionOn = false
    picasso.load(uri)
      .into(coverImage, object : PicassoCallback {
        override fun onError() {
          dismiss()
        }

        override fun onSuccess() {
          cropOverlay.selectionOn = true
          loadingProgressBar.visible = false
        }
      })

    val dialog = MaterialDialog.Builder(context)
      .customView(customView, false)
      .title(R.string.cover)
      .positiveText(R.string.dialog_confirm)
      .build()

    // use a click listener so the dialog stays open till the image was saved
    dialog.getActionButton(DialogAction.POSITIVE).setOnClickListener {
      val r = cropOverlay.selectedRect
      if (!r.isEmpty) {
        val target = object : SimpleTarget() {
          override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom?) {
            imageHelper.saveCover(bitmap, book.coverFile())
            picasso.invalidate(book.coverFile())
            callback().onBookCoverChanged(book)
            dismiss()
          }

          override fun onBitmapFailed(errorDrawable: Drawable?) {
            dismiss()
          }
        }
        // picasso only holds a weak reference so we have to protect against gc
        coverImage.tag = target
        picasso.load(uri)
          .transform(CropTransformation(cropOverlay, coverImage))
          .into(target)
      } else dismiss()
    }
    return dialog
  }

  interface Callback {
    fun onBookCoverChanged(book: Book)
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