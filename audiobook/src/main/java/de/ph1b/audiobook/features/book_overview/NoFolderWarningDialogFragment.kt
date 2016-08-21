package de.ph1b.audiobook.features.book_overview

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.folder_overview.FolderOverviewActivity

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
                .onPositive { materialDialog, dialogAction ->
                    startActivity(Intent(context, FolderOverviewActivity::class.java))
                }
                .cancelable(false)
                .build()
    }

    companion object {
        val TAG: String = NoFolderWarningDialogFragment::class.java.simpleName
    }
}