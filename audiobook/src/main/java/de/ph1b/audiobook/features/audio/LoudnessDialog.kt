package de.ph1b.audiobook.features.audio

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.SeekBar
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.DialogController
import de.ph1b.audiobook.misc.find
import de.ph1b.audiobook.misc.progressChangedStream
import java.text.DecimalFormat
import javax.inject.Inject

/**
 * Dialog for controlling the loudness.
 *
 * @author Paul Woitaschek
 */
class LoudnessDialog : DialogController() {

  @Inject lateinit var loudnessGain: LoudnessGain
  private val dbFormat = DecimalFormat("0.0 dB")

  @SuppressLint("InflateParams")
  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    App.component.inject(this)

    val view = LayoutInflater.from(activity).inflate(R.layout.loudness, null, false)
    val currentText = view.find<TextView>(R.id.currentValue)
    val maxValue = view.find<TextView>(R.id.maxValue)
    val seekBar = view.find<SeekBar>(R.id.seekBar)

    seekBar.max = LoudnessGain.MAX_MB
    seekBar.progress = loudnessGain.gainmB
    seekBar.progressChangedStream()
        .subscribe {
          loudnessGain.gainmB = it
          currentText.text = format(loudnessGain.gainmB)
        }

    currentText.text = format(loudnessGain.gainmB)
    maxValue.text = format(seekBar.max)

    return MaterialDialog.Builder(activity!!)
        .title(R.string.loudness)
        .customView(view, true)
        .build()
  }

  private fun format(milliDb: Int) = dbFormat.format(milliDb / 100.0)
}
