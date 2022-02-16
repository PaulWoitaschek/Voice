package voice.sleepTimer

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.squareup.anvil.annotations.ContributesTo
import de.paulwoitaschek.flowpref.Pref
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import voice.common.conductor.DialogController
import voice.common.pref.PrefKeys
import voice.core.AppScope
import voice.core.rootComponentAs
import voice.data.Book
import voice.data.getBookId
import voice.data.putBookId
import voice.data.repo.BookRepository
import voice.data.repo.BookmarkRepo
import voice.sleepTimer.databinding.DialogSleepBinding
import javax.inject.Inject
import javax.inject.Named

private const val NI_BOOK_ID = "ni#bookId"
private const val SI_MINUTES = "si#time"

/**
 * Simple dialog for activating the sleep timer
 */
class SleepTimerDialogController(bundle: Bundle) : DialogController(bundle) {

  constructor(bookId: Book.Id) : this(
    Bundle().apply {
      putBookId(NI_BOOK_ID, bookId)
    }
  )

  @Inject
  lateinit var bookmarkRepo: BookmarkRepo

  @Inject
  lateinit var sleepTimer: SleepTimer

  @Inject
  lateinit var bookRepo: BookRepository

  init {
    rootComponentAs<Component>().inject(this)
  }

  @field:[Inject Named(PrefKeys.SLEEP_TIME)]
  lateinit var sleepTimePref: Pref<Int>

  private var selectedMinutes = 0

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putInt(SI_MINUTES, selectedMinutes)
  }

  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    val binding = DialogSleepBinding.inflate(activity!!.layoutInflater)

    fun updateTimeState() {
      binding.time.text = activity!!.getString(R.string.min, selectedMinutes.toString())

      if (selectedMinutes > 0) binding.fab.show()
      else binding.fab.hide()
    }

    @SuppressLint("SetTextI18n")
    fun appendNumber(number: Int) {
      val newNumber = selectedMinutes * 10 + number
      if (newNumber > 999) return
      selectedMinutes = newNumber
      updateTimeState()
    }

    selectedMinutes = savedViewState?.getInt(SI_MINUTES) ?: sleepTimePref.value
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
    // upon delete remove the last number
    binding.delete.setOnClickListener {
      selectedMinutes /= 10
      updateTimeState()
    }
    // upon long click remove all numbers
    binding.delete.setOnLongClickListener {
      selectedMinutes = 0
      updateTimeState()
      true
    }


    binding.fab.setOnClickListener {
      require(selectedMinutes > 0) { "fab should be hidden when time is invalid" }
      sleepTimePref.value = selectedMinutes

      lifecycleScope.launch {
        val book = bookRepo.flow(args.getBookId(NI_BOOK_ID)!!).first() ?: return@launch
        bookmarkRepo.addBookmarkAtBookPosition(book = book,
          setBySleepTimer = true,
          title = null)
      }

      sleepTimer.setActive(true)
      dismissDialog()
    }

    return BottomSheetDialog(activity!!).apply {
      setContentView(binding.root)
      // hide the background so the fab looks overlapping
      setOnShowListener {
        val parentView = binding.root.parent as View
        parentView.background = null
        val coordinator = findViewById<FrameLayout>(R.id.design_bottom_sheet)!!
        val behavior = BottomSheetBehavior.from(coordinator)
        behavior.peekHeight = binding.time.bottom
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
      }
    }
  }

  @ContributesTo(AppScope::class)
  interface Component {
    fun inject(target: SleepTimerDialogController)
  }
}
