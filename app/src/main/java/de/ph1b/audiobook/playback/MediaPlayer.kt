package de.ph1b.audiobook.playback

import android.support.v4.media.session.PlaybackStateCompat
import android.view.animation.AccelerateInterpolator
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import de.ph1b.audiobook.common.sparseArray.forEachIndexed
import de.ph1b.audiobook.common.sparseArray.keyAtOrNull
import de.ph1b.audiobook.data.BookContent
import de.ph1b.audiobook.features.audio.Equalizer
import de.ph1b.audiobook.features.audio.LoudnessGain
import de.ph1b.audiobook.injection.PerService
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.misc.checkMainThread
import de.ph1b.audiobook.misc.delay
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.utils.ChangeNotifier
import de.ph1b.audiobook.playback.utils.DataSourceConverter
import de.ph1b.audiobook.playback.utils.onAudioSessionId
import de.ph1b.audiobook.playback.utils.onError
import de.ph1b.audiobook.playback.utils.onPositionDiscontinuity
import de.ph1b.audiobook.playback.utils.onSessionPlaybackStateNeedsUpdate
import de.ph1b.audiobook.playback.utils.onStateChanged
import de.ph1b.audiobook.playback.utils.setPlaybackParameters
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration
import kotlin.time.milliseconds
import kotlin.time.seconds

