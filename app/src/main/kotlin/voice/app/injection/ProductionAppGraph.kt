package voice.app.injection

import android.app.Application
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import voice.common.AppScope

@SingleIn(AppScope::class)
@DependencyGraph(
  scope = AppScope::class,
  isExtendable = true,
)
interface ProductionAppGraph : AppGraph {

  @DependencyGraph.Factory
  interface Factory {
    fun create(@Provides application: Application): ProductionAppGraph
  }
}
