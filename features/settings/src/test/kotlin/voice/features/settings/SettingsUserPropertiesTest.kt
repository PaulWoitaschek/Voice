package voice.features.settings

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import voice.core.analytics.api.Analytics
import voice.core.datastore.VoiceDataStoreFactory
import voice.core.playback.AutoRewindAmountStore
import voice.core.playback.LockScreenSliderStore
import voice.core.playback.SeekTimeStore
import voice.core.sleeptimer.SleepTimerPreference
import voice.core.sleeptimer.SleepTimerPreferenceStore
import voice.core.ui.CoverColorsStore
import voice.core.ui.GridMode
import voice.core.ui.GridModeStore
import voice.core.ui.PureBlackStore
import voice.core.ui.ThemeColorScheme
import voice.core.ui.ThemeColorSchemeStore
import voice.core.ui.ThemeMode
import voice.core.ui.ThemeModeStore

@RunWith(AndroidJUnit4::class)
class SettingsUserPropertiesTest {

  private val scope = TestScope()
  private val analytics = mockk<Analytics>(relaxUnitFun = true)
  private val pureBlackStore = MemoryDataStore(false)
  private val autoRewindAmountStore = MemoryDataStore(2.seconds)
  private val sleepTimerPreferenceStore = MemoryDataStore(SleepTimerPreference.Default)

  private fun TestScope.startTheApp(
    themeMode: DataStore<ThemeMode> = MemoryDataStore(ThemeMode.FollowSystem),
    pureBlack: DataStore<Boolean> = pureBlackStore,
    themeColorScheme: DataStore<ThemeColorScheme> = MemoryDataStore(ThemeColorScheme.VoiceBlue),
    coverColors: DataStore<Boolean> = MemoryDataStore(true),
    seekTime: DataStore<Duration> = MemoryDataStore(20.seconds),
    autoRewindAmount: DataStore<Duration> = autoRewindAmountStore,
    lockScreenSlider: DataStore<Boolean> = MemoryDataStore(true),
    sleepTimerPreference: DataStore<SleepTimerPreference> = sleepTimerPreferenceStore,
    gridMode: DataStore<GridMode> = MemoryDataStore(GridMode.FOLLOW_DEVICE),
  ) {
    SettingsUserProperties(
        analytics = analytics,
        scope = backgroundScope,
        themeModeStore = ThemeModeStore(themeMode),
        pureBlackStore = PureBlackStore(pureBlack),
        themeColorSchemeStore = ThemeColorSchemeStore(themeColorScheme),
        coverColorsStore = CoverColorsStore(coverColors),
        seekTimeStore = SeekTimeStore(seekTime),
        autoRewindAmountStore = AutoRewindAmountStore(autoRewindAmount),
        lockScreenSliderStore = LockScreenSliderStore(lockScreenSlider),
        sleepTimerPreferenceStore = SleepTimerPreferenceStore(sleepTimerPreference),
        gridModeStore = GridModeStore(gridMode),
      )
      .onAppStart(ApplicationProvider.getApplicationContext())
    runCurrent()
  }

  private suspend fun <T> defaultOf(store: DataStore<T>): DataStore<T> =
    MemoryDataStore(store.data.first())

  @Test
  fun `never opened settings, start the app, settings reported with their defaults`() =
    scope.runTest {
      val application: Application = ApplicationProvider.getApplicationContext()
      val sharedPreferences = application.getSharedPreferences("prefs", Context.MODE_PRIVATE)
      val factory = VoiceDataStoreFactory(Json.Default, application)

      startTheApp(
        themeMode = defaultOf(ThemeModeStore(factory, application, sharedPreferences)),
        pureBlack = defaultOf(PureBlackStore(factory)),
        themeColorScheme = defaultOf(ThemeColorSchemeStore(factory)),
        coverColors = defaultOf(CoverColorsStore(factory)),
        seekTime = defaultOf(SeekTimeStore(factory, sharedPreferences)),
        autoRewindAmount = defaultOf(AutoRewindAmountStore(factory, sharedPreferences)),
        lockScreenSlider = defaultOf(LockScreenSliderStore(factory)),
        sleepTimerPreference = defaultOf(SleepTimerPreferenceStore(factory)),
        gridMode = defaultOf(GridModeStore(factory, sharedPreferences)),
      )

      verify(exactly = 1) {
        analytics.setUserProperty("theme", "follow_system")
        analytics.setUserProperty("pure_black", "off")
        analytics.setUserProperty("color_scheme", "voice_blue")
        analytics.setUserProperty("cover_colors", "on")
        analytics.setUserProperty("skip_amount", "20")
        analytics.setUserProperty("auto_rewind", "2")
        analytics.setUserProperty("lock_screen_slider", "on")
        analytics.setUserProperty("auto_sleep_timer", "off")
        analytics.setUserProperty("library_layout", "follow_device")
      }
      verify(exactly = 9) { analytics.setUserProperty(any(), any()) }
    }

