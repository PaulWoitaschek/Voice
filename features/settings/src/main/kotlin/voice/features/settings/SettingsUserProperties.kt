package voice.features.settings

import android.app.Application
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import voice.core.analytics.api.Analytics
import voice.core.initializer.AppInitializer
import voice.core.playback.AutoRewindAmountStore
import voice.core.playback.LockScreenSliderStore
import voice.core.playback.SeekTimeStore
import voice.core.sleeptimer.SleepTimerPreferenceStore
import voice.core.ui.CoverColorsStore
import voice.core.ui.GridMode
import voice.core.ui.GridModeStore
import voice.core.ui.PureBlackStore
import voice.core.ui.ThemeColorScheme
import voice.core.ui.ThemeColorSchemeStore
import voice.core.ui.ThemeMode
import voice.core.ui.ThemeModeStore

@ContributesIntoSet(AppScope::class)
@Inject
class SettingsUserProperties(
  private val analytics: Analytics,
  private val scope: CoroutineScope,
  private val themeModeStore: ThemeModeStore,
  private val pureBlackStore: PureBlackStore,
  private val themeColorSchemeStore: ThemeColorSchemeStore,
  private val coverColorsStore: CoverColorsStore,
  private val seekTimeStore: SeekTimeStore,
  private val autoRewindAmountStore: AutoRewindAmountStore,
  private val lockScreenSliderStore: LockScreenSliderStore,
  private val sleepTimerPreferenceStore: SleepTimerPreferenceStore,
  private val gridModeStore: GridModeStore,
) : AppInitializer {

  override fun onAppStart(application: Application) {
    report("theme", themeModeStore) {
      when (it) {
        ThemeMode.FollowSystem -> "follow_system"
        ThemeMode.Light -> "light"
        ThemeMode.Dark -> "dark"
      }
    }
    report("pure_black", pureBlackStore) { it.onOff() }
    report("color_scheme", themeColorSchemeStore) {
      when (it) {
        ThemeColorScheme.VoiceBlue -> "voice_blue"
        ThemeColorScheme.Dynamic -> "dynamic"
        ThemeColorScheme.Lagoon -> "lagoon"
        ThemeColorScheme.Forest -> "forest"
        ThemeColorScheme.Sunset -> "sunset"
        ThemeColorScheme.Berry -> "berry"
        ThemeColorScheme.Lavender -> "lavender"
      }
    }
    report("cover_colors", coverColorsStore) { it.onOff() }
    report("skip_amount", seekTimeStore) { it.inWholeSeconds.toString() }
    report("auto_rewind", autoRewindAmountStore) { it.inWholeSeconds.toString() }
    report("lock_screen_slider", lockScreenSliderStore) { it.onOff() }
    report("auto_sleep_timer", sleepTimerPreferenceStore) { it.autoSleepTimerEnabled.onOff() }
    report("library_layout", gridModeStore) {
      when (it) {
        GridMode.FOLLOW_DEVICE -> "follow_device"
        GridMode.LIST -> "list"
        GridMode.GRID -> "grid"
      }
    }
  }

  private fun <T> report(
    name: String,
    store: DataStore<T>,
    value: (T) -> String,
  ) {
    scope.launch {
      store.data.map(value).distinctUntilChanged().collect {
        analytics.setUserProperty(name, it)
      }
    }
  }
}

private fun Boolean.onOff(): String = if (this) "on" else "off"
