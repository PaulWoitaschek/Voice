package voice.features.support

import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import voice.navigation.Navigator

class SupportViewModelTest {

  private val scope = TestScope()
  private val backend = FakeSupportBackend()
  private val navigator = mockk<Navigator> {
    every { goBack() } just Runs
  }
  private val viewModel = SupportViewModel(
    backend = backend,
    navigator = navigator,
  )

  @Test
  fun `view state exposes backend state`() = scope.runTest {
    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      awaitItem().backendState shouldBe SupportBackendState.Free(supporterBadgeVisible = false)

      backend.mutableState.value = SupportBackendState.Free(supporterBadgeVisible = true)

      awaitItem().backendState shouldBe SupportBackendState.Free(supporterBadgeVisible = true)
    }
  }

  @Test
  fun `openSupport delegates to backend`() {
    viewModel.openSupport()

    backend.openSupportCount shouldBe 1
  }

  @Test
  fun `setSupporterBadgeVisible delegates to backend`() {
    viewModel.setSupporterBadgeVisible(true)

    backend.lastSupporterBadgeVisible shouldBe true
  }
}

private class FakeSupportBackend : SupportBackend {
  val mutableState = MutableStateFlow<SupportBackendState>(
    SupportBackendState.Free(supporterBadgeVisible = false),
  )
  var openSupportCount = 0
  var lastSupporterBadgeVisible = false

  override val state: StateFlow<SupportBackendState> = mutableState

  override fun openSupport() {
    openSupportCount++
  }

  override fun setSupporterBadgeVisible(visible: Boolean) {
    lastSupporterBadgeVisible = visible
  }
}
