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
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.playback.PlayerController
import io.reactivex.android.schedulers.AndroidSchedulers
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Dialog for controlling the loudness.
 *
 * @author Paul Woitaschek
 */
class LoudnessDialog(args: Bundle) : DialogController(args) {

  @Inject lateinit var repo: BookRepository
  @Inject lateinit var player: PlayerController

  private val dbFormat = DecimalFormat("0.0 dB")

  @SuppressLint("InflateParams")
  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    App.component.inject(this)

    val bookId = args.getLong(NI_BOOK_ID)
    val book = repo.bookById(bookId)
        ?: return MaterialDialog.Builder(activity!!).build()

    val view = LayoutInflater.from(activity).inflate(R.layout.loudness, null, false)
    val currentText = view.find<TextView>(R.id.currentValue)
    val maxValue = view.find<TextView>(R.id.maxValue)
    val seekBar = view.find<SeekBar>(R.id.seekBar)

    seekBar.max = LoudnessGain.MAX_MB
    seekBar.progress = book.loudnessGain
    seekBar.progressChangedStream()
        .throttleLast(200, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
        .subscribe {
          player.setLoudnessGain(it)
          currentText.text = format(it)
        }

    currentText.text = format(book.loudnessGain)
    maxValue.text = format(seekBar.max)

    return MaterialDialog.Builder(activity!!)
        .title(R.string.volume_boost)
        .customView(view, true)
        .build()
  }

  private fun format(milliDb: Int) = dbFormat.format(milliDb / 100.0)

  companion object {
    private const val NI_BOOK_ID = "ni#bookId"
    operator fun invoke(bookId: Long) = LoudnessDialog(Bundle().apply {
      putLong(NI_BOOK_ID, bookId)
    })
  }
}
