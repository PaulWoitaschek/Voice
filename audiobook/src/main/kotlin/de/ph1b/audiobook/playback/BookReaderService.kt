package de.ph1b.audiobook.playback

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.mediaplayer.MediaPlayerController
import de.ph1b.audiobook.persistence.BookShelf
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.receiver.AudioFocusReceiver
import de.ph1b.audiobook.receiver.HeadsetPlugReceiver
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.uitools.ImageHelper
import rx.subscriptions.CompositeSubscription
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject


/**
 * Service that hosts the longtime playback and handles its controls.

 * @author Paul Woitaschek
 */
class BookReaderService : Service() {
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
    private val TAG = BookReaderService::class.java.simpleName
    private val NOTIFICATION_ID = 42
    @Inject internal lateinit var prefs: PrefsManager
    @Inject internal lateinit var controller: MediaPlayerController
    @Inject internal lateinit var db: BookShelf
    @Inject internal lateinit var notificationManager: NotificationManager
    @Inject internal lateinit var audioManager: AudioManager
    @Inject internal lateinit var audioFocusReceiver: AudioFocusReceiver
    @Inject internal lateinit var imageHelper: ImageHelper;
    @Inject internal lateinit var headsetPlugReceiver: HeadsetPlugReceiver
    @Inject internal lateinit var notificationAnnouncher: NotificationAnnouncer
    private val audioBecomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (controller.playState.value === PlayState.PLAYING) {
                pauseReason = PauseReason.BECAUSE_HEADSET
                controller.pause(true)
            }
        }
    }
    private lateinit var mediaSession: MediaSessionCompat
    /**
     * The last file the [.notifyChange] has used to update the metadata.
     */
    @Volatile private var lastFileForMetaData = File("")

    override fun onCreate() {
        super.onCreate()
        App.component().inject(this)

        mediaSession = MediaSessionCompat(this, TAG)
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                val keyEvent = mediaButtonEvent!!.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN && keyEvent.repeatCount == 0) {
                    val keyCode = keyEvent.keyCode
                    Timber.v("handling keyCode: %s", keyCode)
                    when (keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            controller.playPause()
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
                    }
                }

                return super.onMediaButtonEvent(mediaButtonEvent)

            }
        })
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

        registerReceiver(audioBecomingNoisyReceiver, IntentFilter(
                AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        registerReceiver(headsetPlugReceiver.broadcastReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))

        controller.setPlayState(PlayState.STOPPED)

        subscriptions.apply {
            // re-init controller when there is a new book set as the current book
            add(prefs.currentBookId
                    .flatMap({ updatedId ->
                        db.activeBooks.singleOrDefault(null) { it.id == updatedId }
                    })
                    .subscribe {
                        if (it != null && (controller.book?.id != it.id)) {
                            controller.stop()
                            controller.init(it)

                            pauseReason = PauseReason.NONE
                        }
                    })

            // notify player about changes in the current book
            add(db.updateObservable()
                    .filter { it.id == prefs.currentBookId.value }
                    .subscribe {
                        controller.updateBook(it)
                        notifyChange(ChangeType.METADATA)
                    })

            // handle changes on the play state
            add(controller.playState.subscribe {
                Timber.d("onPlayStateChanged:%s", it)
                val controllerBook = controller.book
                if (controllerBook != null) {
                    when (it!!) {
                        PlayState.PLAYING -> {
                            audioManager.requestAudioFocus(audioFocusReceiver.audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

                            mediaSession.isActive = true
                            val notification = notificationAnnouncher.getNotification(controllerBook, it, mediaSession.sessionToken)
                            startForeground(NOTIFICATION_ID, notification)

                            pauseReason = PauseReason.NONE
                        }
                        PlayState.PAUSED -> {
                            stopForeground(false)
                            val notification = notificationAnnouncher.getNotification(controllerBook, it, mediaSession.sessionToken)
                            notificationManager.notify(NOTIFICATION_ID, notification)
                        }
                        PlayState.STOPPED -> {
                            mediaSession.isActive = false

                            audioManager.abandonAudioFocus(audioFocusReceiver.audioFocusListener)
                            notificationManager.cancel(NOTIFICATION_ID)
                            stopForeground(true)
                        }
                    }

                    notifyChange(ChangeType.PLAY_STATE)
                }
            })

            // resume playback when headset is reconnected. (if settings are set)
            add(headsetPlugReceiver.observable()
                    .subscribe { headsetState ->
                        if (headsetState == HeadsetPlugReceiver.HeadsetState.PLUGGED) {
                            if (pauseReason == PauseReason.BECAUSE_HEADSET) {
                                if (prefs.resumeOnReplug()) {
                                    controller.play()
                                }
                            }
                        }
                    }
            )

            add(audioFocusReceiver.focusObservable()
                    .subscribe { audioFocus ->
                        when (audioFocus!!) {
                            AudioFocusReceiver.AudioFocus.GAIN -> {
                                Timber.d("started by audioFocus gained")
                                if (pauseReason == PauseReason.LOSS_TRANSIENT) {
                                    controller.play()
                                } else if (controller.playState.value === PlayState.PLAYING) {
                                    Timber.d("increasing volume")
                                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
                                }
                            }
                            AudioFocusReceiver.AudioFocus.LOSS,
                            AudioFocusReceiver.AudioFocus.INCOMING_CALL -> {
                                Timber.d("paused by audioFocus loss")
                                controller.stop()
                            }
                            AudioFocusReceiver.AudioFocus.LOSS_TRANSIENT_CAN_DUCK -> {
                                if (controller.playState.value === PlayState.PLAYING) {
                                    if (prefs.pauseOnTempFocusLoss()) {
                                        Timber.d("Paused by audio-focus loss transient.")
                                        // Pause is temporary, don't rewind
                                        controller.pause(false)
                                        pauseReason = PauseReason.LOSS_TRANSIENT
                                    } else {
                                        Timber.d("lowering volume")
                                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
                                    }
                                }
                            }
                            AudioFocusReceiver.AudioFocus.LOSS_TRANSIENT -> {
                                if (controller.playState.value === PlayState.PLAYING) {
                                    Timber.d("Paused by audio-focus loss transient.")
                                    controller.pause(true) // auto pause
                                    pauseReason = PauseReason.LOSS_TRANSIENT
                                }
                            }
                        }
                    }
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.v("onStartCommand, intent=%s, flags=%d, startId=%d", intent, flags, startId)

        if (intent?.action == Intent.ACTION_MEDIA_BUTTON ) {
            MediaButtonReceiver.handleIntent(mediaSession, intent)
        }

        return Service.START_STICKY
    }

    override fun onDestroy() {
        Timber.v("onDestroy called")
        controller.stop()
        controller.onDestroy()
        controller.setPlayState(PlayState.STOPPED)

        try {
            unregisterReceiver(audioBecomingNoisyReceiver)
            unregisterReceiver(headsetPlugReceiver.broadcastReceiver)
        } catch (ignored: IllegalArgumentException) {
        }

        mediaSession.release()

        subscriptions.unsubscribe()

        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun notifyChange(what: ChangeType) {
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
                        Timber.e(e, "Error when retrieving cover for book %s", book)
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

    private var pauseReason = PauseReason.NONE

    private enum class PauseReason {
        NONE,
        BECAUSE_HEADSET,
        LOSS_TRANSIENT
    }

    private enum class ChangeType internal constructor(private val intentUrl: String) {
        METADATA("com.android.music.metachanged"),
        PLAY_STATE("com.android.music.playstatechange");

        fun broadcastIntent(author: String?,
                            bookName: String,
                            chapterName: String,
                            playState: PlayState,
                            time: Int): Intent {
            val i = Intent(intentUrl)
            i.apply {
                putExtra("id", 1)
                if (author != null) {
                    putExtra("artist", author)
                }
                putExtra("album", bookName)
                putExtra("track", chapterName)
                putExtra("playing", playState === PlayState.PLAYING)
                putExtra("position", time)
            }
            return i
        }
    }
}