  @Test
  fun `settings, turn on pure black, pure black reported as on`() = scope.runTest {
    startTheApp()

    pureBlackStore.updateData { true }
    runCurrent()

    verify(exactly = 1) { analytics.setUserProperty("pure_black", "on") }
  }

  @Test
  fun `settings, change auto rewind to off, auto rewind reported as off`() = scope.runTest {
    startTheApp()

    autoRewindAmountStore.updateData { 0.seconds }
    runCurrent()

    verify(exactly = 1) { analytics.setUserProperty("auto_rewind", "0") }
  }

  @Test
  fun `settings, turn on the automatic sleep timer, automatic sleep timer reported as on, without its times`() =
    scope.runTest {
      startTheApp()

      sleepTimerPreferenceStore.updateData { it.copy(autoSleepTimerEnabled = true) }
      runCurrent()
      sleepTimerPreferenceStore.updateData {
        it.copy(
          autoSleepStartTime = it.autoSleepStartTime.plusHours(1),
          autoSleepDuration = 30.minutes,
        )
      }
      runCurrent()

      verify(exactly = 1) { analytics.setUserProperty("auto_sleep_timer", "on") }
      verify(exactly = 10) { analytics.setUserProperty(any(), any()) }
    }

  private fun reported(name: String): List<String> {
    val values = mutableListOf<String>()
    verify { analytics.setUserProperty(name, capture(values)) }
    return values
  }

  @Test
  fun `the theme reports follow system, light and dark`() = scope.runTest {
    val themeMode = MemoryDataStore(ThemeMode.FollowSystem)
    startTheApp(themeMode = themeMode)

    themeMode.updateData { ThemeMode.Light }
    runCurrent()
    themeMode.updateData { ThemeMode.Dark }
    runCurrent()

    assertEquals(expected = listOf("follow_system", "light", "dark"), actual = reported("theme"))
  }

  @Test
  fun `the color scheme reports each scheme`() = scope.runTest {
    val themeColorScheme = MemoryDataStore(ThemeColorScheme.VoiceBlue)
    startTheApp(themeColorScheme = themeColorScheme)

    listOf(
        ThemeColorScheme.Dynamic,
        ThemeColorScheme.Berry,
        ThemeColorScheme.Forest,
        ThemeColorScheme.Lagoon,
        ThemeColorScheme.Lavender,
        ThemeColorScheme.Sunset,
      )
      .forEach { scheme ->
        themeColorScheme.updateData { scheme }
        runCurrent()
      }

    assertEquals(
      expected = listOf("voice_blue", "dynamic", "berry", "forest", "lagoon", "lavender", "sunset"),
      actual = reported("color_scheme"),
    )
  }

  @Test
  fun `the library layout reports follow device, list and grid`() = scope.runTest {
    val gridMode = MemoryDataStore(GridMode.FOLLOW_DEVICE)
    startTheApp(gridMode = gridMode)

    gridMode.updateData { GridMode.LIST }
    runCurrent()
    gridMode.updateData { GridMode.GRID }
    runCurrent()

    assertEquals(
      expected = listOf("follow_device", "list", "grid"),
      actual = reported("library_layout"),
    )
  }

  @Test
  fun `pure black, cover colors, lock screen slider and automatic sleep timer report off and on`() =
    scope.runTest {
      val coverColors = MemoryDataStore(false)
      val lockScreenSlider = MemoryDataStore(false)
      startTheApp(coverColors = coverColors, lockScreenSlider = lockScreenSlider)

      pureBlackStore.updateData { true }
      coverColors.updateData { true }
      lockScreenSlider.updateData { true }
      sleepTimerPreferenceStore.updateData { it.copy(autoSleepTimerEnabled = true) }
      runCurrent()

      listOf("pure_black", "cover_colors", "lock_screen_slider", "auto_sleep_timer").forEach {
        assertEquals(expected = listOf("off", "on"), actual = reported(it))
      }
    }
}
