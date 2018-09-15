package de.ph1b.audiobook.misc

import android.content.Context
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import de.ph1b.audiobook.playback.utils.DataSourceConverter
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Analyzes the duration of a file
 */
class DurationAnalyzer(
  private val dataSourceConverter: DataSourceConverter,
  context: Context
) {

  private val exoPlayer: ExoPlayer

  init {
    val trackSelector = DefaultTrackSelector()
    exoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector)
    enableOnlyAudioRenderer(trackSelector, exoPlayer)
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

  private val playbackStateSubject = BehaviorSubject.createDefault(exoPlayer.playbackState)

  init {
    exoPlayer.addListener(
      object : Player.DefaultEventListener() {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
          playbackStateSubject.onNext(playbackState)
        }
      }
    )
  }

  fun duration(file: File): Single<Int> = waitForIdle()
    .flatMap { scan(file) }
    .timeout(3, TimeUnit.SECONDS)
    .onErrorReturnItem(-1)

  private fun waitForIdle() = playbackStateSubject
    .doOnSubscribe {
      if (playbackStateSubject.value != Player.STATE_IDLE) exoPlayer.stop()
    }
    .filter { it == Player.STATE_IDLE }
    .firstOrError()

  private fun scan(file: File) = playbackStateSubject
    .doOnSubscribe {
      val mediaSource = dataSourceConverter.toMediaSource(file)
      exoPlayer.prepare(mediaSource)
    }
    .filter {
      when (it) {
        Player.STATE_READY -> true
        Player.STATE_BUFFERING, Player.STATE_IDLE -> false
        else -> throw IOException()
      }
    }
    .firstOrError()
    .map {
      if (!exoPlayer.isCurrentWindowSeekable) {
        Timber.d("file $file is not seekable")
      }
      val duration = exoPlayer.duration
      if (duration == C.TIME_UNSET) -1
      else duration.toInt()
    }
    .doFinally { exoPlayer.stop() }
}
