package voice.app.playback

import android.app.Application
import android.content.ComponentName
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.zacsweers.metro.createGraphFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import voice.app.di.AppGraph
import voice.core.common.rootGraph
import voice.core.playback.session.PlaybackService
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class PlaybackServiceTest {

  @Test
  fun `the session plays through the same player as the service`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    rootGraph = createGraphFactory<AppGraph.Factory>().create(application)
    // Robolectric connects bound services without a component name, which crashes the MediaController.
    shadowOf(application).setComponentNameAndServiceForBindService(
      ComponentName(application, PlaybackService::class.java),
      null,
    )

    val service = Robolectric.buildService(PlaybackService::class.java).create().get()

    assertSame(service.player, service.session.player)
  }
}
