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
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import voice.app.databinding.DialogCoverEditBinding
import voice.app.injection.appGraph
import voice.app.misc.conductor.context
import voice.app.scanner.CoverSaver
import voice.app.uitools.CropTransformation
import voice.common.BookId
import voice.common.conductor.DialogController
import voice.common.parcelable
import voice.data.repo.BookRepository
import voice.strings.R as StringsR

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
    appGraph.inject(this)

    val binding = DialogCoverEditBinding.inflate(activity!!.layoutInflater)
    val arguments = args.parcelable<Arguments>(NI_ARGS)!!

    binding.cropOverlay.selectionOn = true
    binding.coverImage.load(arguments.coverUri)

    val dialog = MaterialDialog(activity!!).apply {
      customView(view = binding.root)
      title(StringsR.string.cover)
      positiveButton(StringsR.string.dialog_confirm)
    }

    // use a click listener so the dialog stays open till the image was saved
    dialog.getActionButton(WhichButton.POSITIVE).setOnClickListener {
      val r = binding.cropOverlay.selectedRect
      if (!r.isEmpty) {
        onCreateViewScope?.launch {
          val bitmap = context.imageLoader
            .execute(
              ImageRequest.Builder(context)
                .data(arguments.coverUri)
                .transformations(CropTransformation(binding.cropOverlay, binding.coverImage))
                .build(),
            )
            .drawable?.toBitmap()

          if (bitmap != null) {
            coverSaver.save(arguments.bookId, bitmap)
            dismissDialog()
          }
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
