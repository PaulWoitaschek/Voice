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

package de.ph1b.audiobook.playback

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.squareup.picasso.Picasso
import d
import de.ph1b.audiobook.activity.BookActivity
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.persistence.BookChest
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayStateManager.PauseReason
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import de.ph1b.audiobook.receiver.AudioFocus
import de.ph1b.audiobook.receiver.AudioFocusReceiver
import de.ph1b.audiobook.receiver.HeadsetPlugReceiver
import de.ph1b.audiobook.receiver.MediaEventReceiver
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.uitools.ImageHelper
import de.ph1b.audiobook.uitools.blocking
import de.ph1b.audiobook.view.fragment.BookShelfFragment
import e
import i
import rx.Observable
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import v
import java.io.File
import javax.inject.Inject

/**
 * Service that hosts the longtime playback and handles its controls.

 * @author Paul Woitaschek
 */
class BookReaderService : MediaBrowserServiceCompat() {

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        d { "onLoadChildren $parentId, $result" }
        val uri = Uri.parse(parentId)

        val items = when (bookUriConverter.match(uri)) {
            BookUriConverter.BOOKS -> {
                d { "books" }
                db.activeBooks.sorted().map {
                    val description = MediaDescriptionCompat.Builder()
                            .setTitle(it.name)
                            .setMediaId(bookUriConverter.book(it.id).toString())
                            .build()
                    return@map MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
                }
            }
            else -> {
                e { "Illegal parentId$parentId" }
                null
            }
        }

