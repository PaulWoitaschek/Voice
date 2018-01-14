package de.ph1b.audiobook.features.bookPlaying

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialog
import android.support.v7.app.AppCompatDialogFragment
import android.text.format.DateUtils
import android.view.View
import android.widget.FrameLayout
import dagger.android.support.AndroidSupportInjection
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.data.repo.BookmarkRepo
import de.ph1b.audiobook.data.repo.internals.IO
import de.ph1b.audiobook.databinding.DialogSleepBinding
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.ShakeDetector
import de.ph1b.audiobook.playback.SleepTimer
import de.ph1b.audiobook.uitools.visible
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject
import javax.inject.Named

private const val NI_BOOK_ID = "ni#bookId"
private const val SI_MINUTES = "si#time"

/**
 * Simple dialog for activating the sleep timer
 */
class SleepTimerDialogFragment() : AppCompatDialogFragment() {

  @SuppressLint("ValidFragment")
  constructor(bookId: Long) : this() {
    arguments = Bundle().apply {
      putLong(NI_BOOK_ID, bookId)
    }
  }

  @Inject lateinit var bookmarkRepo: BookmarkRepo
  @Inject lateinit var sleepTimer: SleepTimer
  @Inject lateinit var repo: BookRepository
  @Inject lateinit var shakeDetector: ShakeDetector
  @field:[Inject Named(PrefKeys.SHAKE_TO_RESET)]
  lateinit var shakeToResetPref: Pref<Boolean>
  @field:[Inject Named(PrefKeys.BOOKMARK_ON_SLEEP)]
  lateinit var bookmarkOnSleepTimerPref: Pref<Boolean>
  @field:[Inject Named(PrefKeys.SLEEP_TIME)]
  lateinit var sleepTimePref: Pref<Int>

  private val binding: DialogSleepBinding get() = _binding!!
  private var _binding: DialogSleepBinding? = null
  private var selectedMinutes = 0

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putInt(SI_MINUTES, selectedMinutes)
  }

  @SuppressLint("SetTextI18n")
  private fun appendNumber(number: Int) {
    val newNumber = selectedMinutes * 10 + number
    if (newNumber > 999) return
    selectedMinutes = newNumber
    updateTimeState()
  }

  private fun updateTimeState() {
    binding.time.text = getString(R.string.min, selectedMinutes.toString())

    if (selectedMinutes > 0) binding.fab.show()
    else binding.fab.hide()
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    AndroidSupportInjection.inject(this)

    _binding = DialogSleepBinding.inflate(activity!!.layoutInflater)

    // restore or get fresh
    selectedMinutes = savedInstanceState?.getInt(SI_MINUTES) ?: sleepTimePref.value
    updateTimeState()

    // find views and prepare clicks
    binding.one.setOnClickListener { appendNumber(1) }
    binding.two.setOnClickListener { appendNumber(2) }
    binding.three.setOnClickListener { appendNumber(3) }
    binding.four.setOnClickListener { appendNumber(4) }
    binding.five.setOnClickListener { appendNumber(5) }
    binding.six.setOnClickListener { appendNumber(6) }
    binding.seven.setOnClickListener { appendNumber(7) }
    binding.eight.setOnClickListener { appendNumber(8) }
    binding.nine.setOnClickListener { appendNumber(9) }
    binding.zero.setOnClickListener { appendNumber(0) }
    val delete = binding.root.findViewById<View>(R.id.delete)
    // upon delete remove the last number
    delete.setOnClickListener {
      selectedMinutes /= 10
      updateTimeState()
    }
    // upon long click remove all numbers
    delete.setOnLongClickListener {
      selectedMinutes = 0
      updateTimeState()
      true
    }

    val bookId = arguments!!.getLong(NI_BOOK_ID)
    val book = repo.bookById(bookId) ?: return super.onCreateDialog(savedInstanceState)

    binding.fab.setOnClickListener {
      // should be hidden if
      require(selectedMinutes > 0) { "fab should be hidden when time is invalid" }
      sleepTimePref.value = selectedMinutes

      bookmarkOnSleepTimerPref.value = binding.bookmarkSwitch.isChecked
      if (bookmarkOnSleepTimerPref.value) {
        val date = DateUtils.formatDateTime(
            context,
            System.currentTimeMillis(),
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_NUMERIC_DATE
        )
        launch(IO) {
          bookmarkRepo.addBookmarkAtBookPosition(book, date + ": " + getString(R.string.action_sleep))
        }
      }

      shakeToResetPref.value = binding.shakeToResetSwitch.isChecked

      sleepTimer.setActive(true)
      dismiss()
    }

    // setup bookmark toggle
    binding.bookmarkSwitch.isChecked = bookmarkOnSleepTimerPref.value

    // setup shake to reset setting
    binding.shakeToResetSwitch.isChecked = shakeToResetPref.value
    val shakeSupported = shakeDetector.shakeSupported()
    if (!shakeSupported) {
      binding.shakeToResetSwitch.visible = false
    }

    return BottomSheetDialog(context!!, R.style.BottomSheetStyle).apply {
      setContentView(binding.root)
      // hide the background so the fab looks overlapping
      setOnShowListener {
        val parentView = binding.root.parent as View
        parentView.background = null
        val coordinator = findViewById<FrameLayout>(R.id.design_bottom_sheet)
        val behavior = BottomSheetBehavior.from(coordinator)
        behavior.peekHeight = binding.time.bottom
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
