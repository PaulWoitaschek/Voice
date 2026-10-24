package voice.core.datastore

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals

@SingleIn(AppScope::class)
@DependencyGraph(scope = AppScope::class)
interface DataStoreTestGraph {

  val sharedPreferences: SharedPreferences

  @Provides
  val application: Application get() = ApplicationProvider.getApplicationContext()
}

@RunWith(AndroidJUnit4::class)
class DataStoreModuleTest {

  @Test
  fun `migrations read the preferences file older versions wrote`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    application.getSharedPreferences("${application.packageName}_preferences", Context.MODE_PRIVATE)
      .edit { putInt("SEEK_TIME", 15) }

    val sharedPreferences = createGraph<DataStoreTestGraph>().sharedPreferences

    assertEquals(expected = 15, actual = sharedPreferences.getInt("SEEK_TIME", 0))
  }
}
