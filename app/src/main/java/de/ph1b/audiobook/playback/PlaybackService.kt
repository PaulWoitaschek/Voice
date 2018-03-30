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
import dagger.android.AndroidInjection
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.misc.RxBroadcast
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.PlayStateManager.PauseReason
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.events.HeadsetPlugReceiver
import de.ph1b.audiobook.playback.utils.BookUriConverter
import de.ph1b.audiobook.playback.utils.ChangeNotifier
import de.ph1b.audiobook.playback.utils.MediaBrowserHelper
import de.ph1b.audiobook.playback.utils.NotificationCreator
import de.ph1b.audiobook.playback.utils.audioFocus.AudioFocusHandler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

/**
 * Service that hosts the longtime playback and handles its controls.
 */
class PlaybackService : MediaBrowserServiceCompat() {

  override fun onLoadChildren(
    parentId: String,
    result: Result<List<MediaBrowserCompat.MediaItem>>
  ) = mediaBrowserHelper.onLoadChildren(
    parentId,
    result
  )

  override fun onGetRoot(
    clientPackageName: String,
    clientUid: Int,
    rootHints: Bundle?
  ): BrowserRoot = mediaBrowserHelper.onGetRoot()

  private val disposables = CompositeDisposable()
  private var isForeground = false

  @field:[Inject Named(PrefKeys.CURRENT_BOOK)]
  lateinit var currentBookIdPref: Pref<Long>
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
  @Inject
  lateinit var audioFocusHelper: AudioFocusHandler
  @field:[Inject Named(PrefKeys.RESUME_ON_REPLUG)]
  lateinit var resumeOnReplugPref: Pref<Boolean>

  override fun onCreate() {
    AndroidInjection.inject(this)
    super.onCreate()

    sessionToken = mediaSession.sessionToken

    // update book when changed by player
    player.bookStream.distinctUntilChanged()
      .observeOn(Schedulers.io())
      .subscribe {
        runBlocking {
          repo.updateBook(it)
        }
      }

    notifyOnAutoConnectionChange.listen()

    currentBookIdPref.stream
      .subscribe { currentBookIdChanged(it) }
      .disposeOnDestroy()

    val bookUpdated = repo.updateObservable()
      .filter { it.id == currentBookIdPref.value }
    bookUpdated
      .subscribe {
        player.init(it)
        launch {
          changeNotifier.notify(ChangeNotifier.Type.METADATA, it, autoConnected.connected)
        }
      }
      .disposeOnDestroy()

    bookUpdated
      .distinctUntilChanged { book -> book.currentChapter }
      .subscribe {
        if (isForeground) {
          runBlocking {
            updateNotification(it)
          }
        }
      }
      .disposeOnDestroy()

    playStateManager.playStateStream()
      .observeOn(Schedulers.io())
      .subscribe {
        runBlocking {
          handlePlaybackState(it)
        }
      }
      .disposeOnDestroy()

    HeadsetPlugReceiver.events(this@PlaybackService)
      .filter { it == HeadsetPlugReceiver.HeadsetState.PLUGGED }
      .subscribe { headsetPlugged() }
      .disposeOnDestroy()

    repo.booksStream()
      .map { it.size }
      .distinctUntilChanged()
      .subscribe {
        notifyChildrenChanged(bookUriConverter.allBooks().toString())
      }
      .disposeOnDestroy()

    RxBroadcast
      .register(
        this@PlaybackService,
        IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
      )
      .subscribe { audioBecomingNoisy() }
      .disposeOnDestroy()

    tearDownAutomatically()
  }

  private suspend fun updateNotification(book: Book) {
    val notification = notificationCreator.createNotification(book)
    notificationManager.notify(NOTIFICATION_ID, notification)
  }

  private fun tearDownAutomatically() {
    val idleTimeOutInSeconds: Long = 7
    playStateManager.playStateStream()
      .distinctUntilChanged()
      .debounce(idleTimeOutInSeconds, TimeUnit.SECONDS)
      .filter { it == PlayState.STOPPED }
      .subscribe {
        Timber.d("STOPPED for $idleTimeOutInSeconds. Stop self")
        stopSelf()
      }
      .disposeOnDestroy()
  }

  private fun currentBookIdChanged(it: Long) {
    if (player.book()?.id != it) {
      player.stop()
      repo.bookById(it)?.let { player.init(it) }
    }
  }

  private fun headsetPlugged() {
    if (playStateManager.pauseReason == PauseReason.BECAUSE_HEADSET) {
      if (resumeOnReplugPref.value) {
        player.play()
      }
    }
  }

  private fun audioBecomingNoisy() {
    Timber.d("audio becoming noisy. playState=${playStateManager.playState}")
    if (playStateManager.playState === PlayState.PLAYING) {
      playStateManager.pauseReason = PauseReason.BECAUSE_HEADSET
      player.pause(true)
    }
  }

  private suspend fun handlePlaybackState(state: PlayState) {
    Timber.d("handlePlaybackState $state")
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
    mediaSession.isActive = false
    audioFocusHelper.abandon()
    notificationManager.cancel(NOTIFICATION_ID)
    stopForeground(true)
    isForeground = false
  }

  private suspend fun handlePlaybackStatePaused() {
    stopForeground(false)
    isForeground = false
    val book = player.book()
        ?: return
    updateNotification(book)
  }

  private suspend fun handlePlaybackStatePlaying() {
    audioFocusHelper.request()
    Timber.d("set mediaSession to active")
    mediaSession.isActive = true
    val book = player.book()
        ?: return
    val notification = notificationCreator.createNotification(book)
    startForeground(NOTIFICATION_ID, notification)
    isForeground = true
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Timber.v("onStartCommand, intent=$intent, flags=$flags, startId=$startId")

    when (intent?.action) {
      Intent.ACTION_MEDIA_BUTTON -> MediaButtonReceiver.handleIntent(mediaSession, intent)
      PlayerController.ACTION_SPEED -> player.setPlaybackSpeed(
        intent.getFloatExtra(
          PlayerController.EXTRA_SPEED,
          1F
        )
      )
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
      PlayerController.ACTION_PLAY_PAUSE -> {
        if (playStateManager.playState == PlayState.PLAYING) {
          player.pause(true)
        } else player.play()
      }
      PlayerController.ACTION_STOP -> player.stop()
      PlayerController.ACTION_PLAY -> player.play()
      PlayerController.ACTION_REWIND -> player.skip(forward = false)
      PlayerController.ACTION_REWIND_AUTO_PLAY -> {
        player.skip(forward = false)
        player.play()
      }
      PlayerController.ACTION_FAST_FORWARD -> player.skip(forward = true)
      PlayerController.ACTION_FAST_FORWARD_AUTO_PLAY -> {
        player.skip(forward = true)
        player.play()
      }
    }

    return Service.START_NOT_STICKY
  }

  private fun Disposable.disposeOnDestroy() {
    disposables.add(this)
  }

  override fun onDestroy() {
    Timber.v("onDestroy called")
    player.stop()

    mediaSession.release()
    disposables.dispose()

    notifyOnAutoConnectionChange.unregister()
    super.onDestroy()
  }

  companion object {
    private const val NOTIFICATION_ID = 42
  }
}
