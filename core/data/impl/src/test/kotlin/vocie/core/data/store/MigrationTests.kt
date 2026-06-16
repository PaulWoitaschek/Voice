package vocie.core.data.store

import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import voice.core.common.AppInfoProvider
import voice.core.data.GridMode
import voice.core.data.ThemeColorScheme
import voice.core.data.ThemeMode
import voice.core.data.store.AutoRewindAmountStore
import voice.core.data.store.GridModeStore
import voice.core.data.store.SeekTimeStore
import voice.core.data.store.ThemeColorSchemeStore
import voice.core.data.store.ThemeModeStore
import voice.core.data.store.VoiceDataStoreFactory
import voice.core.data.store.intPrefsDataMigration
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant

@Suppress("SUSPICIOUS_UNUSED_MULTIBINDING")
@SingleIn(AppScope::class)
@DependencyGraph(
  scope = AppScope::class,
)
interface MigrationTestGraph {

  @SeekTimeStore
  val seekTimeStore: DataStore<Int>

  @AutoRewindAmountStore
  val autoRewindAmountStore: DataStore<Int>

  @ThemeModeStore
  val themeModeStore: DataStore<ThemeMode>

  @ThemeColorSchemeStore
  val themeColorSchemeStore: DataStore<ThemeColorScheme>

  @GridModeStore
  val gridModeStore: DataStore<GridMode>

  @Provides
  val application: Application get() = ApplicationProvider.getApplicationContext()

  @Provides
  val json: Json get() = Json.Default

  @Provides
  val appInfoProvider: AppInfoProvider
    get() = object : AppInfoProvider {
      override val versionName: String get() = "1.2.3"
      override val analyticsIncluded: Boolean get() = false
      override val supportDevelopmentIncluded: Boolean get() = false
      override val installTime: Instant get() = Instant.parse("2026-06-01T00:00:00Z")
    }

  val sharedPreferences: SharedPreferences
}

@RunWith(AndroidJUnit4::class)
class MigrationTests {

  private val factory: VoiceDataStoreFactory =
    VoiceDataStoreFactory(Json { ignoreUnknownKeys = true }, ApplicationProvider.getApplicationContext())

  private val testGraph: MigrationTestGraph = createGraph()
  private val sharedPreferences: SharedPreferences = testGraph.sharedPreferences

  @Test
  fun `seekTime migrates from SharedPreferences and cleans up`() = runTest {
    val expected = 15
    sharedPreferences.edit {
      clear()
      putInt("SEEK_TIME", expected)
    }

    val store = testGraph.seekTimeStore
    assertEquals(expected = expected, actual = store.data.first())
    assertEquals(expected = false, actual = sharedPreferences.contains("SEEK_TIME"))
  }

  @Test
  fun `autoRewind migrates from SharedPreferences and cleans up`() = runTest {
    val expected = 5
    sharedPreferences.edit {
      clear()
      putInt("AUTO_REWIND", expected)
    }

    val store = testGraph.autoRewindAmountStore
    assertEquals(expected = expected, actual = store.data.first())
    assertEquals(expected = false, actual = sharedPreferences.contains("AUTO_REWIND"))
  }

  @Test
  fun `legacy darkTheme true migrates to dark and cleans up`() = runTest {
    sharedPreferences.edit {
      clear()
      putBoolean("darkTheme", true)
    }

    val store = testGraph.themeModeStore
    assertEquals(expected = ThemeMode.Dark, actual = store.data.first())
    assertEquals(expected = false, actual = sharedPreferences.contains("darkTheme"))
  }

  @Test
  fun `legacy darkTheme false migrates to light and cleans up`() = runTest {
    sharedPreferences.edit {
      clear()
      putBoolean("darkTheme", false)
    }

    val store = testGraph.themeModeStore
    assertEquals(expected = ThemeMode.Light, actual = store.data.first())
    assertEquals(expected = false, actual = sharedPreferences.contains("darkTheme"))
  }

