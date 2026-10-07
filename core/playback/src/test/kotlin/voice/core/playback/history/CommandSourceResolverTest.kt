package voice.core.playback.history

import android.content.Context
import android.os.Bundle
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowSystemClock
import voice.core.data.ListeningEvent
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class CommandSourceResolverTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val player = TestExoPlayerBuilder(context).build()
  private val session = MediaSession.Builder(context, player).build()
  private val resolver = CommandSourceResolver(context).also { it.session = session }

  @After
  fun tearDown() {
    session.release()
    player.release()
  }

  @Test
  fun `a single press on play pause comes from the headset`() {
    // the framework sends the media button, and once no double tap followed, the play / pause under the same caller
    resolver.onMediaButtonEvent(legacyController(SYSTEM_PACKAGE))

    assertEquals(
      expected = CommandSource(ListeningEvent.Source.Headset),
      actual = resolver.sourceFor(session, legacyController(SYSTEM_PACKAGE)),
    )
  }

  @Test
  fun `a media button passed on by the notification keeps its source`() {
    resolver.onMediaButtonEvent(legacyController(BLUETOOTH_PACKAGE))

    assertEquals(
      expected = CommandSource(ListeningEvent.Source.Bluetooth),
      actual = resolver.sourceFor(session, notificationController()),
    )
  }

  @Test
  fun `the caller of an old media button is recorded as itself`() {
    resolver.onMediaButtonEvent(legacyController(SYSTEM_PACKAGE))
    ShadowSystemClock.advanceBy(Duration.ofSeconds(2))

    assertEquals(
      expected = CommandSource(ListeningEvent.Source.OtherApp, SYSTEM_PACKAGE),
      actual = resolver.sourceFor(session, legacyController(SYSTEM_PACKAGE)),
    )
    assertEquals(
      expected = CommandSource(ListeningEvent.Source.Notification),
      actual = resolver.sourceFor(session, notificationController()),
    )
  }

  @Test
  fun `other callers right after a media button are not taken for it`() {
    resolver.onMediaButtonEvent(legacyController(SYSTEM_PACKAGE))

    assertEquals(
      expected = CommandSource(ListeningEvent.Source.OtherApp, OTHER_PACKAGE),
      actual = resolver.sourceFor(session, legacyController(OTHER_PACKAGE)),
    )
    assertEquals(
      expected = CommandSource(ListeningEvent.Source.OtherApp, SYSTEM_PACKAGE),
      actual = resolver.sourceFor(session, legacyController(SYSTEM_PACKAGE, uid = OTHER_UID)),
    )
  }

  @Test
  fun `the app's own commands right after a media button are not recorded here`() {
    resolver.onMediaButtonEvent(notificationController())

    assertNull(resolver.sourceFor(session, appController()))
  }

  private fun legacyController(
    packageName: String,
    uid: Int = SYSTEM_UID,
  ) = MediaSession.ControllerInfo.createTestOnlyControllerInfo(
    packageName,
    0,
    uid,
    MediaSession.ControllerInfo.LEGACY_CONTROLLER_VERSION,
    MediaSession.ControllerInfo.LEGACY_CONTROLLER_INTERFACE_VERSION,
    false,
    Bundle.EMPTY,
    true,
  )

  private fun appController(connectionHints: Bundle = Bundle.EMPTY) = MediaSession.ControllerInfo.createTestOnlyControllerInfo(
    context.packageName,
    0,
    context.applicationInfo.uid,
    MEDIA3_CONTROLLER_VERSION,
    MEDIA3_CONTROLLER_VERSION,
    true,
    connectionHints,
    true,
  )

  private fun notificationController() = appController(
    connectionHints = Bundle().apply {
      putBoolean(MediaController.KEY_MEDIA_NOTIFICATION_CONTROLLER_FLAG, true)
    },
  )

  private companion object {
    const val SYSTEM_PACKAGE = "android"
    const val SYSTEM_UID = 1000
    const val OTHER_UID = 10_123
    const val BLUETOOTH_PACKAGE = "com.android.bluetooth"
    const val OTHER_PACKAGE = "com.example.remote"
    const val MEDIA3_CONTROLLER_VERSION = 1
  }
}
