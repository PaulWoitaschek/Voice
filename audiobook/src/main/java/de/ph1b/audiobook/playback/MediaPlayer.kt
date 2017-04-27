package de.ph1b.audiobook.playback

import android.content.Context
import android.media.AudioManager
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import d
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.features.bookPlaying.Equalizer
import de.ph1b.audiobook.misc.forEachIndexed
import de.ph1b.audiobook.misc.keyAtOrNull
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.utils.*
import e
import i
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import v
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaPlayer
@Inject
constructor(
    context: Context,
    private val playStateManager: PlayStateManager,
    private val prefs: PrefsManager,
    private val equalizer: Equalizer,
    private val wakeLockManager: WakeLockManager,
    private val dataSourceConverter: DataSourceConverter) {

  private val player: SimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector())

  private var bookSubject = BehaviorSubject.create<Book>()

  private val stateSubject = BehaviorSubject.createDefault(PlayerState.IDLE)
  private var state: PlayerState
    get() = stateSubject.value
    set(value) {
      if (stateSubject.value != value) stateSubject.onNext(value)
    }

  private val seekTime: Int
    get() = prefs.seekTime.value
  private val autoRewindAmount: Int
    get() = prefs.autoRewindAmount.value

  fun book(): Book? = bookSubject.value
  val bookStream = bookSubject.hide()!!

  init {
    player.audioStreamType = AudioManager.STREAM_MUSIC

    // delegate player state changes
    player.onStateChanged { state = it }

    // on error reset the playback
    player.onError {
      e { "onError" }
      player.stop()
      player.playWhenReady = false
      state = PlayerState.IDLE
    }

    // upon position change update the book
    player.onPositionDiscontinuity {
      i { "onPositionDiscontinuity with currentPos=${player.currentPosition}" }
      bookSubject.value?.let {
        val index = player.currentWindowIndex
        bookSubject.onNext(it.copy(time = player.currentPosition.toInt(), currentFile = it.chapters[index].file))
      }
    }

    // update equalizer with new audio session upon arrival
    player.onAudioSessionId { equalizer.update(it) }

    stateSubject.subscribe {
      i { "state changed to $it" }

      // set the wake-lock based on the play state
      wakeLockManager.stayAwake(it == PlayerState.PLAYING)

      // upon end stop the player
      if (it == PlayerState.ENDED) {
        v { "onEnded. Stopping player" }
        player.playWhenReady = false
        playStateManager.playState = PlayState.STOPPED
      }
    }

    stateSubject.switchMap {
      if (it == PlayerState.PLAYING) {
        Observable.interval(200L, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            .map { player.currentPosition }
            .distinctUntilChanged { it -> it / 1000 } // let the value only pass the full second changed.
      } else Observable.empty()
    }.subscribe {
      // update the book
      bookSubject.value?.let { book ->
        val index = player.currentWindowIndex
        val copy = book.copy(time = it.toInt(), currentFile = book.chapters[index].file)
        bookSubject.onNext(copy)
      }
    }
  }

  /** Initializes a new book. After this, a call to play can be made. */
  fun init(book: Book) {
    if (bookSubject.value != book) {
      i { "init called with ${book.name}" }
      player.playWhenReady = false
      player.prepare(dataSourceConverter.toMediaSource(book))
      player.seekTo(book.currentChapterIndex(), book.time.toLong())
      player.setPlaybackSpeed(book.playbackSpeed)
      bookSubject.onNext(book)
      state = PlayerState.PAUSED
    }
  }

  fun setVolume(loud: Boolean) {
    player.volume = (if (loud) 1F else 0.1F)
  }

  /** Plays the prepared file. */
  fun play() {
    v { "play called in state $state" }
    bookSubject.value?.let {
      if (state == PlayerState.ENDED) {
        i { "play in state ended. Back to the beginning" }
        changePosition(0, it.chapters.first().file)
      }

      if (state == PlayerState.ENDED || state == PlayerState.PAUSED) {
        player.playWhenReady = true
        playStateManager.playState = PlayState.PLAYING
      } else d { "ignore play in state $state" }
    }
  }

  fun skip(direction: Direction) {
    v { "direction=$direction" }

    if (state == PlayerState.IDLE)
      return

    bookSubject.value?.let {
      val currentPos = player.currentPosition
      val duration = player.duration
      val delta = seekTime * 1000

      val seekTo = if ((direction == Direction.FORWARD)) currentPos + delta else currentPos - delta
      v { "currentPos=$currentPos, seekTo=$seekTo, duration=$duration" }

      if (seekTo < 0) {
        previous(false)
      } else if (seekTo > duration) {
        next()
      } else {
        changePosition(seekTo.toInt())
      }
    }
  }

  /** If current time is > 2000ms, seek to 0. Else play previous chapter if there is one. */
  fun previous(toNullOfNewTrack: Boolean) {
    i { "previous with toNullOfNewTrack=$toNullOfNewTrack called in state $state" }

    if (state == PlayerState.IDLE)
      return

    bookSubject.value?.let {
      val handled = previousByMarks(it)
      if (!handled) previousByFile(it, toNullOfNewTrack)
    }
  }

  private fun previousByFile(book: Book, toNullOfNewTrack: Boolean) {
    val previousChapter = book.previousChapter()
    if (player.currentPosition > 2000 || previousChapter == null) {
      i { "seekTo beginning" }
      changePosition(0)
    } else {
      if (toNullOfNewTrack) {
        changePosition(0, previousChapter.file)
      } else {
        changePosition(previousChapter.duration - (seekTime * 1000), previousChapter.file)
      }
    }
  }

  private fun previousByMarks(book: Book): Boolean {
    val marks = book.currentChapter().marks
    marks.forEachIndexed(reversed = true) { index, startOfMark, _ ->
      if (book.time >= startOfMark) {
        val diff = book.time - startOfMark
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
    v { "pause" }
    when (state) {
      PlayerState.PLAYING -> {
        bookSubject.value?.let {
          player.playWhenReady = false
          playStateManager.playState = PlayState.PAUSED

          if (rewind) {
            val autoRewind = autoRewindAmount * 1000
            if (autoRewind != 0) {
              // get the raw rewinded position
              val currentPosition = player.currentPosition
              var maybeSeekTo = currentPosition - autoRewind
                  .coerceAtLeast(0) // make sure not to get into negative time

              // now try to find the current chapter mark and make sure we don't auto-rewind
              // to a previous mark
              val chapterMarks = it.currentChapter().marks
              chapterMarks.forEachIndexed(reversed = true) findStartOfmark@ { index, startOfMark, _ ->
                if (startOfMark <= currentPosition) {
                  val next = chapterMarks.keyAtOrNull(index + 1)
                  if (next == null || next > currentPosition) {
                    maybeSeekTo = maybeSeekTo.coerceAtLeast(startOfMark.toLong())
                    return@findStartOfmark
                  }
                }
              }

              // finally change position
              changePosition(maybeSeekTo.toInt())
            }
          }
        }
      }
      else -> e { "pause ignored because of $state" }
    }
  }

  /** Plays the next chapter. If there is none, don't do anything **/
  fun next() {
    val book = bookSubject.value
        ?: return

    val nextChapterMarkPosition = book.nextChapterMarkPosition()
    if (nextChapterMarkPosition != null) changePosition(nextChapterMarkPosition)
    else book.nextChapter()?.let { changePosition(0, it.file) }
  }

  /** Changes the current position in book. */
  fun changePosition(time: Int, changedFile: File? = null) {
    v { "changePosition with time $time and file $changedFile" }
    if (state == PlayerState.IDLE)
      return

    bookSubject.value?.let {
      val copy = it.copy(time = time, currentFile = changedFile ?: it.currentFile)
      bookSubject.onNext(copy)
      player.seekTo(copy.currentChapterIndex(), time.toLong())
    }
  }

  /** The current playback speed. 1.0 for normal playback, 2.0 for twice the speed, etc. */
  fun setPlaybackSpeed(speed: Float) {
    bookSubject.value?.let {
      val copy = it.copy(playbackSpeed = speed)
      bookSubject.onNext(copy)
      player.setPlaybackSpeed(speed)
    }
  }

  /** The direction to skip. */
  enum class Direction {
    FORWARD, BACKWARD
  }
}