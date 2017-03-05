package de.ph1b.audiobook.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import i

/**
 * A [BroadcastReceiver] that holds the state if android auto is connected
 *
 * @author Paul Woitaschek
 */
class AndroidAutoStateReceiver : BroadcastReceiver() {

  var connected = false
    private set

  override fun onReceive(context: Context?, intent: Intent) {
    val status: String? = intent.getStringExtra("media_connection_status")
    connected = status == "media_connected"
    i { "connected changed to $connected" }
  }

  companion object {
    fun filter() = IntentFilter("com.google.android.gms.car.media.STATUS")
  }
}