package de.ph1b.audiobook.playback

import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import d
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.Chapter
import de.ph1b.audiobook.features.bookPlaying.Equalizer
import de.ph1b.audiobook.misc.toUri
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.utils.*
import e
import i
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
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
constructor(
  private val player: SimpleExoPlayer,
  private val dataSourceFactory: DataSource.Factory,
  private val playStateManager: PlayStateManager,
  private val equalizer: Equalizer,
  private val wakeLockManager: WakeLockManager,
  private val prefsManager: PrefsManager) {

  private val extractorsFactory = DefaultExtractorsFactory()

  private var book = BehaviorSubject.create<Book>()

  private val errorSubject = PublishSubject.create<Unit>()
  fun onError(): Observable<Unit> = errorSubject
  private val state = BehaviorSubject.createDefault(PlayerState.IDLE)

  init {
    // upon end stop the player
    player.onEnded {
      v { "onEnded. Stopping player" }
      playStateManager.playState = PlayState.STOPPED
      state.onNext(PlayerState.ENDED)
    }

    // upon error stop the player
    player.onError {
      e(it) { "onPlayerError" }
      playStateManager.playState = PlayState.STOPPED
      player.stop()
      player.playWhenReady = false
      errorSubject.onNext(Unit)
    }

    // upon position change update the book
    player.onPositionDiscontinuity {
      i { "onPositionDiscontinuity with currentPos=${player.currentPosition.toInt()}" }
      book.value?.let {
        val index = player.currentWindowIndex
        val updated = it.copy(time = player.currentPosition.toInt(), currentFile = it.chapters[index].file)
        book.onNext(updated)
      }
    }

    // update equalizer with new audio session upon arrival
    player.onAudioSessionId {
      equalizer.update(it)
    }

    // set the wake-lock based on the play state
    state.subscribe { wakeLockManager.stayAwake(it == PlayerState.PLAYING) }

    // when the player is started update the book based on an interval, else don't
    state.switchMap {
      if (it == PlayerState.PLAYING) {
        Observable.interval(200L, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
          .filter { state.value == PlayerState.PLAYING }
          .map { player.currentPosition }
          .distinctUntilChanged { it -> it / 1000 } // let the value only pass the full second changed.
      } else Observable.empty()
    }.subscribe { time ->
      // update the book
      book.value?.let {
        val index = player.currentWindowIndex
        val updated = it.copy(time = time.toInt(), currentFile = it.chapters[index].file)
        book.onNext(updated)
      }
    }
  }

  /** Initializes a new book. After this, a call to play can be made. */
  fun init(book: Book) {
    if (this.book.value != book) {
      i { "init ${book.name}" }

      val source = book.toMediaSource()
      player.playWhenReady = false
      player.prepare(source)
      player.seekTo(book.currentChapterIndex(), book.time.toLong())

      this.book.onNext(book)
      state.onNext(PlayerState.PAUSED)
    }
  }

  fun book(): Book? = book.value

  fun bookObservable(): Observable<Book> = book

  fun setVolume(loud: Boolean) {
    player.volume = if (loud) 1F else 0.1F
  }

  /** Plays the prepared file. */
  fun play() {
    i { "play" }

    book.value?.let {
      val state = this.state.value

      if (state == PlayerState.ENDED) {
        i { "play in state ended. Back to the beginning" }
        changePosition(0, it.chapters.first().file)
      }

      if (state == PlayerState.ENDED || state == PlayerState.PAUSED) {
        player.playWhenReady = true
        this.state.onNext(PlayerState.PLAYING)
        playStateManager.playState = PlayState.PLAYING
      } else d { "ignore play in state $state" }
    }
  }

  /** Skips by the amount, specified in the settings */
  fun skip(direction: Direction) {
    v { "skip $direction" }

    if (state.value == PlayerState.IDLE) return

    book.value?.let {
      val currentPos = player.currentPosition
      val duration = player.duration
      val delta = prefsManager.seekTime.get()!! * 1000

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

  /** If current time is > 2000ms, seek to 0. Else play previous chapter if there is one */
  fun previous(toNullOfNewTrack: Boolean) {
    v { "previous toNullOfTrack $toNullOfNewTrack" }
    if (state.value == PlayerState.IDLE) return

    book.value?.let {
      val previousChapter = it.previousChapter()
      if (player.currentPosition > 2000 || previousChapter == null) {
        changePosition(0, it.currentFile)
      } else {
        if (toNullOfNewTrack) {
          changePosition(0, previousChapter.file)
        } else {
          changePosition(previousChapter.duration - (prefsManager.seekTime.get()!! * 1000), previousChapter.file)
        }
      }
    }
  }

  /** Stops the playback and releases some resources. */
  fun stop() {
    v { "stop" }
    player.playWhenReady = false
    playStateManager.playState = PlayState.STOPPED
  }

  fun audioSessionId() = player.audioSessionId

  /** pause the player. if rewind is true the position gets rewinded */
  fun pause(rewind: Boolean) {
    v { "pause" }
    when (state.value) {
      PlayerState.PLAYING -> {
        book.value?.let {
          player.playWhenReady = false
          if (rewind) {
            val autoRewind = prefsManager.autoRewindAmount.get()!! * 1000
            if (autoRewind != 0) {
              val seekTo = (player.currentPosition - autoRewind)
                .coerceAtLeast(0)
                .toInt()
              changePosition(seekTo, it.currentFile)
            }
          }

          playStateManager.playState = PlayState.PAUSED
          state.onNext(PlayerState.PAUSED)
        }
      }
      else -> e { "pause ignored because of ${state.value}" }
    }
  }

  /** Plays the next chapter. If there is none, don't do anything.  */
  fun next() {
    i { "next" }
    book.value?.nextChapter()?.let {
      changePosition(0, it.file)
    }
  }

  /** Changes the current position in book. */
  fun changePosition(time: Int, file: File) {
    v { "changePosition with time $time and file $file" }
    if (state.value == PlayerState.IDLE) return

    book.value?.let {
      val copy = it.copy(currentFile = file, time = time)
      book.onNext(copy)
      player.seekTo(copy.currentChapterIndex(), time.toLong())
    }
  }

  /** The current playback speed. 1.0 for normal playback, 2.0 for twice the speed, etc.  */
  fun setPlaybackSpeed(speed: Float) {
    book.value?.let {
      val copy = it.copy(playbackSpeed = speed)
      book.onNext(copy)

      player.setPlaybackSpeed(speed)
    }
  }

  /** The direction to skip. */
  enum class Direction {
    FORWARD, BACKWARD
  }

  private fun Chapter.toMediaSource() = ExtractorMediaSource(this.file.toUri(), dataSourceFactory, extractorsFactory, null, null)

  /** convert a book to a media source. If the size is > 1 use a concat media source, else a regular */
  private fun Book.toMediaSource() = if (chapters.size > 1) {
    val allSources = chapters.map {
      it.toMediaSource()
    }
    ConcatenatingMediaSource(*allSources.toTypedArray())
  } else currentChapter().toMediaSource()
}