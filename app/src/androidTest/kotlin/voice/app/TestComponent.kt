package voice.app

import android.app.Application
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import voice.app.injection.AppComponent
import voice.common.AppScope
import javax.inject.Singleton

@Singleton
@DependencyGraph(
  scope = AppScope::class,
  isExtendable = true,
)
interface TestComponent : AppComponent {

  fun inject(target: SleepTimerIntegrationTest)

  @DependencyGraph.Factory
  interface Factory {
    fun create(@Provides application: Application): TestComponent
  }
}
