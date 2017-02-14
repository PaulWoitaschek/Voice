package de.ph1b.audiobook.features.bookPlaying

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatDialogFragment
import android.support.v7.widget.SwitchCompat
import android.text.format.DateUtils
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.find
import de.ph1b.audiobook.misc.layoutInflater
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.BookmarkProvider
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.Sandman
import de.ph1b.audiobook.playback.ShakeDetector
import de.ph1b.audiobook.uitools.visible
import e
import javax.inject.Inject

/**
 * Simple dialog for activating the sleep timer
 *
 * @author Paul Woitaschek
 */
class SleepTimerDialogFragment : AppCompatDialogFragment() {

  @Inject lateinit var bookmarkProvider: BookmarkProvider
  @Inject lateinit var prefs: PrefsManager
  @Inject lateinit var sandMan: Sandman
  @Inject lateinit var repo: BookRepository
  @Inject lateinit var shakeDetector: ShakeDetector

  private lateinit var time: TextView
  private lateinit var fab: FloatingActionButton
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
    time.text = getString(R.string.min, selectedMinutes.toString())

    if (selectedMinutes > 0) fab.show()
    else fab.hide()
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    App.component.inject(this)

    @SuppressLint("InflateParams")
    val layout = context.layoutInflater().inflate(R.layout.dialog_sleep, null)
    val bookmarkSwitch = layout.findViewById(R.id.bookmarkSwitch) as SwitchCompat
    val shakeToResetSwitch = layout.findViewById(R.id.shakeToResetSwitch) as SwitchCompat
    fab = layout.find(R.id.fab)
    time = layout.find(R.id.time)

    // restore or get fresh
    selectedMinutes = savedInstanceState?.getInt(SI_MINUTES) ?: prefs.sleepTime.get()!!
    updateTimeState()

    layout.findViewById(R.id.one).setOnClickListener { appendNumber(1) }
    layout.findViewById(R.id.two).setOnClickListener { appendNumber(2) }
    layout.findViewById(R.id.three).setOnClickListener { appendNumber(3) }
    layout.findViewById(R.id.four).setOnClickListener { appendNumber(4) }
    layout.findViewById(R.id.five).setOnClickListener { appendNumber(5) }
    layout.findViewById(R.id.six).setOnClickListener { appendNumber(6) }
    layout.findViewById(R.id.seven).setOnClickListener { appendNumber(7) }
    layout.findViewById(R.id.eight).setOnClickListener { appendNumber(8) }
    layout.findViewById(R.id.nine).setOnClickListener { appendNumber(9) }
    layout.findViewById(R.id.zero).setOnClickListener { appendNumber(0) }
    layout.findViewById(R.id.delete).setOnClickListener {
      selectedMinutes /= 10
      updateTimeState()
    }

    val bookId = arguments.getLong(NI_BOOK_ID)
    val book = repo.bookById(bookId)
    if (book == null) {
      e { "no book" }
      return super.onCreateDialog(savedInstanceState)
    }

    fab.setOnClickListener {
      // should be hidden if
      check(selectedMinutes > 0) { "fab should be hidden when time is invalid" }
      prefs.sleepTime.set(selectedMinutes)

      prefs.bookmarkOnSleepTimer.set(bookmarkSwitch.isChecked)
      if (prefs.bookmarkOnSleepTimer.value()) {
        val date = DateUtils.formatDateTime(context, System.currentTimeMillis(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_NUMERIC_DATE)
        bookmarkProvider.addBookmarkAtBookPosition(book, date + ": " + getString(R.string.action_sleep))
      }

      prefs.shakeToReset.set(shakeToResetSwitch.isChecked)

      sandMan.setActive(true)
      dismiss()
    }

    // setup bookmark toggle
    bookmarkSwitch.isChecked = prefs.bookmarkOnSleepTimer.value()

    // setup shake to reset setting
    shakeToResetSwitch.isChecked = prefs.shakeToReset.value()
    val shakeSupported = shakeDetector.shakeSupported()
    if (!shakeSupported) {
      shakeToResetSwitch.visible = false
    }

    return BottomSheetDialog(context, R.style.BottomSheetStyle).apply {
      setContentView(layout)
      // hide the background so the fab looks overlapping
      setOnShowListener {
        val parentView = layout.parent as View
        parentView.background = null
        val coordinator = this@apply.findViewById(R.id.design_bottom_sheet) as FrameLayout
        val behavior = BottomSheetBehavior.from(coordinator)
        behavior.peekHeight = time.bottom
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
      }
    }
  }

  companion object {
    private val NI_BOOK_ID = "ni#bookId"
    private val SI_MINUTES = "si#time"
    fun newInstance(book: Book) = SleepTimerDialogFragment().apply {
      arguments = Bundle().apply {
        putLong(NI_BOOK_ID, book.id)
      }
    }
  }
}