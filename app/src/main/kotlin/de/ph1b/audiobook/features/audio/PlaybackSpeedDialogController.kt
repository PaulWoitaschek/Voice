package de.ph1b.audiobook.features.audio

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import androidx.datastore.core.DataStore
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.common.conductor.DialogController
import de.ph1b.audiobook.common.pref.CurrentBook
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.databinding.DialogAmountChooserBinding
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.progressChangedStream
import de.ph1b.audiobook.playback.PlayerController
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.DecimalFormat
import javax.inject.Inject

/**
 * Dialog for setting the playback speed of the current book.
 */
class PlaybackSpeedDialogController : DialogController() {

  @Inject
  lateinit var repo: BookRepository

  @field:[Inject CurrentBook]
  lateinit var currentBook: DataStore<Book.Id?>

  @Inject
  lateinit var playerController: PlayerController

  @SuppressLint("InflateParams", "SetTextI18n")
  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    appComponent.inject(this)

    val binding = DialogAmountChooserBinding.inflate(activity!!.layoutInflater)

    val book = runBlocking { currentBook.data.first()?.let { repo.flow(it).first() } }
      ?: error("Cannot instantiate ${javaClass.name} without a current book")
    val speed = book.content.playbackSpeed
    binding.seekBar.max = ((MAX - MIN) * FACTOR).toInt()
    binding.seekBar.progress = ((speed - MIN) * FACTOR).toInt()

    lifecycleScope.launch {
      binding.seekBar.progressChangedStream()
        .map { Book.SPEED_MIN + it.toFloat() / FACTOR }
        .onEach {
          // update speed text
          binding.textView.text = "${activity!!.getString(R.string.playback_speed)}: ${speedFormatter.format(it)}"
        }
        .debounce(50)
        .collect {
          playerController.setSpeed(it)
        }
    }

    return MaterialDialog(activity!!).apply {
      title(R.string.playback_speed)
      customView(view = binding.root, scrollable = true)
    }
  }

  companion object {
    private const val MAX = Book.SPEED_MAX
    private const val MIN = Book.SPEED_MIN
    private const val FACTOR = 100F
    private val speedFormatter = DecimalFormat("0.0 x")
  }
}
