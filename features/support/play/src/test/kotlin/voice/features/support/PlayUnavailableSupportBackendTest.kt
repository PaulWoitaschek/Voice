package voice.features.support

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayUnavailableSupportBackendTest {

  private val backend = PlayUnavailableSupportBackend()

  @Test
  fun `state is always PlayUnavailable`() = runTest {
    backend.state.test {
      assertEquals(expected = SupportBackendState.PlayUnavailable, actual = awaitItem())

      backend.openSupport()

      expectNoEvents()
    }
  }
}
