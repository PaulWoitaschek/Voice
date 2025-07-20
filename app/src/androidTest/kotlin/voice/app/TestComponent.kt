package voice.app

import android.app.Application
import dagger.BindsInstance
import dev.zacsweers.metro.DependencyGraph
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
    fun create(@BindsInstance application: Application): TestComponent
  }
}
