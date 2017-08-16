package de.ph1b.audiobook.playback

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import d
import dagger.android.AndroidInjection
import de.ph1b.audiobook.misc.RxBroadcast
import de.ph1b.audiobook.misc.asV2Observable
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayStateManager.PauseReason
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.events.HeadsetPlugReceiver
import de.ph1b.audiobook.playback.utils.BookUriConverter
import de.ph1b.audiobook.playback.utils.ChangeNotifier
import de.ph1b.audiobook.playback.utils.MediaBrowserHelper
import de.ph1b.audiobook.playback.utils.NotificationAnnouncer
import de.ph1b.audiobook.playback.utils.audioFocus.AudioFocusHandler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import v
import java.io.File
import javax.inject.Inject

/**
 * Service that hosts the longtime playback and handles its controls.
 */
class PlaybackService : MediaBrowserServiceCompat() {

  override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) = mediaBrowserHelper.onLoadChildren(parentId, result)

  override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot = mediaBrowserHelper.onGetRoot()

  private val disposables = CompositeDisposable()

  @Inject lateinit var prefs: PrefsManager
  @Inject lateinit var player: MediaPlayer
  @Inject lateinit var repo: BookRepository
  @Inject lateinit var notificationManager: NotificationManager
  @Inject lateinit var audioManager: AudioManager
  @Inject lateinit var notificationAnnouncer: NotificationAnnouncer
  @Inject lateinit var playStateManager: PlayStateManager
  @Inject lateinit var bookUriConverter: BookUriConverter
  @Inject lateinit var mediaBrowserHelper: MediaBrowserHelper
  @Inject lateinit var mediaSession: MediaSessionCompat
  @Inject lateinit var changeNotifier: ChangeNotifier
  @Inject lateinit var autoConnected: AndroidAutoConnection
  @Inject lateinit var audioFocusHelper: AudioFocusHandler

  override fun onCreate() {
    AndroidInjection.inject(this)
    super.onCreate()

    sessionToken = mediaSession.sessionToken

    // update book when changed by player
    player.bookStream.distinctUntilChanged().subscribe {
      repo.updateBook(it)
    }

    autoConnected.register(this)

    disposables.apply {
      // re-init controller when there is a new book set as the current book
      add(prefs.currentBookId.asV2Observable()
          .subscribe {
            if (player.book()?.id != it) {
              player.stop()
              repo.bookById(it)?.let { player.init(it) }
            }
          })

      // notify player about changes in the current book
      add(repo.updateObservable()
          .filter { it.id == prefs.currentBookId.value }
          .subscribe {
            player.init(it)
            changeNotifier.notify(ChangeNotifier.Type.METADATA, it, autoConnected.connected)
          })

      // handle changes on the play state
      add(playStateManager.playStateStream()
          .observeOn(Schedulers.io())
          .subscribe { handlePlaybackState(it) })

      // resume playback when headset is reconnected. (if settings are set)
      add(HeadsetPlugReceiver.events(this@PlaybackService)
          .subscribe { headsetState ->
            if (headsetState == HeadsetPlugReceiver.HeadsetState.PLUGGED) {
              if (playStateManager.pauseReason == PauseReason.BECAUSE_HEADSET) {
                if (prefs.resumeOnReplug.value) {
                  player.play()
                }
              }
            }
          })

      // notifies the media service about added or removed books
      add(repo.booksStream().map { it.size }.distinctUntilChanged()
          .subscribe {
            notifyChildrenChanged(bookUriConverter.allBooks().toString())
          })

      // pause when audio is becoming noisy.
      add(RxBroadcast.register(this@PlaybackService, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
          .subscribe {
            d { "audio becoming noisy. playState=${playStateManager.playState}" }
            if (playStateManager.playState === PlayState.PLAYING) {
              playStateManager.pauseReason = PauseReason.BECAUSE_HEADSET
              player.pause(true)
            }
          })
    }
  }

  private fun handlePlaybackState(state: PlayState) {
    d { "handlePlaybackState $state" }
    when (state) {
      PlayState.PLAYING -> handlePlaybackStatePlaying()
      PlayState.PAUSED -> handlePlaybackStatePaused()
      PlayState.STOPPED -> handlePlaybackStateStopped()
    }
    player.book()?.let {
      changeNotifier.notify(ChangeNotifier.Type.PLAY_STATE, it, autoConnected.connected)
    }
  }

  private fun handlePlaybackStateStopped() {
    d { "Set mediaSession to inactive" }
    mediaSession.isActive = false
    audioFocusHelper.abandon()
    notificationManager.cancel(NOTIFICATION_ID)
    stopForeground(true)
  }

  private fun handlePlaybackStatePaused() {
    stopForeground(false)
    val book = player.book()
        ?: return
    val notification = notificationAnnouncer.getNotification(book, PlayState.PAUSED, mediaSession.sessionToken)
    notificationManager.notify(NOTIFICATION_ID, notification)
  }

  private fun handlePlaybackStatePlaying() {
    audioFocusHelper.request()
    d { "set mediaSession to active" }
    mediaSession.isActive = true
    val book = player.book()
        ?: return
    val notification = notificationAnnouncer.getNotification(book, PlayState.PLAYING, mediaSession.sessionToken)
    startForeground(NOTIFICATION_ID, notification)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    v { "onStartCommand, intent=$intent, flags=$flags, startId=$startId" }

    when (intent?.action) {
      Intent.ACTION_MEDIA_BUTTON -> MediaButtonReceiver.handleIntent(mediaSession, intent)
      PlayerController.ACTION_SPEED -> player.setPlaybackSpeed(intent.getFloatExtra(PlayerController.EXTRA_SPEED, 1F))
      PlayerController.ACTION_CHANGE -> {
        val time = intent.getIntExtra(PlayerController.CHANGE_TIME, 0)
        val file = File(intent.getStringExtra(PlayerController.CHANGE_FILE))
        player.changePosition(time, file)
      }
      PlayerController.ACTION_FORCE_NEXT -> player.next()
      PlayerController.ACTION_FORCE_PREVIOUS -> player.previous(toNullOfNewTrack = true)
      PlayerController.ACTION_LOUDNESS -> {
        val loudness = intent.getIntExtra(PlayerController.CHANGE_LOUDNESS, 0)
        player.setLoudnessGain(loudness)
      }
    }

    return Service.START_STICKY
  }

  override fun onDestroy() {
    v { "onDestroy called" }
    player.stop()

    mediaSession.release()
    disposables.dispose()

    autoConnected.unregister(this)
    super.onDestroy()
  }

  companion object {
    private const val NOTIFICATION_ID = 42
  }
}
