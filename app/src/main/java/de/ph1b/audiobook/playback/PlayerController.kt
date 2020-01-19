package de.ph1b.audiobook.playback

import android.content.Context
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class for controlling the player through the service
 */
@Singleton
class PlayerController
@Inject constructor(val context: Context) {

  fun execute(action: PlayerCommand) {
    ContextCompat.startForegroundService(context, action.toIntent(context))
  }
}
