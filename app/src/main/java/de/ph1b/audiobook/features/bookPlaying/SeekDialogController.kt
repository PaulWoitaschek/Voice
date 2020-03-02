package de.ph1b.audiobook.features.bookPlaying

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.databinding.DialogAmountChooserBinding
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.DialogController
import de.ph1b.audiobook.misc.onProgressChanged
import de.ph1b.audiobook.prefs.Pref
import de.ph1b.audiobook.prefs.PrefKeys
import javax.inject.Inject
import javax.inject.Named

class SeekDialogController : DialogController() {

  @field:[Inject Named(PrefKeys.SEEK_TIME)]
  lateinit var seekTimePref: Pref<Int>

  @SuppressLint("InflateParams")
  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    appComponent.inject(this)

    val binding = DialogAmountChooserBinding.inflate(activity!!.layoutInflater)

    // init
    val oldSeekTime = seekTimePref.value
    binding.seekBar.max = (MAX - MIN) * FACTOR
    binding.seekBar.onProgressChanged(initialNotification = true) {
      val value = it / FACTOR + MIN
      binding.textView.text =
        activity!!.resources.getQuantityString(R.plurals.seconds, value, value)
    }
    binding.seekBar.progress = (oldSeekTime - MIN) * FACTOR

    return MaterialDialog(activity!!).apply {
      title(R.string.pref_seek_time)
      customView(view = binding.root, scrollable = true)
      positiveButton(R.string.dialog_confirm) {
        val newSeekTime = binding.seekBar.progress / FACTOR + MIN
        seekTimePref.value = newSeekTime
      }
      negativeButton(R.string.dialog_cancel)
    }
  }

  companion object {
    private const val FACTOR = 10
    private const val MIN = 3
    private const val MAX = 60
  }
}
