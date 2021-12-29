package de.ph1b.audiobook.playback.player

import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.util.Assertions.checkMainThread
import de.paulwoitaschek.flowpref.Pref
import de.ph1b.audiobook.common.pref.PrefKeys
import de.ph1b.audiobook.data.BookContent
import de.ph1b.audiobook.data.Chapter
import de.ph1b.audiobook.data.ChapterMark
import de.ph1b.audiobook.data.markForPosition
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.playback.di.PlaybackScope
import de.ph1b.audiobook.playback.playstate.PlayStateManager
import de.ph1b.audiobook.playback.playstate.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.playstate.PlayerState
import de.ph1b.audiobook.playback.session.ChangeNotifier
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@PlaybackScope
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
  private val changeNotifier: ChangeNotifier,
  @Named(PrefKeys.CURRENT_BOOK)
  private val currentBookIdPref: Pref<UUID>,
  private val repo: BookRepository
) {

  private val scope = MainScope()

  private val _bookContent = MutableStateFlow<BookContent?>(null)
  var bookContent: BookContent?
    get() = _bookContent.value
    private set(value) {
      _bookContent.value = value
    }
  val bookContentFlow: Flow<BookContent> get() = _bookContent.filterNotNull()

  private val _state = MutableStateFlow(PlayerState.IDLE)
  private var state: PlayerState
    get() = _state.value
    set(value) {
      if (_state.value != value) _state.value = value
    }

  private val seekTime: Duration get() = seekTimePref.value.seconds
  private var autoRewindAmount by autoRewindAmountPref

  init {
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
      _state.collect {
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
      _state.map { it == PlayerState.PLAYING }.distinctUntilChanged()
        .transformLatest { playing ->
          if (playing) {
            while (true) {
              delay(200)
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

    scope.launch {
      val notIdleFlow = _state.filter { it != PlayerState.IDLE }
      val contentFlow = currentBookIdPref.flow.flatMapLatest { repo.flow(it) }
        .filterNotNull()
        .map { it.content }
        .distinctUntilChanged()
      combine(notIdleFlow, contentFlow) { _, _ -> }
        .collect { prepare() }
    }
  }

  fun updateMediaSessionPlaybackState() {
    val playbackStateCompat = when (player.playbackState) {
      Player.STATE_READY, Player.STATE_BUFFERING -> {
        if (player.playWhenReady) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
      }
      Player.STATE_ENDED -> PlaybackStateCompat.STATE_STOPPED
      Player.STATE_IDLE -> PlaybackStateCompat.STATE_NONE
      else -> PlaybackStateCompat.STATE_NONE
    }
    changeNotifier.updatePlaybackState(playbackStateCompat, bookContent)
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
    prepare()
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
    prepare()
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
        else -> changePosition(seekTo.inWholeMilliseconds)
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
    prepare()
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
        changePosition(time.inWholeMilliseconds, previousChapter.file)
      }
    }
  }

  private fun previousByMarks(content: BookContent): Boolean {
    val currentChapter = content.currentChapter
    val currentMark = currentChapter.markForPosition(content.positionInChapter)
    val timePlayedInMark = content.positionInChapter - currentMark.startMs
    if (timePlayedInMark > 2000) {
      changePosition(currentMark.startMs)
      return true
    } else {
      // jump to the start of the previous mark
      val indexOfCurrentMark = currentChapter.chapterMarks.indexOf(currentMark)
      if (indexOfCurrentMark > 0) {
        changePosition(currentChapter.chapterMarks[indexOfCurrentMark - 1].startMs)
        return true
      }
    }
    return false
  }

  private fun prepare() {
    val content = repo.bookById(currentBookIdPref.value)?.content ?: return
    val shouldInitialize = player.playbackState == Player.STATE_IDLE || !alreadyInitializedChapters(content)
    if (!shouldInitialize) {
      return
    }
    Timber.i("prepare $content")
    bookContent = content
    checkMainThread()
    player.playWhenReady = false
    player.setMediaSource(dataSourceConverter.toMediaSource(content))
    player.prepare()
    player.seekTo(content.currentChapterIndex, content.positionInChapter)
    player.setPlaybackSpeed(content.playbackSpeed)
    player.skipSilenceEnabled = content.skipSilence
    loudnessGain.gainmB = content.loudnessGain
    state = PlayerState.PAUSED
  }

  fun stop() {
    checkMainThread()
    player.stop()
  }

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
              val currentChapter = it.currentChapter
              val currentMark = currentChapter.markForPosition(currentPosition)
              val markForSeeking = currentChapter.markForPosition(maybeSeekTo)
              if (markForSeeking != currentMark) {
                maybeSeekTo = maybeSeekTo.coerceAtLeast(currentMark.startMs)
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

  fun next() {
    checkMainThread()
    prepare()
    val content = bookContent
      ?: return
    val nextMark = content.currentChapter.nextMark(content.positionInChapter)
    if (nextMark != null) {
      changePosition(nextMark.startMs)
    } else {
      content.nextChapter?.let { changePosition(0, it.file) }
    }
  }

  /** Changes the current position in book. */
  fun changePosition(time: Long, changedFile: File? = null) {
    checkMainThread()
    Timber.v("changePosition with time $time and file $changedFile")
    prepare()
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
    prepare()
    bookContent?.let {
      val copy = it.updateSettings { copy(playbackSpeed = speed) }
      bookContent = copy
      player.setPlaybackSpeed(speed)
    }
  }

  fun setSkipSilences(skip: Boolean) {
    checkMainThread()
    Timber.v("setSkipSilences to $skip")
    prepare()
    bookContent?.let {
      bookContent = it.updateSettings { copy(skipSilence = skip) }
      player.skipSilenceEnabled = skip
    }
  }

  fun release() {
    player.release()
    scope.cancel()
  }
}

private fun Chapter.nextMark(positionInChapterMs: Long): ChapterMark? {
  val markForPosition = markForPosition(positionInChapterMs)
  val marks = chapterMarks
  val index = marks.indexOf(markForPosition)
  return if (index != -1) {
    marks.getOrNull(index + 1)
  } else {
    null
  }
}
