package voice.core.playback

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import voice.core.datastore.VoiceDataStoreFactory
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class PlaybackStoreMigrationTest {

  private val application: Application = ApplicationProvider.getApplicationContext()
  private val sharedPreferences: SharedPreferences = application.getSharedPreferences("prefs", Context.MODE_PRIVATE)
  private val factory = VoiceDataStoreFactory(Json.Default, application)

  @Test
  fun `seekTime migrates from SharedPreferences and cleans up`() = runTest {
    sharedPreferences.edit { putInt("SEEK_TIME", 15) }

    assertEquals(expected = 15, actual = SeekTimeStore(factory, sharedPreferences).data.first())
    assertEquals(expected = false, actual = sharedPreferences.contains("SEEK_TIME"))
  }

  @Test
  fun `autoRewind migrates from SharedPreferences and cleans up`() = runTest {
    sharedPreferences.edit { putInt("AUTO_REWIND", 5) }

    assertEquals(expected = 5, actual = AutoRewindAmountStore(factory, sharedPreferences).data.first())
    assertEquals(expected = false, actual = sharedPreferences.contains("AUTO_REWIND"))
  }

  @Test
  fun `defaults are used when SharedPreferences empty`() = runTest {
    assertEquals(expected = 20, actual = SeekTimeStore(factory, sharedPreferences).data.first())
    assertEquals(expected = 2, actual = AutoRewindAmountStore(factory, sharedPreferences).data.first())
  }

  @Test
  fun `each store migrates only its own key`() = runTest {
    sharedPreferences.edit {
      putInt("SEEK_TIME", 15)
      putInt("AUTO_REWIND", 5)
    }

    assertEquals(expected = 15, actual = SeekTimeStore(factory, sharedPreferences).data.first())
    assertEquals(expected = 5, actual = sharedPreferences.getInt("AUTO_REWIND", 0))

    assertEquals(expected = 5, actual = AutoRewindAmountStore(factory, sharedPreferences).data.first())
    assertEquals(expected = false, actual = sharedPreferences.contains("SEEK_TIME"))
    assertEquals(expected = false, actual = sharedPreferences.contains("AUTO_REWIND"))
  }

  @Test
  fun `migration is skipped when unrelated key present`() = runTest {
    sharedPreferences.edit { putInt("OTHER_KEY", 50) }

    assertEquals(expected = 20, actual = SeekTimeStore(factory, sharedPreferences).data.first())
    assertEquals(expected = true, actual = sharedPreferences.contains("OTHER_KEY"))
  }
}
