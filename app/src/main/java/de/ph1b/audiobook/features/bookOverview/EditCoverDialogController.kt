package de.ph1b.audiobook.features.bookOverview

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.core.view.isVisible
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.DialogController
import de.ph1b.audiobook.misc.DialogLayoutContainer
import de.ph1b.audiobook.misc.coverFile
import de.ph1b.audiobook.misc.getUUID
import de.ph1b.audiobook.misc.putUUID
import de.ph1b.audiobook.uitools.CropTransformation
import de.ph1b.audiobook.uitools.ImageHelper
import de.ph1b.audiobook.uitools.SimpleTarget
import kotlinx.android.synthetic.main.dialog_cover_edit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.squareup.picasso.Callback as PicassoCallback

/**
 * Simple dialog to edit the cover of a book.
 */
class EditCoverDialogController(args: Bundle) : DialogController(args) {

  @Inject
  lateinit var repo: BookRepository
  @Inject
  lateinit var imageHelper: ImageHelper

  @SuppressLint("InflateParams")
  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    appComponent.inject(this)

    val picasso = Picasso.get()

    val container = DialogLayoutContainer(
      activity!!.layoutInflater.inflate(
        R.layout.dialog_cover_edit,
        null,
        false
      )
    )

    // init values
    val bookId = args.getUUID(NI_BOOK_ID)
    val uri = Uri.parse(args.getString(NI_COVER_URI))
    val book = repo.bookById(bookId)!!

    container.coverReplacement.isVisible = true
    container.cropOverlay.selectionOn = false
    picasso.load(uri)
      .into(
        container.coverImage, object : PicassoCallback {
          override fun onError(e: Exception?) {
            dismissDialog()
          }

          override fun onSuccess() {
            container.cropOverlay.selectionOn = true
            container.coverReplacement.isVisible = false
          }
        }
      )

    val dialog = MaterialDialog.Builder(activity!!)
      .customView(container.containerView, false)
      .title(R.string.cover)
      .positiveText(R.string.dialog_confirm)
      .build()

    // use a click listener so the dialog stays open till the image was saved
    dialog.getActionButton(DialogAction.POSITIVE).setOnClickListener {
      val r = container.cropOverlay.selectedRect
      if (!r.isEmpty) {
        val target = object : SimpleTarget {
          override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom?) {
            GlobalScope.launch(Dispatchers.Main) {
              val coverFile = book.coverFile()
              imageHelper.saveCover(bitmap, coverFile)
              picasso.invalidate(coverFile)
              val callback = targetController as Callback
              callback.onBookCoverChanged(book.id)
              dismissDialog()
            }
          }

          override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
            dismissDialog()
          }
        }
        // picasso only holds a weak reference so we have to protect against gc
        container.coverImage.tag = target
        picasso.load(uri)
          .transform(CropTransformation(container.cropOverlay, container.coverImage))
          .into(target)
      } else dismissDialog()
    }
    return dialog
  }

  interface Callback {
    fun onBookCoverChanged(bookId: UUID)
  }

  companion object {
    private const val NI_COVER_URI = "ni#coverPath"
    private const val NI_BOOK_ID = "ni#id"

    operator fun <T> invoke(
      target: T,
      bookId: UUID,
      uri: Uri
    ): EditCoverDialogController where T : Controller, T : Callback {
      val args = Bundle().apply {
        putString(NI_COVER_URI, uri.toString())
        putUUID(NI_BOOK_ID, bookId)
      }
      return EditCoverDialogController(args).apply {
        targetController = target
      }
    }
  }
}
