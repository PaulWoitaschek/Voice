package voice.playback.session

import android.content.IntentFilter
import android.media.AudioManager
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import de.paulwoitaschek.flowpref.Pref
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import voice.common.pref.PrefKeys
import voice.common.rootComponentAs
import voice.logging.core.Logger
import voice.playback.PlayerController
import voice.playback.di.PlaybackComponent
import voice.playback.misc.flowBroadcastReceiver
import voice.playback.player.VoicePlayer
import voice.playback.playstate.PlayStateManager
import voice.playback.session.headset.HeadsetState
import voice.playback.session.headset.headsetStateChangeFlow
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration.Companion.seconds

class PlaybackService : MediaLibraryService() {

  @Inject
  lateinit var session: MediaLibrarySession

  @Inject
  lateinit var scope: CoroutineScope

  @Inject
  lateinit var player: VoicePlayer

  @Inject
  lateinit var playerController: PlayerController

  @field:[
    Inject
    Named(PrefKeys.RESUME_ON_REPLUG)
  ]
  lateinit var resumeOnReplugPref: Pref<Boolean>

  @Inject
  lateinit var playStateManager: PlayStateManager

  override fun onCreate() {
    super.onCreate()
    rootComponentAs<PlaybackComponent.Provider>()
      .playbackComponentFactory
      .create(this)
      .inject(this)

    headsetStateChangeFlow()
      .filter { it == HeadsetState.Plugged }
      .onEach { headsetPlugged() }
      .launchIn(scope)

    flowBroadcastReceiver(IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
      .onEach { audioBecomingNoisy() }
      .launchIn(scope)
  }

  private fun audioBecomingNoisy() {
    Logger.d("audio becoming noisy. playState=${playStateManager.playState}")
    scope.launch {
      if (playStateManager.playState === PlayStateManager.PlayState.Playing) {
        playStateManager.pauseReason = PlayStateManager.PauseReason.BecauseHeadset
        playerController.pauseWithRewind(rewind = 2.seconds)
      }
    }
  }

  private fun headsetPlugged() {
    if (playStateManager.pauseReason == PlayStateManager.PauseReason.BecauseHeadset) {
      if (resumeOnReplugPref.value) {
        playerController.play()
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    scope.cancel()
    player.release()
    session.release()
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session
}
