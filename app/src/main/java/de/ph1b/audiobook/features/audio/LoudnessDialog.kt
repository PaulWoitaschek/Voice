package de.ph1b.audiobook.features.audio

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.common.conductor.DialogController
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.databinding.LoudnessBinding
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.getUUID
import de.ph1b.audiobook.misc.progressChangedStream
import de.ph1b.audiobook.misc.putUUID
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.playback.player.LoudnessGain
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.UUID
import javax.inject.Inject

/**
 * Dialog for controlling the loudness.
 */
class LoudnessDialog(args: Bundle) : DialogController(args) {

  @Inject
  lateinit var repo: BookRepository
  @Inject
  lateinit var player: PlayerController

  private val dbFormat = DecimalFormat("0.0 dB")

  @SuppressLint("InflateParams")
  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    appComponent.inject(this)

    val binding = LoudnessBinding.inflate(activity!!.layoutInflater)
    val bookId = args.getUUID(NI_BOOK_ID)
    val book = repo.bookById(bookId)
      ?: return emptyDialog()

    binding.seekBar.max = LoudnessGain.MAX_MB
    binding.seekBar.progress = book.content.loudnessGain
    lifecycleScope.launch {
      binding.seekBar.progressChangedStream()
        .onEach { binding.currentValue.text = format(it) }
        .debounce(100L)
        .collect {
          player.setLoudnessGain(it)
        }
    }

    binding.currentValue.text = format(book.content.loudnessGain)
    binding.maxValue.text = format(binding.seekBar.max)

    return MaterialDialog(activity!!).apply {
      title(R.string.volume_boost)
      customView(view = binding.root, scrollable = true)
    }
  }

  private fun emptyDialog(): Dialog {
    return MaterialDialog(activity!!)
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
