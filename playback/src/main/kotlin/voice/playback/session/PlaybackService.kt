package voice.playback.session

import android.content.Intent
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import voice.common.rootComponentAs
import voice.logging.core.Logger
import voice.playback.di.PlaybackComponent
import voice.playback.player.VoicePlayer
import javax.inject.Inject

class PlaybackService : MediaLibraryService() {

  @Inject
  lateinit var session: MediaLibrarySession

  @Inject
  lateinit var scope: CoroutineScope

  @Inject
  lateinit var player: VoicePlayer

  @Inject
  lateinit var voiceNotificationProvider: VoiceMediaNotificationProvider

  override fun onCreate() {
    super.onCreate()
    rootComponentAs<PlaybackComponent.Provider>()
      .playbackComponentFactory
      .create(this)
      .inject(this)
    setMediaNotificationProvider(voiceNotificationProvider)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return try {
      super.onStartCommand(intent, flags, startId)
    } catch (e: Exception) {
      Logger.e(e, "onStartCommand crashed")
      START_STICKY
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    release()
  }

  override fun onTaskRemoved(rootIntent: Intent?) {
    super.onTaskRemoved(rootIntent)
    release()
    stopSelf()
  }

  private fun release() {
    scope.cancel()
    player.release()
    session.release()
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
    return session.takeUnless { session ->
      session.invokeIsReleased
    }.also {
      if (it == null) {
        Logger.e("onGetSession returns null because the session is already released")
      }
    }
  }
}

private val MediaSession.invokeIsReleased: Boolean
  get() = try {
    // temporarily checked to debug
    // https://github.com/androidx/media/issues/422
    MediaSession::class.java.getDeclaredMethod("isReleased")
      .apply { isAccessible = true }
      .invoke(this) as Boolean
  } catch (e: Exception) {
    Logger.e(e, "Couldn't check if it's released")
    false
  }
