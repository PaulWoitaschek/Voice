package de.ph1b.audiobook.misc

import android.content.Context
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import d
import de.ph1b.audiobook.playback.utils.DataSourceConverter
import de.ph1b.audiobook.playback.utils.SimpleEventListener
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import java.io.File
import java.io.IOException
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

  private val exoPlayer = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector())
  private val playbackStateSubject = BehaviorSubject.createDefault(exoPlayer.playbackState)

  init {
    exoPlayer.addListener(object : SimpleEventListener {
      override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        playbackStateSubject.onNext(playbackState)
      }
    })
  }

  fun duration(file: File): Single<Int> = waitForIdle()
      .flatMap { scan(file) }
      .timeout(3, TimeUnit.SECONDS)
      .onErrorReturnItem(-1)

  private fun waitForIdle() = playbackStateSubject
      .doOnSubscribe {
        if (playbackStateSubject.value != ExoPlayer.STATE_IDLE) exoPlayer.stop()
      }
      .filter { it == ExoPlayer.STATE_IDLE }
      .firstOrError()

  private fun scan(file: File) = playbackStateSubject
      .doOnSubscribe {
        val mediaSource = dataSourceConverter.toMediaSource(file)
        exoPlayer.prepare(mediaSource)
      }
      .filter {
        when (it) {
          ExoPlayer.STATE_READY -> true
          ExoPlayer.STATE_BUFFERING, ExoPlayer.STATE_IDLE -> false
          else -> throw IOException()
        }
      }
      .firstOrError()
      .map {
        if (!exoPlayer.isCurrentWindowSeekable)
          d { "file $file is not seekable" }
        val duration = exoPlayer.duration
        if (duration == C.TIME_UNSET) -1
        else duration.toInt()
      }
      .doFinally { exoPlayer.stop() }
}
