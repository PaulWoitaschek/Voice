package de.ph1b.audiobook.features.bookPlaying

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.misc.DialogLayoutContainer
import de.ph1b.audiobook.misc.inflate
import de.ph1b.audiobook.misc.onProgressChanged
import de.ph1b.audiobook.persistence.pref.Pref
import kotlinx.android.synthetic.main.dialog_amount_chooser.*
import org.koin.android.ext.android.inject

class SeekDialogFragment : DialogFragment() {

  private val seekTimePref: Pref<Int> by inject(PrefKeys.SEEK_TIME)

  @SuppressLint("InflateParams")
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val container =
      DialogLayoutContainer(activity!!.layoutInflater.inflate(R.layout.dialog_amount_chooser))

    // init
    val oldSeekTime = seekTimePref.value
    container.seekBar.max = (MAX - MIN) * FACTOR
    container.seekBar.onProgressChanged(initialNotification = true) {
      val value = it / FACTOR + MIN
      container.textView.text =
          context!!.resources.getQuantityString(R.plurals.seconds, value, value)
    }
    container.seekBar.progress = (oldSeekTime - MIN) * FACTOR

    return MaterialDialog.Builder(context!!)
      .title(R.string.pref_seek_time)
      .customView(container.containerView, true)
      .positiveText(R.string.dialog_confirm)
      .negativeText(R.string.dialog_cancel)
      .onPositive { _, _ ->
        val newSeekTime = container.seekBar.progress / FACTOR + MIN
        seekTimePref.value = newSeekTime
      }.build()
  }

  companion object {
    val TAG: String = SeekDialogFragment::class.java.simpleName

    private const val FACTOR = 10
    private const val MIN = 3
    private const val MAX = 60
  }
}
