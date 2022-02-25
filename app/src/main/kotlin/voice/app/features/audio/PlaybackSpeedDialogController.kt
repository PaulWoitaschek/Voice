package voice.app.features.audio

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import androidx.datastore.core.DataStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import voice.app.R
import voice.app.databinding.DialogAmountChooserBinding
import voice.app.injection.appComponent
import voice.common.conductor.DialogController
import voice.common.pref.CurrentBook
import voice.data.Book
import voice.data.repo.BookRepository
import voice.playback.PlayerController
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

    val book = runBlocking { currentBook.data.first()?.let { repo.get(it) } }
      ?: error("Cannot instantiate ${javaClass.name} without a current book")
    val speed = book.content.playbackSpeed
    binding.slider.valueFrom = Book.SPEED_MIN
    binding.slider.valueTo = Book.SPEED_MAX
    binding.slider.addOnChangeListener(Slider.OnChangeListener { _, value, _ ->
      binding.textView.text = "${activity!!.getString(R.string.playback_speed)}: ${speedFormatter.format(value)}"
      playerController.setSpeed(value)
    })
    binding.slider.value = speed

    return MaterialAlertDialogBuilder(activity!!)
      .setTitle(R.string.playback_speed)
      .setView(binding.root)
      .create()
  }

  companion object {
    private val speedFormatter = DecimalFormat("0.0 x")
  }
}
