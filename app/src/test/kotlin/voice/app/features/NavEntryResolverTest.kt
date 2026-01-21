package voice.app.features

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.Test
import voice.app.navigation.NavEntryResolver
import voice.navigation.Destination
import kotlin.reflect.KClass

@SingleIn(AppScope::class)
@DependencyGraph(
  scope = AppScope::class,
)
interface NavEntryResolverTestGraph {

  @Provides
  val application: Application get() = ApplicationProvider.getApplicationContext()

  val resolver: NavEntryResolver
}

class NavEntryResolverTest {

  @Test
  fun testClassRegistered() {
    val allDestinations = buildList {
      fun addChildren(from: KClass<out Destination>) {
        from.sealedSubclasses.forEach {
          add(it)
          addChildren(it)
        }
      }
      addChildren(Destination.Compose::class)
    }
    val testGraph: NavEntryResolverTestGraph = createGraph()
    val resolver = testGraph.resolver
    resolver.registeredClasses().shouldContainExactlyInAnyOrder(allDestinations)
  }
}
