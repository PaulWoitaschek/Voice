package voice.core.playback.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

// The same check as CarConnection from androidx.car.app, without pulling in that library.
@Inject
class AndroidAutoConnection(private val context: Context) {

  val connected: Flow<Boolean> = callbackFlow {
    val receiver =
      object : BroadcastReceiver() {
        override fun onReceive(
          context: Context,
          intent: Intent,
        ) {
          trySend(Unit)
        }
      }
    ContextCompat.registerReceiver(
      context,
      receiver,
      IntentFilter(ACTION_CAR_CONNECTION_UPDATED),
      ContextCompat.RECEIVER_EXPORTED,
    )
    send(Unit)
    awaitClose { context.unregisterReceiver(receiver) }
  }
    .conflate()
    .map { isConnected() }
    .flowOn(Dispatchers.IO)
    .distinctUntilChanged()

  private fun isConnected(): Boolean {
    return try {
      context.contentResolver
        .query(CAR_CONNECTION_URI.toUri(), arrayOf(CAR_CONNECTION_STATE), null, null, null)
        ?.use { cursor ->
          val column = cursor.getColumnIndex(CAR_CONNECTION_STATE)
          column != -1 && cursor.moveToNext() && cursor.getInt(column) != 0
        } ?: false
    } catch (_: SecurityException) {
      false
    }
  }

  private companion object {
    const val CAR_CONNECTION_URI = "content://androidx.car.app.connection"
    const val CAR_CONNECTION_STATE = "CarConnectionState"
    const val ACTION_CAR_CONNECTION_UPDATED =
      "androidx.car.app.connection.action.CAR_CONNECTION_UPDATED"
  }
}
