package de.ph1b.audiobook.features.bookPlaying

import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import com.bluelinelabs.conductor.RouterTransaction
import com.getbase.floatingactionbutton.FloatingActionButton
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.BaseController
import de.ph1b.audiobook.features.bookmarks.BookmarkDialogFragment
import de.ph1b.audiobook.features.settings.SettingsController
import de.ph1b.audiobook.features.settings.dialogs.PlaybackSpeedDialogFragment
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.*
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.MediaPlayer
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.playback.Sandman
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.uitools.PlayPauseDrawable
import de.ph1b.audiobook.uitools.ThemeUtil
import de.ph1b.audiobook.uitools.visible
import i
import io.reactivex.Observable
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


/**
 * Base class for book playing interaction.
 *
 * @author Paul Woitaschek
 */
class BookPlayController(bundle: Bundle) : BaseController(bundle) {

  init {
    App.component.inject(this)
    setHasOptionsMenu(true)
  }

  @Inject lateinit var mediaPlayer: PlayerController
  @Inject lateinit var internalPlayer: MediaPlayer
  @Inject lateinit var sandMan: Sandman
  @Inject lateinit var prefs: PrefsManager
  @Inject lateinit var repo: BookRepository
  @Inject lateinit var equalizer: Equalizer
  @Inject lateinit var playStateManager: PlayStateManager

  private val playPauseDrawable = PlayPauseDrawable()
  private var book: Book? = null

  private lateinit var play: FloatingActionButton
  private lateinit var rewind: View
  private lateinit var fastForward: View
  private lateinit var next: View
  private lateinit var previous: View
  private lateinit var playedTime: TextView
  private lateinit var maxTime: TextView
  private lateinit var timerCountdownView: TextView
  private lateinit var bookSpinner: Spinner
  private lateinit var seekBar: SeekBar
  private lateinit var cover: ImageView
  private lateinit var toolbar: Toolbar

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

    play.setOnClickListener { mediaPlayer.playPause() }
    rewind.setOnClickListener { mediaPlayer.rewind() }
    fastForward.setOnClickListener { mediaPlayer.fastForward() }
    next.setOnClickListener { mediaPlayer.next() }
    previous.setOnClickListener { mediaPlayer.previous() }
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
      .subscribe { mediaPlayer.playPause() }

    book = repo.bookById(bookId)

    //setup buttons
    play.setIconDrawable(playPauseDrawable)
    seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(view: SeekBar?, progress: Int, p2: Boolean) {
        //sets text to adjust while using seekBar
        playedTime.text = formatTime(progress.toLong(), seekBar.max.toLong())
      }

      override fun onStartTrackingTouch(view: SeekBar?) {
      }

