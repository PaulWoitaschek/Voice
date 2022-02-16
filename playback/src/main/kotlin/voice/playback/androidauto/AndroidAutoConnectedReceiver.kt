package voice.playback.androidauto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import voice.logging.core.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes the android auto connection state.
 */
@Singleton
class AndroidAutoConnectedReceiver @Inject constructor() {

  private val _connected = MutableStateFlow(false)
  val stream: Flow<Boolean> get() = _connected
  val connected: Boolean get() = _connected.value

  private val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      when (intent?.getStringExtra("media_connection_status")) {
        "media_connected" -> {
          Logger.i("connected")
          _connected.value = true
        }
        "media_disconnected" -> {
          Logger.i("disconnected")
          _connected.value = false
        }
      }
    }
  }

  fun register(context: Context) {
    context.registerReceiver(receiver, IntentFilter("com.google.android.gms.car.media.STATUS"))
  }
}
