package voice.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import voice.app.injection.appComponent
import java.io.File

class SleepTimerIntegrationTest {

  @Test
  fun test() {
    val testComponent = appComponent as TestComponent
    val playerController = testComponent.playerController
    playerController.play()

    val context = ApplicationProvider.getApplicationContext<Context>()
    val outputFile = File(context.filesDir, "auphonic_chapters_demo.m4a")
    InstrumentationRegistry.getInstrumentation().context.assets
      .open("auphonic_chapters_demo.m4a").use { inputStream ->
        outputFile.outputStream().use { outputStream ->
          inputStream.copyTo(outputStream)
        }
      }
  }
}
