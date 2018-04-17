package de.ph1b.audiobook.playback.events

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import de.ph1b.audiobook.misc.RxBroadcast
import io.reactivex.Observable
import timber.log.Timber

/**
 * Simple receiver wrapper which holds a [android.content.BroadcastReceiver] that notifies on Bluetooth device connection state changes.
 */
object BluetoothDeviceConnectionReceiver {

  private val filter = IntentFilter()

  fun init() {
    filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
    filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
    filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
  }

  fun events(c: Context): Observable<BluetoothDeviceState> = RxBroadcast.register(c, filter)
      .map {
        Timber.i("onReceive with intent=$it")
        when (it.getAction()) {
          BluetoothDevice.ACTION_ACL_CONNECTED -> {
            Timber.i("bluetooth connected")
            BluetoothDeviceState.CONNECTED
          }
          BluetoothDevice.ACTION_ACL_DISCONNECTED,
          BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED -> {
            Timber.i("bluetooth disconnected")
            BluetoothDeviceState.DISCONNECTED
          }
          else -> {
            Timber.i("Unknown bluetoothDeviceState ${it.getAction()}")
            BluetoothDeviceState.UNKNOWN
          }
        }
      }

  enum class BluetoothDeviceState {
    CONNECTED,
    DISCONNECTED,
    UNKNOWN
  }
}