        d { "sending result $items" }
        result.sendResult(items)
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        v { "onGetRoot" }
        return BrowserRoot(bookUriConverter.allBooks().toString(), null)
    }


    init {
        App.component().inject(this)
    }

    private val playbackStateBuilder = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_REWIND or
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_FAST_FORWARD or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SEEK_TO)
    private val mediaMetaDataBuilder = MediaMetadataCompat.Builder()
    private val subscriptions = CompositeSubscription()
    @Inject internal lateinit var prefs: PrefsManager
    @Inject internal lateinit var player: MediaPlayer
    @Inject internal lateinit var db: BookChest
    @Inject internal lateinit var notificationManager: NotificationManager
    @Inject internal lateinit var audioManager: AudioManager
    @Inject internal lateinit var audioFocusReceiver: AudioFocusReceiver
    @Inject internal lateinit var imageHelper: ImageHelper
    @Inject internal lateinit var headsetPlugReceiver: HeadsetPlugReceiver
    @Inject internal lateinit var notificationAnnouncer: NotificationAnnouncer
    @Inject internal lateinit var playStateManager: PlayStateManager
    @Inject internal lateinit var audioFocusManager: AudioFocusManager
    @Inject lateinit var bookUriConverter: BookUriConverter
    private val audioBecomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (playStateManager.playState.value === PlayState.PLAYING) {
                playStateManager.pauseReason = PauseReason.BECAUSE_HEADSET
                player.pause(true)
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

        val eventReceiver = ComponentName(packageName, MediaEventReceiver::class.java.name);
        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            component = eventReceiver
        }
        val buttonReceiverIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mediaSession = MediaSessionCompat(this, TAG, eventReceiver, buttonReceiverIntent).apply {

            setCallback(object : MediaSessionCompat.Callback() {
                override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                    i { "onMediaButtonEvent($mediaButtonEvent)" }
                    return super.onMediaButtonEvent(mediaButtonEvent)
                }

                override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                    i { "onPlayFromMediaId $mediaId" }
                    val uri = Uri.parse(mediaId)
                    val type = bookUriConverter.match(uri)
                    if (type == BookUriConverter.BOOKS_ID) {
                        val id = bookUriConverter.extractBook(uri)
                        prefs.setCurrentBookId(id)
                    } else {
                        e { "Invalid mediaId $mediaId" }
                    }
                }

                override fun onSkipToNext() {
                    i { "onSkipToNext" }
                    onFastForward()
                }

                override fun onRewind() {
                    i { "onRewind" }
                    player.skip(MediaPlayer.Direction.BACKWARD)
                }

                override fun onSkipToPrevious() {
                    i { "onSkipToPrevious" }
                    onRewind()
                }

                override fun onFastForward() {
                    i { "onFastForward" }
                    player.skip(MediaPlayer.Direction.FORWARD)
                }

                override fun onStop() {
                    i { "onStop" }
                    player.stop()
                }

                override fun onPause() {
                    i { "onPause" }
                    player.pause(true)
                }

                override fun onPlay() {
                    i { "onPlay" }
                    player.play()
                }
            })
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        }
        sessionToken = mediaSession.sessionToken

        registerReceiver(audioBecomingNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        registerReceiver(headsetPlugReceiver.broadcastReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))

        player.onError()
                .subscribe {
                    // inform user on errors
                    e { "onError" }
                    val book = player.book()
                    if (book != null) {
                        startActivity(BookActivity.malformedFileIntent(this, book.currentFile))
                    } else {
                        val intent = Intent(this, BookShelfFragment::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                    }
                }

        // update book when changed by player
        player.bookObservable()
                .filter { it != null }
                .subscribe { db.updateBook(it) }

        playStateManager.playState.onNext(PlayState.STOPPED)

        subscriptions.apply {
            // set seek time to the player
            add(prefs.seekTime
                    .subscribe { player.seekTime = it })

            // set auto rewind amount to the player
            add(prefs.autoRewindAmount
                    .subscribe { player.autoRewindAmount = it })

            // re-init controller when there is a new book set as the current book
            add(prefs.currentBookId
                    .map { updatedId -> db.bookById(updatedId) }
                    .filter { it != null && (player.book()?.id != it.id) }
                    .subscribe {
                        player.stop()
                        player.init(it!!)
                    })

            // notify player about changes in the current book
            add(db.updateObservable()
                    .filter { it.id == prefs.currentBookId.value }
                    .subscribe {
                        player.init(it)
                        notifyChange(ChangeType.METADATA, it)
                    })

            var currentlyHasFocus = false
            add(audioFocusReceiver.focusObservable()
                    .map { it == AudioFocus.GAIN }
                    .subscribe { currentlyHasFocus = it })

            // handle changes on the play state
            add(playStateManager.playState
                    .observeOn(Schedulers.io())
                    .subscribe {
                        d { "onPlayStateManager.PlayStateChanged:$it" }
                        val controllerBook = player.book()
                        if (controllerBook != null) {
                            when (it!!) {
                                PlayState.PLAYING -> {
                                    if (!currentlyHasFocus) {
                                        audioManager.requestAudioFocus(audioFocusReceiver.audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
                                    }

                                    mediaSession.isActive = true
                                    d { "set mediaSession to active" }
                                    val notification = notificationAnnouncer.getNotification(controllerBook, it, mediaSession.sessionToken)
                                    startForeground(NOTIFICATION_ID, notification)
                                }
                                PlayState.PAUSED -> {
                                    stopForeground(false)
                                    val notification = notificationAnnouncer.getNotification(controllerBook, it, mediaSession.sessionToken)
                                    notificationManager.notify(NOTIFICATION_ID, notification)
                                }
                                PlayState.STOPPED -> {
                                    mediaSession.isActive = false
                                    d { "Set mediaSession to inactive" }

                                    audioManager.abandonAudioFocus(audioFocusReceiver.audioFocusListener)
                                    notificationManager.cancel(NOTIFICATION_ID)
                                    stopForeground(true)
                                }
                            }

                            notifyChange(ChangeType.PLAY_STATE, controllerBook)
                        }
                    })

            // resume playback when headset is reconnected. (if settings are set)
            add(headsetPlugReceiver.observable()
                    .subscribe { headsetState ->
                        if (headsetState == HeadsetPlugReceiver.HeadsetState.PLUGGED) {
                            if (playStateManager.pauseReason == PauseReason.BECAUSE_HEADSET) {
                                if (prefs.resumeOnReplug()) {
                                    player.play()
                                }
                            }
                        }
                    }
            )

            // adjusts stream and playback based on audio focus.
            add(audioFocusManager.handleAudioFocus(audioFocusReceiver.focusObservable()))

            // notifies the media service about added or removed books
            add(Observable.merge(db.addedObservable(), db.removedObservable())
                    .subscribe { notifyChildrenChanged(bookUriConverter.allBooks().toString()) })

        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        v { "onStartCommand, intent=$intent, flags=$flags, startId=$startId" }

        when (intent?.action) {
            Intent.ACTION_MEDIA_BUTTON -> MediaButtonReceiver.handleIntent(mediaSession, intent)
            PlayerController.ACTION_SPEED -> player.setPlaybackSpeed(intent!!.getFloatExtra(PlayerController.EXTRA_SPEED, 1F))
            PlayerController.ACTION_CHANGE -> {
                val time = intent!!.getIntExtra(PlayerController.CHANGE_TIME, 0)
                val file = File(intent.getStringExtra(PlayerController.CHANGE_FILE))
                player.changePosition(time, file)
            }
            PlayerController.ACTION_PAUSE_NON_REWINDING -> player.pause(false)
            PlayerController.ACTION_FORCE_NEXT -> player.next()
            PlayerController.ACTION_FORCE_PREVIOUS -> player.previous(true)
        }

        return Service.START_STICKY
    }

    override fun onDestroy() {
        v { "onDestroy called" }
        player.stop()

        try {
            unregisterReceiver(audioBecomingNoisyReceiver)
            unregisterReceiver(headsetPlugReceiver.broadcastReceiver)
        } catch (ignored: IllegalArgumentException) {
        }

        mediaSession.release()
        subscriptions.unsubscribe()

        super.onDestroy()
    }

    private fun notifyChange(what: ChangeType, book: Book) {
        d { "updateRemoteControlClient called" }

        val c = book.currentChapter()
        val playState = playStateManager.playState.value

        val bookName = book.name
        val chapterName = c.name
        val author = book.author
        val position = book.time

        sendBroadcast(what.broadcastIntent(author, bookName, chapterName, playState, position))

        //noinspection ResourceType
        playbackStateBuilder.setState(playState.playbackStateCompat, position.toLong(), book.playbackSpeed)
        mediaSession.setPlaybackState(playbackStateBuilder.build())

        if (what == ChangeType.METADATA && lastFileForMetaData != book.currentFile) {
            // this check is necessary. Else the lockscreen controls will flicker due to
            // an updated picture
            var bitmap: Bitmap? = null
            val coverFile = book.coverFile()
            if (!book.useCoverReplacement && coverFile.exists() && coverFile.canRead()) {
                bitmap = Picasso.with(this@BookReaderService).blocking { load(coverFile).get() }
            }
            if (bitmap == null) {
                val replacement = CoverReplacement(book.name, this@BookReaderService)
                d { "replacement dimen: ${replacement.intrinsicWidth}:${replacement.intrinsicHeight}" }
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

    private enum class ChangeType internal constructor(private val intentUrl: String) {
        METADATA("com.android.music.metachanged"),
        PLAY_STATE("com.android.music.playstatechange");

        fun broadcastIntent(author: String?,
                            bookName: String,
                            chapterName: String,
                            playState: PlayState,
                            time: Int) =
                Intent(intentUrl).apply {
                    putExtra("id", 1)
                    if (author != null) {
                        putExtra("artist", author)
                    }
                    putExtra("album", bookName)
                    putExtra("track", chapterName)
                    putExtra("playing", playState === PlayState.PLAYING)
                    putExtra("position", time)
                }

    }

    companion object {
        private val TAG = BookReaderService::class.java.simpleName
        private val NOTIFICATION_ID = 42
    }
}