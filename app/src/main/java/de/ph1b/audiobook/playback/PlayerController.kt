package de.ph1b.audiobook.playback

import android.content.Context
import androidx.core.content.ContextCompat
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.persistence.pref.Pref
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Class for controlling the player through the service
 */
@Singleton
class PlayerController
@Inject constructor(
  private val context: Context,
  private val repo: BookRepository,
  @Named(PrefKeys.CURRENT_BOOK)
  private val currentBookIdPref: Pref<UUID>,
  private val playStateManager: PlayStateManager
) {

  fun execute(command: PlayerCommand) {
    Timber.d("execute $command")

    if (command is PlayerCommand.Stop && playStateManager.playState == PlayStateManager.PlayState.Stopped) {
      Timber.d("$command in stopped state. Ignore")
    } else {
      val bookExists = repo.bookById(currentBookIdPref.value) != null
      if (bookExists) {
        Timber.d("execute $command")
        ContextCompat.startForegroundService(context, command.toServiceIntent(context))
      } else {
        Timber.w("ignore $command because there is no book.")
      }
    }
  }
}
