package voice.features.support

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PlayUnavailableSupportBackendTest {

  private val backend = PlayUnavailableSupportBackend()

  @Test
  fun `state is always PlayUnavailable`() = runTest {
    backend.state.test {
      awaitItem() shouldBe SupportBackendState.PlayUnavailable

      backend.openSupport()
      backend.setSupporterBadgeVisible(true)

      expectNoEvents()
    }
  }
}
