package voice.core.playback

import androidx.media3.session.MediaController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlin.test.Test
import kotlin.test.assertSame

class MediaControllerConnectionTest {

  @Test
  fun `a failed connection is retried`() {
    val failed = CompletableDeferred<MediaController>().apply {
      completeExceptionally(SecurityException("Session rejected the connection request."))
    }
    val connected = CompletableDeferred(controller(connected = true))
    val connection = connection(failed, connected)

    assertSame(expected = connected, actual = connection.get())
  }

  @Test
  fun `a dropped connection is released and retried`() {
    val dropped = controller(connected = false)
    val connected = CompletableDeferred(controller(connected = true))
    val connection = connection(CompletableDeferred(dropped), connected)

    assertSame(expected = connected, actual = connection.get())
    verify { dropped.release() }
  }

  @Test
  fun `a connected controller is kept`() {
    val connected = CompletableDeferred(controller(connected = true))
    val connection = connection(connected)

    assertSame(expected = connected, actual = connection.get())
    assertSame(expected = connected, actual = connection.get())
  }

  private fun connection(vararg controllers: Deferred<MediaController>): MediaControllerConnection {
    val remaining = controllers.iterator()
    return MediaControllerConnection { remaining.next() }
  }

  private fun controller(connected: Boolean): MediaController = mockk(relaxUnitFun = true) {
    every { isConnected } returns connected
  }
}
