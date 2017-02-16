package de.ph1b.audiobook.playback

import android.os.PowerManager
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.player.Player
import e
import i
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import v
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaPlayer
@Inject
constructor(private val player: Player, private val playStateManager: PlayStateManager, private val prefs: PrefsManager) {

  private var bookSubject = BehaviorSubject.create<Book>()
  private var stateSubject = BehaviorSubject.createDefault(State.IDLE)
  private var state: State
    set(value) = stateSubject.onNext(value)
    get() = stateSubject.value

  private val seekTime: Int
    get() = prefs.seekTime.value
  private val autoRewindAmount: Int
    get() = prefs.autoRewindAmount.value

  private val errorSubject = PublishSubject.create<Unit>()
  fun onError(): Observable<Unit> = errorSubject.hide()

  init {
    player.setWakeMode(PowerManager.PARTIAL_WAKE_LOCK)
    player.onCompletion {
      // After the current song has ended, prepare the next one if there is one. Else stop the
      // resources.
      bookSubject.value?.let {
        v { "onCompletion called, nextChapter=${it.nextChapter()}" }
        if (it.nextChapter() != null) {
          next()
        } else {
          v { "Reached last track. Stopping player" }
          playStateManager.playState = PlayState.STOPPED

          state = State.PLAYBACK_COMPLETED
        }
      }
    }

    player.onError {
      player.reset()
      state = State.IDLE
      errorSubject.onNext(Unit)
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
    if (this.bookSubject.value != book) {
      i { "constructor called with ${book.name}" }
      this.bookSubject.onNext(book)
    }
  }

  fun book(): Book? = bookSubject.value

  fun bookObservable(): Observable<Book> = bookSubject

  fun setVolume(loud: Boolean) = player.setVolume(if (loud) 1F else 0.1F)

  // Prepares the current chapter set in book.
  private fun prepare() {
    bookSubject.value?.let {
      try {
        player.reset()
        player.prepare(it.currentChapter().file)
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
      State.STOPPED -> {
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
        changePosition(seekTo, it.currentFile)
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

      val previousChapter = it.previousChapter()
      if (player.currentPosition > 2000 || previousChapter == null) {
        i { "seekTo beginning" }
        player.seekTo(0)
        val copy = it.copy(time = 0)
        bookSubject.onNext(copy)
      } else {
        if (toNullOfNewTrack) {
          changePosition(0, previousChapter.file)
        } else {
          changePosition(previousChapter.duration - (seekTime * 1000), previousChapter.file)
        }
      }
    }
  }

  /** Stops the playback and releases some resources. */
  fun stop() {
    if (state == State.STARTED) player.pause()
    playStateManager.playState = PlayState.STOPPED
    state = State.STOPPED
  }


  /**
   * Pauses the player. Also stops the updating mechanism which constantly updates the book to the
   * database.
   *
   * @param rewind true if the player should automatically rewind a little bit.
   */
  fun pause(rewind: Boolean) {
    v { "pause in state $state" }
    bookSubject.value?.let {
      when (state) {
        State.STARTED -> {
          player.pause()

          if (rewind) {
            val autoRewind = autoRewindAmount * 1000
            if (autoRewind != 0) {
              val originalPosition = player.currentPosition
              var seekTo = originalPosition - autoRewind
              seekTo = Math.max(seekTo, 0)
              player.seekTo(seekTo)
              val copy = it.copy(time = seekTo)
              bookSubject.onNext(copy)
            }
          }

          playStateManager.playState = PlayState.PAUSED

          state = State.PAUSED
        }
        else -> e { "pause called in illegal state=$state" }
      }
    }
  }

  /**
   * Plays the next chapter. If there is none, don't do anything.
   */
  fun next() {
    bookSubject.value?.nextChapter()?.let {
      changePosition(0, it.file)
    }
  }

  /**
   * Changes the current position in book. If the path is the same, continues playing the song.
   * Else calls [.prepare] to prepare the next file

   * @param time The time in chapter at which to start
   * *
   * @param file The path of the media to play (relative to the books root path)
   */
  fun changePosition(time: Int, file: File) {
    v { "changePosition with time $time and file $file" }
    bookSubject.value?.let {
      val changeFile = it.currentChapter().file != file
      v { "changeFile=$changeFile" }
      if (changeFile) {
        val wasPlaying = state == State.STARTED
        v { "wasPlaying=$wasPlaying" }

        val copy = it.copy(currentFile = file, time = time)
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
            player.seekTo(time)

            val copy = it.copy(time = time)
            bookSubject.onNext(copy)
          }
          else -> e { "changePosition called in illegal state=$state" }
        }
      }
    }
  }

  fun audioSessionId() = player.audioSessionId()

  /** The current playback speed. 1.0 for normal playback, 2.0 for twice the speed, etc. */
  fun setPlaybackSpeed(speed: Float) {
    bookSubject.value?.let {
      val copy = it.copy(playbackSpeed = speed)
      bookSubject.onNext(copy)
      if (state != State.IDLE && state != State.STOPPED) player.playbackSpeed = speed
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