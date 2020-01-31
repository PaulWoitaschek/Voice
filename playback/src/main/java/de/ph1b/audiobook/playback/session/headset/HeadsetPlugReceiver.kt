package de.ph1b.audiobook.playback.session.headset

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import de.ph1b.audiobook.playback.misc.flowBroadcastReceiver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

private val filter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
private const val PLUGGED = 1
private const val UNPLUGGED = 0

fun Context.headsetStateChangeFlow(): Flow<HeadsetState> {
  return flowBroadcastReceiver(IntentFilter(Intent.ACTION_HEADSET_PLUG))
    .map {
      Timber.i("onReceive with intent=$it")
      when (val intState = it.getIntExtra("state", UNPLUGGED)) {
        UNPLUGGED -> HeadsetState.Unplugged
        PLUGGED -> HeadsetState.Plugged
        else -> {
          Timber.i("Unknown headsetState $intState")
          HeadsetState.Unknown
        }
      }
    }
}
