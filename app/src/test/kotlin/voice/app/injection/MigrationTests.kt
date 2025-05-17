package voice.app.injection

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import voice.datastore.VoiceDataStoreFactory

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class MigrationTests {

  private lateinit var sharedPreferences: SharedPreferences
  private lateinit var factory: VoiceDataStoreFactory

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    sharedPreferences = context.getSharedPreferences("de.ph1b.audiobook_preferences", Context.MODE_PRIVATE)
    factory = VoiceDataStoreFactory(Json { ignoreUnknownKeys = true }, context)
  }

  @Test
  fun `sleepTime migrates from SharedPreferences and cleans up`() = runTest {
    val expected = 30
    sharedPreferences.edit {
      clear()
      putInt("SLEEP_TIME", expected)
    }

    val store: DataStore<Int> = PrefsModule.provideSleepTimePreference(factory, sharedPreferences)
    store.data.first() shouldBe expected
    sharedPreferences.contains("SLEEP_TIME") shouldBe false
  }

  @Test
  fun `seekTime migrates from SharedPreferences and cleans up`() = runTest {
    val expected = 15
    sharedPreferences.edit {
      clear()
      putInt("SEEK_TIME", expected)
    }

    val store = PrefsModule.provideSeekTimePreference(factory, sharedPreferences)
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

    val store = PrefsModule.provideAutoRewindAmountPreference(factory, sharedPreferences)
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

    val store = PrefsModule.darkThemePref(factory, sharedPreferences)
    store.data.first() shouldBe expected
    sharedPreferences.contains("darkTheme") shouldBe false
  }

  @Test
  fun `defaults are used when SharedPreferences empty`() = runTest {
    sharedPreferences.edit { clear() }

    PrefsModule.provideSleepTimePreference(factory, sharedPreferences).data.first() shouldBe 20
    PrefsModule.provideSeekTimePreference(factory, sharedPreferences).data.first() shouldBe 20
    PrefsModule.provideAutoRewindAmountPreference(factory, sharedPreferences).data.first() shouldBe 2
    PrefsModule.darkThemePref(factory, sharedPreferences).data.first() shouldBe false
  }

  @Test
  fun `migration is skipped when unrelated key present`() = runTest {
    sharedPreferences.edit {
      clear()
      putInt("OTHER_KEY", 50)
    }

    PrefsModule.provideSleepTimePreference(factory, sharedPreferences).data.first() shouldBe 20
    sharedPreferences.contains("OTHER_KEY") shouldBe true
  }

  @Test
  fun `multiple migrations applied sequentially`() = runTest {
    sharedPreferences.edit {
      clear()
      putInt("SLEEP_TIME", 30)
      putInt("SEEK_TIME", 15)
      putInt("AUTO_REWIND", 5)
      putBoolean("darkTheme", true)
    }

    PrefsModule.provideSleepTimePreference(factory, sharedPreferences).data.first() shouldBe 30
    PrefsModule.provideSeekTimePreference(factory, sharedPreferences).data.first() shouldBe 15
    PrefsModule.provideAutoRewindAmountPreference(factory, sharedPreferences).data.first() shouldBe 5
    PrefsModule.darkThemePref(factory, sharedPreferences).data.first() shouldBe true

    listOf("SLEEP_TIME", "SEEK_TIME", "AUTO_REWIND", "darkTheme")
      .forEach {
        sharedPreferences.contains(it).shouldBeFalse()
      }
  }

  @Test
  fun `migration runs only once for sleepTime`() = runTest {
    val initial = 55
    sharedPreferences.edit {
      clear()
      putInt("SLEEP_TIME", initial)
    }

    val sleepStore = PrefsModule.provideSleepTimePreference(factory, sharedPreferences)
    sleepStore.data.first() shouldBe initial
    sharedPreferences.contains("SLEEP_TIME") shouldBe false

    // Second read from the same store
    sleepStore.data.first() shouldBe initial
  }

  @Test
  fun `handle max edge case for sleepTime migration`() = runTest {
    sharedPreferences.edit {
      clear()
      putInt("SLEEP_TIME", Int.MAX_VALUE)
    }
    PrefsModule.provideSleepTimePreference(factory, sharedPreferences)
      .data.first() shouldBe Int.MAX_VALUE
  }

  @Test
  fun `handle negative edge case for sleepTime migration`() = runTest {
    sharedPreferences.edit {
      clear()
      putInt("SLEEP_TIME", -10)
    }
    PrefsModule.provideSleepTimePreference(factory, sharedPreferences)
      .data.first() shouldBe -10
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
}
