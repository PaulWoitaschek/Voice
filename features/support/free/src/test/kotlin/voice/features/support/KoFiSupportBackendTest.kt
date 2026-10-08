package voice.features.support

import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import voice.navigation.Destination
import voice.navigation.Navigator
import kotlin.test.Test
import kotlin.test.assertEquals

class KoFiSupportBackendTest {

  @Test
  fun `state is Ko-fi`() = runTest {
    val backend = createBackend()

    backend.state.test {
      assertEquals(expected = SupportBackendState.KoFi, actual = awaitItem())
    }
  }

  @Test
  fun `openKoFi opens Ko-fi`() = runTest {
    val navigator = mockk<Navigator> {
      every { goTo(any()) } just Runs
    }
    val backend = createBackend(navigator = navigator)

    backend.openKoFi()

    verify(exactly = 1) {
      navigator.goTo(Destination.Website("https://ko-fi.com/paul_voice"))
    }
  }

  private fun createBackend(
    navigator: Navigator = mockk {
      every { goTo(any()) } just Runs
    },
  ): KoFiSupportBackend {
    return KoFiSupportBackend(
      navigator = navigator,
    )
  }
}
