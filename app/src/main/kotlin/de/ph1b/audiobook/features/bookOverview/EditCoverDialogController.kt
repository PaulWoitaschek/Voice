package de.ph1b.audiobook.features.bookOverview

import android.annotation.SuppressLint
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import com.bluelinelabs.conductor.Controller
import de.ph1b.audiobook.R
import de.ph1b.audiobook.common.ImageHelper
import de.ph1b.audiobook.common.conductor.DialogController
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.repo.BookRepo2
import de.ph1b.audiobook.databinding.DialogCoverEditBinding
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.conductor.context
import de.ph1b.audiobook.uitools.CropTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * Simple dialog to edit the cover of a book.
 */
class EditCoverDialogController(bundle: Bundle) : DialogController(bundle) {

  @Inject
  lateinit var repo: BookRepo2

  @Inject
  lateinit var imageHelper: ImageHelper

  @SuppressLint("InflateParams")
  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    appComponent.inject(this)

    val binding = DialogCoverEditBinding.inflate(activity!!.layoutInflater)
    // init values
    val arguments = args.getParcelable<Arguments>(NI_ARGS)!!

    binding.cropOverlay.selectionOn = false
    binding.coverImage.load(arguments.coverUri)

    val dialog = MaterialDialog(activity!!).apply {
      customView(view = binding.root)
      title(R.string.cover)
      positiveButton(R.string.dialog_confirm)
    }

    // use a click listener so the dialog stays open till the image was saved
    dialog.getActionButton(WhichButton.POSITIVE).setOnClickListener {
      val r = binding.cropOverlay.selectedRect
      if (!r.isEmpty) {
        lifecycleScope.launch {
          val bitmap = context.imageLoader
            .execute(
              ImageRequest.Builder(context)
                .data(arguments.coverUri)
                .transformations(CropTransformation(binding.cropOverlay, binding.coverImage))
                .build()
            )
            .drawable!!.toBitmap()
          val newCover = File(context.filesDir, UUID.randomUUID().toString() + ".webp")
          imageHelper.saveCover(bitmap, newCover)

          val oldCover = repo.flow(arguments.bookId).first()?.content?.cover
          if (oldCover != null) {
            withContext(Dispatchers.IO) {
              oldCover.delete()
            }
          }

          repo.updateBook(arguments.bookId) {
            it.copy(cover = newCover)
          }

          dismissDialog()
        }
      } else {
        dismissDialog()
      }
    }
    return dialog
  }

  interface Callback {
    fun onBookCoverChanged(bookId: UUID)
  }

  companion object {
    private const val NI_ARGS = "ni#bundle"

    operator fun <T> invoke(target: T, args: Arguments): EditCoverDialogController where T : Controller, T : Callback {
      val bundle = Bundle().apply {
        putParcelable(NI_ARGS, args)
      }
      return EditCoverDialogController(bundle).apply {
        targetController = target
      }
    }
  }

  @Parcelize
  data class Arguments(
    val coverUri: Uri,
    val bookId: Book2.Id
  ) : Parcelable
}
