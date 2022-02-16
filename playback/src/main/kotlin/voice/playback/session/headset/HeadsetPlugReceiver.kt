package voice.playback.session.headset

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import voice.logging.core.Logger
import voice.playback.misc.flowBroadcastReceiver

private val filter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
private const val PLUGGED = 1
private const val UNPLUGGED = 0

fun Context.headsetStateChangeFlow(): Flow<HeadsetState> {
  return flowBroadcastReceiver(filter)
    .map {
      Logger.i("onReceive with intent=$it")
      when (val intState = it.getIntExtra("state", UNPLUGGED)) {
        UNPLUGGED -> HeadsetState.Unplugged
        PLUGGED -> HeadsetState.Plugged
        else -> {
          Logger.i("Unknown headsetState $intState")
          HeadsetState.Unknown
        }
      }
    }
}
