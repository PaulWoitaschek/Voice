package voice.core.ui

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
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class UiStoreMigrationTest {

  private val application: Application = ApplicationProvider.getApplicationContext()
  private val sharedPreferences: SharedPreferences = application.getSharedPreferences("prefs", Context.MODE_PRIVATE)
  private val factory = VoiceDataStoreFactory(Json.Default, application)

  private fun themeModeStore() = ThemeModeStore(factory, application, sharedPreferences)

  private fun gridModeStore() = GridModeStore(factory, sharedPreferences)

  @Test
  fun `legacy darkTheme true migrates to dark and cleans up`() = runTest {
    sharedPreferences.edit { putBoolean("darkTheme", true) }

    assertEquals(expected = ThemeMode.Dark, actual = themeModeStore().data.first())
    assertEquals(expected = false, actual = sharedPreferences.contains("darkTheme"))
  }

  @Test
  fun `legacy darkTheme false migrates to light and cleans up`() = runTest {
    sharedPreferences.edit { putBoolean("darkTheme", false) }

    assertEquals(expected = ThemeMode.Light, actual = themeModeStore().data.first())
    assertEquals(expected = false, actual = sharedPreferences.contains("darkTheme"))
  }

  @Test
  fun `legacy darkTheme DataStore migrates and cleans up`() = runTest {
    val oldDataStoreFile = File(application.filesDir, "datastore/darkTheme")
    oldDataStoreFile.parentFile!!.mkdirs()
    oldDataStoreFile.writeText("true")

    assertEquals(expected = ThemeMode.Dark, actual = themeModeStore().data.first())
    assertEquals(expected = false, actual = oldDataStoreFile.exists())
  }

  @Test
  fun `defaults are used when SharedPreferences empty`() = runTest {
    assertEquals(expected = ThemeMode.FollowSystem, actual = themeModeStore().data.first())
    assertEquals(expected = ThemeColorScheme.VoiceBlue, actual = ThemeColorSchemeStore(factory).data.first())
  }

  @Test
  fun `migrates LIST from SharedPreferences and cleans up`() = runTest {
    sharedPreferences.edit { putString("gridView", "LIST") }

    assertEquals(expected = GridMode.LIST, actual = gridModeStore().data.first())
    assertEquals(expected = false, actual = sharedPreferences.contains("gridView"))
  }

  @Test
  fun `migrates GRID from SharedPreferences and cleans up`() = runTest {
    sharedPreferences.edit { putString("gridView", "GRID") }

    assertEquals(expected = GridMode.GRID, actual = gridModeStore().data.first())
    assertEquals(expected = false, actual = sharedPreferences.contains("gridView"))
  }

  @Test
  fun `falls back to FOLLOW_DEVICE when key missing`() = runTest {
    assertEquals(expected = GridMode.FOLLOW_DEVICE, actual = gridModeStore().data.first())
    assertEquals(expected = false, actual = sharedPreferences.contains("gridView"))
  }

  @Test
  fun `falls back to FOLLOW_DEVICE on unknown legacy value and cleans up`() = runTest {
    sharedPreferences.edit { putString("gridView", "UNKNOWN") }

    assertEquals(expected = GridMode.FOLLOW_DEVICE, actual = gridModeStore().data.first())
    assertEquals(expected = false, actual = sharedPreferences.contains("gridView"))
  }
}
