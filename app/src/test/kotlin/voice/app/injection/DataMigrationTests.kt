package voice.app.injection

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import voice.datastore.VoiceDataStoreFactory

@RunWith(AndroidJUnit4::class)
class DataMigrationTests {

  private lateinit var sharedPreferences: SharedPreferences
  private lateinit var factory: VoiceDataStoreFactory

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    sharedPreferences = context.getSharedPreferences("de.ph1b.audiobook_preferences", Context.MODE_PRIVATE)
    factory = VoiceDataStoreFactory(Json { ignoreUnknownKeys = true }, context)
  }

  @Test
  fun `intPrefsDataMigration migrates and cleans up arbitrary key`() = runTest {
    sharedPreferences.edit {
      clear()
      putInt("TEST_INT_KEY", 123)
    }
    val dataStore = factory.int(
      fileName = "testInt",
      defaultValue = 0,
      migrations = listOf(intPrefsDataMigration(sharedPreferences, "TEST_INT_KEY")),
    )
    dataStore.data.first() shouldBe 123
    sharedPreferences.contains("TEST_INT_KEY") shouldBe false
  }

  @Test
  fun `booleanPrefsDataMigration migrates and cleans up arbitrary key`() = runTest {
    sharedPreferences.edit {
      clear()
      putBoolean("testBoolKey", true)
    }
    val dataStore = factory.boolean(
      fileName = "testBool",
      defaultValue = false,
      migrations = listOf(booleanPrefsDataMigration(sharedPreferences, "testBoolKey")),
    )
    dataStore.data.first() shouldBe true
    sharedPreferences.contains("testBoolKey") shouldBe false
  }
}
