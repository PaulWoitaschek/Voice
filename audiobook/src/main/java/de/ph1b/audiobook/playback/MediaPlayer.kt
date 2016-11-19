package de.ph1b.audiobook.playback

import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.features.book_playing.Equalizer
import de.ph1b.audiobook.playback.utils.onAudioSessionId
import de.ph1b.audiobook.playback.utils.onEnded
import de.ph1b.audiobook.playback.utils.onError
import e
import i
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import v
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class MediaPlayer
@Inject
constructor(private val player: SimpleExoPlayer, private val dataSourceFactory: DataSource.Factory, private val playStateManager: PlayStateManager, private val equalizer: Equalizer) {

  private var book = BehaviorSubject.create<Book>()
  private var state = BehaviorSubject.createDefault(State.IDLE)

  private var updatingDisposable: Disposable? = null
  private val errorSubject = PublishSubject.create<Unit>()
  fun onError(): Observable<Unit> = errorSubject

  init {
    player.onEnded {
      // After the current song has ended, prepare the next one if there is one. Else stop the
      // resources.
      book.value?.let {
        v { "onCompletion called, nextChapter=${it.nextChapter()}" }
        if (it.nextChapter() != null) {
          next()
        } else {
          v { "Reached last track. Stopping player" }
          playStateManager.playState.onNext(PlayStateManager.PlayState.STOPPED)
          player.playWhenReady = false

          state.onNext(State.IDLE)
        }
      }
    }
    player.onError {
      e { "exoPlayer error $it" }
      player.stop()
      state.onNext(MediaPlayer.State.IDLE)
      errorSubject.onNext(Unit)
    }

    player.onAudioSessionId {
      equalizer.update(it)
    }

    state.map { it == State.STARTED }
      .subscribe { isPlaying ->
        if (isPlaying) {
          v { "startUpdating" }
          if (updatingDisposable?.isDisposed ?: true) {
            // updates the book automatically with the current position
            updatingDisposable = Observable.interval(200L, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
              .map { if (state.value == State.STARTED) player.currentPosition else -1L }
              .filter { it != -1L }
              .distinct { it / 1000 } // let the value only pass the full second changed.
              .subscribe {
                // update the book
                book.value?.copy(time = it.toInt()).let {
                  book.onNext(it)
                }
              }
          }
        } else {
          v { "stop updating" }
          updatingDisposable?.dispose()
        }
      }
  }


  /**
   * Initializes a new book. After this, a call to play can be made.

   * @param book The book to be initialized.
   */
  fun init(book: Book) {
    if (this.book.value != book) {
      i { "constructor called with ${book.name}" }
      this.book.onNext(book)
    }
  }

  fun book(): Book? = book.value

  fun bookObservable(): Observable<Book> = book

  fun setVolume(loud: Boolean) {
    player.volume = if (loud) 1F else 0.1F
  }

  // Prepares the current chapter set in book.
  private fun prepare() {
    book.value?.let {
      i { "prepare ${it.currentFile}" }
      val extractorsFactory = DefaultExtractorsFactory()
      val uri = Uri.fromFile(it.currentFile)
      val source = ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null)
      player.prepare(source)
      equalizer.update(player.audioSessionId)
      player.seekTo(it.time.toLong())
      // player.playbackSpeed = it.playbackSpeed
      state.onNext(State.PAUSED)
    }
  }

  /**
   * Plays the prepared file.
   */
  fun play() {
    i { "play called in state ${state.value}" }
    when (state.value) {
      State.PAUSED -> {
        player.playWhenReady = true
        playStateManager.playState.onNext(PlayStateManager.PlayState.PLAYING)
        state.onNext(State.STARTED)
      }
      State.IDLE -> {
        prepare()
        if (state.value == State.PAUSED) {
          play()
        }
      }
      else -> i { "Play ignores state=${state.value} " }
    }
  }

  var seekTime = 0
  var autoRewindAmount = 0

  /**
   * Skips by the amount, specified in the settings.

   * @param direction The direction to skip
   */
  fun skip(direction: Direction) {
    v { "direction=$direction" }
    book.value?.let {
      if (state.value == State.IDLE) {
        prepare()
        if (state.value != State.PAUSED) return
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
        changePosition(seekTo.toInt(), it.currentFile)
      }
    }
  }

  /**
   * If current time is > 2000ms, seek to 0. Else play previous chapter if there is one.
   */
  fun previous(toNullOfNewTrack: Boolean) {
    book.value?.let {
      if (state.value == State.IDLE) {
        prepare()
        if (state.value != State.PAUSED) return
      }

      val previousChapter = it.previousChapter()
      if (player.currentPosition > 2000 || previousChapter == null) {
        player.seekTo(0)
        val copy = it.copy(time = 0)
        book.onNext(copy)
      } else {
        if (toNullOfNewTrack) {
          changePosition(0, previousChapter.file)
        } else {
          changePosition(previousChapter.duration - (seekTime * 1000), previousChapter.file)
        }
      }
    }
  }

  /**
   * Stops the playback and releases some resources.
   */
  fun stop() {
    if (state.value == State.STARTED) player.playWhenReady = false
    playStateManager.playState.onNext(PlayStateManager.PlayState.STOPPED)
    state.onNext(State.IDLE)
  }

  fun audioSessionId() = player.audioSessionId


  /**
   * Pauses the player. Also stops the updating mechanism which constantly updates the book to the
   * database.

   * @param rewind true if the player should automatically rewind a little bit.
   */
  fun pause(rewind: Boolean) {
    v { "pause acquired lock. state is=${state.value}" }
    book.value?.let {
      when (state.value) {
        State.STARTED -> {
          player.playWhenReady = false

          if (rewind) {
            val autoRewind = autoRewindAmount * 1000
            if (autoRewind != 0) {
              val originalPosition = player.currentPosition
              var seekTo = originalPosition - autoRewind
              seekTo = Math.max(seekTo, 0)
              player.seekTo(seekTo)
              val copy = it.copy(time = seekTo.toInt())
              book.onNext(copy)
            }
          }

          playStateManager.playState.onNext(PlayStateManager.PlayState.PAUSED)

          state.onNext(State.PAUSED)
        }
        else -> e { "pause called in illegal state=${state.value}" }
      }
    }
  }

  /**
   * Plays the next chapter. If there is none, don't do anything.
   */
  fun next() {
    book.value?.nextChapter()?.let {
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
    book.value?.let {
      val changeFile = (it.currentChapter().file != file)
      v { "changeFile=$changeFile" }
      if (changeFile) {
        val wasPlaying = (state.value == State.STARTED)

        val copy = it.copy(currentFile = file, time = time)
        book.onNext(copy)

        prepare()
        if (wasPlaying) {
          player.playWhenReady = true
          state.onNext(State.STARTED)
          playStateManager.playState.onNext(PlayStateManager.PlayState.PLAYING)
        } else {
          playStateManager.playState.onNext(PlayStateManager.PlayState.PAUSED)
        }
      } else {
        if (state.value == State.IDLE) prepare()
        when (state.value) {
          State.STARTED, State.PAUSED -> {
            player.seekTo(time.toLong())

            val copy = it.copy(time = time)
            book.onNext(copy)
          }
          else -> e { "changePosition called in illegal state=${state.value}" }
        }
      }
    }
  }

  /**
   * The current playback speed. 1.0 for normal playback, 2.0 for twice the speed, etc.
   */
  fun setPlaybackSpeed(speed: Float) {
    book.value?.let {
      val copy = it.copy(playbackSpeed = speed)
      book.onNext(copy)


      player.playbackSpeed = speed
    }
  }

  private var SimpleExoPlayer.playbackSpeed: Float
    set(value) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (playbackSpeed == value) return

        playbackParams = PlaybackParams().apply {
          speed = value
        }
      }
    }
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      playbackParams?.speed ?: 1F
    } else 1F

  /**
   * The direction to skip.
   */
  enum class Direction {
    FORWARD, BACKWARD
  }

  /**
   * The various internal states the player can have.
   */
  private enum class State {
    IDLE,
    STARTED,
    PAUSED
  }
}