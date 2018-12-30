package de.ph1b.audiobook.playback

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import de.ph1b.audiobook.common.getIfPresent
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.RxBroadcast
import de.ph1b.audiobook.misc.rxCompletable
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
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
    appComponent.playbackComponent()
      .playbackService(this)
      .build()
      .inject(this)
    super.onCreate()

    sessionToken = mediaSession.sessionToken

    // update book when changed by player
    player.bookContentStream.map { it.settings }
      .distinctUntilChanged()
      .switchMapCompletable { settings ->
        rxCompletable { repo.updateBookSettings(settings) }
      }
      .subscribe()
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
      .doOnNext {
        Timber.i("init ${it.name}")
        player.init(it.content)
      }
      .switchMapCompletable {
        rxCompletable {
          changeNotifier.notify(ChangeNotifier.Type.METADATA, it, autoConnected.connected)
        }
      }
      .subscribe()
      .disposeOnDestroy()

    bookUpdated
      .distinctUntilChanged { book -> book.content.currentChapter }
      .switchMapCompletable {
        rxCompletable {
          if (isForeground) {
            updateNotification(it)
          }
        }
      }
      .subscribe()
      .disposeOnDestroy()

    playStateManager.playStateStream()
      .observeOn(Schedulers.io())
      .switchMapCompletable {
        rxCompletable { handlePlaybackState(it) }
      }
      .subscribe()
      .disposeOnDestroy()

    HeadsetPlugReceiver.events(this@PlaybackService)
      .filter { it == HeadsetPlugReceiver.HeadsetState.PLUGGED }
      .subscribe { headsetPlugged() }
      .disposeOnDestroy()

    repo.booksStream()
      .map { it.size }
      .distinctUntilChanged()
      .subscribe {
        notifyChildrenChanged(bookUriConverter.allBooksId())
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          // Android O has the dumb restriction that a service that was launched by startForegroundService must go to foreground within
          // 10 seconds - even if we are going to stop it anyways.
          // @see [https://issuetracker.google.com/issues/76112072]
          startForeground(NOTIFICATION_ID, notificationCreator.createDummyNotification())
        }
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
      Intent.ACTION_MEDIA_BUTTON -> {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
      }
      PlayerController.ACTION_SPEED -> {
        val speed = intent.getFloatExtra(PlayerController.EXTRA_SPEED, 1F)
        player.setPlaybackSpeed(speed)
      }
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

  override fun onLoadChildren(
    parentId: String,
    result: Result<List<MediaBrowserCompat.MediaItem>>
  ) {
    result.detach()
    val job = GlobalScope.launch {
      val children = mediaBrowserHelper.loadChildren(parentId)
      result.sendResult(children)
    }
    Disposables.fromAction { job.cancel() }.disposeOnDestroy()
  }

  override fun onGetRoot(
    clientPackageName: String,
    clientUid: Int,
    rootHints: Bundle?
  ): BrowserRoot {
    return MediaBrowserServiceCompat.BrowserRoot(mediaBrowserHelper.root(), null)
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
