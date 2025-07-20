package voice.app

import dev.zacsweers.metro.createGraphFactory
import voice.app.injection.App
import voice.app.injection.AppComponent

class TestApp : App() {

  override fun createAppComponent(): AppComponent {
    return createGraphFactory<TestComponent.Factory>()
      .create(this)
  }
}
