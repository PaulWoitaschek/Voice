package de.ph1b.audiobook.playback

import android.content.Context
import androidx.core.content.ContextCompat
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class for controlling the player through the service
 */
@Singleton
class PlayerController
@Inject constructor(val context: Context) {

  fun execute(action: PlayerCommand) {
    Timber.d("execute $action")
    ContextCompat.startForegroundService(context, action.toIntent(context))
  }
}
