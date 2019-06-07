package de.ph1b.audiobook.playback

import androidx.annotation.FloatRange
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import de.ph1b.audiobook.common.sparseArray.forEachIndexed
import de.ph1b.audiobook.common.sparseArray.keyAtOrNull
import de.ph1b.audiobook.data.BookContent
import de.ph1b.audiobook.features.audio.Equalizer
import de.ph1b.audiobook.features.audio.LoudnessGain
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.misc.checkMainThread
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.utils.DataSourceConverter
import de.ph1b.audiobook.playback.utils.WakeLockManager
import de.ph1b.audiobook.playback.utils.onAudioSessionId
import de.ph1b.audiobook.playback.utils.onError
import de.ph1b.audiobook.playback.utils.onPositionDiscontinuity
import de.ph1b.audiobook.playback.utils.onStateChanged
import de.ph1b.audiobook.playback.utils.setPlaybackParameters
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class MediaPlayer
@Inject
constructor(
  private val playStateManager: PlayStateManager,
  @Named(PrefKeys.AUTO_REWIND_AMOUNT)
  private val autoRewindAmountPref: Pref<Int>,
  @Named(PrefKeys.SEEK_TIME)
  private val seekTimePref: Pref<Int>,
  private val equalizer: Equalizer,
  private val loudnessGain: LoudnessGain,
  private val wakeLockManager: WakeLockManager,
  private val dataSourceConverter: DataSourceConverter,
  private val player: SimpleExoPlayer
) {

  private val _bookContent = BehaviorSubject.create<BookContent>()
  val bookContent: BookContent? get() = _bookContent.value
  val bookContentStream = _bookContent.hide()!!

  private val _state = BehaviorSubject.createDefault(PlayerState.IDLE)
  private var state: PlayerState
    get() = _state.value!!
    set(value) {
      if (_state.value != value) _state.onNext(value)
    }

  private val seekTimeInSeconds by seekTimePref
  private var autoRewindAmount by autoRewindAmountPref

  init {
    checkMainThread()
    val audioAttributes = AudioAttributes.Builder()
      .setContentType(C.CONTENT_TYPE_SPEECH)
      .setUsage(C.USAGE_MEDIA)
      .build()
    player.setAudioAttributes(audioAttributes, true)

    player.onStateChanged {
      playStateManager.playState = when (it) {
        PlayerState.IDLE -> PlayState.STOPPED
        PlayerState.ENDED -> PlayState.STOPPED
        PlayerState.PAUSED -> PlayState.PAUSED
        PlayerState.PLAYING -> PlayState.PLAYING
      }
      state = it
    }

    player.onError {
      Timber.e("onError")
      player.playWhenReady = false
    }

    // upon position change update the book
    player.onPositionDiscontinuity {
      val position = player.currentPosition
        .coerceAtLeast(0)
      Timber.i("onPositionDiscontinuity with currentPos=$position")
      bookContent?.let {
        checkMainThread()
        val index = player.currentWindowIndex
        _bookContent.onNext(
          it.updateSettings {
            copy(
              positionInChapter = position,
              currentFile = it.chapters[index].file
            )
          }
        )
      }
    }

    // update equalizer with new audio session upon arrival
    player.onAudioSessionId {
      equalizer.update(it)
      loudnessGain.update(it)
    }

    @Suppress("CheckResult")
    _state.subscribe {
      Timber.i("state changed to $it")

      // set the wake-lock based on the play state
      wakeLockManager.stayAwake(it == PlayerState.PLAYING)

      // upon end stop the player
      if (it == PlayerState.ENDED) {
        Timber.v("onEnded. Stopping player")
        checkMainThread()
        player.playWhenReady = false
      }
    }

    @Suppress("CheckResult")
    _state
      .switchMap {
        if (it == PlayerState.PLAYING) {
          Observable.interval(200L, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            .map {
              checkMainThread()
              player.currentPosition.coerceAtLeast(0)
            }
            .distinctUntilChanged { position -> position / 1000 } // let the value only pass the full second changed.
        } else Observable.empty()
      }
      .subscribe { time ->
        // update the book
        bookContent?.let { book ->
          checkMainThread()
          val index = player.currentWindowIndex
          val copy = book.updateSettings {
            copy(positionInChapter = time, currentFile = book.chapters[index].file)
          }
          _bookContent.onNext(copy)
        }
      }
  }

  fun setVolume(@FloatRange(from = 0.0, to = 1.0) volume: Float) {
    require(volume in 0F..1F) { "volume $volume must be in [0,1]" }
    checkMainThread()
    player.volume = volume
  }

  fun init(content: BookContent) {
    val shouldInitialize = player.playbackState == Player.STATE_IDLE ||
        !alreadyInitializedChapters(content)
    if (!shouldInitialize) {
      return
    }
    Timber.i("init")
    _bookContent.onNext(content)
    checkMainThread()
    player.playWhenReady = false
    player.prepare(dataSourceConverter.toMediaSource(content))
    player.seekTo(content.currentChapterIndex, content.positionInChapter)
    player.setPlaybackParameters(content.playbackSpeed, content.skipSilence)
    loudnessGain.gainmB = content.loudnessGain
    state = PlayerState.PAUSED
  }

  private fun alreadyInitializedChapters(content: BookContent): Boolean {
    val currentContent = this.bookContent
      ?: return false
    return currentContent.chapters == content.chapters
  }

  fun setLoudnessGain(mB: Int) {
    Timber.v("setLoudnessGain to $mB mB")

    bookContent?.let {
      val copy = it.updateSettings { copy(loudnessGain = mB) }
      _bookContent.onNext(copy)
      loudnessGain.gainmB = mB
    }
  }

  fun play() {
    Timber.v("play called in state $state")
    prepareIfIdle()
    bookContent?.let {
      val withChangedPlayedAtTime = it.updateSettings {
        copy(lastPlayedAtMillis = System.currentTimeMillis())
      }
      _bookContent.onNext(withChangedPlayedAtTime)
    }
    bookContent?.let {
      if (state == PlayerState.ENDED) {
        Timber.i("play in state ended. Back to the beginning")
        changePosition(0, it.chapters.first().file)
      }

      if (state == PlayerState.ENDED || state == PlayerState.PAUSED) {
        checkMainThread()
        player.playWhenReady = true
      } else Timber.d("ignore play in state $state")
    }
  }

  fun skip(time: Long, timeUnit: TimeUnit) {
    checkMainThread()
    prepareIfIdle()
    if (state == PlayerState.IDLE)
      return

    bookContent?.let {
      val currentPos = player.currentPosition
        .coerceAtLeast(0)
      val duration = player.duration

      val seekTo = currentPos + timeUnit.toMillis(time)
      Timber.v("currentPos=$currentPos, seekTo=$seekTo, duration=$duration")

      when {
        seekTo < 0 -> previous(false)
        seekTo > duration -> next()
        else -> changePosition(seekTo)
      }
    }
  }

  fun skip(forward: Boolean) {
    Timber.v("skip forward=$forward")
    skip(
      time = if (forward) seekTimeInSeconds.toLong() else -seekTimeInSeconds.toLong(),
      timeUnit = TimeUnit.SECONDS
    )
  }

  /** If current time is > 2000ms, seek to 0. Else play previous chapter if there is one. */
  fun previous(toNullOfNewTrack: Boolean) {
    Timber.i("previous with toNullOfNewTrack=$toNullOfNewTrack called in state $state")
    prepareIfIdle()
    if (state == PlayerState.IDLE)
      return

    bookContent?.let {
      val handled = previousByMarks(it)
      if (!handled) previousByFile(it, toNullOfNewTrack)
    }
  }

  private fun previousByFile(content: BookContent, toNullOfNewTrack: Boolean) {
    checkMainThread()
    val previousChapter = content.previousChapter
    if (player.currentPosition > 2000 || previousChapter == null) {
      Timber.i("seekTo beginning")
      changePosition(0)
    } else {
      if (toNullOfNewTrack) {
        changePosition(0, previousChapter.file)
      } else {
        val time = (previousChapter.duration - (seekTimeInSeconds * 1000))
          .coerceAtLeast(0)
        changePosition(time, previousChapter.file)
      }
    }
  }

  private fun previousByMarks(content: BookContent): Boolean {
    val marks = content.currentChapter.marks
    marks.forEachIndexed(reversed = true) { index, startOfMark, _ ->
      if (content.positionInChapter >= startOfMark) {
        val diff = content.positionInChapter - startOfMark
        if (diff > 2000) {
          changePosition(startOfMark.toLong())
          return true
        } else if (index > 0) {
          val seekTo = marks.keyAt(index - 1)
          changePosition(seekTo.toLong())
          return true
        }
      }
    }
    return false
  }

  private fun prepareIfIdle() {
    if (state == PlayerState.IDLE) {
      Timber.d("state is idle so ExoPlayer might have an error. Try to prepare it")
      bookContent?.let(::init)
    }
  }

  /** Stops the playback and releases some resources. */
  fun stop() {
    checkMainThread()
    player.stop()
  }

  /**
   * Pauses the player.
   * @param rewind true if the player should automatically rewind a little bit.
   */
  fun pause(rewind: Boolean) {
    Timber.v("pause")
    checkMainThread()
    when (state) {
      PlayerState.PLAYING -> {
        bookContent?.let {
          player.playWhenReady = false

          if (rewind) {
            val autoRewind = autoRewindAmount * 1000
            if (autoRewind != 0) {
              // get the raw position with rewinding applied
              val currentPosition = player.currentPosition
                .coerceAtLeast(0)
              var maybeSeekTo = (currentPosition - autoRewind)
                .coerceAtLeast(0) // make sure not to get into negative time

              // now try to find the current chapter mark and make sure we don't auto-rewind
              // to a previous mark
              val chapterMarks = it.currentChapter.marks
              chapterMarks.forEachIndexed(reversed = true)
              findStartOfMark@{ index, startOfMark, _ ->
                if (startOfMark <= currentPosition) {
                  val next = chapterMarks.keyAtOrNull(index + 1)
                  if (next == null || next > currentPosition) {
                    maybeSeekTo = maybeSeekTo.coerceAtLeast(startOfMark.toLong())
                    return@findStartOfMark
                  }
                }
              }

              // finally change position
              changePosition(maybeSeekTo)
            }
          }
        }
      }
      else -> Timber.d("pause ignored because of $state")
    }
  }

  /** Plays the next chapter. If there is none, don't do anything **/
  fun next() {
    checkMainThread()
    prepareIfIdle()
    val content = bookContent
      ?: return

    val nextChapterMarkPosition = content.nextChapterMarkPosition
    if (nextChapterMarkPosition != null) changePosition(nextChapterMarkPosition)
    else content.nextChapter?.let { changePosition(0, it.file) }
  }

  /** Changes the current position in book. */
  fun changePosition(time: Long, changedFile: File? = null) {
    checkMainThread()
    Timber.v("changePosition with time $time and file $changedFile")
    prepareIfIdle()
    if (state == PlayerState.IDLE)
      return

    bookContent?.let {
      val copy = it.updateSettings {
        copy(positionInChapter = time, currentFile = changedFile ?: currentFile)
      }
      _bookContent.onNext(copy)
      player.seekTo(copy.currentChapterIndex, time)
    }
  }

  /** The current playback speed. 1.0 for normal playback, 2.0 for twice the speed, etc. */
  fun setPlaybackSpeed(speed: Float) {
    checkMainThread()
    bookContent?.let {
      val copy = it.updateSettings { copy(playbackSpeed = speed) }
      _bookContent.onNext(copy)
      player.setPlaybackParameters(speed, it.skipSilence)
    }
  }

  fun setSkipSilences(skip: Boolean) {
    checkMainThread()
    Timber.v("setSkipSilences to $skip")

    bookContent?.let {
      val copy = it.updateSettings { copy(skipSilence = skip) }
      _bookContent.onNext(copy)
      player.setPlaybackParameters(it.playbackSpeed, skip)
    }
  }
}
