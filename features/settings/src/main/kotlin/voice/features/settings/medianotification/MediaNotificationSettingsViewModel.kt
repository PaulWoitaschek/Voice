package voice.features.settings.medianotification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.data.notification.MediaNotificationPreferences
import voice.core.data.notification.NotificationAction
import voice.core.data.store.MediaNotificationPreferencesStore
import voice.navigation.Navigator

@Inject
class MediaNotificationSettingsViewModel(
  @MediaNotificationPreferencesStore
  private val preferencesStore: DataStore<MediaNotificationPreferences>,
  private val navigator: Navigator,
  dispatcherProvider: DispatcherProvider,
) {

  private val mainScope = MainScope(dispatcherProvider)
  private val editingSlot = mutableStateOf<Int?>(null)

  @Composable
  fun viewState(): MediaNotificationSettingsViewState {
    val preferences by remember { preferencesStore.data }.collectAsState(initial = MediaNotificationPreferences.Default)
    return MediaNotificationSettingsViewState(
      slot1 = preferences.slot1,
      slot2 = preferences.slot2,
      slot3 = preferences.slot3,
      editingSlot = editingSlot.value,
    )
  }

  fun close() {
    navigator.goBack()
  }

  fun editSlot(slot: Int) {
    editingSlot.value = slot
  }

  fun dismissDialog() {
    editingSlot.value = null
  }

  fun selectAction(
    slot: Int,
    action: NotificationAction,
  ) {
    mainScope.launch {
      preferencesStore.updateData { preferences ->
        when (slot) {
          1 -> preferences.copy(slot1 = action)
          2 -> preferences.copy(slot2 = action)
          else -> preferences.copy(slot3 = action)
        }
      }
    }
    editingSlot.value = null
  }
}
