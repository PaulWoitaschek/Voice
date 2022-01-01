package de.ph1b.audiobook.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import de.ph1b.audiobook.playback.session.PlaybackService
import de.ph1b.audiobook.playback.session.forcedNext
import de.ph1b.audiobook.playback.session.forcedPrevious
import de.ph1b.audiobook.playback.session.playPause
import de.ph1b.audiobook.playback.session.setLoudnessGain
import de.ph1b.audiobook.playback.session.setPosition
import de.ph1b.audiobook.playback.session.skipSilence
import timber.log.Timber
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

  fun setPosition(time: Long, uri: Uri) = execute { it.setPosition(time, uri) }

  fun setLoudnessGain(mB: Int) = execute { it.setLoudnessGain(mB) }

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
