@file:Suppress("EXPERIMENTAL_API_USAGE")

package de.ph1b.audiobook.misc

import android.content.Context
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import de.ph1b.audiobook.playback.utils.DataSourceConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.filter
import kotlinx.coroutines.channels.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Analyzes the duration of a file
 */
class DurationAnalyzer
@Inject constructor(
  private val dataSourceConverter: DataSourceConverter,
  context: Context
) {

  private val player: ExoPlayer

  init {
    val trackSelector = DefaultTrackSelector()
    player = ExoPlayerFactory.newSimpleInstance(context, trackSelector)
    enableOnlyAudioRenderer(trackSelector, player)
  }

  private fun enableOnlyAudioRenderer(
    trackSelector: DefaultTrackSelector,
    exoPlayer: SimpleExoPlayer
  ) {
    val builder = trackSelector.buildUponParameters()
    for (i in 0 until exoPlayer.rendererCount) {
      builder.setRendererDisabled(i, exoPlayer.getRendererType(i) != C.TRACK_TYPE_AUDIO)
    }
    trackSelector.parameters = builder.build()
  }

  private val playbackState = ConflatedBroadcastChannel(player.playbackState)

  init {
    player.addListener(
      object : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
          this@DurationAnalyzer.playbackState.offer(playbackState)
        }
      }
    )
  }

  suspend fun CoroutineScope.duration(file: File): Int? {
    waitForIdle()
    val cancelJob = launch {
      delay(TimeUnit.SECONDS.toMillis(3))
      coroutineContext.cancel()
    }
    try {
      return scan(file).takeIf { it > 0 }
    } finally {
      cancelJob.cancel()
    }
  }

  private suspend fun waitForIdle() {
    if (playbackState.value != Player.STATE_IDLE) {
      withContext(Main) {
        player.stop()
      }
    }
    playbackState.openSubscription()
      .filter { it == Player.STATE_IDLE }
      .first()
  }

  private suspend fun CoroutineScope.scan(file: File): Int {
    val mediaSource = dataSourceConverter.toMediaSource(file)
    withContext(Main) {
      player.prepare(mediaSource)
    }
    playbackState.openSubscription()
      .filter {
        when (it) {
          Player.STATE_READY -> true
          Player.STATE_ENDED -> {
            Timber.e("ended!. Cancel")
            coroutineContext.cancel()
            false
          }
          else -> false
        }
      }
      .first()

    return withContext(Main) {
      if (!player.isCurrentWindowSeekable) {
        Timber.d("file $file is not seekable")
      }
      val duration = player.duration
      try {
        if (duration == C.TIME_UNSET) -1
        else duration.toInt()
      } finally {
        player.stop()
      }
    }
  }
}
