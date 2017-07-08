package de.ph1b.audiobook.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.utils.ChangeNotifier
import javax.inject.Inject

/**
 * Holds the current connection status and notifies about
 * changes upon connection.
 *
 * @author Paul Woitaschek
 */
class AndroidAutoConnection @Inject constructor(
    private val changeNotifier: ChangeNotifier,
    private val repo: BookRepository,
    private val prefs: PrefsManager
) {

  var connected = false
    private set

  private val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      when (intent?.getStringExtra("media_connection_status")) {
        "media_connected" -> connected = true
        "media_disconnected" -> connected = false
      }

      if (connected) {
        // display the current book but don't play it
        repo.bookById(prefs.currentBookId.value)?.let {
          changeNotifier.notify(ChangeNotifier.Type.METADATA, it, connected)
        }
      }
    }
  }

  fun register(context: Context) {
    context.registerReceiver(receiver, IntentFilter("com.google.android.gms.car.media.STATUS"))
  }

  fun unregister(context: Context) {
    context.unregisterReceiver(receiver)
  }
}
