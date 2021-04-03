package voice.loudness

import android.widget.SeekBar
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun SeekBar.progressChangedStream(): Flow<Int> {
  return callbackFlow {
    val listener = object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        offer(progress)
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {
      }

      override fun onStopTrackingTouch(seekBar: SeekBar) {
      }
    }

    setOnSeekBarChangeListener(listener)
    listener.onProgressChanged(this@progressChangedStream, progress, false)

    awaitClose {
      setOnSeekBarChangeListener(null)
    }
  }
}
