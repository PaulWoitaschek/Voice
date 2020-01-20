package de.ph1b.audiobook.playback

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import de.ph1b.audiobook.common.orNull
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.awaitFirst
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

  private var started = false

  override fun onCreate() {
    appComponent.playbackComponent()
      .playbackService(this)
      .build()
      .inject(this)
    super.onCreate()

    mediaSession.isActive = true
    sessionToken = mediaSession.sessionToken

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
        currentBookIdChanged(it)
      }
    }

    val bookUpdated = currentBookIdPref.flow
      .mapLatest { repo.byId(it).awaitFirst().orNull }
      .filterNotNull()
      .distinctUntilChangedBy {
        it.content
      }
    scope.launch {
      bookUpdated
        .collectLatest {
          player.init(it.content)
          changeNotifier.notify(ChangeNotifier.Type.METADATA, it, autoConnected.connected)
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
      playStateManager.playStateStream().latestAsFlow()
        .collect {
          handlePlaybackState(it)
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

  private suspend fun updateNotification(book: Book) {
    val notification = notificationCreator.createNotification(book)
    notificationManager.notify(NOTIFICATION_ID, notification)
  }

  private fun currentBookIdChanged(id: UUID) {
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
        execute(PlayerCommand.Play)
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

  private suspend fun handlePlaybackState(state: PlayState) {
    Timber.d("handlePlaybackState $state")
    when (state) {
      PlayState.Playing -> handlePlaybackStatePlaying()
      PlayState.Paused -> handlePlaybackStatePaused()
      PlayState.Stopped -> handlePlaybackStateStopped()
    }
    currentBook()?.let {
      changeNotifier.notify(ChangeNotifier.Type.PLAY_STATE, it, autoConnected.connected)
    }
  }

  private fun currentBook(): Book? {
    val id = currentBookIdPref.value
    return repo.bookById(id)
  }

  private fun handlePlaybackStateStopped() {
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
    stopSelf()
  }

  private suspend fun handlePlaybackStatePaused() {
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
    currentBook()?.let {
      updateNotification(it)
    }
  }

  private suspend fun handlePlaybackStatePlaying() {
    Timber.d("set mediaSession to active")
    currentBook()?.let {
      if (!started) {
        started = true
        // in case this service was not started but just bound, start it.
        ContextCompat.startForegroundService(this, Intent(this, javaClass))
      }
      val notification = notificationCreator.createNotification(it)
      startForeground(NOTIFICATION_ID, notification)
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Timber.v("onStartCommand, intent=$intent, flags=$flags, startId=$startId")

    if (intent != null) {
      if (intent.action == Intent.ACTION_MEDIA_BUTTON) {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
      } else {
        PlayerCommand.fromIntent(intent)?.let(::execute)
      }
    }

    return Service.START_NOT_STICKY
  }

  fun execute(command: PlayerCommand) {
    return when (command) {
      PlayerCommand.Play -> {
        scope.launch { repo.markBookAsPlayedNow(currentBookIdPref.value) }
        player.play()
      }
      PlayerCommand.Next -> {
        player.next()
      }
      PlayerCommand.PlayPause -> {
        if (playStateManager.playState == PlayState.Playing) {
          player.pause(true)
        } else {
          execute(PlayerCommand.Play)
        }
      }
      PlayerCommand.Rewind -> {
        player.skip(forward = false)
      }
      PlayerCommand.FastForward -> {
        player.skip(forward = true)
      }
      PlayerCommand.Previous -> {
        player.previous(toNullOfNewTrack = true)
      }
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
      PlayerCommand.FastForwardAutoPlay -> {
        player.skip(forward = true)
        execute(PlayerCommand.Play)
      }
      PlayerCommand.Stop -> {
        player.stop()
      }
      PlayerCommand.RewindAutoPlay -> {
        player.skip(forward = false)
        execute(PlayerCommand.Play)
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
    Timber.v("onDestroy called")
    player.stop()
    mediaSession.isActive = false
    mediaSession.release()
    scope.cancel()
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null
}
