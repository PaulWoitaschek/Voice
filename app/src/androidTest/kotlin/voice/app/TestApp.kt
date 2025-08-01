package voice.app

import dev.zacsweers.metro.createGraphFactory
import voice.app.injection.App
import voice.app.injection.AppGraph

class TestApp : App() {

  override fun createGraph(): AppGraph {
    return createGraphFactory<TestGraph.Factory>()
      .create(this)
  }
}
