package voice.app

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import voice.app.di.AppGraph

@SingleIn(AppScope::class)
@DependencyGraph(
  scope = AppScope::class,
)
interface TestGraph : AppGraph {

  fun inject(target: SleepTimerIntegrationTest)

  @DependencyGraph.Factory
  interface Factory {
    fun create(@Provides application: Application): TestGraph
  }
}
