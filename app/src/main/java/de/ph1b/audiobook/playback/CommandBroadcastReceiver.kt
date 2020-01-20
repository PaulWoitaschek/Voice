package de.ph1b.audiobook.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.persistence.pref.Pref
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

class CommandBroadcastReceiver : BroadcastReceiver() {

  @Inject
  lateinit var repo: BookRepository
  @field:[Inject Named(PrefKeys.CURRENT_BOOK)]
  lateinit var currentBookIdPref: Pref<UUID>
  @Inject
  lateinit var playerController: PlayerController

  override fun onReceive(context: Context, intent: Intent?) {
    appComponent.inject(this)

    intent ?: return
    val command = PlayerCommand.fromIntent(intent) ?: return
    playerController.execute(command)
  }
}
