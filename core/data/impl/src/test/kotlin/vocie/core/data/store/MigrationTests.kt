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
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test
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
    store.data.first() shouldBe expected
    sharedPreferences.contains("SEEK_TIME") shouldBe false
  }

  @Test
  fun `autoRewind migrates from SharedPreferences and cleans up`() = runTest {
    val expected = 5
    sharedPreferences.edit {
      clear()
      putInt("AUTO_REWIND", expected)
    }

    val store = testGraph.autoRewindAmountStore
    store.data.first() shouldBe expected
    sharedPreferences.contains("AUTO_REWIND") shouldBe false
  }

  @Test
  fun `legacy darkTheme true migrates to dark and cleans up`() = runTest {
    sharedPreferences.edit {
      clear()
      putBoolean("darkTheme", true)
    }

    val store = testGraph.themeModeStore
    store.data.first() shouldBe ThemeMode.Dark
    sharedPreferences.contains("darkTheme") shouldBe false
  }

  @Test
  fun `legacy darkTheme false migrates to light and cleans up`() = runTest {
    sharedPreferences.edit {
      clear()
      putBoolean("darkTheme", false)
    }

    val store = testGraph.themeModeStore
    store.data.first() shouldBe ThemeMode.Light
    sharedPreferences.contains("darkTheme") shouldBe false
  }

  @Test
  fun `legacy darkTheme DataStore migrates and cleans up`() = runTest {
    sharedPreferences.edit { clear() }
    val oldDataStoreFile = File(ApplicationProvider.getApplicationContext<Application>().filesDir, "datastore/darkTheme")
    oldDataStoreFile.parentFile!!.mkdirs()
    oldDataStoreFile.writeText("true")

    val store = testGraph.themeModeStore
    store.data.first() shouldBe ThemeMode.Dark
    oldDataStoreFile.exists() shouldBe false
  }

  @Test
  fun `defaults are used when SharedPreferences empty`() = runTest {
    sharedPreferences.edit { clear() }

    testGraph.seekTimeStore.data.first() shouldBe 20
    testGraph.autoRewindAmountStore.data.first() shouldBe 2
    testGraph.themeModeStore.data.first() shouldBe ThemeMode.FollowSystem
    testGraph.themeColorSchemeStore.data.first() shouldBe ThemeColorScheme.VoiceBlue
  }

  @Test
  fun `migration is skipped when unrelated key present`() = runTest {
    sharedPreferences.edit {
      clear()
      putInt("OTHER_KEY", 50)
    }

    testGraph.seekTimeStore.data.first() shouldBe 20
    sharedPreferences.contains("OTHER_KEY") shouldBe true
  }

  @Test
  fun `multiple migrations applied sequentially`() = runTest {
    sharedPreferences.edit {
      clear()
      putInt("SEEK_TIME", 15)
      putInt("AUTO_REWIND", 5)
      putBoolean("darkTheme", true)
    }

    testGraph.seekTimeStore.data.first() shouldBe 15
    testGraph.autoRewindAmountStore.data.first() shouldBe 5
    testGraph.themeModeStore.data.first() shouldBe ThemeMode.Dark

    listOf("SLEEP_TIME", "SEEK_TIME", "AUTO_REWIND", "darkTheme")
      .forEach {
        sharedPreferences.contains(it).shouldBeFalse()
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
    ds.data.first() shouldBe 123
    sharedPreferences.contains("TEST_INT_KEY") shouldBe false
  }

  @Test
  fun `migrates LIST from SharedPreferences and cleans up`() = runTest {
    sharedPreferences.edit {
      clear()
      putString("gridView", "LIST")
    }

    val store = testGraph.gridModeStore
    store.data.first() shouldBe GridMode.LIST
    sharedPreferences.contains("gridView") shouldBe false
  }

  @Test
  fun `migrates GRID from SharedPreferences and cleans up`() = runTest {
    sharedPreferences.edit {
      clear()
      putString("gridView", "GRID")
    }

    val store = testGraph.gridModeStore
    store.data.first() shouldBe GridMode.GRID
    sharedPreferences.contains("gridView") shouldBe false
  }

  @Test
  fun `falls back to FOLLOW_DEVICE when key missing`() = runTest {
    sharedPreferences.edit { clear() }

    val store = testGraph.gridModeStore
    store.data.first() shouldBe GridMode.FOLLOW_DEVICE
    sharedPreferences.contains("gridView") shouldBe false
  }

  @Test
  fun `falls back to FOLLOW_DEVICE on unknown legacy value and cleans up`() = runTest {
    sharedPreferences.edit {
      clear()
      putString("gridView", "UNKNOWN")
    }

    val store = testGraph.gridModeStore
    store.data.first() shouldBe GridMode.FOLLOW_DEVICE
    sharedPreferences.contains("gridView") shouldBe false
  }
}
