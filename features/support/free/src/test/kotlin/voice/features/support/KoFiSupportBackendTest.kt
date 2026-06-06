package voice.features.support

import androidx.datastore.core.DataStore
import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import voice.core.common.DispatcherProvider
import voice.navigation.Destination
import voice.navigation.Navigator

class KoFiSupportBackendTest {

  @Test
  fun `state exposes local supporter badge visibility`() = runTest {
    val supporterBadgeVisibleStore = MemoryDataStore(false)
    val backend = createBackend(supporterBadgeVisibleStore)

    backend.state.test {
      awaitItem() shouldBe SupportBackendState.Free(supporterBadgeVisible = false)

      backend.setSupporterBadgeVisible(true)

      awaitItem() shouldBe SupportBackendState.Free(supporterBadgeVisible = true)
    }
  }

  @Test
  fun `openSupport opens Ko-fi`() = runTest {
    val navigator = mockk<Navigator> {
      every { goTo(any()) } just Runs
    }
    val backend = createBackend(navigator = navigator)

    backend.openSupport()

    verify(exactly = 1) {
      navigator.goTo(Destination.Website("https://ko-fi.com/paul_voice"))
    }
  }

  @Test
  fun `setSupporterBadgeVisible updates store`() = runTest {
    val supporterBadgeVisibleStore = MemoryDataStore(false)
    val backend = createBackend(supporterBadgeVisibleStore)

    supporterBadgeVisibleStore.data.test {
      awaitItem() shouldBe false

      backend.setSupporterBadgeVisible(true)
      awaitItem() shouldBe true

      backend.setSupporterBadgeVisible(false)
      awaitItem() shouldBe false
    }
  }

  private fun TestScope.createBackend(
    supporterBadgeVisibleStore: MemoryDataStore<Boolean> = MemoryDataStore(false),
    navigator: Navigator = mockk {
      every { goTo(any()) } just Runs
    },
  ): KoFiSupportBackend {
    return KoFiSupportBackend(
      supporterBadgeVisibleStore = supporterBadgeVisibleStore,
      navigator = navigator,
      dispatcherProvider = DispatcherProvider(
        backgroundScope.coroutineContext,
        backgroundScope.coroutineContext,
        backgroundScope.coroutineContext,
      ),
    )
  }
}

private class MemoryDataStore<T>(initial: T) : DataStore<T> {

  override val data = MutableStateFlow(initial)

  override suspend fun updateData(transform: suspend (t: T) -> T): T {
    return data.updateAndGet { transform(it) }
  }
}
