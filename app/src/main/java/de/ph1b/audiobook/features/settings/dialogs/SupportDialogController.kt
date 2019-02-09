package de.ph1b.audiobook.features.settings.dialogs

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import de.ph1b.audiobook.R
import de.ph1b.audiobook.misc.DialogController
import timber.log.Timber

private val GITHUB_URL = "https://github.com/PaulWoitaschek/Voice".toUri()
private val TRANSLATION_URL = "https://www.transifex.com/projects/p/voice".toUri()

class SupportDialogController : DialogController() {

  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    return MaterialDialog(activity!!).apply {
      title(R.string.pref_support_title)
      listItems(R.array.pref_support_values) { _, index, _ ->
        when (index) {
          0 -> visitUri(GITHUB_URL)
          1 -> visitUri(TRANSLATION_URL)
          else -> error("Invalid index $index")
        }
      }
    }
  }

  private fun visitUri(uri: Uri) {
    try {
      startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (exception: ActivityNotFoundException) {
      Timber.e(exception)
    }
  }
}
