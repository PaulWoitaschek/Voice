package voice.app.di

import android.app.Application
import androidx.annotation.VisibleForTesting
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.createGraphFactory
import voice.core.common.rootGraph
import voice.core.initializer.AppInitializer

open class App : Application() {

  @Inject
  lateinit var appInitializers: Set<AppInitializer>

  override fun onCreate() {
    super.onCreate()

    appGraph = createGraph()
    rootGraph = appGraph
    appGraph.inject(this)

    appInitializers.forEach {
      it.onAppStart(this)
    }
  }

  open fun createGraph(): AppGraph {
    return createGraphFactory<ProductionAppGraph.Factory>().create(this)
  }
}

lateinit var appGraph: AppGraph
  @VisibleForTesting set
