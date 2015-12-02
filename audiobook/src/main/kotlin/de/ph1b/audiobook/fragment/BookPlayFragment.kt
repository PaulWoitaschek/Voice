package de.ph1b.audiobook.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.text.format.DateUtils
import android.view.*
import android.widget.*
import com.getbase.floatingactionbutton.FloatingActionButton
import com.jakewharton.rxbinding.view.clicks
import com.jakewharton.rxbinding.view.longClicks
import com.jakewharton.rxbinding.widget.RxAdapterView
import com.jakewharton.rxbinding.widget.RxSeekBar
import com.jakewharton.rxbinding.widget.SeekBarProgressChangeEvent
import com.jakewharton.rxbinding.widget.SeekBarStopChangeEvent
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.R
import de.ph1b.audiobook.activity.SettingsActivity
import de.ph1b.audiobook.dialog.BookmarkDialogFragment
import de.ph1b.audiobook.dialog.JumpToPositionDialogFragment
import de.ph1b.audiobook.dialog.prefs.PlaybackSpeedDialogFragment
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.injection.BaseModule
import de.ph1b.audiobook.mediaplayer.MediaPlayerController
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.persistence.BookShelf
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.service.ServiceController
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.uitools.PlayPauseDrawable
import de.ph1b.audiobook.uitools.ThemeUtil
import de.ph1b.audiobook.utils.BookVendor
import de.ph1b.audiobook.utils.Communication
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

    @Inject internal lateinit var communication: Communication
    @Inject internal lateinit var mediaPlayerController: MediaPlayerController
    @Inject internal lateinit var prefs: PrefsManager
    @Inject internal lateinit var db: BookShelf
    @Inject internal lateinit var bookVendor: BookVendor
    @Inject internal lateinit var serviceController: ServiceController

    private val playPauseDrawable = PlayPauseDrawable()
    private var subscriptions: CompositeSubscription? = null
    private var countDownTimer: CountDownTimer? = null
    private var book: Book? = null

    private lateinit var hostingActivity: AppCompatActivity

    private lateinit var timerCountdownView: TextView
    private lateinit var playedTimeView: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var bookSpinner: Spinner
    private lateinit var maxTimeView: TextView

    private val listener = object : Communication.SimpleBookCommunication() {
        override fun onSleepStateChanged() {
            hostingActivity.runOnUiThread {
                hostingActivity.invalidateOptionsMenu()
                initializeTimerCountdown()
            }
        }
    }

    /**
     * @return the book id this fragment was instantiated with.
     */
    val bookId: Long
        get() = arguments.getLong(NI_BOOK_ID)

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_book_play, container, false)
        timerCountdownView = view.findViewById(R.id.timerView) as TextView
        maxTimeView = view.findViewById(R.id.maxTime) as TextView
        bookSpinner = view.findViewById(R.id.book_spinner) as Spinner
        seekBar = view.findViewById(R.id.seekBar) as SeekBar
        playedTimeView = view.findViewById(R.id.played) as TextView
        val coverFrame = view.findViewById(R.id.cover_frame)
        val coverView = view.findViewById(R.id.book_cover) as ImageView
        val nextButton = view.findViewById(R.id.next)
        val fastForwardButton = view.findViewById(R.id.fastForward)
        val playButton = view.findViewById(R.id.play) as FloatingActionButton
        val rewindButton = view.findViewById(R.id.rewind)
        val previousButton = view.findViewById(R.id.previous)

        Observable.merge(playButton.clicks(), coverFrame.longClicks())
                .subscribe { serviceController.playPause() }
        rewindButton.clicks().subscribe { serviceController.rewind() }
        fastForwardButton.clicks().subscribe { serviceController.fastForward() }
        nextButton.clicks().subscribe { serviceController.next() }
        previousButton.clicks().subscribe { serviceController.previous() }
        playedTimeView.clicks().subscribe { launchJumpToPositionDialog() }

        book = bookVendor.byId(bookId)

        //init views
        hostingActivity.supportActionBar.setDisplayHomeAsUpEnabled(true)

        //setup buttons
        playButton.setIconDrawable(playPauseDrawable)
        RxSeekBar.changeEvents(seekBar)
                .subscribe { eventType ->
                    when (eventType ) {
                        is  SeekBarProgressChangeEvent -> {
                            //sets text to adjust while using seekBar
                            playedTimeView.text = formatTime(eventType.progress(), seekBar.max)
                        }
                        is SeekBarStopChangeEvent -> {
                            val progress = seekBar.progress
                            serviceController.changeTime(progress, book!!.currentChapter().file)
                            playedTimeView.text = formatTime(progress, seekBar.max)
                        }
                    }
                }

        if (book != null) {
            hostingActivity.supportActionBar.title = book!!.name

            // adapter
            val chapters = book!!.chapters
            val chaptersAsStrings = ArrayList<String>(chapters.size)
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

                chaptersAsStrings.add(chapterName)
            }

            val adapter = object : ArrayAdapter<String>(context, R.layout.fragment_book_play_spinner, R.id.spinnerTextItem, chaptersAsStrings) {
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val dropDownView = super.getDropDownView(position, convertView, parent)
                    val textView = dropDownView.findViewById(R.id.spinnerTextItem) as TextView

                    // highlights the selected item and un-highlights an item if it is not selected.
                    // default implementation uses a ViewHolder, so this is necessary.
                    if (position == bookSpinner.selectedItemPosition) {
                        textView.setBackgroundResource(R.drawable.spinner_selected_background)
                        textView.setTextColor(ContextCompat.getColor(context, R.color.copy_abc_primary_text_material_dark))
                    } else {
                        textView.setBackgroundResource(ThemeUtil.getResourceId(context,
                                R.attr.selectableItemBackground))
                        textView.setTextColor(ContextCompat.getColor(context, ThemeUtil.getResourceId(
                                context, android.R.attr.textColorPrimary)))
                    }

                    return dropDownView
                }
            }
            bookSpinner.adapter = adapter
            RxAdapterView.itemSelections(bookSpinner).subscribe {
                // fire event only when that tag has been set (= this is not the first event) and
                // this is a new value
                val realInput = bookSpinner.tag != null && bookSpinner.tag != it
                if (realInput) {
                    Timber.i("spinner: onItemSelected. firing: %d", it)
                    serviceController.changeTime(0, book!!.chapters[it].file)
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
                    coverView.viewTreeObserver.removeOnPreDrawListener(this);
                    coverView.setImageDrawable(coverReplacement);
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

    private fun initializeTimerCountdown() {
        countDownTimer?.cancel()

        if (mediaPlayerController.isSleepTimerActive) {
            timerCountdownView.visibility = View.VISIBLE
            countDownTimer = object : CountDownTimer(mediaPlayerController.leftSleepTimerTime, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timerCountdownView.text = formatTime(millisUntilFinished.toInt(), millisUntilFinished.toInt())
                }

                override fun onFinish() {
                    timerCountdownView.visibility = View.GONE
                    Timber.i("Countdown timer finished")
                }
            }.start()
        } else {
            timerCountdownView.visibility = View.GONE
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        hostingActivity = context as AppCompatActivity
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.book_play, menu)

        // sets playback speed icon enabled / disabled depending on device functionallity
        val timeLapseItem = menu!!.findItem(R.id.action_time_lapse)
        timeLapseItem.setVisible(BaseModule.canSetSpeed())

        // sets the correct sleep timer icon
        val sleepTimerItem = menu.findItem(R.id.action_sleep)
        if (mediaPlayerController.isSleepTimerActive) {
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
                serviceController.toggleSleepSand()
                if (prefs.setBookmarkOnSleepTimer() && !mediaPlayerController.isSleepTimerActive) {
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
        subscriptions!!.add(mediaPlayerController.playState
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Action1<MediaPlayerController.PlayState> {
                    private var firstRun = true

                    override fun call(playState: MediaPlayerController.PlayState) {
                        // animate only if this is not the first run
                        Timber.i("onNext with playState %s", playState)
                        if (playState === MediaPlayerController.PlayState.PLAYING) {
                            playPauseDrawable.transformToPause(!firstRun)
                        } else {
                            playPauseDrawable.transformToPlay(!firstRun)
                        }

                        firstRun = false
                    }
                }))


        subscriptions!!.add((Observable.merge(db.activeBooks, db.updateObservable()))
                .observeOn(AndroidSchedulers.mainThread())
                .filter { book -> book.id == bookId }
                .doOnNext { book -> this@BookPlayFragment.book = book }
                .subscribe { book ->
                    if (book == null) {
                        Timber.e("Book is null. Returning immediately.")
                        return@subscribe
                    }
                    Timber.i("New book with getTime %d and content %s", book.time, book)

                    val chapters = book.chapters
                    val chapter = book.currentChapter()

                    val position = chapters.indexOf(chapter)
                    /* Setting position as a tag, so we can make sure onItemSelected is only fired when
                     the user changes the position himself.  */
                    bookSpinner.tag = position
                    bookSpinner.setSelection(position, true)
                    val duration = chapter.duration
                    seekBar.max = duration
                    maxTimeView.text = formatTime(duration, duration)

                    // Setting seekBar and played getTime view
                    val progress = book.time
                    if (!seekBar.isPressed) {
                        seekBar.progress = progress
                        playedTimeView.text = formatTime(progress, duration)
                    }
                })

        hostingActivity.invalidateOptionsMenu()

        communication.addBookCommunicationListener(listener)

        // Sleep timer countdown view
        initializeTimerCountdown()
    }

    override fun onStop() {
        super.onStop()

        subscriptions!!.unsubscribe()

        communication.removeBookCommunicationListener(listener)

        countDownTimer?.cancel()
    }

    private fun formatTime(ms: Int, duration: Int): String {
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
