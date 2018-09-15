package de.ph1b.audiobook.features.bookPlaying

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.data.repo.BookmarkRepo
import de.ph1b.audiobook.data.repo.internals.IO
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.misc.DialogLayoutContainer
import de.ph1b.audiobook.misc.getUUID
import de.ph1b.audiobook.misc.inflate
import de.ph1b.audiobook.misc.putUUID
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.ShakeDetector
import de.ph1b.audiobook.playback.SleepTimer
import kotlinx.android.synthetic.main.dialog_sleep.*
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.launch
import org.koin.android.ext.android.inject
import java.util.UUID

private const val NI_BOOK_ID = "ni#bookId"
private const val SI_MINUTES = "si#time"

/**
 * Simple dialog for activating the sleep timer
 */
class SleepTimerDialogFragment() : AppCompatDialogFragment() {

  @SuppressLint("ValidFragment")
  constructor(bookId: UUID) : this() {
    arguments = Bundle().apply {
      putUUID(NI_BOOK_ID, bookId)
    }
  }

  private val bookmarkRepo: BookmarkRepo by inject()
  private val sleepTimer: SleepTimer by inject()
  private val repo: BookRepository by inject()
  private val shakeDetector: ShakeDetector by inject()
  private val shakeToResetPref: Pref<Boolean> by inject(PrefKeys.SHAKE_TO_RESET)
  private val bookmarkOnSleepTimerPref: Pref<Boolean> by inject(PrefKeys.BOOKMARK_ON_SLEEP)
  private val sleepTimePref: Pref<Int> by inject(PrefKeys.SLEEP_TIME)

  private var _layoutContainer: DialogLayoutContainer? = null
  private val layoutContainer: DialogLayoutContainer get() = _layoutContainer!!
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
    layoutContainer.time.text = getString(R.string.min, selectedMinutes.toString())

    if (selectedMinutes > 0) layoutContainer.fab.show()
    else layoutContainer.fab.hide()
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    _layoutContainer = DialogLayoutContainer(
      activity!!.layoutInflater.inflate(R.layout.dialog_sleep)
    )

    // restore or get fresh
    selectedMinutes = savedInstanceState?.getInt(SI_MINUTES) ?: sleepTimePref.value
    updateTimeState()

    // find views and prepare clicks
    layoutContainer.one.setOnClickListener { appendNumber(1) }
    layoutContainer.two.setOnClickListener { appendNumber(2) }
    layoutContainer.three.setOnClickListener { appendNumber(3) }
    layoutContainer.four.setOnClickListener { appendNumber(4) }
    layoutContainer.five.setOnClickListener { appendNumber(5) }
    layoutContainer.six.setOnClickListener { appendNumber(6) }
    layoutContainer.seven.setOnClickListener { appendNumber(7) }
    layoutContainer.eight.setOnClickListener { appendNumber(8) }
    layoutContainer.nine.setOnClickListener { appendNumber(9) }
    layoutContainer.zero.setOnClickListener { appendNumber(0) }
    // upon delete remove the last number
    layoutContainer.delete.setOnClickListener {
      selectedMinutes /= 10
      updateTimeState()
    }
    // upon long click remove all numbers
    layoutContainer.delete.setOnLongClickListener {
      selectedMinutes = 0
      updateTimeState()
      true
    }

    val bookId = arguments!!.getUUID(NI_BOOK_ID)
    val book = repo.bookById(bookId) ?: return super.onCreateDialog(savedInstanceState)

    layoutContainer.fab.setOnClickListener {
      // should be hidden if
      require(selectedMinutes > 0) { "fab should be hidden when time is invalid" }
      sleepTimePref.value = selectedMinutes

      bookmarkOnSleepTimerPref.value = layoutContainer.bookmarkSwitch.isChecked
      if (bookmarkOnSleepTimerPref.value) {
        val date = DateUtils.formatDateTime(
          context,
          System.currentTimeMillis(),
          DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_NUMERIC_DATE
        )
        GlobalScope.launch(IO) {
          bookmarkRepo.addBookmarkAtBookPosition(
            book,
            date + ": " + getString(R.string.action_sleep)
          )
        }
      }

      shakeToResetPref.value = layoutContainer.shakeToResetSwitch.isChecked

      sleepTimer.setActive(true)
      dismiss()
    }

    // setup bookmark toggle
    layoutContainer.bookmarkSwitch.isChecked = bookmarkOnSleepTimerPref.value

    // setup shake to reset setting
    layoutContainer.shakeToResetSwitch.isChecked = shakeToResetPref.value
    val shakeSupported = shakeDetector.shakeSupported()
    if (!shakeSupported) {
      layoutContainer.shakeToResetSwitch.isVisible = false
    }

    return BottomSheetDialog(
      context!!,
      R.style.BottomSheetStyle
    ).apply {
      setContentView(layoutContainer.containerView)
      // hide the background so the fab looks overlapping
      setOnShowListener {
        val parentView = layoutContainer.containerView.parent as View
        parentView.background = null
        val coordinator = findViewById<FrameLayout>(R.id.design_bottom_sheet)
        val behavior = BottomSheetBehavior.from(coordinator)
        behavior.peekHeight = layoutContainer.time.bottom
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _layoutContainer = null
  }
}
