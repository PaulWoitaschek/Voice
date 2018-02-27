package de.ph1b.audiobook.features.bookPlaying

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import dagger.android.support.AndroidSupportInjection
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.databinding.DialogTimePickerBinding
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.uitools.visible
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

class JumpToPositionDialogFragment : DialogFragment() {

  @field:[Inject Named(PrefKeys.CURRENT_BOOK)]
  lateinit var currentBookIdPref: Pref<Long>
  @Inject
  lateinit var repo: BookRepository
  @Inject
  lateinit var playerController: PlayerController

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    AndroidSupportInjection.inject(this)
    val binding = DialogTimePickerBinding.inflate(activity!!.layoutInflater)

    // init
    val book = repo.bookById(currentBookIdPref.value)!!
    val duration = book.currentChapter.duration
    val position = book.positionInChapter
    val biggestHour = TimeUnit.MILLISECONDS.toHours(duration.toLong()).toInt()
    val durationInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration.toLong()).toInt()
    if (biggestHour == 0) {
      //sets visibility of hour related things to gone if max.hour is zero
      binding.colon.visible = false
      binding.numberHour.visible = false
    }

    //set maximum values
    binding.numberHour.maxValue = biggestHour
    if (biggestHour == 0) {
      binding.numberMinute.maxValue = TimeUnit.MILLISECONDS.toMinutes(duration.toLong()).toInt()
    } else {
      binding.numberMinute.maxValue = 59
    }

    //set default values
    val defaultHour = TimeUnit.MILLISECONDS.toHours(position.toLong()).toInt()
    val defaultMinute = TimeUnit.MILLISECONDS.toMinutes(position.toLong()).toInt() % 60
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

      //scrolling forward
      if (oldVal == 59 && newVal == 0) {
        binding.numberHour.value = ++hValue
      }
      //scrolling backward
      if (oldVal == 0 && newVal == 59) {
        binding.numberHour.value = --hValue
      }
    }

    return MaterialDialog.Builder(context!!)
      .customView(binding.root, true)
      .title(R.string.action_time_change)
      .onPositive { _, _ ->
        val h = binding.numberHour.value
        val m = binding.numberMinute.value
        val newPosition = (m + 60 * h) * 60 * 1000
        playerController.changePosition(newPosition, book.currentChapter.file)
      }
      .positiveText(R.string.dialog_confirm)
      .negativeText(R.string.dialog_cancel)
      .build()
  }

  companion object {

    val TAG: String = JumpToPositionDialogFragment::class.java.simpleName
  }
}
