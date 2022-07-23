package voice.sleepTimer

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.squareup.anvil.annotations.ContributesTo
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import voice.common.AppScope
import voice.common.BookId
import voice.common.conductor.DialogController
import voice.common.rootComponentAs
import voice.data.getBookId
import voice.data.putBookId
import voice.sleepTimer.databinding.DialogSleepBinding
import javax.inject.Inject

private const val NI_BOOK_ID = "ni#bookId"

class SleepTimerDialogController(bundle: Bundle) : DialogController(bundle) {

  constructor(bookId: BookId) : this(
    Bundle().apply {
      putBookId(NI_BOOK_ID, bookId)
    }
  )

  @Inject
  lateinit var viewModel: SleepTimerDialogViewModel

  init {
    rootComponentAs<Component>().inject(this)
  }

  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    val binding = DialogSleepBinding.inflate(activity!!.layoutInflater)

    listOf(
      binding.zero,
      binding.one,
      binding.two,
      binding.three,
      binding.four,
      binding.five,
      binding.six,
      binding.seven,
      binding.eight,
      binding.nine,
    ).forEachIndexed { index, textView ->
      textView.setOnClickListener {
        viewModel.onNumberClicked(index)
      }
    }

    binding.delete.setOnClickListener {
      viewModel.onNumberDeleteClicked()
    }
    binding.delete.setOnLongClickListener {
      viewModel.onNumberDeleteLongClicked()
      true
    }

    lifecycleScope.launch {
      viewModel.viewState().collectLatest { viewState ->
        binding.time.text = activity!!.getString(R.string.min, viewState.selectedMinutes.toString())

        if (viewState.showFab) {
          binding.fab.show()
        } else {
          binding.fab.hide()
        }
      }
    }
    binding.fab.setOnClickListener {
      viewModel.onConfirmButtonClicked(args.getBookId(NI_BOOK_ID)!!)
      dismissDialog()
    }

    return BottomSheetDialog(activity!!).apply {
      setContentView(binding.root)
      // hide the background so the fab appears overlapping
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
