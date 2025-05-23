package voice.app

import org.junit.Test
import voice.app.injection.appComponent

class SleepTimerIntegrationTest {

  @Test
  fun test() {
    val testComponent = appComponent as TestComponent
    val playerController = testComponent.playerController
    playerController.play()
  }
}
