package voice.app.injection

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import voice.common.AppScope
import javax.inject.Singleton

@Singleton
@DependencyGraph(
  scope = AppScope::class,
  isExtendable = true,
)
interface ProductionAppComponent : AppComponent {

  @DependencyGraph.Factory
  interface Factory {
    fun create(@Provides application: Application): ProductionAppComponent
  }
}
