package de.ph1b.audiobook.misc

import android.widget.SeekBar
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun SeekBar.onProgressChanged(
  initialNotification: Boolean = false,
  progressChanged: (Int) -> Unit
) {
  val listener = object : SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
      progressChanged(progress)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
    }
  }

  setOnSeekBarChangeListener(listener)
  if (initialNotification) listener.onProgressChanged(this, progress, false)
}

fun SeekBar.progressChangedStream(): Flow<Int> {
  return callbackFlow {
    onProgressChanged(initialNotification = true) { position ->
      trySend(position)
    }
    awaitClose {
      setOnSeekBarChangeListener(null)
    }
  }
}
