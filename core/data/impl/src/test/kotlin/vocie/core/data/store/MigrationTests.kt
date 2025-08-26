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
import voice.core.common.grid.GridMode
import voice.core.data.store.AutoRewindAmountStore
import voice.core.data.store.DarkThemeStore
import voice.core.data.store.GridModeStore
import voice.core.data.store.SeekTimeStore
import voice.core.data.store.VoiceDataStoreFactory
import voice.core.data.store.intPrefsDataMigration

@SingleIn(AppScope::class)
@DependencyGraph(
  scope = AppScope::class,
)
interface MigrationTestGraph {

  @SeekTimeStore
  val seekTimeStore: DataStore<Int>

  @AutoRewindAmountStore
  val autoRewindAmountStore: DataStore<Int>

  @DarkThemeStore
  val darkThemeStore: DataStore<Boolean>

  @GridModeStore
  val gridModeStore: DataStore<GridMode>

  @get:Provides
  val application: Application get() = ApplicationProvider.getApplicationContext()

  @get:Provides
  val json: Json get() = Json.Default

  @get:Provides
  val appInfoProvider: AppInfoProvider
    get() = object : AppInfoProvider {
      override val applicationID: String
        get() = "de.ph1b.audiobook"
      override val versionName: String
        get() = "1.2.3"
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
  fun `darkTheme migrates from SharedPreferences and cleans up`() = runTest {
    val expected = true
    sharedPreferences.edit {
      clear()
      putBoolean("darkTheme", expected)
    }

    val store = testGraph.darkThemeStore
    store.data.first() shouldBe expected
    sharedPreferences.contains("darkTheme") shouldBe false
  }

  @Test
  fun `defaults are used when SharedPreferences empty`() = runTest {
    sharedPreferences.edit { clear() }

    testGraph.seekTimeStore.data.first() shouldBe 20
    testGraph.autoRewindAmountStore.data.first() shouldBe 2
    testGraph.darkThemeStore.data.first() shouldBe false
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
    testGraph.darkThemeStore.data.first() shouldBe true

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
