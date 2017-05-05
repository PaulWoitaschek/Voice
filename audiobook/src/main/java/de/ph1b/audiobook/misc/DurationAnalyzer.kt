package de.ph1b.audiobook.misc

import android.content.Context
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import de.ph1b.audiobook.playback.utils.DataSourceConverter
import de.ph1b.audiobook.playback.utils.SimpleEventListener
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
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
        Timber.i("state changed to $playbackState")
        playbackStateSubject.onNext(playbackState)
      }
    })
  }

  fun duration(file: File): Single<Int> = playbackStateSubject
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
      .timeout(10, TimeUnit.SECONDS)
      .map { exoPlayer.duration.toInt() }
      .firstOrError()
      .onErrorReturnItem(-1)
      .doFinally { exoPlayer.stop() }
}