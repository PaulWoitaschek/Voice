package voice.features.support

import androidx.navigation3.runtime.get
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import voice.navigation.BottomSheetNav
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.navigation.Navigator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
      assertEquals(expected = SupportBackendState.Free, actual = awaitItem().backendState)
    }
  }

  @Test
  fun `openSupport delegates to backend`() {
    viewModel.openSupport()

    assertEquals(expected = 1, actual = backend.openSupportCount)
  }

  @Test
  @Suppress("UNCHECKED_CAST")
  fun `support nav entry is bottom sheet`() {
    val provider = object : SupportProvider {}
      .supportNavEntryProvider() as NavEntryProvider<Destination.SupportVoice>
    val navEntry = provider.create(Destination.SupportVoice)

    assertNotNull(navEntry.metadata[BottomSheetNav.BottomSheetKey])
  }
}

private class FakeSupportBackend : SupportBackend {
  val mutableState = MutableStateFlow<SupportBackendState>(
    SupportBackendState.Free,
  )
  var openSupportCount = 0

  override val state: StateFlow<SupportBackendState> = mutableState

  override fun openSupport() {
    openSupportCount++
  }
}
