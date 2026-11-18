package voice.core.analytics.firebase

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.analytics.FirebaseAnalytics as GmsFirebaseAnalytics
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertSame
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import voice.core.analytics.api.Analytics
import voice.core.initializer.AppInitializer

@SingleIn(AppScope::class)
@DependencyGraph(scope = AppScope::class, excludes = [AnalyticsModule::class])
interface FirebaseAnalyticsTestGraph {

  val analytics: Analytics
  val initializers: Set<AppInitializer>

  @Provides
  val application: Application
    get() = ApplicationProvider.getApplicationContext()

  @Provides fun context(application: Application): Context = application

  @Provides fun json(): Json = Json.Default

  @Provides fun firebaseAnalytics(): GmsFirebaseAnalytics = mockk(relaxUnitFun = true)
}

@RunWith(AndroidJUnit4::class)
class FirebaseAnalyticsGraphTest {

  @Test
  fun `the consent turns on the same analytics that keeps the user properties`() {
    val graph = createGraph<FirebaseAnalyticsTestGraph>()

    val enableOnConsent = graph.initializers.filterIsInstance<EnableAnalyticsOnConsent>().single()

    assertSame(expected = graph.analytics, actual = enableOnConsent.analytics)
  }
}
