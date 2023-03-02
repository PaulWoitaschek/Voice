package voice.playback.session

import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import voice.common.rootComponentAs
import voice.playback.di.PlaybackComponent
import voice.playback.player.VoicePlayer
import javax.inject.Inject

class PlaybackService : MediaLibraryService() {

  @Inject
  lateinit var session: MediaLibrarySession

  @Inject
  lateinit var player: VoicePlayer

  override fun onCreate() {
    super.onCreate()
    rootComponentAs<PlaybackComponent.Provider>()
      .playbackComponentFactory
      .create(this)
      .inject(this)
  }

  override fun onDestroy() {
    super.onDestroy()
    player.release()
    session.release()
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session
}
