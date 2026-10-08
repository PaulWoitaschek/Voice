package voice.core.playback.session

import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import voice.core.common.rootGraphAs
import voice.core.logging.api.Logger
import voice.core.playback.di.PlaybackGraph
import voice.core.playback.player.VoicePlayer
import voice.core.playback.playstate.PositionUpdater
import voice.core.playback.stats.ListeningSessionRecorder

class PlaybackService : MediaLibraryService() {

  @Inject
  lateinit var session: MediaLibrarySession

  @Inject
  lateinit var scope: CoroutineScope

  @Inject
  lateinit var player: VoicePlayer

  @Inject
  lateinit var positionUpdater: PositionUpdater

  @Inject
  lateinit var listeningSessionRecorder: ListeningSessionRecorder

  private var released = false

  override fun onCreate() {
    super.onCreate()
    rootGraphAs<PlaybackGraph.Provider>()
      .playbackGraphFactory
      .create(this)
      .inject(this)
  }

  private fun release() {
    released = true
    runBlocking {
      positionUpdater.flushPositionNow()
    }
    positionUpdater.release()
    listeningSessionRecorder.stopped()
    player.release()
    session.release()
    scope.cancel()
  }

  override fun onDestroy() {
    release()
    super.onDestroy()
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
    // Media3 rejects a released session, and controllers can still connect while the service shuts down.
    if (released) {
      Logger.w("onGetSession returns null because the session is already released")
      return null
    }
    return session
  }
}