      override fun onStopTrackingTouch(view: SeekBar?) {
        val progress = seekBar.progress
        mediaPlayer.changePosition(progress, book!!.currentChapter().file)
        playedTime.text = formatTime(progress.toLong(), seekBar.max.toLong())
      }
    })

    book?.let { book ->
      // adapter
      val chapters = book.chapters
      val chapterNames = ArrayList<MultiLineSpinnerAdapter.Data<String>>(chapters.size)
      for (i in chapters.indices) {
        var chapterName = chapters[i].name

        // cutting leading zeros
        chapterName = chapterName.replaceFirst("^0".toRegex(), "")
        val number = (i + 1).toString()

        // desired format is "1 - Title"
        if (!chapterName.startsWith(number + " - ")) {
          // if getTitle does not match desired format
          if (chapterName.startsWith(number)) {
            // if it starts with a number, a " - " should follow
            chapterName = number + " - " + chapterName.substring(chapterName.indexOf(number) + number.length)
          } else {
            // if the name does not match at all, set the correct format
            chapterName = number + " - " + chapterName
          }
        }

        chapterNames.add(MultiLineSpinnerAdapter.Data(chapterName, chapterName))
      }

      val adapter = MultiLineSpinnerAdapter<String>(bookSpinner, activity, activity.color(ThemeUtil.getResourceId(activity, android.R.attr.textColorPrimary)))
      adapter.setData(chapterNames)
      bookSpinner.adapter = adapter
      bookSpinner.itemSelections {
        // fire event only when that tag has been set (= this is not the first event) and
        // this is a new value
        val realInput = bookSpinner.tag != null && bookSpinner.tag != it
        if (realInput) {
          i { "spinner: onItemSelected. firing: $it" }
          mediaPlayer.changePosition(0, book.chapters[it].file)
          bookSpinner.tag = it
        }
      }

      // Next/Prev/spinner/book progress views hiding
      val multipleChapters = book.chapters.size > 1
      next.visible = multipleChapters
      previous.visible = multipleChapters
      bookSpinner.visible = multipleChapters

      cover.supportTransitionName = book.coverTransitionName
    }

    // (Cover)
    val coverReplacement = CoverReplacement(book?.name ?: "M", activity)
    if (book?.coverFile()?.canRead() ?: false) {
      Picasso.with(activity)
        .load(book!!.coverFile())
        .placeholder(coverReplacement)
        .into(cover)
    } else {
      // we have to set the cover in onPreDraw. Else the transition will fail.
      cover.onFirstPreDraw { cover.setImageDrawable(coverReplacement) }
    }

    return view
  }

  override fun onAttach(view: View) {
    setupActionbar(toolbar = toolbar, upIndicator = R.drawable.ic_arrow_back, title = book?.name)

    var firstPlayStateChange = true
    playStateManager.playStateStream()
      .bindToLifeCycle()
      .subscribe {
        // animate only if this is not the first run
        i { "onNext with playState $it" }
        if (it === PlayStateManager.PlayState.PLAYING) {
          playPauseDrawable.transformToPause(!firstPlayStateChange)
        } else {
          playPauseDrawable.transformToPlay(!firstPlayStateChange)
        }

        firstPlayStateChange = false
      }


    Observable.merge(Observable.fromIterable(repo.activeBooks), repo.updateObservable())
      .filter { it.id == bookId }
      .bindToLifeCycle()
      .subscribe { book: Book ->
        this@BookPlayController.book = book

        val chapters = book.chapters
        val chapter = book.currentChapter()

        val position = chapters.indexOf(chapter)
        /* Setting position as a tag, so we can make sure onItemSelected is only fired when
         the user changes the position himself.  */
        bookSpinner.tag = position
        bookSpinner.setSelection(position, true)
        val duration = chapter.duration
        seekBar.max = duration
        maxTime.text = formatTime(duration.toLong(), duration.toLong())

        // Setting seekBar and played getTime view
        val progress = book.time
        if (!seekBar.isPressed) {
          seekBar.progress = progress
          playedTime.text = formatTime(progress.toLong(), duration.toLong())
        }
      }

    sandMan.sleepSand
      .map { it > 0 } // sleep timer is active
      .distinctUntilChanged() // only notify when event has changed
      .bindToLifeCycle()
      .subscribe {
        // hide / show left time view
        timerCountdownView.visible = it
        // invalidates the actionbar items
        activity.invalidateOptionsMenu()
      }

    // set the correct time to the sleep time view
    sandMan.sleepSand
      .distinctUntilChanged()
      .filter { it > 0 }
      .map { formatTime(it, it) }
      .bindToLifeCycle()
      .subscribe { timerCountdownView.text = it }
  }

  private fun launchJumpToPositionDialog() {
    JumpToPositionDialogFragment().show(fragmentManager, JumpToPositionDialogFragment.TAG)
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.book_play, menu)

    // sets the correct sleep timer icon
    val sleepTimerItem = menu.findItem(R.id.action_sleep)
    if (sandMan.sleepTimerActive()) {
      sleepTimerItem.setIcon(R.drawable.alarm_off)
    } else {
      sleepTimerItem.setIcon(R.drawable.alarm)
    }

    val equalizerItem = menu.findItem(R.id.action_equalizer)
    equalizerItem.isVisible = equalizer.exists

    // hide bookmark and getTime change item if there is no valid book
    val currentBookExists = book != null
    val bookmarkItem = menu.findItem(R.id.action_bookmark)
    val timeChangeItem = menu.findItem(R.id.action_time_change)
    bookmarkItem.isVisible = currentBookExists
    timeChangeItem.isVisible = currentBookExists
  }

  override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
    R.id.action_settings -> {
      router.pushController(RouterTransaction.with(SettingsController()))
      true
    }
    R.id.action_time_change -> {
      launchJumpToPositionDialog()
      true
    }
    R.id.action_sleep -> {
      if (sandMan.sleepTimerActive()) sandMan.setActive(false)
      else {
        book?.let {
          SleepTimerDialogFragment.newInstance(it)
            .show(fragmentManager, "fmSleepTimer")
        }
      }
      true
    }
    R.id.action_time_lapse -> {
      PlaybackSpeedDialogFragment().show(fragmentManager,
        PlaybackSpeedDialogFragment.TAG)
      true
    }
    R.id.action_bookmark -> {
      BookmarkDialogFragment.newInstance(bookId).show(fragmentManager,
        BookmarkDialogFragment.TAG)
      true
    }
    R.id.action_equalizer -> {
      equalizer.launch(activity, internalPlayer.audioSessionId())
      true
    }
    android.R.id.home -> {
      router.popCurrentController()
      true
    }
    else -> super.onOptionsItemSelected(item)
  }

  private fun formatTime(ms: Long, duration: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(ms).toString()
    val m = "%02d".format((TimeUnit.MILLISECONDS.toMinutes(ms) % 60))
    val s = "%02d".format((TimeUnit.MILLISECONDS.toSeconds(ms) % 60))

    if (TimeUnit.MILLISECONDS.toHours(duration) == 0L) {
      return m + ":" + s
    } else {
      return "$h:$m:$s"
    }
  }

  companion object {
    const val NI_BOOK_ID = "niBookId"
    fun newInstance(bookId: Long) = BookPlayController(Bundle().apply {
      putLong(NI_BOOK_ID, bookId)
    })
  }

  val bookId = bundle.getLong(NI_BOOK_ID)
}
