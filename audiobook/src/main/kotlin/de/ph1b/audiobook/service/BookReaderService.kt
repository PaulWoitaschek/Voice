package de.ph1b.audiobook.service

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.NotificationCompat
import android.telephony.TelephonyManager
import android.view.KeyEvent
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.R
import de.ph1b.audiobook.activity.BookActivity
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.mediaplayer.MediaPlayerController
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.persistence.BookShelf
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.uitools.ImageHelper
import de.ph1b.audiobook.utils.BookVendor
import rx.functions.Func1
import rx.subscriptions.CompositeSubscription
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.inject.Inject


/**
 * Service that hosts the longtime playback and handles its controls.

 * @author Paul Woitaschek
 */
class BookReaderService : Service(), AudioManager.OnAudioFocusChangeListener {
    private val executor = Executors.newCachedThreadPool()
    private val playerExecutor = ThreadPoolExecutor(
            1, 1, // single thread
            2, TimeUnit.SECONDS,
            LinkedBlockingQueue<Runnable>(3), // queue capacity
            ThreadPoolExecutor.DiscardOldestPolicy())
    private val playbackStateBuilder = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_REWIND or
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_FAST_FORWARD or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SEEK_TO)
    private val mediaMetaDataBuilder = MediaMetadataCompat.Builder()
    private val subscriptions = CompositeSubscription()
    @Inject internal lateinit var prefs: PrefsManager
    @Inject internal lateinit var controller: MediaPlayerController
    @Inject internal lateinit var db: BookShelf
    @Inject internal lateinit var notificationManager: NotificationManager
    @Inject internal lateinit var audioManager: AudioManager
    @Inject internal lateinit var bookVendor: BookVendor
    @Inject internal lateinit var telephonyManager: TelephonyManager
    @Inject internal lateinit var imageHelper: ImageHelper;
    @Volatile private var pauseBecauseLossTransient = false
    @Volatile private var pauseBecauseHeadset = false
    private val audioBecomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (controller.playState.value === MediaPlayerController.PlayState.PLAYING) {
                pauseBecauseHeadset = true
                controller.pause(true)
            }
        }
    }
    private val headsetPlugReceiver = object : BroadcastReceiver() {
        private val PLUGGED = 1
        private val UNPLUGGED = 0

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != null && action == Intent.ACTION_HEADSET_PLUG) {
                if (intent.getIntExtra("state", UNPLUGGED) == PLUGGED) {
                    if (pauseBecauseHeadset) {
                        if (prefs.resumeOnReplug()) {
                            controller.play()
                        }
                        pauseBecauseHeadset = false
                    }
                }
            }
        }
    }
    private lateinit var mediaSession: MediaSessionCompat
    /**
     * The last file the [.notifyChange] has used to update the metadata.
     */
    @Volatile private var lastFileForMetaData = File("")

    init {
        App.getComponent().inject(this)
    }

    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSessionCompat(this, TAG)
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                val keyEvent = mediaButtonEvent!!.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN && keyEvent.repeatCount == 0) {
                    val keyCode = keyEvent.keyCode
                    Timber.d("onMediaButtonEvent Received command=%s", keyEvent)
                    return handleKeyCode(keyCode)
                } else {
                    return super.onMediaButtonEvent(mediaButtonEvent)
                }
            }
        })
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

        registerReceiver(audioBecomingNoisyReceiver, IntentFilter(
                AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        registerReceiver(headsetPlugReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))

        controller.setPlayState(MediaPlayerController.PlayState.STOPPED)

        val book = bookVendor.byId(prefs.currentBookId.value)
        if (book != null) {
            Timber.d("onCreated initialized book=%s", book)
            reInitController(book)
        }

        subscriptions.add(prefs.currentBookId.map<Book>(Func1<Long, Book> { bookVendor.byId(it) })
                .subscribe {
                    val controllerBook = controller.book
                    if (it != null && (controllerBook == null || controllerBook.id != it.id)) {
                        reInitController(it)
                    }
                })

        subscriptions.add(db.updateObservable()
                .filter { it.id == prefs.currentBookId.value }
                .subscribe {
                    controller.updateBook(it)
                    notifyChange(ChangeType.METADATA)
                })

        subscriptions.add(controller.playState.subscribe {
            Timber.d("onPlayStateChanged:%s", it)
            executor.execute {
                Timber.d("onPlayStateChanged executed:%s", it)
                val controllerBook = controller.book
                if (controllerBook != null) {
                    when (it!!) {
                        MediaPlayerController.PlayState.PLAYING -> {
                            audioManager.requestAudioFocus(this@BookReaderService, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

                            mediaSession.isActive = true

                            startForeground(NOTIFICATION_ID, getNotification(controllerBook))
                        }
                        MediaPlayerController.PlayState.PAUSED -> {
                            stopForeground(false)
                            notificationManager.notify(NOTIFICATION_ID, getNotification(controllerBook))
                        }
                        MediaPlayerController.PlayState.STOPPED -> {
                            mediaSession.isActive = false

                            audioManager.abandonAudioFocus(this@BookReaderService)
                            notificationManager.cancel(NOTIFICATION_ID)
                            stopForeground(true)
                        }
                    }

                    notifyChange(ChangeType.PLAY_STATE)
                }
            }
        })
    }

    private fun handleKeyCode(keyCode: Int): Boolean {
        Timber.v("handling keyCode: %s", keyCode)
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                if (controller.playState.value === MediaPlayerController.PlayState.PLAYING) {
                    controller.pause(true)
                } else {
                    controller.play()
                }
                return true
            }
            KeyEvent.KEYCODE_MEDIA_STOP -> {
                controller.stop()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                controller.skip(MediaPlayerController.Direction.FORWARD)
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_REWIND -> {
                controller.skip(MediaPlayerController.Direction.BACKWARD)
                return true
            }
            else -> return false
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.v("onStartCommand, intent=%s, flags=%d, startId=%d", intent, flags, startId)
        if (intent != null && intent.action != null) {
            playerExecutor.execute {
                Timber.v("handling intent action:%s", intent.action)
                when (intent.action!!) {
                    Intent.ACTION_MEDIA_BUTTON -> MediaButtonReceiver.handleIntent(mediaSession, intent)
                    ServiceController.CONTROL_SET_PLAYBACK_SPEED -> {
                        val speed = intent.getFloatExtra(ServiceController.CONTROL_SET_PLAYBACK_SPEED_EXTRA_SPEED, 1f)
                        controller.playbackSpeed = speed
                    }
                    ServiceController.CONTROL_TOGGLE_SLEEP_SAND -> controller.toggleSleepSand()
                    ServiceController.CONTROL_CHANGE_POSITION -> {
                        val newTime = intent.getIntExtra(ServiceController.CONTROL_CHANGE_POSITION_EXTRA_TIME, 0)
                        val file = intent.getSerializableExtra(ServiceController.CONTROL_CHANGE_POSITION_EXTRA_FILE) as File
                        controller.changePosition(newTime, file)
                    }
                    ServiceController.CONTROL_NEXT -> controller.next()
                    ServiceController.CONTROL_PREVIOUS -> controller.previous(true)
                    else -> {
                    }
                }
            }
        }

        return Service.START_STICKY
    }

    override fun onDestroy() {
        Timber.v("onDestroy called")
        controller.stop()
        controller.onDestroy()

        controller.setPlayState(MediaPlayerController.PlayState.STOPPED)

        try {
            unregisterReceiver(audioBecomingNoisyReceiver)
            unregisterReceiver(headsetPlugReceiver)
        } catch (ignored: IllegalArgumentException) {
        }

        mediaSession.release()

        subscriptions.unsubscribe()

        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun reInitController(book: Book) {
        controller.stop()
        controller.init(book)

        pauseBecauseHeadset = false
        pauseBecauseLossTransient = false
    }

    override fun onAudioFocusChange(focusChange: Int) {
        var newFocus = focusChange
        Timber.d("Call state is: %s", telephonyManager.callState)
        if (telephonyManager.callState != TelephonyManager.CALL_STATE_IDLE) {
            newFocus = AudioManager.AUDIOFOCUS_LOSS
            // if there is an incoming call, we pause permanently. (tricking switch condition)
        }
        when (newFocus) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Timber.d("started by audioFocus gained")
                if (pauseBecauseLossTransient) {
                    controller.play()
                    pauseBecauseLossTransient = false
                } else if (controller.playState.value === MediaPlayerController.PlayState.PLAYING) {
                    Timber.d("increasing volume")
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                Timber.d("paused by audioFocus loss")
                controller.stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (controller.playState.value === MediaPlayerController.PlayState.PLAYING) {
                    if (prefs.pauseOnTempFocusLoss()) {
                        Timber.d("Paused by audio-focus loss transient.")
                        // Only rewind if loss is transient. When we only pause temporary, don't rewind
                        // automatically.
                        controller.pause(newFocus == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
                        pauseBecauseLossTransient = true
                    } else {
                        Timber.d("lowering volume")
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
                        pauseBecauseHeadset = false
                    }
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> if (controller.playState.value === MediaPlayerController.PlayState.PLAYING) {
                Timber.d("Paused by audio-focus loss transient.")
                controller.pause(true) // auto pause
                pauseBecauseLossTransient = true
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun getNotification(book: Book): Notification {
        // cover
        val width = imageHelper.smallerScreenSize
        val height = imageHelper.smallerScreenSize
        var cover: Bitmap? = null
        try {
            val coverFile = book.coverFile()
            if (!book.useCoverReplacement && coverFile.exists() && coverFile.canRead()) {
                cover = Picasso.with(this@BookReaderService)
                        .load(coverFile)
                        .resize(width, height)
                        .get()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (cover == null) {
            cover = imageHelper.drawableToBitmap(CoverReplacement(book.name, this), width, height)
        }

        val notificationBuilder = NotificationCompat.Builder(this)
        val chapter = book.currentChapter()

        val chapters = book.chapters
        if (chapters.size > 1) {
            // we need the current chapter title and number only if there is more than one chapter.
            notificationBuilder.setContentInfo("${(chapters.indexOf(chapter) + 1)}/${chapters.size}")
            notificationBuilder.setContentText(chapter.name)
        }

        // rewind
        val rewindIntent = ServiceController.getRewindIntent(this)
        val rewindPI = PendingIntent.getService(applicationContext, KeyEvent.KEYCODE_MEDIA_REWIND, rewindIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notificationBuilder.addAction(R.drawable.ic_fast_rewind, getString(R.string.rewind), rewindPI)

        // play/pause
        val playPauseIntent = ServiceController.getPlayPauseIntent(this)
        val playPausePI = PendingIntent.getService(this, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        if (controller.playState.value === MediaPlayerController.PlayState.PLAYING) {
            notificationBuilder.addAction(R.drawable.ic_pause, getString(R.string.pause), playPausePI)
        } else {
            notificationBuilder.addAction(R.drawable.ic_play_arrow, getString(R.string.play), playPausePI)
        }

        // fast forward
        val fastForwardIntent = ServiceController.getFastForwardIntent(this)
        val fastForwardPI = PendingIntent.getService(applicationContext, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, fastForwardIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notificationBuilder.addAction(R.drawable.ic_fast_forward, getString(R.string.fast_forward), fastForwardPI)

        // stop intent
        val stopIntent = ServiceController.getStopIntent(this)
        val stopPI = PendingIntent.getService(this, KeyEvent.KEYCODE_MEDIA_STOP, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        // content click
        val contentIntent = BookActivity.goToBookIntent(this, book.id)
        val contentPI = PendingIntent.getActivity(this, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        return notificationBuilder
                .setStyle(NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1)
                        .setCancelButtonIntent(stopPI)
                        .setShowCancelButton(true)
                        .setMediaSession(mediaSession.sessionToken))
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentPI)
                .setContentTitle(book.name)
                .setSmallIcon(R.drawable.ic_notification)
                .setWhen(0)
                .setDeleteIntent(stopPI)
                .setAutoCancel(true)
                .setLargeIcon(cover)
                .build()
    }

    private fun notifyChange(what: ChangeType) {
        executor.execute {
            Timber.d("updateRemoteControlClient called")

            val book = controller.book
            if (book != null) {
                val c = book.currentChapter()
                val playState = controller.playState.value

                val bookName = book.name
                val chapterName = c.name
                val author = book.author
                val position = book.time

                sendBroadcast(what.broadcastIntent(author, bookName, chapterName, playState, position))

                //noinspection ResourceType
                playbackStateBuilder.setState(playState.playbackStateCompat, position.toLong(), controller.playbackSpeed)
                mediaSession.setPlaybackState(playbackStateBuilder.build())

                if (what == ChangeType.METADATA && lastFileForMetaData != book.currentFile) {
                    // this check is necessary. Else the lockscreen controls will flicker due to
                    // an updated picture
                    var bitmap: Bitmap? = null
                    val coverFile = book.coverFile()
                    if (!book.useCoverReplacement && coverFile.exists() && coverFile.canRead()) {
                        try {
                            bitmap = Picasso.with(this@BookReaderService).load(coverFile).get()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                    }
                    if (bitmap == null) {
                        val replacement = CoverReplacement(book.name, this@BookReaderService)
                        Timber.d("replacement dimen: %d:%d", replacement.intrinsicWidth, replacement.intrinsicHeight)
                        bitmap = imageHelper.drawableToBitmap(replacement, imageHelper.smallerScreenSize, imageHelper.smallerScreenSize)
                    }
                    // we make a copy because we do not want to use picassos bitmap, since
                    // MediaSessionCompat recycles our bitmap eventually which would make
                    // picassos cached bitmap useless.
                    bitmap = bitmap.copy(bitmap.config, true)
                    mediaMetaDataBuilder
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, c.duration.toLong())
                            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, (book.chapters.indexOf(book.currentChapter()) + 1).toLong())
                            .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, book.chapters.size.toLong())
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, chapterName)
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, bookName)
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, author)
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, author)
                            .putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, author)
                            .putString(MediaMetadataCompat.METADATA_KEY_COMPOSER, author)
                            .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "Audiobook")
                    mediaSession.setMetadata(mediaMetaDataBuilder.build())

                    lastFileForMetaData = book.currentFile
                }
            }
        }
    }

    private enum class ChangeType internal constructor(private val intentUrl: String) {
        METADATA("com.android.music.metachanged"),
        PLAY_STATE("com.android.music.playstatechange");

        fun broadcastIntent(author: String?,
                            bookName: String,
                            chapterName: String,
                            playState: MediaPlayerController.PlayState,
                            time: Int): Intent {
            val i = Intent(intentUrl)
            i.putExtra("id", 1)
            if (author != null) {
                i.putExtra("artist", author)
            }
            i.putExtra("album", bookName)
            i.putExtra("track", chapterName)
            i.putExtra("playing", playState === MediaPlayerController.PlayState.PLAYING)
            i.putExtra("position", time)
            return i
        }
    }

    companion object {

        private val TAG = BookReaderService::class.java.simpleName
        private val NOTIFICATION_ID = 42
    }
}
