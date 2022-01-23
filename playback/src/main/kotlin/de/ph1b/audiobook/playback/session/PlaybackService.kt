package de.ph1b.audiobook.playback.session

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
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
import de.ph1b.audiobook.common.pref.CurrentBook
import de.ph1b.audiobook.common.pref.PrefKeys
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.repo.BookRepo2
import de.ph1b.audiobook.playback.androidauto.NotifyOnAutoConnectionChange
import de.ph1b.audiobook.playback.di.PlaybackComponentFactoryProvider
import de.ph1b.audiobook.playback.misc.flowBroadcastReceiver
import de.ph1b.audiobook.playback.notification.NotificationCreator
import de.ph1b.audiobook.playback.player.MediaPlayer
import de.ph1b.audiobook.playback.playstate.PlayStateManager
import de.ph1b.audiobook.playback.playstate.PlayStateManager.PauseReason
import de.ph1b.audiobook.playback.playstate.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.session.headset.HeadsetState
import de.ph1b.audiobook.playback.session.headset.headsetStateChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

private const val NOTIFICATION_ID = 42

/**
 * Service that hosts the longtime playback and handles its controls.
 */
class PlaybackService : MediaBrowserServiceCompat() {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  @field:[Inject CurrentBook]
  lateinit var currentBookIdPref: DataStore<Book2.Id?>

  @Inject
  lateinit var player: MediaPlayer

  @Inject
  lateinit var repo: BookRepo2

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
      player.bookFlow
        .collect { book ->
          repo.updateBook(book.content)
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
      repo.flow()
        .map { it.size }
        .distinctUntilChanged()
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

  private suspend fun updateNotification(book: Book2): Notification {
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
    Timber.d("audio becoming noisy. playState=${playStateManager.playState}")
    if (playStateManager.playState === PlayState.Playing) {
      playStateManager.pauseReason = PauseReason.BecauseHeadset
      player.pause(true)
    }
  }

  override fun onLoadChildren(
    parentId: String,
    result: Result<List<MediaBrowserCompat.MediaItem>>
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
    rootHints: Bundle?
  ): BrowserRoot {
    return BrowserRoot(mediaBrowserHelper.root(), null)
  }

  override fun onDestroy() {
    scope.cancel()
    mediaSession.release()
    player.release()
    super.onDestroy()
  }

  private suspend fun updateNotification(state: PlaybackStateCompat) {
    val updatedState = state.state

    val book = currentBookIdPref.data.first()
      ?.let { repo.flow(it).first() }
    val notification = if (book != null && updatedState != PlaybackStateCompat.STATE_NONE) {
      notificationCreator.createNotification(book)
    } else {
      null
    }

    when (updatedState) {
      PlaybackStateCompat.STATE_BUFFERING,
      PlaybackStateCompat.STATE_PLAYING -> {
        if (notification != null) {
          notificationManager.notify(NOTIFICATION_ID, notification)

          if (!isForeground) {
            ContextCompat.startForegroundService(
              applicationContext,
              Intent(applicationContext, this@PlaybackService.javaClass)
            )
            startForeground(NOTIFICATION_ID, notification)
            isForeground = true
          }
        }
      }
      else -> {
        if (isForeground) {
          stopForeground(false)
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
