package de.ph1b.audiobook.playback.events

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import de.ph1b.audiobook.misc.RxBroadcast
import i
import io.reactivex.Observable

/**
 * Simple receiver wrapper which holds a [android.content.BroadcastReceiver] that notifies on headset changes.
 *
 * @author Paul Woitaschek
 */
object HeadsetPlugReceiver {

  private val filter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
  private val PLUGGED = 1
  private val UNPLUGGED = 0

  fun events(c: Context): Observable<HeadsetState> = RxBroadcast.register(c, filter)
      .map {
        i { "onReceive with intent=$it" }
        val intState = it.getIntExtra("state", UNPLUGGED)
        when (it.getIntExtra("state", UNPLUGGED)) {
          UNPLUGGED -> HeadsetState.UNPLUGGED
          PLUGGED -> HeadsetState.PLUGGED
          else -> {
            i { "Unknown headsetState $intState" }
            HeadsetState.UNKNOWN
          }
        }
      }

  enum class HeadsetState {
    PLUGGED,
    UNPLUGGED,
    UNKNOWN
  }
}
