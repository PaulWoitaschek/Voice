package voice.playbackScreen

import android.os.Bundle
import android.view.GestureDetector
import android.view.MenuItem
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import coil.load
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.squareup.anvil.annotations.ContributesTo
import kotlinx.coroutines.launch
import voice.common.AppScope
import voice.common.PlayPauseDrawableSetter
import voice.common.conductor.ViewBindingController
import voice.common.conductor.clearAfterDestroyView
import voice.common.formatTime
import voice.common.rootComponentAs
import voice.data.Book
import voice.data.getBookId
import voice.data.putBookId
import voice.logging.core.Logger
import voice.playbackScreen.databinding.BookPlayBinding
import voice.sleepTimer.SleepTimerDialogController
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.DurationUnit

private const val NI_BOOK_ID = "niBookId"

class BookPlayController(bundle: Bundle) : ViewBindingController<BookPlayBinding>(bundle, BookPlayBinding::inflate) {

  constructor(bookId: Book.Id) : this(Bundle().apply { putBookId(NI_BOOK_ID, bookId) })

  @Inject
  lateinit var viewModel: BookPlayViewModel

  private val bookId: Book.Id = bundle.getBookId(NI_BOOK_ID)!!
  private var coverLoaded = false

  private var sleepTimerItem: MenuItem by clearAfterDestroyView()
  private var skipSilenceItem: MenuItem by clearAfterDestroyView()

  private var playPauseDrawableSetter by clearAfterDestroyView<PlayPauseDrawableSetter>()

  init {
    rootComponentAs<Component>().inject(this)
    this.viewModel.bookId = bookId
  }

  override fun BookPlayBinding.onBindingCreated() {
    coverLoaded = false
    playPauseDrawableSetter = PlayPauseDrawableSetter(play)
    setupClicks()
    setupSlider()
    setupToolbar()
  }

  override fun BookPlayBinding.onAttach() {
    lifecycleScope.launch {
      this@BookPlayController.viewModel.viewState().collect {
        this@onAttach.render(it)
      }
    }
    lifecycleScope.launch {
      this@BookPlayController.viewModel.viewEffects.collect {
        handleViewEffect(it)
      }
    }
  }

  private fun handleViewEffect(effect: BookPlayViewEffect) {
    when (effect) {
      BookPlayViewEffect.BookmarkAdded -> {
        Snackbar.make(view!!, R.string.bookmark_added, Snackbar.LENGTH_SHORT)
          .show()
      }
      BookPlayViewEffect.ShowSleepTimeDialog -> {
        openSleepTimeDialog()
      }
    }
  }

  private fun BookPlayBinding.render(viewState: BookPlayViewState) {
    Logger.d("render $viewState")
    binding.title.text = viewState.title
    currentChapterText.text = viewState.chapterName
    currentChapterContainer.isVisible = viewState.chapterName != null
    previous.isVisible = viewState.showPreviousNextButtons
    next.isVisible = viewState.showPreviousNextButtons
    playedTime.text = formatTime(
      viewState.playedTime.inWholeMilliseconds,
      viewState.duration.inWholeMilliseconds
    )
    maxTime.text = formatTime(
      viewState.duration.inWholeMilliseconds,
      viewState.duration.inWholeMilliseconds
    )
    slider.valueTo = viewState.duration.toDouble(DurationUnit.MILLISECONDS).toFloat()
    if (!slider.isPressed) {
      slider.value = viewState.playedTime.coerceAtMost(viewState.duration)
        .toDouble(DurationUnit.MILLISECONDS).toFloat()
    }
    skipSilenceItem.isChecked = viewState.skipSilence
    playPauseDrawableSetter.setPlaying(viewState.playing)
    showLeftSleepTime(this, viewState.sleepTime)

    if (!coverLoaded) {
      coverLoaded = true
      val coverFile = viewState.cover
      cover.load(coverFile) {
        fallback(R.drawable.album_art)
        error(R.drawable.album_art)
      }
    }
  }

  private fun setupClicks() {
    binding.play.setOnClickListener { viewModel.playPause() }
    binding.rewind.setOnClickListener { viewModel.rewind() }
    binding.fastForward.setOnClickListener { viewModel.fastForward() }
    binding.previous.setOnClickListener { viewModel.previous() }
    binding.next.setOnClickListener { viewModel.next() }
    binding.currentChapterContainer.setOnClickListener {
      viewModel.onCurrentChapterClicked()
    }

    val detector = GestureDetectorCompat(
      activity!!,
      object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent?): Boolean {
          viewModel.playPause()
          return true
        }
      }
    )
    binding.cover.isClickable = true
    @Suppress("ClickableViewAccessibility")
    binding.cover.setOnTouchListener { _, event ->
      detector.onTouchEvent(event)
    }
  }

  private fun setupSlider() {
    binding.slider.setLabelFormatter {
      formatTime(it.toLong(), binding.slider.valueTo.toLong())
    }
    binding.slider.addOnChangeListener { slider, value, fromUser ->
      if (isAttached && !fromUser) {
        binding.playedTime.text = formatTime(value.toLong(), slider.valueTo.toLong())
      }
    }
    binding.slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
      override fun onStartTrackingTouch(slider: Slider) {
      }

      override fun onStopTrackingTouch(slider: Slider) {
        val progress = slider.value.toLong()
        this@BookPlayController.viewModel.seekTo(progress)
      }
    })
  }

  private fun setupToolbar() {
    val menu = binding.toolbar.menu

    sleepTimerItem = menu.findItem(R.id.action_sleep)

    skipSilenceItem = menu.findItem(R.id.action_skip_silence)

    binding.toolbar.setNavigationOnClickListener { router.popController(this) }
    binding.toolbar.setOnMenuItemClickListener {
      when (it.itemId) {
        R.id.action_sleep -> {
          viewModel.toggleSleepTimer()
          true
        }
        R.id.action_time_lapse -> {
          viewModel.onPlaybackSpeedIconClicked()
          true
        }
        R.id.action_bookmark -> {
          viewModel.onBookmarkClicked()
          true
        }
        R.id.action_skip_silence -> {
          this.viewModel.toggleSkipSilence()
          true
        }
        else -> false
      }
    }
  }

  private fun showLeftSleepTime(binding: BookPlayBinding, duration: Duration) {
    val active = duration > Duration.ZERO
    sleepTimerItem.setIcon(if (active) R.drawable.alarm_off else R.drawable.alarm)
    binding.timerCountdownView.text = formatTime(duration.inWholeMilliseconds, duration.inWholeMilliseconds)
    binding.timerCountdownView.isVisible = active
  }

  private fun openSleepTimeDialog() {
    SleepTimerDialogController(bookId)
      .showDialog(router)
  }

  @ContributesTo(AppScope::class)
  interface Component {
    fun inject(target: BookPlayController)
  }
}
