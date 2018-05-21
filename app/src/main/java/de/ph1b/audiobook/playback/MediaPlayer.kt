package de.ph1b.audiobook.playback

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import de.ph1b.audiobook.common.sparseArray.forEachIndexed
import de.ph1b.audiobook.common.sparseArray.keyAtOrNull
import de.ph1b.audiobook.data.BookContent
import de.ph1b.audiobook.features.audio.Equalizer
import de.ph1b.audiobook.features.audio.LoudnessGain
import de.ph1b.audiobook.injection.PrefKeys
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
  onlyAudioRenderersFactory: OnlyAudioRenderersFactory
) {

  private val player = ExoPlayerFactory.newSimpleInstance(
    onlyAudioRenderersFactory, DefaultTrackSelector()
  )

  private val _bookContent = BehaviorSubject.create<BookContent>()
  val bookContent: BookContent? get() = _bookContent.value
  val bookContentStream = _bookContent.hide()!!

  private val _state = BehaviorSubject.createDefault(PlayerState.IDLE)
  private var state: PlayerState
    get() = _state.value!!
    set(value) {
      if (_state.value != value) _state.onNext(value)
    }

  private val seekTime by seekTimePref
  private var autoRewindAmount by autoRewindAmountPref

  init {
    player.audioAttributes = AudioAttributes.Builder()
      .setContentType(C.CONTENT_TYPE_SPEECH)
      .setUsage(C.USAGE_MEDIA)
      .build()

    player.onStateChanged {
      state = it
    }

    player.onError {
      Timber.e("onError")
      player.playWhenReady = false
      playStateManager.playState = PlayState.STOPPED
    }

    // upon position change update the book
    player.onPositionDiscontinuity {
      val position = player.currentPosition
        .coerceAtLeast(0)
        .toInt()
      Timber.i("onPositionDiscontinuity with currentPos=$position")
      bookContent?.let {
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

    _state.subscribe {
      Timber.i("state changed to $it")

      // set the wake-lock based on the play state
      wakeLockManager.stayAwake(it == PlayerState.PLAYING)

      // upon end stop the player
      if (it == PlayerState.ENDED) {
        Timber.v("onEnded. Stopping player")
        player.playWhenReady = false
        playStateManager.playState = PlayState.STOPPED
      }
    }

    _state
      .switchMap {
        if (it == PlayerState.PLAYING) {
          Observable.interval(200L, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            .map { player.currentPosition }
            .distinctUntilChanged { position -> position / 1000 } // let the value only pass the full second changed.
        } else Observable.empty()
      }
      .subscribe {
        // update the book
        bookContent?.let { book ->
          val index = player.currentWindowIndex
          val time = it.coerceAtLeast(0)
            .toInt()
          val copy = book.updateSettings {
            copy(positionInChapter = time, currentFile = book.chapters[index].file)
          }
          _bookContent.onNext(copy)
        }
      }
  }

  fun init(content: BookContent) {
    val shouldInitialize = player.playbackState == Player.STATE_IDLE
        || !alreadyInitializedChapters(content)
    _bookContent.onNext(content)
    if (shouldInitialize) {
      Timber.i("init")
      player.playWhenReady = false
      player.prepare(dataSourceConverter.toMediaSource(content))
      player.seekTo(content.currentChapterIndex, content.positionInChapter.toLong())
      player.setPlaybackParameters(content.playbackSpeed, content.skipSilence)
      loudnessGain.gainmB = content.loudnessGain
      state = PlayerState.PAUSED
    }
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
      if (state == PlayerState.ENDED) {
        Timber.i("play in state ended. Back to the beginning")
        changePosition(0, it.chapters.first().file)
      }

      if (state == PlayerState.ENDED || state == PlayerState.PAUSED) {
        player.playWhenReady = true
        playStateManager.playState = PlayState.PLAYING
      } else Timber.d("ignore play in state $state")
    }
  }

  fun skip(forward: Boolean) {
    Timber.v("skip forward=$forward")

    prepareIfIdle()
    if (state == PlayerState.IDLE)
      return

    bookContent?.let {
      val currentPos = player.currentPosition
        .coerceAtLeast(0)
      val duration = player.duration
      val delta = seekTime * 1000

      val seekTo = if (forward) currentPos + delta else currentPos - delta
      Timber.v("currentPos=$currentPos, seekTo=$seekTo, duration=$duration")

      when {
        seekTo < 0 -> previous(false)
        seekTo > duration -> next()
        else -> changePosition(seekTo.toInt())
      }
    }
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
    val previousChapter = content.previousChapter
    if (player.currentPosition > 2000 || previousChapter == null) {
      Timber.i("seekTo beginning")
      changePosition(0)
    } else {
      if (toNullOfNewTrack) {
        changePosition(0, previousChapter.file)
      } else {
        val time = (previousChapter.duration - (seekTime * 1000))
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
          changePosition(startOfMark)
          return true
        } else if (index > 0) {
          val seekTo = marks.keyAt(index - 1)
          changePosition(seekTo)
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
    player.playWhenReady = false
    playStateManager.playState = PlayState.STOPPED
  }

  /**
   * Pauses the player.
   * @param rewind true if the player should automatically rewind a little bit.
   */
  fun pause(rewind: Boolean) {
    Timber.v("pause")
    when (state) {
      PlayerState.PLAYING -> {
        bookContent?.let {
          player.playWhenReady = false
          playStateManager.playState = PlayState.PAUSED

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
              chapterMarks.forEachIndexed(reversed = true) findStartOfMark@{ index, startOfMark, _ ->
                if (startOfMark <= currentPosition) {
                  val next = chapterMarks.keyAtOrNull(index + 1)
                  if (next == null || next > currentPosition) {
                    maybeSeekTo = maybeSeekTo.coerceAtLeast(startOfMark.toLong())
                    return@findStartOfMark
                  }
                }
              }

              // finally change position
              changePosition(maybeSeekTo.toInt())
            }
          }
        }
      }
      else -> Timber.d("pause ignored because of $state")
    }
  }

  /** Plays the next chapter. If there is none, don't do anything **/
  fun next() {
    prepareIfIdle()
    val content = bookContent
        ?: return

    val nextChapterMarkPosition = content.nextChapterMarkPosition
    if (nextChapterMarkPosition != null) changePosition(nextChapterMarkPosition)
    else content.nextChapter?.let { changePosition(0, it.file) }
  }

  /** Changes the current position in book. */
  fun changePosition(time: Int, changedFile: File? = null) {
    Timber.v("changePosition with time $time and file $changedFile")
    prepareIfIdle()
    if (state == PlayerState.IDLE)
      return

    bookContent?.let {
      val copy = it.updateSettings {
        copy(positionInChapter = time, currentFile = changedFile ?: currentFile)
      }
      _bookContent.onNext(copy)
      player.seekTo(copy.currentChapterIndex, time.toLong())
    }
  }

  /** The current playback speed. 1.0 for normal playback, 2.0 for twice the speed, etc. */
  fun setPlaybackSpeed(speed: Float) {
    bookContent?.let {
      val copy = it.updateSettings { copy(playbackSpeed = speed) }
      _bookContent.onNext(copy)
      player.setPlaybackParameters(speed, it.skipSilence)
    }
  }

  fun setSkipSilences(skip: Boolean) {
    Timber.v("setSkipSilences to $skip")

    bookContent?.let {
      val copy = it.updateSettings { copy(skipSilence = skip) }
      _bookContent.onNext(copy)
      player.setPlaybackParameters(it.playbackSpeed, skip)
    }
  }
}
