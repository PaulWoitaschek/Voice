/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.text.format.DateUtils
import android.view.*
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import com.getbase.floatingactionbutton.FloatingActionButton
import com.jakewharton.rxbinding.view.clicks
import com.jakewharton.rxbinding.widget.RxAdapterView
import com.jakewharton.rxbinding.widget.RxSeekBar
import com.jakewharton.rxbinding.widget.SeekBarProgressChangeEvent
import com.jakewharton.rxbinding.widget.SeekBarStopChangeEvent
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.R
import de.ph1b.audiobook.activity.SettingsActivity
import de.ph1b.audiobook.adapter.MultiLineSpinnerAdapter
import de.ph1b.audiobook.dialog.BookmarkDialogFragment
import de.ph1b.audiobook.dialog.JumpToPositionDialogFragment
import de.ph1b.audiobook.dialog.prefs.PlaybackSpeedDialogFragment
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.mediaplayer.MediaPlayerController
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.persistence.BookChest
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.uitools.PlayPauseDrawable
import de.ph1b.audiobook.uitools.ThemeUtil
import de.ph1b.audiobook.utils.BookVendor
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Action1
import rx.subscriptions.CompositeSubscription
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


/**
 * Base class for book playing interaction.

 * @author Paul Woitaschek
 */
class BookPlayFragment : BaseFragment() {

    @Inject internal lateinit var mediaPlayerController: MediaPlayerController
    @Inject internal lateinit var prefs: PrefsManager
    @Inject internal lateinit var db: BookChest
    @Inject internal lateinit var bookVendor: BookVendor
    @Inject internal lateinit var playStateManager: PlayStateManager

    private val playPauseDrawable = PlayPauseDrawable()
    private var subscriptions: CompositeSubscription? = null
    private var book: Book? = null

    private lateinit var hostingActivity: AppCompatActivity

    private lateinit var timerCountdownView: TextView
    private lateinit var playedTimeView: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var bookSpinner: Spinner
    private lateinit var maxTimeView: TextView

    /**
     * @return the book id this fragment was instantiated with.
     */
    val bookId: Long
        get() = arguments.getLong(NI_BOOK_ID)

