package voice.app

import voice.app.injection.App
import voice.app.injection.AppComponent

class TestApp : App() {

  override fun createAppComponent(): AppComponent {
    return TestComponent.factory()
      .create(this)
  }
}
