package de.ph1b.audiobook.playback

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import de.ph1b.audiobook.common.getIfPresent
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.injection.App
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
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import timber.log.Timber
import java.io.File
import java.util.UUID
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
  ) {
    mediaBrowserHelper.onLoadChildren(
      parentId,
      result
    )
  }

  override fun onGetRoot(
    clientPackageName: String,
    clientUid: Int,
    rootHints: Bundle?
  ): BrowserRoot = mediaBrowserHelper.onGetRoot()

  private val disposables = CompositeDisposable()
  private var isForeground = false

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
  @Inject
  lateinit var audioFocusHelper: AudioFocusHandler
  @field:[Inject Named(PrefKeys.RESUME_ON_REPLUG)]
  lateinit var resumeOnReplugPref: Pref<Boolean>

  override fun onCreate() {
    App.component.playbackComponent()
      .playbackService(this)
      .build()
      .inject(this)
    super.onCreate()

    sessionToken = mediaSession.sessionToken

    // update book when changed by player
    player.bookContentStream.distinctUntilChanged()
      .observeOn(Schedulers.io())
      .subscribe { content ->
        runBlocking {
          repo.bookById(content.id)
            ?.copy(content = content)
            ?.let { repo.updateBook(it) }
        }
      }
      .disposeOnDestroy()

    notifyOnAutoConnectionChange.listen()

    currentBookIdPref.stream
      .subscribe { currentBookIdChanged(it) }
      .disposeOnDestroy()

    val bookUpdated = currentBookIdPref.stream
      .switchMap { repo.byId(it).getIfPresent() }
      .distinctUntilChanged { old, new ->
        old.content == new.content
      }
    bookUpdated
      .subscribe {
        Timber.i("init ${it.name}")
        player.init(it.content)
        GlobalScope.launch {
          changeNotifier.notify(ChangeNotifier.Type.METADATA, it, autoConnected.connected)
        }
      }
      .disposeOnDestroy()

    bookUpdated
      .distinctUntilChanged { book -> book.content.currentChapter }
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

  private fun currentBookIdChanged(id: UUID) {
    if (player.bookContent?.id != id) {
      player.stop()
      repo.bookById(id)?.let { player.init(it.content) }
    }
  }

  private fun headsetPlugged() {
    if (playStateManager.pauseReason == PauseReason.BECAUSE_HEADSET) {
      if (resumeOnReplugPref.value) {
        play()
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
    currentBook()?.let {
      changeNotifier.notify(ChangeNotifier.Type.PLAY_STATE, it, autoConnected.connected)
    }
  }

  private fun currentBook(): Book? {
    val id = currentBookIdPref.value
    return repo.bookById(id)
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
    currentBook()?.let {
      updateNotification(it)
    }
  }

  private suspend fun handlePlaybackStatePlaying() {
    audioFocusHelper.request()
    Timber.d("set mediaSession to active")
    mediaSession.isActive = true
    currentBook()?.let {
      val notification = notificationCreator.createNotification(it)
      startForeground(NOTIFICATION_ID, notification)
      isForeground = true
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Timber.v("onStartCommand, intent=$intent, flags=$flags, startId=$startId")

    when (intent?.action) {
      Intent.ACTION_MEDIA_BUTTON -> androidx.media.session.MediaButtonReceiver.handleIntent(
        mediaSession,
        intent
      )
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
      PlayerController.ACTION_SKIP_SILENCE -> {
        val skipSilences = intent.getBooleanExtra(PlayerController.SKIP_SILENCE, false)
        player.setSkipSilences(skipSilences)
      }
      PlayerController.ACTION_PLAY_PAUSE -> {
        if (playStateManager.playState == PlayState.PLAYING) {
          player.pause(true)
        } else {
          play()
        }
      }
      PlayerController.ACTION_STOP -> player.stop()
      PlayerController.ACTION_PLAY -> play()
      PlayerController.ACTION_REWIND -> player.skip(forward = false)
      PlayerController.ACTION_REWIND_AUTO_PLAY -> {
        player.skip(forward = false)
        play()
      }
      PlayerController.ACTION_FAST_FORWARD -> player.skip(forward = true)
      PlayerController.ACTION_FAST_FORWARD_AUTO_PLAY -> {
        player.skip(forward = true)
        play()
      }
    }

    return Service.START_NOT_STICKY
  }

  private fun play() {
    GlobalScope.launch { repo.markBookAsPlayedNow(currentBookIdPref.value) }
    player.play()
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
