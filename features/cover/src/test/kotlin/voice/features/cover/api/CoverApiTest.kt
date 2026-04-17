package voice.features.cover.api

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.string.shouldNotBeEmpty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import voice.core.ui.SharedGraph

@Suppress("SUSPICIOUS_UNUSED_MULTIBINDING")
@SingleIn(AppScope::class)
@DependencyGraph(
  scope = AppScope::class,
  excludes = [SharedGraph::class],
)
interface TestGraph {
  val coverApi: CoverApi

  @Provides
  fun application(): Application = ApplicationProvider.getApplicationContext()

  @Provides
  fun context(): Context = ApplicationProvider.getApplicationContext()

  @Provides
  fun scope(): CoroutineScope = CoroutineScope(Dispatchers.Main)

  @Provides
  fun json(): Json = Json.Default
}

@RunWith(AndroidJUnit4::class)
internal class CoverApiTest {

  @Test
  fun test() {
    runBlocking {
      val api = createGraph<TestGraph>().coverApi
      val query = "unicorns"
      val token = api.token(query).shouldNotBeEmpty()!!
      api.search(query = query, auth = token)
        .results
        .shouldNotBeEmpty()
    }
  }
}
