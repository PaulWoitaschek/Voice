package de.ph1b.audiobook.features.book_playing

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

class SeekDialogFragment : DialogFragment() {

  @Inject lateinit var prefs: PrefsManager

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    App.component.inject(this)

    // find views
    val view = context.layoutInflater().inflate(R.layout.dialog_amount_chooser, null)
    val seekBar: SeekBar = view.find(R.id.seekBar)
    val textView: TextView = view.find(R.id.textView)

    // init
    val oldSeekTime = prefs.seekTime.value()
    seekBar.max = (MAX - MIN) * FACTOR
    seekBar.onProgressChanged(initialNotification = true) {
      val value = it / FACTOR + MIN
      textView.text = context.resources.getQuantityString(R.plurals.seconds, value, value)
    }
    seekBar.progress = (oldSeekTime - MIN) * FACTOR

    return MaterialDialog.Builder(context)
      .title(R.string.pref_seek_time)
      .customView(view, true)
      .positiveText(R.string.dialog_confirm)
      .negativeText(R.string.dialog_cancel)
      .onPositive { materialDialog, dialogAction ->
        val newSeekTime = seekBar.progress / FACTOR + MIN
        prefs.seekTime.set(newSeekTime)
      }.build()
  }

  companion object {
    val TAG: String = SeekDialogFragment::class.java.simpleName

    private val FACTOR = 10
    private val MIN = 3
    private val MAX = 60
  }
}
