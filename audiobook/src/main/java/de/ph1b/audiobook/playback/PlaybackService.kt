package de.ph1b.audiobook.playback

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import d
import de.ph1b.audiobook.features.MainActivity
import de.ph1b.audiobook.features.bookOverview.BookShelfController
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.RxBroadcast
import de.ph1b.audiobook.misc.asV2Observable
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayStateManager.PauseReason
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.events.AudioFocus
import de.ph1b.audiobook.playback.events.AudioFocusReceiver
import de.ph1b.audiobook.playback.events.HeadsetPlugReceiver
import de.ph1b.audiobook.playback.events.MediaEventReceiver
import de.ph1b.audiobook.playback.utils.BookUriConverter
import de.ph1b.audiobook.playback.utils.ChangeNotifier
import de.ph1b.audiobook.playback.utils.MediaBrowserHelper
import de.ph1b.audiobook.playback.utils.NotificationAnnouncer
import e
import i
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import v
import java.io.File
import javax.inject.Inject

/**
 * Service that hosts the longtime playback and handles its controls.
 *
 * @author Paul Woitaschek
 */
class PlaybackService : MediaBrowserServiceCompat() {

  override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) = mediaBrowserHelper.onLoadChildren(parentId, result)

  override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot = mediaBrowserHelper.onGetRoot()

  init {
    App.component.inject(this)
  }

  private val disposables = CompositeDisposable()
  @Inject lateinit var prefs: PrefsManager
  @Inject lateinit var player: MediaPlayer
  @Inject lateinit var repo: BookRepository
  @Inject lateinit var notificationManager: NotificationManager
  @Inject lateinit var audioManager: AudioManager
  @Inject lateinit var audioFocusReceiver: AudioFocusReceiver
  @Inject lateinit var notificationAnnouncer: NotificationAnnouncer
  @Inject lateinit var playStateManager: PlayStateManager
  @Inject lateinit var bookUriConverter: BookUriConverter
  @Inject lateinit var mediaBrowserHelper: MediaBrowserHelper
  private lateinit var mediaSession: MediaSessionCompat
  private lateinit var changeNotifier: ChangeNotifier


  override fun onCreate() {
    super.onCreate()

    val eventReceiver = ComponentName(packageName, MediaEventReceiver::class.java.name)
    val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
      component = eventReceiver
    }
    val buttonReceiverIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    mediaSession = MediaSessionCompat(this, TAG, eventReceiver, buttonReceiverIntent).apply {

      setCallback(object : MediaSessionCompat.Callback() {

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
          i { "onPlayFromMediaId $mediaId" }
          val uri = Uri.parse(mediaId)
          val type = bookUriConverter.match(uri)
          if (type == BookUriConverter.BOOK_ID) {
            val id = bookUriConverter.extractBook(uri)
            prefs.currentBookId.set(id)
            onPlay()
          } else {
            e { "Invalid mediaId $mediaId" }
          }
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
          i { "onPlayFromSearch $query" }
          if (query != null) {
            val match = repo.activeBooks.firstOrNull {
              it.name.contentEquals(query)
            }
            i { "found a match ${match?.name}" }
            if (match != null) {
              prefs.currentBookId.set(match.id)
              player.play()
            }
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
    changeNotifier = ChangeNotifier(mediaSession)

    player.onError()
      .subscribe {
        // inform user on errors
        e { "onError" }
        val book = player.book()
        if (book != null) {
          startActivity(MainActivity.malformedFileIntent(this, book.currentFile))
        } else {
          val intent = Intent(this, BookShelfController::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
          }
          startActivity(intent)
        }
      }

    // update book when changed by player
    player.bookObservable()
      .filter { it != null }
      .subscribe { repo.updateBook(it) }

    playStateManager.playState.onNext(PlayState.STOPPED)

    disposables.apply {
      // re-init controller when there is a new book set as the current book
      add(prefs.currentBookId.asV2Observable()
        .subscribe {
          if (player.book()?.id != it) {
            player.stop()

            val newBook = repo.bookById(it)
            if (newBook != null) player.init(newBook)
          }
        })

      // notify player about changes in the current book
      add(repo.updateObservable()
        .filter { it.id == prefs.currentBookId.value() }
        .subscribe {
          player.init(it)
          changeNotifier.notify(ChangeNotifier.Type.METADATA, it)
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

            changeNotifier.notify(ChangeNotifier.Type.PLAY_STATE, controllerBook)
          }
        })

      // resume playback when headset is reconnected. (if settings are set)
      add(HeadsetPlugReceiver.events(this@PlaybackService)
        .subscribe { headsetState ->
          if (headsetState == HeadsetPlugReceiver.HeadsetState.PLUGGED) {
            if (playStateManager.pauseReason == PauseReason.BECAUSE_HEADSET) {
              if (prefs.resumeOnReplug.value()) {
                player.play()
              }
            }
          }
        })

      // adjusts stream and playback based on audio focus.
      add(audioFocusReceiver.focusObservable().subscribe { audioFocus: AudioFocus ->
        i { "handleAudioFocus changed to $audioFocus" }
        when (audioFocus) {
          AudioFocus.GAIN -> {
            d { "started by audioFocus gained" }
            if (playStateManager.pauseReason == PauseReason.LOSS_TRANSIENT) {
              player.play()
            } else if (playStateManager.playState.value === PlayState.PLAYING) {
              d { "increasing volume" }
              player.setVolume(loud = true)
            }
          }
          AudioFocus.LOSS,
          AudioFocus.LOSS_INCOMING_CALL -> {
            d { "paused by audioFocus loss" }
            player.pause(true)
            playStateManager.pauseReason = PauseReason.NONE
          }
          AudioFocus.LOSS_TRANSIENT_CAN_DUCK -> {
            if (playStateManager.playState.value === PlayState.PLAYING) {
              if (prefs.pauseOnTempFocusLoss.value()) {
                d { "Paused by audio-focus loss transient." }
                // Pause is temporary, don't rewind
                player.pause(false)
                playStateManager.pauseReason = PauseReason.LOSS_TRANSIENT
              } else {
                d { "lowering volume" }
                player.setVolume(loud = false)
              }
            }
          }
          AudioFocus.LOSS_TRANSIENT -> {
            if (playStateManager.playState.value === PlayState.PLAYING) {
              d { "Paused by audio-focus loss transient." }
              player.pause(true) // auto pause
              playStateManager.pauseReason = PauseReason.LOSS_TRANSIENT
            }
          }
        }
      })

      // notifies the media service about added or removed books
      add(repo.booksStream().map { it.size }.distinctUntilChanged()
        .subscribe {
          v { "notify media browser service about children changed." }
          notifyChildrenChanged(bookUriConverter.allBooks().toString())
        })

      // pause when audio is becoming noisy.
      add(RxBroadcast.register(this@PlaybackService, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        .subscribe {
          d { "audio becoming noisy. playState=${playStateManager.playState.value}" }
          if (playStateManager.playState.value === PlayState.PLAYING) {
            playStateManager.pauseReason = PauseReason.BECAUSE_HEADSET
            player.pause(true)
          }
        })
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
      PlayerController.ACTION_FORCE_NEXT -> player.next()
      PlayerController.ACTION_FORCE_PREVIOUS -> player.previous(toNullOfNewTrack = true)
    }

    return Service.START_STICKY
  }

  override fun onDestroy() {
    v { "onDestroy called" }
    player.stop()

    mediaSession.release()
    disposables.dispose()

    super.onDestroy()
  }

  companion object {
    private val TAG = PlaybackService::class.java.simpleName
    private val NOTIFICATION_ID = 42
  }
}