package de.ph1b.audiobook.playback

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class for controlling the player through the service
 */
@Singleton
class PlayerController
@Inject constructor(val context: Context) {

  var service: PlaybackService? = null

  fun execute(action: PlayerCommand) {
    val service = service
    if (service != null) {
      service.execute(action)
    } else {
      context.startService(action.toIntent(context))
    }
  }
}
