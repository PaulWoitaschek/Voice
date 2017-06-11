package de.ph1b.audiobook.features.bookPlaying

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewConfiguration
import android.widget.SeekBar
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.R
import de.ph1b.audiobook.databinding.BookPlayBinding
import de.ph1b.audiobook.features.audio.Equalizer
import de.ph1b.audiobook.features.audio.LoudnessDialog
import de.ph1b.audiobook.features.audio.LoudnessGain
import de.ph1b.audiobook.features.bookmarks.BookmarkController
import de.ph1b.audiobook.features.settings.SettingsController
import de.ph1b.audiobook.features.settings.dialogs.PlaybackSpeedDialogFragment
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.*
import de.ph1b.audiobook.mvp.MvpController
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.uitools.PlayPauseDrawable
import de.ph1b.audiobook.uitools.ThemeUtil
import de.ph1b.audiobook.uitools.visible
import i
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val NI_BOOK_ID = "niBookId"

/**
 * Base class for book playing interaction.
 *
 * @author Paul Woitaschek
 */
class BookPlayController(bundle: Bundle) : MvpController<BookPlayMvp.View, BookPlayMvp.Presenter, BookPlayBinding>(bundle), BookPlayMvp.View {

  constructor(bookId: Long) : this(Bundle().apply { putLong(NI_BOOK_ID, bookId) })

  @Inject lateinit var equalizer: Equalizer
  @Inject lateinit var loudnessGain: LoudnessGain

  private val data = ArrayList<BookPlayChapter>()
  private val bookId = bundle.getLong(NI_BOOK_ID)
  private val playPauseDrawable = PlayPauseDrawable()
  private var currentChapter: BookPlayChapter? = null
  private var firstPlayStateChange = true

  override val layoutRes = R.layout.book_play
  override val presenter = BookPlayPresenter(bookId)

  private lateinit var spinnerAdapter: MultiLineSpinnerAdapter<BookPlayChapter>
  private lateinit var sleepTimerItem: MenuItem

  init {
    App.component.inject(this)
  }

  override fun render(book: Book) {
    // adapter
    data.clear()
    book.chapters.forEach {
      if (it.marks.size() > 1) {
        it.marks.forEachIndexed { index, position, name ->
          val start = if (index == 0) 0 else position
          val nextPosition = it.marks.keyAtOrNull(index + 1)
          val stop = if (nextPosition == null) it.duration else nextPosition - 1
          data.add(BookPlayChapter(it.file, start, stop, name))
        }
      } else {
        data.add(BookPlayChapter(it.file, 0, it.duration, it.name))
      }
    }
    spinnerAdapter.setData(data)

    val dataForCurrentFile = data.filter { it.file == book.currentFile }

    // find closest position
    val currentChapter = dataForCurrentFile.firstOrNull { book.time >= it.start && book.time < it.stop }
        ?: dataForCurrentFile.firstOrNull { book.time == it.stop }
        ?: dataForCurrentFile.first()
    this.currentChapter = currentChapter

    val chapterIndex = data.indexOf(currentChapter)
    binding.bookSpinner.setSelection(chapterIndex, true)
    val duration = currentChapter.duration
    binding.seekBar.max = duration
    binding.maxTime.text = formatTime(duration, duration)

    // Setting seekBar and played getTime view
    val progress = book.time - currentChapter.start
    if (!binding.seekBar.isPressed) {
      binding.seekBar.progress = progress
      binding.playedTime.text = formatTime(progress, duration)
    }

    // name
    binding.toolbar.title = book.name

    // Next/Prev/spinner/book progress views hiding
    val multipleChapters = data.size > 1
    binding.next.visible = multipleChapters
    binding.previous.visible = multipleChapters
    binding.bookSpinner.visible = multipleChapters

    binding.cover.supportTransitionName = book.coverTransitionName

    // (Cover)
    val coverReplacement = CoverReplacement(book.name, activity)
    if (book.coverFile().canRead()) {
      Picasso.with(activity)
          .load(book.coverFile())
          .placeholder(coverReplacement)
          .into(binding.cover)
    } else {
      binding.cover.setImageDrawable(coverReplacement)
    }
  }

  override fun finish() {
    router.popController(this)
  }

  override fun onBindingCreated(binding: BookPlayBinding) {
    setupClicks()
    setupFab()
    setupSeekbar()
    setupSpinner()
    setupToolbar()
  }

