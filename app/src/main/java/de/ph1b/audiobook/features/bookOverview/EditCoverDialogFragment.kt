package de.ph1b.audiobook.features.bookOverview

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import com.squareup.picasso.Picasso
import dagger.android.support.AndroidSupportInjection
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.databinding.DialogCoverEditBinding
import de.ph1b.audiobook.misc.coverFile
import de.ph1b.audiobook.misc.findCallback
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

  @Inject
  lateinit var repo: BookRepository
  @Inject
  lateinit var imageHelper: ImageHelper

  @SuppressLint("InflateParams")
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    AndroidSupportInjection.inject(this)

    val picasso = Picasso.with(context)

    val binding = DialogCoverEditBinding.inflate(activity!!.layoutInflater)!!

    // init values
    val bookId = arguments!!.getLong(NI_BOOK_ID)
    val uri = Uri.parse(arguments!!.getString(NI_COVER_URI))
    val book = repo.bookById(bookId)!!

    binding.coverReplacement.visible = true
    binding.cropOverlay.selectionOn = false
    picasso.load(uri)
      .into(
        binding.coverImage, object : PicassoCallback {
          override fun onError() {
            dismiss()
          }

          override fun onSuccess() {
            binding.cropOverlay.selectionOn = true
            binding.coverReplacement.visible = false
          }
        }
      )

    val dialog = MaterialDialog.Builder(context!!)
      .customView(binding.root, false)
      .title(R.string.cover)
      .positiveText(R.string.dialog_confirm)
      .build()

    // use a click listener so the dialog stays open till the image was saved
    dialog.getActionButton(DialogAction.POSITIVE).setOnClickListener {
      val r = binding.cropOverlay.selectedRect
      if (!r.isEmpty) {
        val target = object : SimpleTarget {
          override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom?) {
            val coverFile = book.coverFile()
            imageHelper.saveCover(bitmap, coverFile)
            picasso.invalidate(coverFile)
            findCallback<Callback>(NI_TARGET).onBookCoverChanged(book)
            dismiss()
          }

          override fun onBitmapFailed(errorDrawable: Drawable?) {
            dismiss()
          }
        }
        // picasso only holds a weak reference so we have to protect against gc
        binding.coverImage.tag = target
        picasso.load(uri)
          .transform(CropTransformation(binding.cropOverlay, binding.coverImage))
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

    private val NI_COVER_URI = "ni#coverPath"
    private val NI_BOOK_ID = "ni#id"
    private val NI_TARGET = "ni#target"

    fun <T> newInstance(target: T, book: Book, uri: Uri) where T : Controller, T : Callback =
      EditCoverDialogFragment().apply {
        arguments = Bundle().apply {
          putString(NI_COVER_URI, uri.toString())
          putLong(NI_BOOK_ID, book.id)
          putString(NI_TARGET, target.instanceId)
        }
      }
  }
}
