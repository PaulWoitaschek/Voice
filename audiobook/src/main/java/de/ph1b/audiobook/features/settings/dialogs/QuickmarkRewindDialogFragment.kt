package de.ph1b.audiobook.features.settings.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.widget.SeekBar
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.find
import de.ph1b.audiobook.misc.layoutInflater
import de.ph1b.audiobook.misc.onProgressChanged
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.PrefsManager
import javax.inject.Inject


class QuickmarkRewindDialogFragment : DialogFragment() {

  @Inject lateinit var prefs: PrefsManager

  @SuppressLint("InflateParams")
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    App.component.inject(this)

    // view binding
    val view = context.layoutInflater().inflate(R.layout.dialog_amount_chooser, null)
    val rewindText: TextView = view.find(R.id.textView)

    val oldRewindAmount = prefs.quickmarkRewindAmount.value
    val seekBar: SeekBar = view.find(R.id.seekBar)
    seekBar.max = (MAX - MIN) * FACTOR
    seekBar.progress = (oldRewindAmount - MIN) * FACTOR
    seekBar.onProgressChanged(initialNotification = true) {
      val progress = it / FACTOR
      val quickmarkRewindSummary = context.resources.getQuantityString(R.plurals.pref_quickmark_rewind_summary, progress, progress)
      rewindText.text = quickmarkRewindSummary
    }

    return MaterialDialog.Builder(context)
        .title(R.string.pref_quickmark_rewind_title)
        .customView(view, true)
        .positiveText(R.string.dialog_confirm)
        .negativeText(R.string.dialog_cancel)
        .onPositive { _, _ ->
          val newRewindAmount = seekBar.progress / FACTOR + MIN
          prefs.quickmarkRewindAmount.value = newRewindAmount
        }
        .build()
  }

  companion object {
    val TAG: String = QuickmarkRewindDialogFragment::class.java.simpleName

    private val MIN = 0
    private val MAX = 20
    private val FACTOR = 10
  }
}
