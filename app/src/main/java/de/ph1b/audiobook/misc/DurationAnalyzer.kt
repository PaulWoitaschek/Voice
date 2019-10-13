package de.ph1b.audiobook.misc

import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player.*
import com.google.android.exoplayer2.SimpleExoPlayer
import de.ph1b.audiobook.playback.utils.DataSourceConverter
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject

class DurationAnalyzer
@Inject constructor(
    private val dataSourceConverter: DataSourceConverter,
    private val player: SimpleExoPlayer
) {

  private val _state = ConflatedBroadcastChannel(player.playbackState)
  private val stateFlow = _state.asFlow()
  private var state: Int
    get() = _state.value
    set(value) {
      _state.offer(value)
    }

  init {
    player.addListener(
        object : EventListener {
          override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            state = playbackState
          }
        }
    )
  }

  suspend fun duration(uri: Uri): Long? {
    waitForIdle()
    return withTimeoutOrNull(3000) {
      scan(uri).takeIf { it != null && it > 0 }
    }
  }

  private suspend fun waitForIdle() {
    val playState = state
    if (playState != STATE_IDLE) {
      withContext(Main) {
        player.stop()
        Timber.v("will stop player. because state is ${stateName[playState]}")
      }
      stateFlow.first {
        Timber.v("state is ${stateName[it]}")
        it == STATE_IDLE
      }
    }
  }

  private suspend fun scan(uri: Uri): Long? {
    Timber.v("scan $uri start")
    val mediaSource = dataSourceConverter.toMediaSource(uri)
    withContext(Main) {
      player.prepare(mediaSource)
    }
    Timber.v("scan, prepared $uri.")
    stateFlow.first {
      Timber.v("scan, state=${stateName[it]}")
      when (it) {
        STATE_READY -> true
        STATE_ENDED, STATE_IDLE -> {
          Timber.e("Couldn't prepare. Return no duration.")
          true
        }
        else -> false
      }
    }
    if (state != STATE_READY) {
      return null
    }

    return withContext(Main) {
      if (!player.isCurrentWindowSeekable) {
        Timber.d("uri $uri is not seekable")
      }
      val duration = player.duration
      try {
        duration.takeUnless { it == C.TIME_UNSET }
      } finally {
        Timber.v("scan $uri stop")
        player.stop()
      }
    }
  }

  private val stateName = mapOf(
      STATE_READY to "ready",
      STATE_ENDED to "ended",
      STATE_IDLE to "idle",
      STATE_BUFFERING to "buffering"
  )
}
