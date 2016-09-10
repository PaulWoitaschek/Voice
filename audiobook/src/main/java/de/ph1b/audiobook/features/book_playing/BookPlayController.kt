package de.ph1b.audiobook.features.book_playing

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
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
import de.ph1b.audiobook.misc.MultiLineSpinnerAdapter
import de.ph1b.audiobook.misc.clicks
import de.ph1b.audiobook.misc.itemSelections
import de.ph1b.audiobook.misc.setupActionbar
import de.ph1b.audiobook.persistence.BookChest
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.playback.Sandman
import de.ph1b.audiobook.playback.utils.MediaPlayerCapabilities
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.uitools.PlayPauseDrawable
import de.ph1b.audiobook.uitools.ThemeUtil
import i
import kotlinx.android.synthetic.main.book_play.view.*
import rx.Observable
import rx.functions.Action1
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


/**
 * Base class for book playing interaction.

 * @author Paul Woitaschek
 */
class BookPlayController(bundle: Bundle) : BaseController() {

    init {
        App.component().inject(this)
        setHasOptionsMenu(true)
    }

    @Inject lateinit var mediaPlayer: PlayerController
    @Inject lateinit var sandMan: Sandman
    @Inject lateinit var prefs: PrefsManager
    @Inject lateinit var bookChest: BookChest
    @Inject lateinit var playStateManager: PlayStateManager
    @Inject lateinit var playerCapabilities: MediaPlayerCapabilities

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

        play = view.play
        rewind = view.rewind
        fastForward = view.fastForward
        next = view.next
        previous = view.previous
        playedTime = view.playedTime
        maxTime = view.maxTime
        timerCountdownView = view.timerCountdownView
        bookSpinner = view.bookSpinner
        seekBar = view.seekBar
        cover = view.cover
        toolbar = view.toolbar

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
                .onBackpressureLatest()
                .subscribe { mediaPlayer.playPause() }

        book = bookChest.bookById(bookId)

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

        if (book != null) {
            setupActionbar(toolbar = toolbar, upIndicator = R.drawable.ic_arrow_back, title = book!!.name)

            // adapter
            val chapters = book!!.chapters
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

            val adapter = MultiLineSpinnerAdapter<String>(bookSpinner, activity, ContextCompat.getColor(activity, ThemeUtil.getResourceId(activity, android.R.attr.textColorPrimary)))
            adapter.setData(chapterNames)
            bookSpinner.adapter = adapter
            bookSpinner.itemSelections {
                // fire event only when that tag has been set (= this is not the first event) and
                // this is a new value
                val realInput = bookSpinner.tag != null && bookSpinner.tag != it
                if (realInput) {
                    i { "spinner: onItemSelected. firing: $it" }
                    mediaPlayer.changePosition(0, book!!.chapters[it].file)
                    bookSpinner.tag = it
                }
            }

            // Next/Prev/spinner/book progress views hiding
            if (book!!.chapters.size == 1) {
                next.visibility = View.GONE
                previous.visibility = View.GONE
                bookSpinner.visibility = View.GONE
            } else {
                next.visibility = View.VISIBLE
                previous.visibility = View.VISIBLE
                bookSpinner.visibility = View.VISIBLE
            }

            ViewCompat.setTransitionName(cover, book!!.coverTransitionName)
        }

        // (Cover)
        val coverReplacement = CoverReplacement(if (book == null) "M" else book!!.name, activity)
        if (book != null && !book!!.useCoverReplacement && book!!.coverFile().canRead()) {
            Picasso.with(activity).load(book!!.coverFile()).placeholder(coverReplacement).into(cover)
        } else {
            // we have to set the cover in onPreDraw. Else the transition will fail.
            cover.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    cover.viewTreeObserver.removeOnPreDrawListener(this)
                    cover.setImageDrawable(coverReplacement)
                    return true
                }
            })
        }

        return view
    }

    override fun onAttach(view: View) {
        super.onAttach(view)

        playStateManager.playState
                .bindToLifeCycle()
                .subscribe(object : Action1<PlayStateManager.PlayState> {
                    private var firstRun = true

                    override fun call(playState: PlayStateManager.PlayState) {
                        // animate only if this is not the first run
                        i { "onNext with playState $playState" }
                        if (playState === PlayStateManager.PlayState.PLAYING) {
                            playPauseDrawable.transformToPause(!firstRun)
                        } else {
                            playPauseDrawable.transformToPlay(!firstRun)
                        }

                        firstRun = false
                    }
                })

        Observable.merge(Observable.from(bookChest.activeBooks), bookChest.updateObservable())
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

        // hide / show left time view
        sandMan.sleepSand
                .map { if (it > 0) View.VISIBLE else View.GONE }
                .distinctUntilChanged() // only set when visibility has changed
                .bindToLifeCycle()
                .subscribe { visibility ->
                    timerCountdownView.visibility = visibility
                }

        // invalidates the actionbar items
        sandMan.sleepSand
                .map { it > 0 } // sleep timer is active
                .distinctUntilChanged() // only notify when event has changed
                .bindToLifeCycle()
                .subscribe { activity.invalidateOptionsMenu() }


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

        val speedItem = menu.findItem(R.id.action_time_lapse)
        speedItem.isEnabled = playerCapabilities.useCustomMediaPlayer

        // sets the correct sleep timer icon
        val sleepTimerItem = menu.findItem(R.id.action_sleep)
        if (sandMan.sleepTimerActive()) {
            sleepTimerItem.setIcon(R.drawable.alarm_off)
        } else {
            sleepTimerItem.setIcon(R.drawable.alarm)
        }

        // hide bookmark and getTime change item if there is no valid book
        val currentBookExists = book != null
        val bookmarkItem = menu.findItem(R.id.action_bookmark)
        val timeChangeItem = menu.findItem(R.id.action_time_change)
        bookmarkItem.isVisible = currentBookExists
        timeChangeItem.isVisible = currentBookExists
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                router.pushController(RouterTransaction.with(SettingsController()))
                return true
            }
            R.id.action_time_change -> {
                launchJumpToPositionDialog()
                return true
            }
            R.id.action_sleep -> {
                if (sandMan.sleepTimerActive()) sandMan.setActive(false)
                else SleepTimerDialogFragment.newInstance(book!!)
                        .show(fragmentManager, "fmSleepTimer")
                return true
            }
            R.id.action_time_lapse -> {
                PlaybackSpeedDialogFragment().show(fragmentManager,
                        PlaybackSpeedDialogFragment.TAG)
                return true
            }
            R.id.action_bookmark -> {
                BookmarkDialogFragment.newInstance(bookId).show(fragmentManager,
                        BookmarkDialogFragment.TAG)
                return true
            }
            android.R.id.home -> {
                router.popCurrentController()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun formatTime(ms: Long, duration: Long): String {
        val h = TimeUnit.MILLISECONDS.toHours(ms.toLong()).toString()
        val m = "%02d".format((TimeUnit.MILLISECONDS.toMinutes(ms.toLong()) % 60))
        val s = "%02d".format((TimeUnit.MILLISECONDS.toSeconds(ms.toLong()) % 60))

        if (TimeUnit.MILLISECONDS.toHours(duration.toLong()) == 0L) {
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
