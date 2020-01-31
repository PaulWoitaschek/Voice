package de.ph1b.audiobook.playback

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
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
import androidx.media.MediaBrowserServiceCompat
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.data.repo.flowById
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.flowBroadcastReceiver
import de.ph1b.audiobook.misc.latestAsFlow
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.PlayStateManager.PauseReason
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.events.HeadsetState
import de.ph1b.audiobook.playback.events.headsetStateChangeFlow
import de.ph1b.audiobook.playback.utils.BookUriConverter
import de.ph1b.audiobook.playback.utils.ChangeNotifier
import de.ph1b.audiobook.playback.utils.MediaBrowserHelper
import de.ph1b.audiobook.playback.utils.NotificationCreator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

private const val NOTIFICATION_ID = 42

/**
 * Service that hosts the longtime playback and handles its controls.
 */
class PlaybackService : MediaBrowserServiceCompat() {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  @field:[Inject Named(PrefKeys.CURRENT_BOOK)]
  lateinit var currentBookIdPref: Pref<UUID>
  @Inject
  lateinit var player: MediaPlayer
  @Inject
  lateinit var repo: BookRepository
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
  lateinit var autoConnected: AndroidAutoConnectedReceiver
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
    appComponent.playbackComponent()
      .playbackService(this)
      .build()
      .inject(this)
    super.onCreate()

    // this is necessary because otherwise after a the service gets restarted,
    // the media session is not updated any longer.
    notificationManager.cancel(NOTIFICATION_ID)

    mediaSession.isActive = true
    mediaSession.setCallback(callback)
    sessionToken = mediaSession.sessionToken

    mediaController.registerCallback(MediaControllerCallback())

    scope.launch {
      player.bookContentStream.latestAsFlow()
        .distinctUntilChangedBy { it.settings }
        .collect { content ->
          repo.updateBookContent(content)
        }
    }

    scope.launch {
      notifyOnAutoConnectionChange.listen()
    }

    scope.launch {
      currentBookIdPref.flow.collect {
        initPlayer(it)
      }
    }

    val bookUpdated = currentBookIdPref.flow
      .flatMapLatest { repo.flowById(it) }
      .filterNotNull()
      .distinctUntilChangedBy {
        it.content
      }
    scope.launch {
      bookUpdated
        .collectLatest {
          player.init(it.content)
          changeNotifier.updateMetadata(it)
        }
    }

    scope.launch {
      bookUpdated
        .distinctUntilChangedBy { book -> book.content.currentChapter }
        .collectLatest {
          updateNotification(it)
        }
    }

    scope.launch {
      headsetStateChangeFlow()
        .filter { it == HeadsetState.Plugged }
        .collect {
          headsetPlugged()
        }
    }

    scope.launch {
      repo.booksStream().latestAsFlow()
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

  private suspend fun updateNotification(book: Book): Notification {
    Timber.i("updateNotification for ${book.name}")
    return notificationCreator.createNotification(book).also {
      notificationManager.notify(NOTIFICATION_ID, it)
    }
  }

  private fun initPlayer(id: UUID) {
    if (player.bookContent?.id != id) {
      val book = repo.bookById(id)
      if (book != null) {
        player.init(book.content)
      } else {
        player.stop()
      }
    }
  }

  private fun headsetPlugged() {
    if (playStateManager.pauseReason == PauseReason.BECAUSE_HEADSET) {
      if (resumeOnReplugPref.value) {
        mediaController.transportControls.play()
      }
    }
  }

  private fun audioBecomingNoisy() {
    Timber.d("audio becoming noisy. playState=${playStateManager.playState}")
    if (playStateManager.playState === PlayState.Playing) {
      playStateManager.pauseReason = PauseReason.BECAUSE_HEADSET
      player.pause(true)
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Timber.v("onStartCommand, intent=$intent, flags=$flags, startId=$startId")

    initPlayer(currentBookIdPref.value)
    if (intent != null) {
      PlayerCommand.fromIntent(intent)?.let(::execute)
    }

    return super.onStartCommand(intent, flags, startId)
  }

  private fun execute(command: PlayerCommand) {
    initPlayer(currentBookIdPref.value)
    return when (command) {
      is PlayerCommand.SkipSilence -> {
        player.setSkipSilences(command.skipSilence)
      }
      is PlayerCommand.SetLoudnessGain -> {
        player.setLoudnessGain(command.mB)
      }
      is PlayerCommand.SetPlaybackSpeed -> {
        player.setPlaybackSpeed(command.speed)
      }
      is PlayerCommand.SetPosition -> {
        player.changePosition(command.time, command.file)
      }
      is PlayerCommand.PlayChapterAtIndex -> {
        val chapter = player.bookContent
          ?.chapters?.getOrNull(command.index.toInt()) ?: return
        player.changePosition(0, chapter.file)
        player.play()
      }
      is PlayerCommand.Seek -> {
        player.changePosition(command.time)
      }
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
    player.stop()
    mediaSession.release()
    super.onDestroy()
  }

  private suspend fun updateNotification(state: PlaybackStateCompat) {
    val updatedState = state.state

    val book = repo.bookById(currentBookIdPref.value)
    val notification = if (book != null &&
      updatedState != PlaybackStateCompat.STATE_NONE
    ) {
      notificationCreator.createNotification(book)
    } else {
      null
    }

    when (updatedState) {
      PlaybackStateCompat.STATE_BUFFERING,
      PlaybackStateCompat.STATE_PLAYING -> {

        /**
         * This may look strange, but the documentation for [Service.startForeground]
         * notes that "calling this method does *not* put the service in the started
         * state itself, even though the name sounds like it."
         */
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

          // If playback has ended, also stop the service.
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
