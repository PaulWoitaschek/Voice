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
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Analyzes the duration of a file
 *
 * @author Paul Woitaschek
 */
class DurationAnalyzer
@Inject constructor(
    private val dataSourceConverter: DataSourceConverter,
    context: Context
) {

  private val exoPlayer = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector())
  private val playbackStateSubject = PublishSubject.create<Int>()

  init {
    exoPlayer.addListener(object : SimpleEventListener {
      override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        playbackStateSubject.onNext(playbackState)
      }
    })
  }

  fun duration(file: File): Single<Int> {
    val waitForIdle = playbackStateSubject
        .startWith(exoPlayer.playbackState)
        .doOnNext {
          if (it != ExoPlayer.STATE_IDLE) exoPlayer.stop()
        }
        .filter { it == ExoPlayer.STATE_IDLE }
        .firstOrError()

    val scanFile = playbackStateSubject
        .doOnSubscribe {
          val mediaSource = dataSourceConverter.toMediaSource(file)
          exoPlayer.prepare(mediaSource)
        }
        .filter {
          when (it) {
            ExoPlayer.STATE_READY -> true
            ExoPlayer.STATE_BUFFERING -> false
            else -> throw IOException()
          }
        }
        .firstOrError()
        .timeout(3, TimeUnit.SECONDS)
        .map {
          if (!exoPlayer.isCurrentWindowSeekable)
            d { "file $file is not seekable" }
          val duration = exoPlayer.duration
          if (duration == C.TIME_UNSET) -1
          else duration.toInt()
        }
        .onErrorReturnItem(-1)
        .doFinally { exoPlayer.stop() }

    return waitForIdle.flatMap { scanFile }
  }
}