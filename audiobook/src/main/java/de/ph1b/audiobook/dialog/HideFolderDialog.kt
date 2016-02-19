package de.ph1b.audiobook.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import e
import i

import java.io.File
import java.io.IOException

/**
 * A dialog giving the option to hide the selected book from other players.
 *
 * @author Paul Woitaschek
 */
class HideFolderDialog : DialogFragment() {

    private lateinit var callback: OnChosenListener

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        callback = context as OnChosenListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val pathToHide = arguments.getString(PATH_TO_HIDE)!!
        val hideFile = getNoMediaFileByFolder(File(pathToHide))
        return MaterialDialog.Builder(activity)
                .title(R.string.hide_folder_title)
                .content(R.string.hide_folder_content)
                .positiveText(R.string.hide_confirm)
                .negativeText(R.string.dialog_cancel)
                .onPositive { materialDialog, dialogAction ->
                    try {
                        i { "Create new File will be called." }
                        //noinspection ResultOfMethodCallIgnored
                        hideFile.createNewFile()
                    } catch (ex: IOException) {
                        e(ex) { "Error at creating the hide-file" }
                    }
                }
                .onAny { materialDialog, dialogAction -> callback.onChosen() }
                .build()
    }


    interface OnChosenListener {
        fun onChosen()
    }

    companion object {

        private val PATH_TO_HIDE = "pathToHide"

        /**
         * Returns a file that called .nomedia that prevents music players from recognizing the book as
         * music.

         * @param folder The folder
         * *
         * @return The file that provides the hiding
         */
        fun getNoMediaFileByFolder(folder: File): File {
            return File(folder, ".nomedia")
        }

        fun newInstance(pathToHide: File): HideFolderDialog {
            val args = Bundle()
            args.putString(PATH_TO_HIDE, pathToHide.absolutePath)

            val hideFolderDialog = HideFolderDialog()
            hideFolderDialog.arguments = args
            return hideFolderDialog
        }

        val TAG = HideFolderDialog::class.java.simpleName
    }
}