@PerService
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
  private val dataSourceConverter: DataSourceConverter,
  private val player: SimpleExoPlayer,
  private val changeNotifier: ChangeNotifier
) {

  private val scope = MainScope()

  private val _bookContent = ConflatedBroadcastChannel<BookContent?>(null)
  var bookContent: BookContent?
    get() = _bookContent.value
    private set(value) {
      _bookContent.offer(value)
    }
  val bookContentFlow: Flow<BookContent> get() = _bookContent.asFlow().filterNotNull()

  private val _state = ConflatedBroadcastChannel(PlayerState.IDLE)
  private var state: PlayerState
    get() = _state.value
    set(value) {
      if (_state.value != value) _state.offer(value)
    }

  private val seekTime: Duration get() = seekTimePref.value.seconds
  private var autoRewindAmount by autoRewindAmountPref

  init {
    checkMainThread()
    val audioAttributes = AudioAttributes.Builder()
      .setContentType(C.CONTENT_TYPE_SPEECH)
      .setUsage(C.USAGE_MEDIA)
      .build()
    player.setAudioAttributes(audioAttributes, true)

    player.onSessionPlaybackStateNeedsUpdate {
      updateMediaSessionPlaybackState()
    }
    player.onStateChanged {
      playStateManager.playState = when (it) {
        PlayerState.IDLE -> PlayState.Stopped
        PlayerState.ENDED -> PlayState.Stopped
        PlayerState.PAUSED -> PlayState.Paused
        PlayerState.PLAYING -> PlayState.Playing
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
        bookContent = it.updateSettings {
          copy(
            positionInChapter = position,
            currentFile = it.chapters[index].file
          )
        }
      }
    }

    // update equalizer with new audio session upon arrival
    player.onAudioSessionId {
      equalizer.update(it)
      loudnessGain.update(it)
    }

    scope.launch {
      _state.asFlow().collect {
        Timber.i("state changed to $it")
        // upon end stop the player
        if (it == PlayerState.ENDED) {
          Timber.v("onEnded. Stopping player")
          checkMainThread()
          player.playWhenReady = false
        }
      }
    }

    scope.launch {
      _state.asFlow().map { it == PlayerState.PLAYING }.distinctUntilChanged()
        .transformLatest { playing ->
          if (playing) {
            while (true) {
              delay(200.milliseconds)
              emit(player.currentPosition.coerceAtLeast(0))
            }
          }
        }
        .distinctUntilChangedBy {
          // only if the second changed, emit
          it / 1000
        }
        .collect { time ->
          bookContent?.let { book ->
            val index = player.currentWindowIndex
            bookContent = book.updateSettings {
              copy(positionInChapter = time, currentFile = book.chapters[index].file)
            }
          }
        }
    }
  }

  fun updateMediaSessionPlaybackState() {
    val playbackStateCompat = when (player.playbackState) {
      Player.STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
      Player.STATE_READY -> if (player.playWhenReady) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
      Player.STATE_ENDED -> PlaybackStateCompat.STATE_STOPPED
      Player.STATE_IDLE -> PlaybackStateCompat.STATE_NONE
      else -> PlaybackStateCompat.STATE_NONE
    }
    changeNotifier.updatePlaybackState(playbackStateCompat, bookContent)
  }

  fun init(content: BookContent) {
    val shouldInitialize = player.playbackState == Player.STATE_IDLE ||
      !alreadyInitializedChapters(content)
    if (!shouldInitialize) {
      return
    }
    Timber.i("init ${content.currentFile}")
    bookContent = content
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

  fun playPause() {
    if (state == PlayerState.PLAYING) {
      pause(rewind = true)
    } else {
      play()
    }
  }

  fun setLoudnessGain(mB: Int) {
    Timber.v("setLoudnessGain to $mB mB")

    bookContent?.let {
      bookContent = it.updateSettings { copy(loudnessGain = mB) }
      loudnessGain.gainmB = mB
    }
  }

  fun play() {
    Timber.v("play called in state $state, currentFile=${bookContent?.currentFile}")
    prepareIfIdle()
    bookContent?.let {
      bookContent = it.updateSettings {
        copy(lastPlayedAtMillis = System.currentTimeMillis())
      }
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

  private fun skip(skipAmount: Duration) {
    checkMainThread()
    prepareIfIdle()
    if (state == PlayerState.IDLE)
      return

    bookContent?.let {
      val currentPos = player.currentPosition.milliseconds
        .coerceAtLeast(Duration.ZERO)
      val duration = player.duration.milliseconds

      val seekTo = currentPos + skipAmount
      Timber.v("currentPos=$currentPos, seekTo=$seekTo, duration=$duration")
      when {
        seekTo < Duration.ZERO -> previous(false)
        seekTo > duration -> next()
        else -> changePosition(seekTo.toLongMilliseconds())
      }
    }
  }

  fun skip(forward: Boolean) {
    Timber.v("skip forward=$forward")
    skip(skipAmount = if (forward) seekTime else -seekTime)
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
        val time = (previousChapter.duration.milliseconds - seekTime)
          .coerceAtLeast(Duration.ZERO)
        changePosition(time.toLongMilliseconds(), previousChapter.file)
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
      bookContent = copy
      player.seekTo(copy.currentChapterIndex, time)
    }
  }

  /** The current playback speed. 1.0 for normal playback, 2.0 for twice the speed, etc. */
  fun setPlaybackSpeed(speed: Float) {
    checkMainThread()
    bookContent?.let {
      val copy = it.updateSettings { copy(playbackSpeed = speed) }
      bookContent = copy
      player.setPlaybackParameters(speed, it.skipSilence)
    }
  }

  fun setSkipSilences(skip: Boolean) {
    checkMainThread()
    Timber.v("setSkipSilences to $skip")

    bookContent?.let {
      bookContent = it.updateSettings { copy(skipSilence = skip) }
      player.setPlaybackParameters(it.playbackSpeed, skip)
    }
  }

  private var fadeOutJob: Job? = null

  fun fadeOut() {
    if (fadeOutJob?.isActive == true) {
      return
    }
    fadeOutJob = scope.launch {
      var timeLeft = FADE_OUT_DURATION
      val step = 100.milliseconds
      while (timeLeft > Duration.ZERO) {
        delay(step)
        timeLeft -= step
        player.volume = volumeForFadeOutTimeLeft(timeLeft)
      }
      pause(rewind = false)
      skip(-FADE_OUT_DURATION)
      player.volume = 1F
      player.stop()
    }
  }

  fun cancelFadeOut() {
    fadeOutJob?.cancel()
    player.volume = 1F
    play()
  }

  fun release() {
    player.release()
    scope.cancel()
  }
}

private fun volumeForFadeOutTimeLeft(timeLeft: Duration): Float {
  val fraction = (timeLeft / FADE_OUT_DURATION).toFloat()
  return FADE_OUT_INTERPOLATOR.getInterpolation(fraction)
    .also { Timber.i("set volume to $it") }
}

private val FADE_OUT_INTERPOLATOR = AccelerateInterpolator()
