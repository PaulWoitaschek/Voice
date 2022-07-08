package voice.playback

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import voice.data.Chapter
import voice.logging.core.Logger
import voice.playback.session.PlaybackService
import voice.playback.session.forcedNext
import voice.playback.session.forcedPrevious
import voice.playback.session.pauseWithRewind
import voice.playback.session.playPause
import voice.playback.session.setPosition
import voice.playback.session.setVolume
import voice.playback.session.skipSilence
import javax.inject.Inject
import kotlin.time.Duration

class PlayerController
@Inject constructor(
  private val context: Context
) {

  private var _controller: MediaControllerCompat? = null

  private val callback = object : MediaBrowserCompat.ConnectionCallback() {
    override fun onConnected() {
      super.onConnected()
      Logger.v("onConnected")
      _controller = MediaControllerCompat(context, browser.sessionToken)
    }

    override fun onConnectionSuspended() {
      super.onConnectionSuspended()
      Logger.v("onConnectionSuspended")
      _controller = null
    }

    override fun onConnectionFailed() {
      super.onConnectionFailed()
      Logger.d("onConnectionFailed")
      _controller = null
    }
  }

  private val browser: MediaBrowserCompat = MediaBrowserCompat(
    context,
    ComponentName(context, PlaybackService::class.java),
    callback,
    null
  )

  init {
    browser.connect()
  }

  fun setPosition(time: Long, id: Chapter.Id) = execute { it.setPosition(time, id) }

  fun skipSilence(skip: Boolean) = execute { it.skipSilence(skip) }

  fun fastForward() = execute { it.fastForward() }

  fun rewind() = execute { it.rewind() }

  fun previous() = execute { it.forcedPrevious() }

  fun next() = execute { it.forcedNext() }

  fun play() = execute { it.play() }

  fun playPause() = execute { it.playPause() }

  fun pauseWithRewind(rewind: Duration) = execute { it.pauseWithRewind(rewind) }

  fun setSpeed(speed: Float) = execute { it.setPlaybackSpeed(speed) }

  fun setVolume(volume: Float) = execute {
    require(volume in 0F..1F)
    it.setVolume(volume)
  }

  private inline fun execute(action: (MediaControllerCompat.TransportControls) -> Unit) {
    _controller?.transportControls?.let(action)
  }
}
