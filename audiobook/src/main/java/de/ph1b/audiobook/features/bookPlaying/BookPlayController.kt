package de.ph1b.audiobook.features.bookPlaying

import android.os.Bundle
import android.support.v7.widget.AppCompatSpinner
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import com.getbase.floatingactionbutton.FloatingActionButton
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.bookmarks.BookmarkDialogFragment
import de.ph1b.audiobook.features.settings.SettingsController
import de.ph1b.audiobook.features.settings.dialogs.PlaybackSpeedDialogFragment
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.*
import de.ph1b.audiobook.mvp.MvpBaseController
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
class BookPlayController(bundle: Bundle) : MvpBaseController<BookPlayMvp.View, BookPlayMvp.Presenter>(bundle), BookPlayMvp.View {

  constructor(bookId: Long) : this(Bundle().apply { putLong(NI_BOOK_ID, bookId) })

  @Inject lateinit var equalizer: Equalizer

  private val data = ArrayList<BookPlayChapter>()
  private val bookId = bundle.getLong(NI_BOOK_ID)
  private val playPauseDrawable = PlayPauseDrawable()
  private var currentChapter: BookPlayChapter? = null
  private var firstPlayStateChange = true
  private var leftSleepTime = 0

  override val presenter = BookPlayPresenter(bookId)

  private lateinit var spinnerAdapter: MultiLineSpinnerAdapter<BookPlayChapter>
  private lateinit var sleepTimerItem: MenuItem

  private lateinit var play: FloatingActionButton
  private lateinit var rewind: View
  private lateinit var fastForward: View
  private lateinit var next: View
  private lateinit var previous: View
  private lateinit var playedTime: TextView
  private lateinit var maxTime: TextView
  private lateinit var timerCountdownView: TextView
  private lateinit var bookSpinner: AppCompatSpinner
  private lateinit var seekBar: SeekBar
  private lateinit var cover: ImageView
  private lateinit var toolbar: Toolbar

  init {
    App.component.inject(this)

    setHasOptionsMenu(true)
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
    bookSpinner.setSelection(chapterIndex, true)
    val duration = currentChapter.duration
    seekBar.max = duration
    maxTime.text = formatTime(duration, duration)

    // Setting seekBar and played getTime view
    val progress = book.time - currentChapter.start
    if (!seekBar.isPressed) {
      seekBar.progress = progress
      playedTime.text = formatTime(progress, duration)
    }

    // name
    activity.supportActionBar!!.apply {
      title = book.name
      setDisplayShowTitleEnabled(true)
    }

    // Next/Prev/spinner/book progress views hiding
    val multipleChapters = data.size > 1
    next.visible = multipleChapters
    previous.visible = multipleChapters
    bookSpinner.visible = multipleChapters

    cover.supportTransitionName = book.coverTransitionName

    // (Cover)
    val coverReplacement = CoverReplacement(book.name, activity)
    if (book.coverFile().canRead()) {
      Picasso.with(activity)
          .load(book.coverFile())
          .placeholder(coverReplacement)
          .into(cover)
    } else {
      cover.setImageDrawable(coverReplacement)
    }
  }

  override fun finish() {
    router.popController(this)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
    val view = inflater.inflate(R.layout.book_play, container, false)

    play = view.find(R.id.play)
    rewind = view.find(R.id.rewind)
    fastForward = view.find(R.id.fastForward)
    next = view.find(R.id.next)
    previous = view.find(R.id.previous)
    playedTime = view.find(R.id.playedTime)
    maxTime = view.find(R.id.maxTime)
    timerCountdownView = view.find(R.id.timerCountdownView)
    bookSpinner = view.find(R.id.bookSpinner)
    seekBar = view.find(R.id.seekBar)
    cover = view.find(R.id.cover)
    toolbar = view.find(R.id.toolbar)

    play.setOnClickListener { presenter.playPause() }
    rewind.setOnClickListener { presenter.rewind() }
    fastForward.setOnClickListener { presenter.fastForward() }
    next.setOnClickListener { presenter.next() }
    previous.setOnClickListener { presenter.previous() }
    playedTime.setOnClickListener { launchJumpToPositionDialog() }

    var lastClick = 0L
    val doubleClickTime = ViewConfiguration.getDoubleTapTimeout()
    cover.clicks()
        .filter {
          val currentTime = System.currentTimeMillis()
          val doubleClick = currentTime - lastClick < doubleClickTime
          lastClick = currentTime
          doubleClick
        }
        .doOnNext { lastClick = 0 } // resets so triple clicks won't cause another invoke
        .subscribe { presenter.playPause() }

    //setup buttons
    play.setIconDrawable(playPauseDrawable)
    seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(view: SeekBar?, progress: Int, p2: Boolean) {
        //sets text to adjust while using seekBar
        playedTime.text = formatTime(progress, seekBar.max)
      }

      override fun onStartTrackingTouch(view: SeekBar?) {
      }

      override fun onStopTrackingTouch(view: SeekBar?) {
        currentChapter?.let {
          val progress = seekBar.progress
          presenter.seekTo(it.start + progress, it.file)
        }
      }
    })

    spinnerAdapter = MultiLineSpinnerAdapter(spinner = bookSpinner, context = activity, unselectedTextColor = activity.color(ThemeUtil.getResourceId(activity, android.R.attr.textColorPrimary)), resolveName = BookPlayChapter::correctedName)
    bookSpinner.adapter = spinnerAdapter

    bookSpinner.itemSelections {
      i { "spinner: onItemSelected. firing: $it" }
      val item = data[it]
      presenter.seekTo(item.start, item.file)
    }

    return view
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
    setupActionbar(toolbar = toolbar, upIndicator = R.drawable.ic_arrow_back)

    firstPlayStateChange = true
  }

  private fun launchJumpToPositionDialog() {
    JumpToPositionDialogFragment().show(fragmentManager, JumpToPositionDialogFragment.TAG)
  }

  override fun showLeftSleepTime(ms: Int) {
    this.leftSleepTime = ms
    activity.invalidateOptionsMenu()
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.book_play, menu)
    sleepTimerItem = menu.findItem(R.id.action_sleep)
    val equalizerItem = menu.findItem(R.id.action_equalizer)
    equalizerItem.isVisible = equalizer.exists
  }

  override fun onPrepareOptionsMenu(menu: Menu) {
    super.onPrepareOptionsMenu(menu)
    val active = leftSleepTime > 0
    // sets the correct sleep timer icon
    sleepTimerItem.setIcon(if (active) R.drawable.alarm_off else R.drawable.alarm)
    // set text and visibility
    timerCountdownView.text = formatTime(leftSleepTime, leftSleepTime)
    timerCountdownView.visible = active
  }

  override fun openSleepTimeDialog() {
    SleepTimerDialogFragment.newInstance(bookId)
        .show(fragmentManager, "fmSleepTimer")
  }

  override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
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
      BookmarkDialogFragment.newInstance(bookId).show(
          fragmentManager, BookmarkDialogFragment.TAG
      )
      true
    }
    R.id.action_equalizer -> {
      equalizer.launch(activity)
      true
    }
    android.R.id.home -> {
      router.popCurrentController()
      true
    }
    else -> super.onOptionsItemSelected(item)
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
