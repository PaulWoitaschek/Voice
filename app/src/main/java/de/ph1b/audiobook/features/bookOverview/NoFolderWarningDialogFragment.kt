package de.ph1b.audiobook.features.bookOverview

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R

/**
 * Dialog that shows a warning
 */
class NoFolderWarningDialogFragment : DialogFragment() {

  init {
    isCancelable = false
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return MaterialDialog.Builder(context)
        .title(R.string.no_audiobook_folders_title)
        .content(getString(R.string.no_audiobook_folders_summary_start) +
            "\n\n" + getString(R.string.no_audiobook_folders_end))
        .positiveText(R.string.dialog_confirm)
        .onPositive { _, _ ->
          (activity as Callback).onNoFolderWarningConfirmed()
        }
        .cancelable(false)
        .build()
  }

  companion object {
    val TAG: String = NoFolderWarningDialogFragment::class.java.simpleName
  }

  interface Callback {
    fun onNoFolderWarningConfirmed()
  }
}
