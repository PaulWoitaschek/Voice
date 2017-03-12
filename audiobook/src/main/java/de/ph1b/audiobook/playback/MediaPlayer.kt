package de.ph1b.audiobook.playback

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import com.crashlytics.android.Crashlytics
import d
import de.paul_woitaschek.mediaplayer.AndroidPlayer
import de.paul_woitaschek.mediaplayer.SpeedPlayer
import de.ph1b.audiobook.Book
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
import java.lang.IllegalStateException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import de.paul_woitaschek.mediaplayer.MediaPlayer as InternalPlayer

@Singleton
class MediaPlayer
@Inject
constructor(
    private val context: Context,
    private val playStateManager: PlayStateManager,
    private val prefs: PrefsManager,
    private val playerCapabilities: MediaPlayerCapabilities) {

  // on android >= M we use the regular android player as it can use speed. Else use it only if there is a bug in the device
  private var player = newPlayer()
  private val nextPlayer = NextPlayer(newPlayer())

  private fun newPlayer(): InternalPlayer {
    val player = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M || !playerCapabilities.useCustomMediaPlayer()) {
      AndroidPlayer(context)
    } else SpeedPlayer(context)
    return player.apply {
      setWakeMode(PowerManager.PARTIAL_WAKE_LOCK)
      setAudioStreamType(AudioManager.STREAM_MUSIC)
    }
  }

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

  private fun attachCallbacks(player: InternalPlayer) {
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
      e { "onError" }
      player.reset()
      state = State.IDLE
    }
  }

  init {
    attachCallbacks(player)

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

      // prepare the nextPlayer with the current file so upon prepare the file will be ready
      player = nextPlayer.swap(player, book.currentFile)
          .player
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

        var prepared = false
        // if nextPlayer is has the file we are looking for, use it. Else swap the files so
        // the nextPlayer can prepare the future file
        if (nextPlayer.fileToPrepare == fileToPrepare || nextPlayer.ready()) {
          val (newPlayer, ready, preparedFile) = nextPlayer.swap(player, it.nextChapter()?.file)
          player = newPlayer
          attachCallbacks(player)
          prepared = ready && preparedFile == fileToPrepare
          if (!prepared) d { "still have to prepare because ready=$ready, rightFile=${preparedFile == fileToPrepare}" }
        } else d { "nextPlayer is still preparing" }

        if (!prepared) {
          d { "prepare blocking" }
          player.reset()
          val uri = Uri.fromFile(fileToPrepare)
          try {
            player.prepare(uri)
          } catch (e: IllegalStateException) {
            Crashlytics.logException(RuntimeException("IllegalStateException after reset. Android Bug"))
            player.reset()
            player.prepare(uri)
          }
          state = State.PREPARED
        }

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