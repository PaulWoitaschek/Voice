package voice.app.di

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
@DependencyGraph(
  scope = AppScope::class,
)
interface ProductionAppGraph : AppGraph {
  @DependencyGraph.Factory
  interface Factory {
    fun create(@Provides application: Application): ProductionAppGraph
  }
}
