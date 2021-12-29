package de.ph1b.audiobook.features.bookPlaying

import android.os.Bundle
import android.view.GestureDetector
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.R
import de.ph1b.audiobook.databinding.BookPlayBinding
import de.ph1b.audiobook.features.ViewBindingController
import de.ph1b.audiobook.features.audio.PlaybackSpeedDialogController
import de.ph1b.audiobook.features.bookPlaying.selectchapter.SelectChapterDialog
import de.ph1b.audiobook.features.bookmarks.BookmarkController
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.CircleOutlineProvider
import de.ph1b.audiobook.misc.conductor.asTransaction
import de.ph1b.audiobook.misc.conductor.clearAfterDestroyView
import de.ph1b.audiobook.misc.formatTime
import de.ph1b.audiobook.misc.getUUID
import de.ph1b.audiobook.misc.putUUID
import de.ph1b.audiobook.playback.player.Equalizer
import de.ph1b.audiobook.uitools.PlayPauseDrawableSetter
import kotlinx.coroutines.launch
import timber.log.Timber
import voice.loudness.LoudnessDialog
import voice.playbackScreen.BookPlayViewEffect
import voice.playbackScreen.BookPlayViewModel
import voice.playbackScreen.BookPlayViewState
import voice.playbackScreen.JumpToPositionDialogController
import voice.settings.SettingsController
import voice.sleepTimer.SleepTimerDialogController
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.DurationUnit

private const val NI_BOOK_ID = "niBookId"

/**
 * Base class for book playing interaction.
 */
class BookPlayController(bundle: Bundle) : ViewBindingController<BookPlayBinding>(bundle, BookPlayBinding::inflate) {

  constructor(bookId: UUID) : this(Bundle().apply { putUUID(NI_BOOK_ID, bookId) })

  @Inject
  lateinit var equalizer: Equalizer

  @Inject
  lateinit var viewModel: BookPlayViewModel

  private val bookId = bundle.getUUID(NI_BOOK_ID)
  private var coverLoaded = false

  private var sleepTimerItem: MenuItem by clearAfterDestroyView()
  private var skipSilenceItem: MenuItem by clearAfterDestroyView()

  private var playPauseDrawableSetter by clearAfterDestroyView<PlayPauseDrawableSetter>()

  init {
    appComponent.inject(this)
    this.viewModel.bookId = bookId
  }

  override fun BookPlayBinding.onBindingCreated() {
    coverLoaded = false
    playPauseDrawableSetter = PlayPauseDrawableSetter(play)
    setupClicks()
    setupSlider()
    setupToolbar()
    play.apply {
      outlineProvider = CircleOutlineProvider()
      clipToOutline = true
    }
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
    Timber.d("render $viewState")
    toolbar.title = viewState.title
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
      slider.value = viewState.playedTime.toDouble(DurationUnit.MILLISECONDS).toFloat()
    }
    skipSilenceItem.isChecked = viewState.skipSilence
    playPauseDrawableSetter.setPlaying(viewState.playing)
    showLeftSleepTime(this, viewState.sleepTime)

    if (!coverLoaded) {
      coverLoaded = true
      val coverFile = viewState.cover.file(activity!!)
      val placeholder = viewState.cover.placeholder(activity!!)
      if (!coverFile.exists()) {
        Picasso.get().cancelRequest(cover)
        cover.setImageDrawable(placeholder)
      } else {
        Picasso.get().load(coverFile).placeholder(placeholder).into(cover)
      }
    }
  }

  private fun setupClicks() {
    binding.play.setOnClickListener { this.viewModel.playPause() }
    binding.rewind.setOnClickListener { this.viewModel.rewind() }
    binding.fastForward.setOnClickListener { this.viewModel.fastForward() }
    binding.playedTime.setOnClickListener { launchJumpToPositionDialog() }
    binding.previous.setOnClickListener { this.viewModel.previous() }
    binding.next.setOnClickListener { this.viewModel.next() }
    binding.currentChapterContainer.setOnClickListener {
      SelectChapterDialog(bookId).showDialog(router)
    }

    val detector = GestureDetectorCompat(
      activity,
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
    val equalizerItem = menu.findItem(R.id.action_equalizer)
    equalizerItem.isVisible = equalizer.exists

    skipSilenceItem = menu.findItem(R.id.action_skip_silence)

    binding.toolbar.findViewById<View>(R.id.action_bookmark)
      .setOnLongClickListener {
        this.viewModel.addBookmark()
        true
      }

    binding.toolbar.setNavigationOnClickListener { router.popController(this) }
    binding.toolbar.setOnMenuItemClickListener {
      when (it.itemId) {
        R.id.action_settings -> {
          router.pushController(SettingsController().asTransaction())
          true
        }
        R.id.action_time_change -> {
          launchJumpToPositionDialog()
          true
        }
        R.id.action_sleep -> {
          this.viewModel.toggleSleepTimer()
          true
        }
        R.id.action_time_lapse -> {
          PlaybackSpeedDialogController().showDialog(router)
          true
        }
        R.id.action_bookmark -> {
          val bookmarkController = BookmarkController(bookId)
            .asTransaction()
          router.pushController(bookmarkController)
          true
        }
        R.id.action_equalizer -> {
          equalizer.launch(activity!!)
          true
        }
        R.id.action_skip_silence -> {
          this.viewModel.toggleSkipSilence()
          true
        }
        R.id.loudness -> {
          LoudnessDialog(bookId).showDialog(router)
          true
        }
        else -> false
      }
    }
  }

  private fun launchJumpToPositionDialog() {
    JumpToPositionDialogController().showDialog(router)
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
}
