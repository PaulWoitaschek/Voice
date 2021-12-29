package voice.loudness

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.squareup.anvil.annotations.ContributesTo
import de.ph1b.audiobook.AppScope
import de.ph1b.audiobook.common.conductor.DialogController
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.playback.player.LoudnessGain
import de.ph1b.audiobook.rootComponentAs
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import voice.loudness.databinding.LoudnessBinding
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

  init {
    rootComponentAs<Component>().inject(this)
  }

  private val dbFormat = DecimalFormat("0.0 dB")

  @SuppressLint("InflateParams")
  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    val binding = LoudnessBinding.inflate(activity!!.layoutInflater)
    val bookId = args.getSerializable(NI_BOOK_ID) as UUID
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
        putSerializable(NI_BOOK_ID, bookId)
      }
    )
  }

  @ContributesTo(AppScope::class)
  interface Component {
    fun inject(target: LoudnessDialog)
  }
}
