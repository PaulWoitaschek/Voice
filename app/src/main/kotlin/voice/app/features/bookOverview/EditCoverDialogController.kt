package voice.app.features.bookOverview

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
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import voice.app.R
import voice.app.databinding.DialogCoverEditBinding
import voice.app.injection.appComponent
import voice.app.misc.conductor.context
import voice.app.scanner.CoverSaver
import voice.app.uitools.CropTransformation
import voice.common.BookId
import voice.common.conductor.DialogController
import voice.data.repo.BookRepository
import javax.inject.Inject

private const val NI_ARGS = "ni#bundle"

class EditCoverDialogController(bundle: Bundle) : DialogController(bundle) {

  constructor(args: Arguments) : this(
    Bundle().apply {
      putParcelable(NI_ARGS, args)
    },
  )

  @Inject
  lateinit var repo: BookRepository

  @Inject
  lateinit var coverSaver: CoverSaver

  @SuppressLint("InflateParams")
  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    appComponent.inject(this)

    val binding = DialogCoverEditBinding.inflate(activity!!.layoutInflater)
    // init values
    val arguments = args.getParcelable<Arguments>(NI_ARGS)!!

    binding.cropOverlay.selectionOn = true
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
                .build(),
            )
            .drawable!!.toBitmap()

          coverSaver.save(arguments.bookId, bitmap)

          dismissDialog()
        }
      } else {
        dismissDialog()
      }
    }
    return dialog
  }

  @Parcelize
  data class Arguments(
    val coverUri: Uri,
    val bookId: BookId,
  ) : Parcelable
}
