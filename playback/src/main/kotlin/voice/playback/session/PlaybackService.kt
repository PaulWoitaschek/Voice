package voice.playback.session

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.media.MediaBrowserServiceCompat
import de.paulwoitaschek.flowpref.Pref
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import voice.common.BookId
import voice.common.pref.CurrentBook
import voice.common.pref.PrefKeys
import voice.data.Book
import voice.data.repo.BookContentRepo
import voice.data.repo.BookRepository
import voice.logging.core.Logger
import voice.playback.androidauto.NotifyOnAutoConnectionChange
import voice.playback.di.PlaybackComponentFactoryProvider
import voice.playback.misc.flowBroadcastReceiver
import voice.playback.notification.NotificationCreator
import voice.playback.player.MediaPlayer
import voice.playback.playstate.PlayStateManager
import voice.playback.playstate.PlayStateManager.PauseReason
import voice.playback.playstate.PlayStateManager.PlayState
import voice.playback.session.headset.HeadsetState
import voice.playback.session.headset.headsetStateChangeFlow
import javax.inject.Inject
import javax.inject.Named

private const val NOTIFICATION_ID = 42

/**
 * Service that hosts the longtime playback and handles its controls.
 */
class PlaybackService : MediaBrowserServiceCompat() {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  @field:[Inject CurrentBook]
  lateinit var currentBookIdPref: DataStore<BookId?>

  @Inject
  lateinit var player: MediaPlayer

  @Inject
  lateinit var repo: BookRepository

  @Inject
  lateinit var contentRepo: BookContentRepo

  @Inject
  lateinit var notificationManager: NotificationManager

  @Inject
  lateinit var notificationCreator: NotificationCreator

  @Inject
  lateinit var playStateManager: PlayStateManager

  @Inject
  lateinit var bookUriConverter: BookUriConverter

  @Inject
  lateinit var mediaBrowserHelper: MediaBrowserHelper

  @Inject
  lateinit var mediaSession: MediaSessionCompat

  @Inject
  lateinit var changeNotifier: ChangeNotifier

  @Inject
  lateinit var notifyOnAutoConnectionChange: NotifyOnAutoConnectionChange

  @field:[Inject Named(PrefKeys.RESUME_ON_REPLUG)]
  lateinit var resumeOnReplugPref: Pref<Boolean>

  @Inject
  lateinit var mediaController: MediaControllerCompat

  @Inject
  lateinit var callback: MediaSessionCallback

  private var isForeground = false

  override fun onCreate() {
    (application as PlaybackComponentFactoryProvider)
      .factory()
      .create(this)
      .inject(this)
    super.onCreate()

    // this is necessary because otherwise after a the service gets restarted,
    // the media session is not updated any longer.
    notificationManager.cancel(NOTIFICATION_ID)

    mediaSession.isActive = true
    mediaSession.setCallback(callback)
    sessionToken = mediaSession.sessionToken

    player.updateMediaSessionPlaybackState()

    mediaController.registerCallback(MediaControllerCallback())

    scope.launch {
      currentBookIdPref.data
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { repo.flow(it).filterNotNull() }
        .distinctUntilChanged()
        .collect { book ->
          changeNotifier.updateMetadata(book)
          updateNotification(book)
        }
    }

    scope.launch {
      notifyOnAutoConnectionChange.listen()
    }

    scope.launch {
      headsetStateChangeFlow()
        .filter { it == HeadsetState.Plugged }
        .collect {
          headsetPlugged()
        }
    }

    scope.launch {
      contentRepo.flow()
        .distinctUntilChangedBy { contents ->
          contents
            .filter { it.isActive }
            .map { content -> content.id }
        }
        .collect {
          notifyChildrenChanged(bookUriConverter.allBooksId())
        }
    }

    scope.launch {
      flowBroadcastReceiver(IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)).collect {
        audioBecomingNoisy()
      }
    }
  }

  private suspend fun updateNotification(book: Book): Notification {
    return notificationCreator.createNotification(book).also {
      notificationManager.notify(NOTIFICATION_ID, it)
    }
  }

  private fun headsetPlugged() {
    if (playStateManager.pauseReason == PauseReason.BecauseHeadset) {
      if (resumeOnReplugPref.value) {
        mediaController.transportControls.play()
      }
    }
  }

  private fun audioBecomingNoisy() {
    Logger.d("audio becoming noisy. playState=${playStateManager.playState}")
    scope.launch {
      if (playStateManager.playState === PlayState.Playing) {
        playStateManager.pauseReason = PauseReason.BecauseHeadset
        player.pause(true)
      }
    }
  }

  override fun onLoadChildren(
    parentId: String,
    result: Result<List<MediaBrowserCompat.MediaItem>>,
  ) {
    result.detach()
    scope.launch {
      val children = mediaBrowserHelper.loadChildren(parentId)
      result.sendResult(children)
    }
  }

  override fun onGetRoot(
    clientPackageName: String,
    clientUid: Int,
    rootHints: Bundle?,
  ): BrowserRoot = BrowserRoot(mediaBrowserHelper.root(), null)

  override fun onDestroy() {
    scope.cancel()
    mediaSession.release()
    player.release()
    super.onDestroy()
  }

  private suspend fun updateNotification(state: PlaybackStateCompat) {
    val updatedState = state.state

    val book = currentBookIdPref.data.first()
      ?.let { repo.get(it) }
    val notification = if (book != null && updatedState != PlaybackStateCompat.STATE_NONE) {
      notificationCreator.createNotification(book)
    } else {
      null
    }

    when (updatedState) {
      PlaybackStateCompat.STATE_BUFFERING,
      PlaybackStateCompat.STATE_PLAYING,
      -> {
        if (notification != null) {
          notificationManager.notify(NOTIFICATION_ID, notification)

          if (!isForeground) {
            ContextCompat.startForegroundService(
              applicationContext,
              Intent(applicationContext, this@PlaybackService.javaClass),
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
              startForeground(NOTIFICATION_ID, notification)
            }
            isForeground = true
          }
        }
      }
      else -> {
        if (isForeground) {
          stopForeground(STOP_FOREGROUND_DETACH)
          isForeground = false

          if (updatedState == PlaybackStateCompat.STATE_NONE) {
            stopSelf()
          }

          if (notification != null) {
            notificationManager.notify(NOTIFICATION_ID, notification)
          } else {
            removeNowPlayingNotification()
          }
        }
      }
    }
  }

  private fun removeNowPlayingNotification() {
    notificationManager.cancel(NOTIFICATION_ID)
  }

  private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

    override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
      mediaController.playbackState?.let { state ->
        scope.launch {
          player.updateMediaSessionPlaybackState()
          updateNotification(state)
        }
      }
    }

    override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
      state ?: return
      scope.launch {
        updateNotification(state)
      }
    }
  }
}
