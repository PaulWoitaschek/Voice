package voice.core.playback

import androidx.media3.session.MediaController
import kotlinx.coroutines.Deferred

internal class MediaControllerConnection(private val connect: () -> Deferred<MediaController>) {

  private var controller: Deferred<MediaController> = connect()

  fun get(): Deferred<MediaController> {
    if (controller.isCompleted) {
      // a connection that failed is retried, like one that was dropped
      val completedController = if (controller.getCompletionExceptionOrNull() == null) controller.getCompleted() else null
      if (completedController?.isConnected != true) {
        completedController?.release()
        controller = connect()
      }
    }
    return controller
  }
}