    private lateinit var coverFrame: View

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_book_play, container, false)
        timerCountdownView = view.findViewById(R.id.timerView) as TextView
        maxTimeView = view.findViewById(R.id.maxTime) as TextView
        bookSpinner = view.findViewById(R.id.book_spinner) as Spinner
        seekBar = view.findViewById(R.id.seekBar) as SeekBar
        playedTimeView = view.findViewById(R.id.played) as TextView
        coverFrame = view.findViewById(R.id.cover_frame)
        val coverView = view.findViewById(R.id.book_cover) as ImageView
        val nextButton = view.findViewById(R.id.next)
        val fastForwardButton = view.findViewById(R.id.fastForward)
        val playButton = view.findViewById(R.id.play) as FloatingActionButton
        val rewindButton = view.findViewById(R.id.rewind)
        val previousButton = view.findViewById(R.id.previous)

        playButton.clicks()
                .onBackpressureLatest()
                .subscribe { mediaPlayerController.playPause() }
        rewindButton.clicks()
                .onBackpressureLatest()
                .subscribe { mediaPlayerController.skip(MediaPlayerController.Direction.BACKWARD) }
        fastForwardButton.clicks()
                .onBackpressureLatest()
                .subscribe { mediaPlayerController.skip(MediaPlayerController.Direction.FORWARD) }
        nextButton.clicks()
                .onBackpressureLatest()
                .subscribe { mediaPlayerController.next() }
        previousButton.clicks()
                .onBackpressureLatest()
                .subscribe { mediaPlayerController.previous(true) }
        playedTimeView.clicks()
                .subscribe { launchJumpToPositionDialog() }

        // double click (=more than one click in a 200ms frame)
        var lastClick = 0L
        coverFrame.clicks()
                .filter {
                    val currentTime = System.currentTimeMillis()
                    val doubleClick = currentTime - lastClick < 200
                    lastClick = currentTime
                    doubleClick
                }
                .doOnNext { lastClick = 0 } // resets so triple clicks won't cause another invoke
                .onBackpressureLatest()
                .subscribe { mediaPlayerController.playPause() }

        book = bookVendor.byId(bookId)

        //init views
        hostingActivity.supportActionBar.setDisplayHomeAsUpEnabled(true)

        //setup buttons
        playButton.setIconDrawable(playPauseDrawable)
        RxSeekBar.changeEvents(seekBar)
                .subscribe {
                    when (it ) {
                        is  SeekBarProgressChangeEvent -> {
                            //sets text to adjust while using seekBar
                            playedTimeView.text = formatTime(it.progress().toLong(), seekBar.max.toLong())
                        }
                        is SeekBarStopChangeEvent -> {
                            val progress = seekBar.progress
                            mediaPlayerController.changePosition(progress, book!!.currentChapter().file)
                            playedTimeView.text = formatTime(progress.toLong(), seekBar.max.toLong())
                        }
                    }
                }

        if (book != null) {
            hostingActivity.supportActionBar.title = book!!.name

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

            val adapter = MultiLineSpinnerAdapter<String>(bookSpinner, context, ContextCompat.getColor(context, ThemeUtil.getResourceId(context, android.R.attr.textColorPrimary)))
            adapter.setData(chapterNames)
            bookSpinner.adapter = adapter
            //bookSpinner.adapter = spinnerAdapter
            RxAdapterView.itemSelections(bookSpinner).subscribe {
                // fire event only when that tag has been set (= this is not the first event) and
                // this is a new value
                val realInput = bookSpinner.tag != null && bookSpinner.tag != it
                if (realInput) {
                    Timber.i("spinner: onItemSelected. firing: %d", it)
                    mediaPlayerController.changePosition(0, book!!.chapters[it].file)
                    bookSpinner.tag = it
                }
            }

            // Next/Prev/spinner/book progress views hiding
            if (book!!.chapters.size == 1) {
                nextButton.visibility = View.GONE
                previousButton.visibility = View.GONE
                bookSpinner.visibility = View.GONE
            } else {
                nextButton.visibility = View.VISIBLE
                previousButton.visibility = View.VISIBLE
                bookSpinner.visibility = View.VISIBLE
            }

            ViewCompat.setTransitionName(coverView, book!!.coverTransitionName)
        }

        // (Cover)
        val coverReplacement = CoverReplacement(if (book == null) "M" else book!!.name, context)
        if (book != null && !book!!.useCoverReplacement && book!!.coverFile().canRead()) {
            Picasso.with(context).load(book!!.coverFile()).placeholder(coverReplacement).into(coverView)
        } else {
            // we have to set the cover in onPreDraw. Else the transition will fail.
            coverView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    coverView.viewTreeObserver.removeOnPreDrawListener(this)
                    coverView.setImageDrawable(coverReplacement)
                    return true
                }
            })
        }

        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        App.component().inject(this)

        setHasOptionsMenu(true)
    }


    private fun launchJumpToPositionDialog() {
        JumpToPositionDialogFragment().show(fragmentManager, JumpToPositionDialogFragment.TAG)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        hostingActivity = context as AppCompatActivity
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.book_play, menu)

        // sets the correct sleep timer icon
        val sleepTimerItem = menu.findItem(R.id.action_sleep)
        if (mediaPlayerController.sleepTimerActive()) {
            sleepTimerItem.setIcon(R.drawable.ic_alarm_on_white_24dp)
        } else {
            sleepTimerItem.setIcon(R.drawable.ic_snooze_white_24dp)
        }

        // hide bookmark and getTime change item if there is no valid book
        val currentBookExists = book != null
        val bookmarkItem = menu.findItem(R.id.action_bookmark)
        val timeChangeItem = menu.findItem(R.id.action_time_change)
        bookmarkItem.setVisible(currentBookExists)
        timeChangeItem.setVisible(currentBookExists)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(context, SettingsActivity::class.java))
                return true
            }
            R.id.action_time_change -> {
                launchJumpToPositionDialog()
                return true
            }
            R.id.action_sleep -> {
                mediaPlayerController.toggleSleepSand()
                if (prefs.setBookmarkOnSleepTimer() && mediaPlayerController.sleepTimerActive()) {
                    val date = DateUtils.formatDateTime(context, System.currentTimeMillis(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_NUMERIC_DATE)
                    BookmarkDialogFragment.addBookmark(bookId, date + ": " + getString(R.string.action_sleep), db)
                }
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
                hostingActivity.onBackPressed()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()

        subscriptions = CompositeSubscription()
        subscriptions!!.apply {
            add(playStateManager.playState
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : Action1<PlayStateManager.PlayState> {
                        private var firstRun = true

                        override fun call(playState: PlayStateManager.PlayState) {
                            // animate only if this is not the first run
                            Timber.i("onNext with playState %s", playState)
                            if (playState === PlayStateManager.PlayState.PLAYING) {
                                playPauseDrawable.transformToPause(!firstRun)
                            } else {
                                playPauseDrawable.transformToPlay(!firstRun)
                            }

                            firstRun = false
                        }
                    }))

            add(Observable.merge(db.activeBooks, db.updateObservable())
                    .observeOn(AndroidSchedulers.mainThread())
                    .filter { it.id == bookId }
                    .doOnNext { this@BookPlayFragment.book = it }
                    .subscribe { book: Book ->
                        val chapters = book.chapters
                        val chapter = book.currentChapter()

                        val position = chapters.indexOf(chapter)
                        /* Setting position as a tag, so we can make sure onItemSelected is only fired when
                         the user changes the position himself.  */
                        bookSpinner.tag = position
                        bookSpinner.setSelection(position, true)
                        val duration = chapter.duration
                        seekBar.max = duration
                        maxTimeView.text = formatTime(duration.toLong(), duration.toLong())

                        // Setting seekBar and played getTime view
                        val progress = book.time
                        if (!seekBar.isPressed) {
                            seekBar.progress = progress
                            playedTimeView.text = formatTime(progress.toLong(), duration.toLong())
                        }
                    })

            // hide / show left time view
            add(mediaPlayerController.sleepSand
                    .observeOn(AndroidSchedulers.mainThread())
                    .map { it > 0 }
                    .map { active ->
                        if (active) View.VISIBLE else View.GONE
                    }
                    .distinctUntilChanged() // only set when visibility has changed
                    .subscribe { visibility ->
                        timerCountdownView.visibility = visibility
                    })

            // invalidates the actionbar items
            add(mediaPlayerController.sleepSand
                    .map { it > 0 } // sleep timer is active
                    .distinctUntilChanged() // only notify when event has changed
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { hostingActivity.invalidateOptionsMenu() }
            )

            // set the correct time to the sleep time view
            add(mediaPlayerController.sleepSand
                    .distinctUntilChanged()
                    .filter { it > 0 }
                    .map { formatTime(it, it) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { timerCountdownView.text = it })
        }

    }

    override fun onStop() {
        super.onStop()

        subscriptions!!.unsubscribe()
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

        val TAG = BookPlayFragment::class.java.simpleName
        private val NI_BOOK_ID = "niBookId"


        /**
         * Method to create a new instance of this fragment. Do not create a new instance yourself.

         * @param bookId the id to use
         * *
         * @return The new instance
         */
        fun newInstance(bookId: Long): BookPlayFragment {
            val bookPlayFragment = BookPlayFragment()

            val args = Bundle()
            args.putLong(NI_BOOK_ID, bookId)
            bookPlayFragment.arguments = args

            return bookPlayFragment
        }
    }
}
