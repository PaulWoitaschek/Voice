package de.ph1b.audiobook.features.audio

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.misc.DialogController
import de.ph1b.audiobook.misc.DialogLayoutContainer
import de.ph1b.audiobook.misc.getUUID
import de.ph1b.audiobook.misc.progressChangedStream
import de.ph1b.audiobook.misc.putUUID
import de.ph1b.audiobook.playback.PlayerController
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.loudness.*
import org.koin.standalone.inject
import java.text.DecimalFormat
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Dialog for controlling the loudness.
 */
class LoudnessDialog(args: Bundle) : DialogController(args) {

  private val repo: BookRepository by inject()
  private val player: PlayerController by inject()

  private val dbFormat = DecimalFormat("0.0 dB")

  @SuppressLint("InflateParams", "CheckResult")
  override fun onCreateDialog(savedViewState: Bundle?): Dialog {

    val container = DialogLayoutContainer(
      activity!!.layoutInflater.inflate(R.layout.loudness, null, false)
    )

    val bookId = args.getUUID(NI_BOOK_ID)
    val book = repo.bookById(bookId)
      ?: return MaterialDialog.Builder(activity!!).build()

    container.seekBar.max = LoudnessGain.MAX_MB
    container.seekBar.progress = book.content.loudnessGain
    container.seekBar.progressChangedStream()
      .throttleLast(200, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
      .subscribe {
        player.setLoudnessGain(it)
        container.currentValue.text = format(it)
      }

    container.currentValue.text = format(book.content.loudnessGain)
    container.maxValue.text = format(container.seekBar.max)

    return MaterialDialog.Builder(activity!!)
      .title(R.string.volume_boost)
      .customView(container.containerView, true)
      .build()
  }

  private fun format(milliDb: Int) = dbFormat.format(milliDb / 100.0)

  companion object {
    private const val NI_BOOK_ID = "ni#bookId"
    operator fun invoke(bookId: UUID) = LoudnessDialog(
      Bundle().apply {
        putUUID(NI_BOOK_ID, bookId)
      }
    )
  }
}