  @Test
  fun `legacy darkTheme DataStore migrates and cleans up`() = runTest {
    sharedPreferences.edit { clear() }
    val oldDataStoreFile = File(ApplicationProvider.getApplicationContext<Application>().filesDir, "datastore/darkTheme")
    oldDataStoreFile.parentFile!!.mkdirs()
    oldDataStoreFile.writeText("true")

    val store = testGraph.themeModeStore
    assertEquals(expected = ThemeMode.Dark, actual = store.data.first())
    assertEquals(expected = false, actual = oldDataStoreFile.exists())
  }

  @Test
  fun `defaults are used when SharedPreferences empty`() = runTest {
    sharedPreferences.edit { clear() }

    assertEquals(expected = 20, actual = testGraph.seekTimeStore.data.first())
    assertEquals(expected = 2, actual = testGraph.autoRewindAmountStore.data.first())
    assertEquals(expected = ThemeMode.FollowSystem, actual = testGraph.themeModeStore.data.first())
    assertEquals(expected = ThemeColorScheme.VoiceBlue, actual = testGraph.themeColorSchemeStore.data.first())
  }

  @Test
  fun `migration is skipped when unrelated key present`() = runTest {
    sharedPreferences.edit {
      clear()
      putInt("OTHER_KEY", 50)
    }

    assertEquals(expected = 20, actual = testGraph.seekTimeStore.data.first())
    assertEquals(expected = true, actual = sharedPreferences.contains("OTHER_KEY"))
  }

  @Test
  fun `multiple migrations applied sequentially`() = runTest {
    sharedPreferences.edit {
      clear()
      putInt("SEEK_TIME", 15)
      putInt("AUTO_REWIND", 5)
      putBoolean("darkTheme", true)
    }

    assertEquals(expected = 15, actual = testGraph.seekTimeStore.data.first())
    assertEquals(expected = 5, actual = testGraph.autoRewindAmountStore.data.first())
    assertEquals(expected = ThemeMode.Dark, actual = testGraph.themeModeStore.data.first())

    listOf("SLEEP_TIME", "SEEK_TIME", "AUTO_REWIND", "darkTheme")
      .forEach {
        assertFalse(sharedPreferences.contains(it))
      }
  }

  @Test
  fun `intPrefsDataMigration migrates and cleans up arbitrary key`() = runTest {
    sharedPreferences.edit {
      clear()
      putInt("TEST_INT_KEY", 123)
    }
    val ds = factory.int(
      fileName = "testInt",
      defaultValue = 0,
      migrations = listOf(intPrefsDataMigration(sharedPreferences, "TEST_INT_KEY")),
    )
    assertEquals(expected = 123, actual = ds.data.first())
    assertEquals(expected = false, actual = sharedPreferences.contains("TEST_INT_KEY"))
  }

  @Test
  fun `migrates LIST from SharedPreferences and cleans up`() = runTest {
    sharedPreferences.edit {
      clear()
      putString("gridView", "LIST")
    }

    val store = testGraph.gridModeStore
    assertEquals(expected = GridMode.LIST, actual = store.data.first())
    assertEquals(expected = false, actual = sharedPreferences.contains("gridView"))
  }

  @Test
  fun `migrates GRID from SharedPreferences and cleans up`() = runTest {
    sharedPreferences.edit {
      clear()
      putString("gridView", "GRID")
    }

    val store = testGraph.gridModeStore
    assertEquals(expected = GridMode.GRID, actual = store.data.first())
    assertEquals(expected = false, actual = sharedPreferences.contains("gridView"))
  }

  @Test
  fun `falls back to FOLLOW_DEVICE when key missing`() = runTest {
    sharedPreferences.edit { clear() }

    val store = testGraph.gridModeStore
    assertEquals(expected = GridMode.FOLLOW_DEVICE, actual = store.data.first())
    assertEquals(expected = false, actual = sharedPreferences.contains("gridView"))
  }

  @Test
  fun `falls back to FOLLOW_DEVICE on unknown legacy value and cleans up`() = runTest {
    sharedPreferences.edit {
      clear()
      putString("gridView", "UNKNOWN")
    }

    val store = testGraph.gridModeStore
    assertEquals(expected = GridMode.FOLLOW_DEVICE, actual = store.data.first())
    assertEquals(expected = false, actual = sharedPreferences.contains("gridView"))
  }
}
