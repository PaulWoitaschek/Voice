package voice.features.settings.medianotification

import androidx.datastore.core.DataStore
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import voice.core.common.DispatcherProvider
import voice.core.data.notification.MediaNotificationPreferences
import voice.core.data.notification.NotificationAction
import voice.navigation.Navigator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaNotificationSettingsViewModelTest {

  private val scope = TestScope()
  private val preferencesStore = MemoryDataStore(MediaNotificationPreferences.Default)
  private val navigator = mockk<Navigator> {
    every { goBack() } just Runs
  }

  private val viewModel = MediaNotificationSettingsViewModel(
    preferencesStore = preferencesStore,
    navigator = navigator,
    dispatcherProvider = DispatcherProvider(scope.coroutineContext, scope.coroutineContext, scope.coroutineContext),
  )

  @Test
  fun `view state defaults to the stored preferences with no dialog open`() = scope.runTest {
    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      awaitItem().let {
        assertEquals(expected = NotificationAction.REWIND, actual = it.slot1)
        assertEquals(expected = NotificationAction.FAST_FORWARD, actual = it.slot2)
        assertEquals(expected = NotificationAction.SLEEP_TIMER, actual = it.slot3)
        assertNull(it.editingSlot)
      }
    }
  }

  @Test
  fun `editing a slot opens and closes the dialog`() = scope.runTest {
    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      assertNull(awaitItem().editingSlot)

      viewModel.editSlot(2)
      assertEquals(expected = 2, actual = awaitItem().editingSlot)

      viewModel.dismissDialog()
      assertNull(awaitItem().editingSlot)
    }
  }

  @Test
  fun `selecting an action persists it to the matching slot and closes the dialog`() = scope.runTest {
    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      awaitItem()

      viewModel.editSlot(3)
      assertEquals(expected = 3, actual = awaitItem().editingSlot)

      viewModel.selectAction(3, NotificationAction.BOOKMARK)
      awaitItem().let {
        assertEquals(expected = NotificationAction.BOOKMARK, actual = it.slot3)
        assertNull(it.editingSlot)
      }
    }
  }

  @Test
  fun `close navigates back`() {
    viewModel.close()

    verify { navigator.goBack() }
  }

  private class MemoryDataStore<T>(initial: T) : DataStore<T> {

    private val value = MutableStateFlow(initial)

    override val data: Flow<T> get() = value

    override suspend fun updateData(transform: suspend (t: T) -> T): T {
      return value.updateAndGet { transform(it) }
    }
  }
}
