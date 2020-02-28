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
import de.ph1b.audiobook.features.SyntheticViewController
import de.ph1b.audiobook.features.audio.LoudnessDialog
import de.ph1b.audiobook.features.bookPlaying.selectchapter.SelectChapterDialog
import de.ph1b.audiobook.features.bookmarks.BookmarkController
import de.ph1b.audiobook.features.settings.SettingsController
import de.ph1b.audiobook.features.settings.dialogs.PlaybackSpeedDialogController
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.CircleOutlineProvider
import de.ph1b.audiobook.misc.conductor.asTransaction
import de.ph1b.audiobook.misc.conductor.clearAfterDestroyView
import de.ph1b.audiobook.misc.formatTime
import de.ph1b.audiobook.misc.getUUID
import de.ph1b.audiobook.misc.putUUID
import de.ph1b.audiobook.playback.player.Equalizer
import de.ph1b.audiobook.uitools.PlayPauseDrawableSetter
import kotlinx.android.synthetic.main.book_play.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration

private const val NI_BOOK_ID = "niBookId"

/**
 * Base class for book playing interaction.
 */
class BookPlayController(bundle: Bundle) : SyntheticViewController(bundle) {

  constructor(bookId: UUID) : this(Bundle().apply { putUUID(NI_BOOK_ID, bookId) })

  @Inject
  lateinit var equalizer: Equalizer
  @Inject
  lateinit var viewModel: BookPlayViewModel

  private val bookId = bundle.getUUID(NI_BOOK_ID)
  private var coverLoaded = false

  override val layoutRes = R.layout.book_play

  private var sleepTimerItem: MenuItem by clearAfterDestroyView()
  private var skipSilenceItem: MenuItem by clearAfterDestroyView()

  private var playPauseDrawableSetter by clearAfterDestroyView<PlayPauseDrawableSetter>()

  init {
    appComponent.inject(this)
    this.viewModel.bookId = bookId
  }

  override fun onViewCreated() {
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

  override fun onAttach(view: View) {
    super.onAttach(view)

    lifecycleScope.launch {
      this@BookPlayController.viewModel.viewState().collect {
        render(it)
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

  private fun render(viewState: BookPlayViewState) {
    Timber.d("render $viewState")
    toolbar.title = viewState.title
    currentChapterText.text = viewState.chapterName
    currentChapterText.isVisible = viewState.chapterName != null
    previous.isVisible = viewState.showPreviousNextButtons
    next.isVisible = viewState.showPreviousNextButtons
    playedTime.text = formatTime(viewState.playedTime.toLongMilliseconds(), viewState.duration.toLongMilliseconds())
    maxTime.text = formatTime(viewState.duration.toLongMilliseconds(), viewState.duration.toLongMilliseconds())
    slider.valueTo = viewState.duration.inMilliseconds.toFloat()
    if (!slider.isPressed) {
      slider.value = viewState.playedTime.inMilliseconds.toFloat()
    }
    playPauseDrawableSetter.setPlaying(viewState.playing)
    showLeftSleepTime(viewState.sleepTime)

    if (!coverLoaded) {
      coverLoaded = true
      // we need to synchronously load this because the transition breaks otherwise
      runBlocking {
        val coverFile = viewState.cover.file()
        val placeholder = viewState.cover.placeholder(activity!!)
        if (coverFile == null) {
          Picasso.get().cancelRequest(cover)
          cover.setImageDrawable(placeholder)
        } else {
          Picasso.get().load(coverFile).placeholder(placeholder).into(cover)
        }
      }
    }
  }

  private fun setupClicks() {
    play.setOnClickListener { this.viewModel.playPause() }
    rewind.setOnClickListener { this.viewModel.rewind() }
    fastForward.setOnClickListener { this.viewModel.fastForward() }
    playedTime.setOnClickListener { launchJumpToPositionDialog() }
    previous.setOnClickListener { this.viewModel.previous() }
    next.setOnClickListener { this.viewModel.next() }
    currentChapterText.setOnClickListener {
      SelectChapterDialog(bookId).showDialog(router)
    }

    val detector = GestureDetectorCompat(activity, object : GestureDetector.SimpleOnGestureListener() {
      override fun onDoubleTap(e: MotionEvent?): Boolean {
        viewModel.playPause()
        return true
      }
    })
    cover.isClickable = true
    cover.setOnTouchListener { _, event ->
      detector.onTouchEvent(event)
    }
  }

  private fun setupSlider() {
    slider.setLabelFormatter {
      formatTime(it.toLong(), slider.valueTo.toLong())
    }
    slider.addOnChangeListener { slider, value, fromUser ->
      if (isAttached && !fromUser) {
        playedTime.text = formatTime(value.toLong(), slider.valueTo.toLong())
      }
    }
    slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
      override fun onStartTrackingTouch(slider: Slider) {
      }

      override fun onStopTrackingTouch(slider: Slider) {
        val progress = slider.value.toLong()
        this@BookPlayController.viewModel.seekTo(progress)
      }
    })
  }

  private fun setupToolbar() {
    val menu = toolbar.menu

    sleepTimerItem = menu.findItem(R.id.action_sleep)
    val equalizerItem = menu.findItem(R.id.action_equalizer)
    equalizerItem.isVisible = equalizer.exists

    skipSilenceItem = menu.findItem(R.id.action_skip_silence)

    toolbar.findViewById<View>(R.id.action_bookmark)
      .setOnLongClickListener {
        this.viewModel.addBookmark()
        true
      }

    toolbar.setNavigationOnClickListener { router.popController(this) }
    toolbar.setOnMenuItemClickListener {
      when (it.itemId) {
        R.id.action_settings -> {
          val transaction = SettingsController().asTransaction()
          router.pushController(transaction)
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

  private fun showLeftSleepTime(duration: Duration) {
    val active = duration > Duration.ZERO
    sleepTimerItem.setIcon(if (active) R.drawable.alarm_off else R.drawable.alarm)
    timerCountdownView.text = formatTime(duration.toLongMilliseconds(), duration.toLongMilliseconds())
    timerCountdownView.isVisible = active
  }

  private fun openSleepTimeDialog() {
    SleepTimerDialogFragment(bookId)
      .showDialog(router)
  }
}
