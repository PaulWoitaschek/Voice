package voice.playback

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import timber.log.Timber
import voice.data.Chapter
import voice.playback.session.PlaybackService
import voice.playback.session.forcedNext
import voice.playback.session.forcedPrevious
import voice.playback.session.playPause
import voice.playback.session.setPosition
import voice.playback.session.skipSilence
import javax.inject.Inject

class PlayerController
@Inject constructor(
  private val context: Context
) {

  private var _controller: MediaControllerCompat? = null

  private val callback = object : MediaBrowserCompat.ConnectionCallback() {
    override fun onConnected() {
      super.onConnected()
      Timber.d("onConnected")
      _controller = MediaControllerCompat(context, browser.sessionToken)
    }

    override fun onConnectionSuspended() {
      super.onConnectionSuspended()
      Timber.d("onConnectionSuspended")
      _controller = null
    }

    override fun onConnectionFailed() {
      super.onConnectionFailed()
      Timber.d("onConnectionFailed")
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

  fun pause() = execute { it.pause() }

  fun setSpeed(speed: Float) = execute { it.setPlaybackSpeed(speed) }

  private inline fun execute(action: (MediaControllerCompat.TransportControls) -> Unit) {
    _controller?.transportControls?.let(action)
  }
}
