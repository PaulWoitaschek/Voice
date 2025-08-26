package voice.app

import dev.zacsweers.metro.createGraphFactory
import voice.app.di.App
import voice.app.di.AppGraph

class TestApp : App() {

  override fun createGraph(): AppGraph {
    return createGraphFactory<TestGraph.Factory>()
      .create(this)
  }
}
