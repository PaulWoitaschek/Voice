package de.ph1b.audiobook.playback

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import androidx.core.content.ContextCompat
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.persistence.pref.Pref
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Class for controlling the player through the service
 */
@Singleton
class PlayerController
@Inject constructor(
  private val context: Context,
  private val repo: BookRepository,
  @Named(PrefKeys.CURRENT_BOOK)
  private val currentBookIdPref: Pref<UUID>
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

  fun execute(command: PlayerCommand) {
    Timber.d("execute $command")
    val bookExists = repo.bookById(currentBookIdPref.value) != null
    if (bookExists) {
      Timber.d("execute $command")
      ContextCompat.startForegroundService(context, command.toServiceIntent(context))
    } else {
      Timber.w("ignore $command because there is no book.")
    }
  }

  fun stop() = execute { it.stop() }

  fun fastForward() = execute { it.fastForward() }

  fun rewind() = execute { it.rewind() }

  fun play() = execute { it.play() }

  fun playPause() = execute { it.playPause() }

  private inline fun execute(action: (MediaControllerCompat.TransportControls) -> Unit) {
    _controller?.transportControls?.let(action)
  }

  fun fadeOut() {
    // todo implement
  }

  fun cancelFadeout() {
    // todo implement
  }
}
