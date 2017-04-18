package de.ph1b.audiobook.playback

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.PowerManager
import de.paul_woitaschek.mediaplayer.AndroidPlayer
import de.paul_woitaschek.mediaplayer.SpeedPlayer
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.misc.forEachIndexed
import de.ph1b.audiobook.misc.keyAtOrNull
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.utils.MediaPlayerCapabilities
import e
import i
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import v
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import de.paul_woitaschek.mediaplayer.MediaPlayer as InternalPlayer

@Singleton
class MediaPlayer
@Inject
constructor(
    context: Context,
    private val playStateManager: PlayStateManager,
    private val prefs: PrefsManager,
    playerCapabilities: MediaPlayerCapabilities) {

  private val player = (if (!playerCapabilities.useCustomMediaPlayer()) {
    AndroidPlayer(context)
  } else SpeedPlayer(context))

  private var bookSubject = BehaviorSubject.create<Book>()
  private var stateSubject = BehaviorSubject.createDefault(State.IDLE)
  private var state: State
    set(value) = stateSubject.onNext(value)
    get() = stateSubject.value

  private val seekTime: Int
    get() = prefs.seekTime.value
  private val autoRewindAmount: Int
    get() = prefs.autoRewindAmount.value

  fun book(): Book? = bookSubject.value
  val bookStream = bookSubject.hide()!!

  init {
    player.setWakeMode(PowerManager.PARTIAL_WAKE_LOCK)
    player.setAudioStreamType(AudioManager.STREAM_MUSIC)
    player.onCompletion {
      // After the current song has ended, prepare the next one if there is one. Else stop the
      // resources.
      bookSubject.value?.let {
        v { "onCompletion called, nextChapter=${it.nextChapter()}" }
        if (it.nextChapterMarkPosition() != null || it.nextChapter() != null) {
          next()
        } else {
          v { "Reached last track. Stopping player" }
          playStateManager.playState = PlayState.STOPPED

          state = State.PLAYBACK_COMPLETED
        }
      }
    }

    player.onError {
      e { "onError" }
      player.reset()
      state = State.IDLE
    }

    stateSubject.switchMap {
      if (it == State.STARTED) {
        Observable.interval(200L, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            .map { player.currentPosition }
            .distinctUntilChanged { it -> it / 1000 } // let the value only pass the full second changed.
      } else Observable.empty()
    }.subscribe {
      // update the book
      bookSubject.value?.copy(time = it).let {
        bookSubject.onNext(it)
      }
    }
  }

  /** Initializes a new book. After this, a call to play can be made. */
  fun init(book: Book) {
    if (bookSubject.value != book) {
      i { "init called with ${book.name}" }
      bookSubject.onNext(book)
      state = State.IDLE
    }
  }

  fun setVolume(loud: Boolean) = player.setVolume(if (loud) 1F else 0.1F)

  // Prepares the current chapter set in book.
  private fun prepare() {
    bookSubject.value?.let {
      val start = System.currentTimeMillis()
      try {
        val fileToPrepare = it.currentChapter().file
        player.reset()
        val uri = Uri.fromFile(fileToPrepare)
        player.prepare(uri)
        state = State.PREPARED

        v { "preparing took ${System.currentTimeMillis() - start}ms" }
        player.seekTo(it.time)
        player.playbackSpeed = it.playbackSpeed
        state = State.PREPARED
      } catch (ex: IOException) {
        e(ex) { "Error when preparing the player." }
        state = State.STOPPED
      }
    }
  }

  /** Plays the prepared file. */
  fun play() {
    v { "play called in state $state" }
    when (state) {
      State.PLAYBACK_COMPLETED -> {
        player.seekTo(0)
        player.start()
        playStateManager.playState = PlayState.PLAYING
        state = State.STARTED
      }
      State.PREPARED, State.PAUSED -> {
        player.start()
        playStateManager.playState = PlayState.PLAYING
        state = State.STARTED
      }
      State.STOPPED, State.IDLE -> {
        prepare()
        if (state == State.PREPARED) {
          play()
        }
      }
      else -> i { "Play ignores state=$state " }
    }
  }

  fun skip(direction: Direction) {
    v { "direction=$direction" }
    bookSubject.value?.let {
      if (state == State.IDLE && state == State.STOPPED) {
        prepare()
        if (state != State.PREPARED) return
      }

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
        changePosition(seekTo)
      }
    }
  }

  /** If current time is > 2000ms, seek to 0. Else play previous chapter if there is one. */
  fun previous(toNullOfNewTrack: Boolean) {
    i { "previous with toNullOfNewTrack=$toNullOfNewTrack called in state $state" }
    bookSubject.value?.let {
      if (state == State.IDLE || state == State.STOPPED) {
        prepare()
        if (state != State.PREPARED) return
      }

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
    if (state == State.STARTED) player.pause()
    playStateManager.playState = PlayState.STOPPED
    state = State.STOPPED
  }

  /**
   * Pauses the player.
   * @param rewind true if the player should automatically rewind a little bit.
   */
  fun pause(rewind: Boolean) {
    v { "pause in state $state" }
    bookSubject.value?.let { it ->
      when (state) {
        State.STARTED -> {
          player.pause()

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
                    maybeSeekTo = maybeSeekTo.coerceAtLeast(startOfMark)
                    return@findStartOfmark
                  }
                }
              }

              // finally change position
              changePosition(maybeSeekTo)
            }
          }

          playStateManager.playState = PlayState.PAUSED

          state = State.PAUSED
        }
        else -> e { "pause called in illegal state=$state" }
      }
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

  /**
   * Changes the current position in book. If the path is the same, continues playing the song.
   * Else calls [.prepare] to prepare the next file
   */
  fun changePosition(time: Int, changedFile: File? = null) {
    v { "changePosition with time $time and file $changedFile" }
    bookSubject.value?.let {
      val file = changedFile ?: it.currentFile
      val changeFile = it.currentChapter().file != file
      v { "changeFile=$changeFile" }
      if (changeFile) {
        val wasPlaying = state == State.STARTED
        v { "wasPlaying=$wasPlaying" }

        val copy = it.copy(time = time, currentFile = file)
        bookSubject.onNext(copy)

        prepare()
        if (state != State.PREPARED) return
        if (wasPlaying) {
          player.start()
          state = State.STARTED
          playStateManager.playState = PlayState.PLAYING
        } else {
          playStateManager.playState = PlayState.PAUSED
        }
      } else {
        if (state == State.STOPPED || state == State.IDLE) prepare()
        when (state) {
          State.STARTED, State.PAUSED, State.PREPARED -> {
            val copy = it.copy(time = time)
            bookSubject.onNext(copy)

            player.seekTo(time)
          }
          else -> e { "changePosition called in illegal state=$state" }
        }
      }
    }
  }

  /** The current playback speed. 1.0 for normal playback, 2.0 for twice the speed, etc. */
  fun setPlaybackSpeed(speed: Float) {
    bookSubject.value?.let {
      val copy = it.copy(playbackSpeed = speed)
      bookSubject.onNext(copy)
      if (state != State.IDLE && state != State.STOPPED) {
        player.playbackSpeed = speed
      }
    }
  }

  /** The direction to skip. */
  enum class Direction {
    FORWARD, BACKWARD
  }

  /** The various internal states the player can have. */
  private enum class State {
    IDLE,
    PREPARED,
    STARTED,
    PAUSED,
    STOPPED,
    PLAYBACK_COMPLETED;
  }
}