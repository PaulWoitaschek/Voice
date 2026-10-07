package voice.core.playback.history

import android.content.Context
import android.os.SystemClock
import androidx.media3.session.MediaSession
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import voice.core.data.ListeningEvent
import voice.core.playback.di.PlaybackScope

data class CommandSource(
  val source: ListeningEvent.Source,
  val packageName: String? = null,
)

/**
 * Tells where a command that reaches the session's player came from.
 *
 * Commands from the app's own controllers are recorded where they are sent, with
 * their intent, so they resolve to null here.
 */
@SingleIn(PlaybackScope::class)
@Inject
class CommandSourceResolver(private val context: Context) {

  internal var session: MediaSession? = null

  private var lastMediaButton: CommandSource? = null
  private var lastMediaButtonAt = 0L

  internal fun onMediaButtonEvent(controller: MediaSession.ControllerInfo) {
    val session = session ?: return
    lastMediaButton = when {
      session.isMediaNotificationController(controller) -> CommandSource(ListeningEvent.Source.Notification)
      controller.packageName == BLUETOOTH_PACKAGE -> CommandSource(ListeningEvent.Source.Bluetooth)
      else -> sourceForPackage(session, controller)
        ?.takeUnless { it.source == ListeningEvent.Source.OtherApp || it.source == ListeningEvent.Source.Unknown }
        ?: CommandSource(ListeningEvent.Source.Headset)
    }
    lastMediaButtonAt = SystemClock.elapsedRealtime()
  }

  internal fun current(): CommandSource? {
    val session = session ?: return null
    val controller = session.controllerForCurrentRequest ?: return null
    if (session.isMediaNotificationController(controller)) {
      // media buttons from headsets are passed on as if the notification sent them
      val mediaButton = lastMediaButton
      return if (mediaButton != null && SystemClock.elapsedRealtime() - lastMediaButtonAt < MEDIA_BUTTON_WINDOW_MS) {
        mediaButton
      } else {
        CommandSource(ListeningEvent.Source.Notification)
      }
    }
    if (controller.packageName == context.packageName) {
      return null
    }
    return sourceForPackage(session, controller)
  }

  private fun sourceForPackage(
    session: MediaSession,
    controller: MediaSession.ControllerInfo,
  ): CommandSource? {
    val packageName = controller.packageName
    return when {
      session.isAutomotiveController(controller) ||
        session.isAutoCompanionController(controller) ||
        packageName == ANDROID_AUTO_PACKAGE -> CommandSource(ListeningEvent.Source.Car)
      packageName in watchPackages -> CommandSource(ListeningEvent.Source.Watch)
      packageName == BLUETOOTH_PACKAGE -> CommandSource(ListeningEvent.Source.Bluetooth)
      packageName == SYSTEM_UI_PACKAGE -> CommandSource(ListeningEvent.Source.Notification)
      packageName == MediaSession.ControllerInfo.LEGACY_CONTROLLER_PACKAGE_NAME -> CommandSource(ListeningEvent.Source.Unknown)
      packageName == context.packageName -> null
      else -> CommandSource(ListeningEvent.Source.OtherApp, packageName)
    }
  }

  private companion object {
    const val MEDIA_BUTTON_WINDOW_MS = 1500L
    const val ANDROID_AUTO_PACKAGE = "com.google.android.projection.gearhead"
    const val BLUETOOTH_PACKAGE = "com.android.bluetooth"
    const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    val watchPackages = setOf(
      "com.google.android.wearable.app",
      "com.google.android.apps.wear.companion",
      "com.samsung.android.app.watchmanager",
    )
  }
}
