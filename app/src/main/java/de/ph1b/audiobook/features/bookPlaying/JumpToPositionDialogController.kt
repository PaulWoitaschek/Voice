package de.ph1b.audiobook.features.bookPlaying

import android.app.Dialog
import android.os.Bundle
import androidx.core.view.isVisible
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.databinding.DialogTimePickerBinding
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.DialogController
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.prefs.Pref
import de.ph1b.audiobook.prefs.PrefKeys
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

class JumpToPositionDialogController : DialogController() {

  @field:[Inject Named(PrefKeys.CURRENT_BOOK)]
  lateinit var currentBookIdPref: Pref<UUID>
  @Inject
  lateinit var repo: BookRepository
  @Inject
  lateinit var playerController: PlayerController

  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    appComponent.inject(this)

    val binding = DialogTimePickerBinding.inflate(activity!!.layoutInflater)

    // init
    val book = repo.bookById(currentBookIdPref.value)!!
    val duration = book.content.currentChapter.duration
    val position = book.content.positionInChapter
    val biggestHour = TimeUnit.MILLISECONDS.toHours(duration).toInt()
    val durationInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration).toInt()
    if (biggestHour == 0) {
      // sets visibility of hour related things to gone if max.hour is zero
      binding.colon.isVisible = false
      binding.numberHour.isVisible = false
    }

    // set maximum values
    binding.numberHour.maxValue = biggestHour
    if (biggestHour == 0) {
      binding.numberMinute.maxValue = TimeUnit.MILLISECONDS.toMinutes(duration).toInt()
    } else {
      binding.numberMinute.maxValue = 59
    }

    // set default values
    val defaultHour = TimeUnit.MILLISECONDS.toHours(position).toInt()
    val defaultMinute = TimeUnit.MILLISECONDS.toMinutes(position).toInt() % 60
    binding.numberHour.value = defaultHour
    binding.numberMinute.value = defaultMinute

    binding.numberHour.setOnValueChangedListener { _, _, newVal ->
      if (newVal == biggestHour) {
        binding.numberMinute.maxValue = (durationInMinutes - newVal * 60) % 60
      } else {
        binding.numberMinute.maxValue = 59
      }
    }

    binding.numberMinute.setOnValueChangedListener { _, oldVal, newVal ->
      var hValue = binding.numberHour.value

      // scrolling forward
      if (oldVal == 59 && newVal == 0) {
        binding.numberHour.value = ++hValue
      }
      // scrolling backward
      if (oldVal == 0 && newVal == 59) {
        binding.numberHour.value = --hValue
      }
    }

    return MaterialDialog(activity!!).apply {
      customView(view = binding.root, scrollable = true)
      title(R.string.action_time_change)
      positiveButton(R.string.dialog_confirm) {
        val h = binding.numberHour.value
        val m = binding.numberMinute.value
        val newPosition = (m + 60 * h) * 60 * 1000L
        playerController.setPosition(newPosition, book.content.currentChapter.file)
      }
      negativeButton(R.string.dialog_cancel)
    }
  }
}