  private fun setupClicks() {
    binding.play.setOnClickListener { presenter.playPause() }
    binding.rewind.setOnClickListener { presenter.rewind() }
    binding.fastForward.setOnClickListener { presenter.fastForward() }
    binding.next.setOnClickListener { presenter.next() }
    binding.previous.setOnClickListener { presenter.previous() }
    binding.playedTime.setOnClickListener { launchJumpToPositionDialog() }

    var lastClick = 0L
    val doubleClickTime = ViewConfiguration.getDoubleTapTimeout()
    binding.cover.clicks()
        .filter {
          val currentTime = System.currentTimeMillis()
          val doubleClick = currentTime - lastClick < doubleClickTime
          lastClick = currentTime
          doubleClick
        }
        .doOnNext { lastClick = 0 } // resets so triple clicks won't cause another invoke
        .subscribe { presenter.playPause() }
  }

  private fun setupFab() {
    binding.play.setIconDrawable(playPauseDrawable)
  }

  private fun setupSeekbar() {
    binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(view: SeekBar?, progress: Int, p2: Boolean) {
        //sets text to adjust while using seekBar
        binding.playedTime.text = formatTime(progress, binding.seekBar.max)
      }

      override fun onStartTrackingTouch(view: SeekBar?) {
      }

      override fun onStopTrackingTouch(view: SeekBar?) {
        currentChapter?.let {
          val progress = binding.seekBar.progress
          presenter.seekTo(it.start + progress, it.file)
        }
      }
    })
  }

  private fun setupSpinner() {
    spinnerAdapter = MultiLineSpinnerAdapter(
        spinner = binding.bookSpinner,
        context = activity,
        unselectedTextColor = activity.color(ThemeUtil.getResourceId(activity, android.R.attr.textColorPrimary)),
        resolveName = BookPlayChapter::correctedName
    )
    binding.bookSpinner.adapter = spinnerAdapter

    binding.bookSpinner.itemSelections {
      i { "spinner: onItemSelected. firing: $it" }
      val item = data[it]
      presenter.seekTo(item.start, item.file)
    }
  }

  private fun setupToolbar() {
    binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)

    binding.toolbar.inflateMenu(R.menu.book_play)
    val menu = binding.toolbar.menu

    sleepTimerItem = menu.findItem(R.id.action_sleep)
    val equalizerItem = menu.findItem(R.id.action_equalizer)
    equalizerItem.isVisible = equalizer.exists

    // hide loudness gain item if not supported
    menu.findItem(R.id.loudness).isVisible = loudnessGain.supported

    binding.toolbar.setNavigationOnClickListener { router.popController(this) }
    binding.toolbar.setOnMenuItemClickListener {
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
          presenter.toggleSleepTimer()
          true
        }
        R.id.action_time_lapse -> {
          PlaybackSpeedDialogFragment().show(fragmentManager,
              PlaybackSpeedDialogFragment.TAG)
          true
        }
        R.id.action_bookmark -> {
          val bookmarkController = BookmarkController.newInstance(bookId)
              .asTransaction()
          router.pushController(bookmarkController)
          true
        }
        R.id.action_equalizer -> {
          equalizer.launch(activity)
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

  override fun showPlaying(playing: Boolean) {
    if (playing) {
      playPauseDrawable.transformToPause(!firstPlayStateChange)
    } else {
      playPauseDrawable.transformToPlay(!firstPlayStateChange)
    }

    firstPlayStateChange = false
  }


  override fun onAttach(view: View) {
    firstPlayStateChange = true
  }

  private fun launchJumpToPositionDialog() {
    JumpToPositionDialogFragment().show(fragmentManager, JumpToPositionDialogFragment.TAG)
  }

  override fun showLeftSleepTime(ms: Int) {
    val active = ms > 0
    // sets the correct sleep timer icon
    sleepTimerItem.setIcon(if (active) R.drawable.alarm_off else R.drawable.alarm)
    // set text and visibility
    binding.timerCountdownView.text = formatTime(ms, ms)
    binding.timerCountdownView.visible = active
  }

  override fun openSleepTimeDialog() {
    SleepTimerDialogFragment.newInstance(bookId)
        .show(fragmentManager, "fmSleepTimer")
  }

  private fun formatTime(ms: Int, duration: Int): String {
    val h = TimeUnit.MILLISECONDS.toHours(ms.toLong()).toString()
    val m = "%02d".format((TimeUnit.MILLISECONDS.toMinutes(ms.toLong()) % 60))
    val s = "%02d".format((TimeUnit.MILLISECONDS.toSeconds(ms.toLong()) % 60))

    if (TimeUnit.MILLISECONDS.toHours(duration.toLong()) == 0L) {
      return "$m:$s"
    } else {
      return "$h:$m:$s"
    }
  }
}
