package de.ph1b.audiobook.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes the android auto connection state.
 */
@Singleton
class AndroidAutoConnectedReceiver @Inject constructor() {

  private val connectedRelay = BehaviorSubject.createDefault(false)
  val stream = connectedRelay.hide()!!
  val connected = connectedRelay.value!!

  private val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      when (intent?.getStringExtra("media_connection_status")) {
        "media_connected" -> {
          Timber.i("connected")
          connectedRelay.onNext(true)
        }
        "media_disconnected" -> {
          Timber.i("disconnected")
          connectedRelay.onNext(false)
        }
      }
    }
  }

  fun register(context: Context) {
    context.registerReceiver(receiver, IntentFilter("com.google.android.gms.car.media.STATUS"))
  }
}
